package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.irondiscipline.model.JailRecord;
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
        plugin.getStorageManager().getJailRecordAsync(targetId).thenAccept(record -> {
            plugin.getTaskScheduler().runEntity(target, () -> {
                if (record != null) {
                    if (record.getInventoryBackup() != null) {
                        ItemStack[] items = InventoryUtil.fromBase64(record.getInventoryBackup());
                        if (items != null) {
                            target.getInventory().setContents(items);
                        }
                    }
                    if (record.getArmorBackup() != null) {
                        ItemStack[] armor = InventoryUtil.fromBase64(record.getArmorBackup());
                        if (armor != null) {
                            target.getInventory().setArmorContents(armor);
                        }
                    }
                }

                // DB削除 (非同期)
                // 注意: Restoreが完了してからDeleteする
                plugin.getStorageManager().removeJailedPlayerAsync(targetId).thenRun(() -> {
                    plugin.getTaskScheduler().runEntity(target, () -> {
                        // 通知 (DB削除完了後)
                        target.sendMessage(plugin.getConfigManager().getMessage("jail_you_released"));
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
     * PreLogin時にDBから状態を同期
     * AsyncPlayerPreLoginEventで呼び出されることを想定
     */
    public void loadJailStatusSync(UUID playerId) {
        try {
            boolean isJailed = plugin.getStorageManager().isJailedAsync(playerId).join();
            if (isJailed) {
                knownJailedIds.add(playerId);
            } else {
                knownJailedIds.remove(playerId);
                jailedPlayers.remove(playerId);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Jail status load failed for " + playerId);
            e.printStackTrace();
        }
    }

    /**
     * ログイン時の隔離チェックと復元
     */
    public void onPlayerJoin(Player player) {
        // PreLoginでロードされたキャッシュを使用して隔離処理を行う
        if (knownJailedIds.contains(player.getUniqueId())) {
            handleJailJoin(player);
        }
    }

    private void handleJailJoin(Player player) {
        UUID playerId = player.getUniqueId();
        
        // 隔離場所チェック
        Location jailLocation = plugin.getConfigManager().getJailLocation();
        if (jailLocation == null) {
            plugin.getLogger().warning("Jail location not set, cannot jail player " + player.getName());
            knownJailedIds.remove(playerId);
            jailedPlayers.remove(playerId);
            return;
        }
        
        // 元の場所とインベントリをキャプチャ (バックアップ作成用)
        Location initialLocation = player.getLocation();
        ItemStack[] initialContents = cloneItems(player.getInventory().getContents());
        ItemStack[] initialArmor = cloneItems(player.getInventory().getArmorContents());

        // 即座に隔離場所へ飛ばす
        player.teleport(jailLocation);
        player.setGameMode(GameMode.ADVENTURE);

        // アイテム使用防止 & レースコンディション対策 (即時クリア)
        player.closeInventory();
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);

        // 非同期処理
        plugin.getStorageManager().getJailRecordAsync(playerId).thenAccept(record -> {
             plugin.getTaskScheduler().runEntity(player, () -> {
                 if (!player.isOnline()) return;

                 if (record == null) {
                     // 不整合時の解放 (バックアップ変数から復元)
                     plugin.getLogger().warning("Jail record missing for " + player.getName() + " but flagged jailed. Releasing.");
                     knownJailedIds.remove(playerId);
                     jailedPlayers.remove(playerId);

                     // インベントリ復元
                     player.getInventory().setContents(initialContents);
                     player.getInventory().setArmorContents(initialArmor);

                     player.teleport(initialLocation);
                     player.setGameMode(GameMode.SURVIVAL);
                     return;
                 }

                 // バックアップがない場合（オフライン処罰、または初回Jail Join）
                 if (record.getInventoryBackup() == null) {
                     // バックアップ作成 (DBへ保存)
                     String newInvBackup = InventoryUtil.toBase64(initialContents);
                     String newArmorBackup = InventoryUtil.toBase64(initialArmor);
                     String locString = serializeLocation(initialLocation);

                     // 既存情報を維持しつつ更新
                     plugin.getStorageManager().saveJailedPlayerAsync(
                         playerId, player.getName(),
                         record.getReason(),
                         record.getJailedBy(),
                         locString,
                         newInvBackup, newArmorBackup
                     ).thenAccept(success -> {
                         plugin.getTaskScheduler().runEntity(player, () -> {
                             if (success) {
                                 // インベントリは既にクリア済み
                                 updateJailDataCache(record, locString);
                             } else {
                                 player.kickPlayer("Critical Error: Failed to save inventory backup.");
                             }
                         });
                     });
                 } else {
                     // バックアップがある場合 -> 既にクリア済みなのでキャッシュ更新のみ
                     updateJailDataCache(record, record.getOriginalLocation());
                 }

                 // 通知
                 String reason = record.getReason() != null ? record.getReason() : "Unknown";
                 player.sendMessage(plugin.getConfigManager().getMessage("jail_you_jailed",
                        "%reason%", reason));
             });
        });
    }

    private void updateJailDataCache(JailRecord record, String location) {
         // 上書き更新して、オフライン処罰時などのLocation未設定状態を解消する
         jailedPlayers.put(record.getPlayerId(), new JailData(
             record.getPlayerId(), record.getPlayerName(), record.getReason(),
             record.getJailedAt(), record.getJailedBy(), location
         ));
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
        final UUID playerId;
        final String playerName;
        final String reason;
        final long jailedAt;
        final UUID jailedBy;
        final String originalLocation;

        JailData(UUID playerId, String playerName, String reason,
                long jailedAt, UUID jailedBy, String originalLocation) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.reason = reason;
            this.jailedAt = jailedAt;
            this.jailedBy = jailedBy;
            this.originalLocation = originalLocation;
        }
    }
}
