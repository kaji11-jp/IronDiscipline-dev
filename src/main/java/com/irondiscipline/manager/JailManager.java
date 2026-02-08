package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.irondiscipline.util.InventoryUtil;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 隔離マネージャー
 * プレイヤーの拘留と釈放を管理
 */
public class JailManager {

    private final IronDiscipline plugin;

    // 隔離中プレイヤー (キャッシュ - 詳細データ)
    private final Map<UUID, JailData> jailedPlayers = new ConcurrentHashMap<>();
    
    // 隔離中プレイヤーID (高速チェック用キャッシュ)
    private final Set<UUID> knownJailedIds = ConcurrentHashMap.newKeySet();
    private volatile boolean cacheLoaded = false;

    public JailManager(IronDiscipline plugin) {
        this.plugin = plugin;
        loadJailedPlayers();
    }

    /**
     * プレイヤーを隔離
     */
    public boolean jail(Player target, Player jailer, String reason) {
        Location jailLocation = plugin.getConfigManager().getJailLocation();
        if (jailLocation == null) {
            return false;
        }

        UUID targetId = target.getUniqueId();

        // 既に隔離中なら何もしない
        if (isJailed(target)) {
            return false;
        }

        // 現在位置を保存
        Location originalLocation = target.getLocation();
        String locString = serializeLocation(originalLocation);

        // インベントリのクローンをメインスレッドで作成 (スレッドセーフ)
        ItemStack[] invContents = cloneItems(target.getInventory().getContents());
        ItemStack[] armorContents = cloneItems(target.getInventory().getArmorContents());

        // キャッシュに先行追加 (二重処理防止)
        knownJailedIds.add(targetId);

        // 非同期処理開始
        CompletableFuture.supplyAsync(() -> {
            // インベントリのシリアライズ (重い処理)
            String invBackup = InventoryUtil.toBase64(invContents);
            String armorBackup = InventoryUtil.toBase64(armorContents);
            return new String[]{invBackup, armorBackup};
        }).thenCompose(backups -> {
            // DB保存
            return plugin.getStorageManager().saveJailedPlayerAsync(targetId, target.getName(), reason,
                    jailer != null ? jailer.getUniqueId() : null, locString, backups[0], backups[1]);
        }).thenAccept(success -> {
            if (success) {
                plugin.getTaskScheduler().runEntity(target, () -> {
                    // DB保存成功後にインベントリ操作とテレポート
                    if (!target.isOnline())
                        return;

                    // インベントリクリア
                    target.getInventory().clear();
                    target.getInventory().setArmorContents(new ItemStack[4]);

                    // ゲームモードをアドベンチャーに
                    target.setGameMode(GameMode.ADVENTURE);

                    // 隔離場所へテレポート
                    target.teleport(jailLocation);

                    // データ保存 (キャッシュ)
                    JailData data = new JailData(targetId, target.getName(), reason,
                            System.currentTimeMillis(), jailer != null ? jailer.getUniqueId() : null,
                            locString);
                    jailedPlayers.put(targetId, data);

                    // 通知
                    target.sendMessage(plugin.getConfigManager().getMessage("jail_you_jailed",
                            "%reason%", reason != null ? reason : plugin.getConfigManager().getRawMessage("jail_reason_default")));
                });
            } else {
                plugin.getLogger().warning("隔離処理中断: DB保存に失敗しました - " + target.getName());
                knownJailedIds.remove(targetId); // 失敗時はキャッシュから削除
            }
        });

        return true; // 処理を開始したことを返す
    }

    private ItemStack[] cloneItems(ItemStack[] items) {
        if (items == null) return new ItemStack[0];
        ItemStack[] cloned = new ItemStack[items.length];
        for (int i = 0; i < items.length; i++) {
            if (items[i] != null) {
                cloned[i] = items[i].clone();
            }
        }
        return cloned;
    }

    /**
     * オフラインプレイヤーを隔離 (DBのみ更新)
     */
    public boolean jailOffline(UUID targetId, String targetName, UUID jailerId, String reason) {
        if (isJailed(targetId)) {
            return false;
        }

        // DB保存 (インベントリバックアップはnull = ログイン時にバックアップ)
        plugin.getStorageManager().saveJailedPlayerAsync(targetId, targetName, plugin.getConfigManager().getRawMessage("jail_reason_offline"),
                jailerId, null, null, null);

        // キャッシュ更新
        JailData data = new JailData(targetId, targetName, reason,
                System.currentTimeMillis(), jailerId, null);
        jailedPlayers.put(targetId, data);
        knownJailedIds.add(targetId);

        return true;
    }

    /**
     * プレイヤーを釈放
     */
    public boolean unjail(Player target) {
        UUID targetId = target.getUniqueId();

        if (!isJailed(target)) {
            return false;
        }

        JailData data = jailedPlayers.remove(targetId);
        knownJailedIds.remove(targetId);

        // 元の場所へテレポート
        if (data != null && data.originalLocation != null) {
            Location original = deserializeLocation(data.originalLocation);
            if (original != null) {
                target.teleport(original);
            }
        }

        // ゲームモード復元
        target.setGameMode(GameMode.SURVIVAL);

        // インベントリ復元 (DBから非同期取得)
        plugin.getStorageManager().getInventoryBackupAsync(targetId).thenAccept(invData -> {
            plugin.getStorageManager().getArmorBackupAsync(targetId).thenAccept(armorData -> {
                plugin.getTaskScheduler().runEntity(target, () -> {
                    if (invData != null) {
                        ItemStack[] items = InventoryUtil.fromBase64(invData);
                        if (items != null) {
                            target.getInventory().setContents(items);
                        }
                    }

                    if (armorData != null) {
                        ItemStack[] armor = InventoryUtil.fromBase64(armorData);
                        if (armor != null) {
                            target.getInventory().setArmorContents(armor);
                        }
                    }

                    // DB削除 (非同期)
                    plugin.getStorageManager().removeJailedPlayerAsync(targetId).thenRun(() -> {
                        plugin.getTaskScheduler().runEntity(target, () -> {
                            // 通知 (DB削除完了後)
                            target.sendMessage(plugin.getConfigManager().getMessage("jail_you_released"));
                        });
                    });
                });
            });
        });

        return true;
    }

    /**
     * 隔離中かどうかチェック
     */
    public boolean isJailed(Player player) {
        return knownJailedIds.contains(player.getUniqueId());
    }

    /**
     * UUIDで隔離中かチェック
     */
    public boolean isJailed(UUID playerId) {
        return knownJailedIds.contains(playerId);
    }

    /**
     * ログイン時の隔離チェックと復元
     */
    public void onPlayerJoin(Player player) {
        UUID playerId = player.getUniqueId();

        // キャッシュがロード済みで、かつIDが含まれていなければ即リターン (高速化)
        if (cacheLoaded && !knownJailedIds.contains(playerId)) {
            return;
        }

        // キャッシュ未ロード、または隔離リストに含まれる場合は詳細チェック
        
        // 元のゲームモードを保存 (Race Condition対策で一時的にスペクテイターにするため)
        GameMode originalMode = player.getGameMode();

        // 即座に行動制限 (まだDBチェックが終わっていない場合のため)
        player.setGameMode(GameMode.SPECTATOR);

        // 非同期チェック
        plugin.getStorageManager().isJailedAsync(playerId).thenAccept(isJailed -> {
            if (!isJailed) {
                // 隔離中でなければ元のモードに復元
                plugin.getTaskScheduler().runEntity(player, () -> {
                    if (player.isOnline()) {
                        player.setGameMode(originalMode);
                    }
                });
                // キャッシュとの不整合があれば修正
                knownJailedIds.remove(playerId);
                return;
            }
            
            // 隔離確定 -> キャッシュに追加
            knownJailedIds.add(playerId);

            // DBからバックアップ状況を確認
            plugin.getStorageManager().getInventoryBackupAsync(playerId).thenAccept(invBackup -> {
                // 元座標も非同期で取得
                plugin.getStorageManager().getOriginalLocationAsync(playerId).thenAccept(savedOriginalLoc -> {
                    plugin.getTaskScheduler().runEntity(player, () -> {
                        if (!player.isOnline())
                            return;

                        String finalOriginalLoc = savedOriginalLoc;

                        // バックアップがない場合（オフライン処罰時）は今すぐバックアップ
                        if (invBackup == null) {
                            // インベントリバックアップ
                            String newInvBackup = InventoryUtil.toBase64(player.getInventory().getContents());
                            String newArmorBackup = InventoryUtil.toBase64(player.getInventory().getArmorContents());

                            // 元の場所保存
                            String locString = serializeLocation(player.getLocation());
                            finalOriginalLoc = locString;

                            // DB更新
                            plugin.getStorageManager().saveJailedPlayerAsync(playerId, player.getName(), plugin.getConfigManager().getRawMessage("jail_reason_offline"),
                                    null, locString, newInvBackup, newArmorBackup);

                            // インベントリクリア
                            player.getInventory().clear();
                            player.getInventory().setArmorContents(new ItemStack[4]);
                        }

                        // DBに隔離記録がある場合
                        // キャッシュ復元
                        if (!jailedPlayers.containsKey(playerId)) {
                            jailedPlayers.put(playerId,
                                    new JailData(playerId, player.getName(), plugin.getConfigManager().getRawMessage("jail_reason_reconnect"), System.currentTimeMillis(), null,
                                            finalOriginalLoc));
                        }

                        // 隔離場所にテレポート
                        Location jailLocation = plugin.getConfigManager().getJailLocation();
                        if (jailLocation != null) {
                            player.teleport(jailLocation);
                            player.setGameMode(GameMode.ADVENTURE);
                            player.sendMessage(plugin.getConfigManager().getMessage("jail_you_jailed",
                                    "%reason%", plugin.getConfigManager().getRawMessage("jail_reason_relocated")));
                        }
                    });
                });
            });
        });
    }

    /**
     * プレイヤーが隔離場所から逃げようとした時の処理
     */
    public void preventEscape(Player player) {
        if (!isJailed(player))
            return;

        Location jailLocation = plugin.getConfigManager().getJailLocation();
        if (jailLocation != null) {
            // 隔離場所から離れすぎていたら戻す
            if (player.getLocation().distance(jailLocation) > 10) {
                player.teleport(jailLocation);
            }
        }
    }

    /**
     * 保存済み隔離プレイヤーをロード
     */
    private void loadJailedPlayers() {
        plugin.getStorageManager().getJailedPlayerIdsAsync().thenAccept(ids -> {
            knownJailedIds.addAll(ids);
            cacheLoaded = true;
            plugin.getLogger().info("隔離プレイヤーリストをロードしました: " + ids.size() + "件");
            
            // ロード完了前に参加していたプレイヤーを再チェック
            plugin.getTaskScheduler().runGlobal(() -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (knownJailedIds.contains(p.getUniqueId())) {
                         // 既に処理済みかチェックはonPlayerJoin内で行われるが、
                         // GameModeがSPECTATORのまま放置されている可能性を防ぐため再呼び出しは慎重に。
                         // ここでは「キャッシュに含まれているのに隔離処理されていない」ケースを救済したいが
                         // 二重テレポート等のリスクもあるため、ログ出力にとどめるか、
                         // 安全な再チェックロジックが必要。
                         // 今回はシンプルに、onPlayerJoinは参加時イベントで確実に呼ばれているので
                         // ここでは何もしない（onPlayerJoinがcacheLoaded=falseの時はDB見に行くので安全）。
                    }
                }
            });
        });
    }

    /**
     * 全データ保存
     */
    public void saveAll() {
        // シャットダウン時に呼ばれる
        // インベントリバックアップは消えるがDBに隔離状態は残る
    }

    /**
     * 位置のシリアライズ
     */
    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + ";" +
                loc.getX() + ";" + loc.getY() + ";" + loc.getZ() + ";" +
                loc.getYaw() + ";" + loc.getPitch();
    }

    /**
     * 位置のデシリアライズ
     */
    private Location deserializeLocation(String str) {
        try {
            String[] parts = str.split(";");
            return new Location(
                    Bukkit.getWorld(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Float.parseFloat(parts[4]),
                    Float.parseFloat(parts[5]));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 隔離データ内部クラス
     */
    private static class JailData {
        final String originalLocation;

        JailData(UUID playerId, String playerName, String reason,
                long jailedAt, UUID jailedBy, String originalLocation) {
            this.originalLocation = originalLocation;
        }
    }
}
