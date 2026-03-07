package xyz.irondiscipline.manager;

import xyz.irondiscipline.IronDiscipline;
import xyz.irondiscipline.api.event.RankChangeEvent;
import xyz.irondiscipline.api.provider.IRankProvider;
import xyz.irondiscipline.api.rank.IRank;
import xyz.irondiscipline.model.Rank;
import xyz.irondiscipline.util.TabNametagUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 階級マネージャー
 * RankStorageManagerを使用した階級管理（LuckPerms非依存）
 * <p>{@link IRankProvider} を実装し、アドオンプラグインに階級管理機能を提供します。</p>
 */
public class RankManager implements IRankProvider {

    private final IronDiscipline plugin;
    private final RankStorageManager rankStorage;

    // インメモリキャッシュ（高速アクセス用）
    private final Map<UUID, Rank> rankCache = new ConcurrentHashMap<>();

    public RankManager(IronDiscipline plugin, RankStorageManager rankStorage) {
        this.plugin = plugin;
        this.rankStorage = rankStorage;
    }

    /**
     * プレイヤーの現在階級を取得（キャッシュ優先・同期）
     */
    public Rank getRank(Player player) {
        // キャッシュにあればそれを返す
        Rank rank = rankCache.get(player.getUniqueId());
        if (rank != null) {
            return rank;
        }

        // キャッシュミス時のフォールバック (非同期ロード)
        plugin.getLogger().warning("Rank cache miss for online player: " + player.getName());

        // メインスレッドをブロックせず、非同期で読み込みを開始する
        getRankAsync(player.getUniqueId());

        // 読み込み完了まではデフォルト階級を返す
        return Rank.PRIVATE;
    }

    /**
     * UUIDで階級取得（オフラインプレイヤー対応）
     */
    @Override
    public CompletableFuture<IRank> getRankAsync(UUID playerId) {
        if (rankCache.containsKey(playerId)) {
            return CompletableFuture.completedFuture(rankCache.get(playerId));
        }

        return rankStorage.getRank(playerId).thenApply(rank -> {
            rankCache.put(playerId, rank);
            return rank;
        });
    }

    /**
     * プレイヤーの階級を設定
     */
    public CompletableFuture<Boolean> setRank(Player player, Rank newRank) {
        return setRank(player, (IRank) newRank);
    }

    /**
     * プレイヤーの階級を設定 (IRank版 - API実装)
     */
    @Override
    public CompletableFuture<Boolean> setRank(Player player, IRank newRank) {
        Rank oldRank = getRank(player);
        Rank enumRank = Rank.fromIRank(newRank);
        return setRankByUUID(player.getUniqueId(), player.getName(), newRank).thenApply(success -> {
            if (success) {
                // キャッシュ更新
                rankCache.put(player.getUniqueId(), enumRank);

                // RankChangeEvent 発火
                Bukkit.getPluginManager().callEvent(
                        new RankChangeEvent(player, oldRank, newRank, RankChangeEvent.Cause.API));

                // Tab/ネームタグ即時更新
                plugin.getTaskScheduler().runEntity(player, () -> {
                    if (!player.isOnline())
                        return;

                    String divisionDisplay = plugin.getDivisionManager().getDivisionDisplay(player.getUniqueId());
                    TabNametagUtil.updatePlayer(player, enumRank, divisionDisplay);

                    // 本人に通知
                    player.sendMessage(plugin.getConfigManager().getMessage("rank_changed_self",
                            "%rank%", newRank.getDisplay()));
                });
            }
            return success;
        });
    }

    /**
     * UUIDで階級設定 (Rank enum版)
     */
    public CompletableFuture<Boolean> setRankByUUID(UUID playerId, String playerName, Rank newRank) {
        return setRankByUUID(playerId, playerName, (IRank) newRank);
    }

    /**
     * UUIDで階級設定 (IRank版 - API実装)
     */
    @Override
    public CompletableFuture<Boolean> setRankByUUID(UUID playerId, String playerName, IRank newRank) {
        Rank enumRank = Rank.fromIRank(newRank);
        return rankStorage.setRank(playerId, playerName, enumRank).thenApply(success -> {
            if (success) {
                rankCache.put(playerId, enumRank);
            }
            return success;
        });
    }

    /**
     * 昇進
     */
    @Override
    public CompletableFuture<IRank> promote(Player player) {
        Rank current = getRank(player);
        Rank next = current.getNextRank();

        if (next == null) {
            return CompletableFuture.completedFuture(null); // 最高階級
        }

        return setRank(player, (IRank) next).thenApply(success -> {
            if (success) {
                Bukkit.getPluginManager().callEvent(
                        new RankChangeEvent(player, current, next, RankChangeEvent.Cause.PROMOTE));
            }
            return success ? next : null;
        });
    }

    /**
     * 降格
     */
    @Override
    public CompletableFuture<IRank> demote(Player player) {
        Rank current = getRank(player);
        Rank prev = current.getPreviousRank();

        if (prev == null) {
            return CompletableFuture.completedFuture(null); // 最低階級
        }

        return setRank(player, (IRank) prev).thenApply(success -> {
            if (success) {
                Bukkit.getPluginManager().callEvent(
                        new RankChangeEvent(player, current, prev, RankChangeEvent.Cause.DEMOTE));
            }
            return success ? prev : null;
        });
    }

    /**
     * PTSが必要かどうか
     */
    @Override
    public boolean requiresPTS(Player player) {
        int threshold = plugin.getConfigManager().getPTSRequireBelowWeight();
        return !getRank(player).isHigherThan(Rank.fromWeight(threshold));
    }

    /**
     * 対象より上位階級かどうか
     */
    @Override
    public boolean isHigherRank(Player officer, Player target) {
        return getRank(officer).isHigherThan(getRank(target));
    }

    /**
     * キャッシュを無効化
     */
    public void invalidateCache(UUID playerId) {
        rankCache.remove(playerId);
        // Storageのキャッシュ無効化は不要 (キャッシュ削除済み)

        // オンラインならTab更新
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            plugin.getTaskScheduler().runEntity(player, () -> {
                // 再ロード
                getRankAsync(playerId).thenAccept(rank -> {
                    plugin.getTaskScheduler().runEntity(player, () -> {
                        String divisionDisplay = plugin.getDivisionManager().getDivisionDisplay(playerId);
                        TabNametagUtil.updatePlayer(player, rank, divisionDisplay);
                    });
                });
            });
        }
    }

    /**
     * プレイヤー参加時のキャッシュ読み込み (非同期・PreLogin推奨)
     */
    public void loadPlayerCache(UUID playerId) {
        // 同期的に待機してキャッシュを確実にする (AsyncPlayerPreLoginEvent用)
        try {
            getRankAsync(playerId).get(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            plugin.getLogger().warning("Rank load timed out for " + playerId);
            throw new RuntimeException("Rank loading timed out", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load rank", e);
        }
    }

    /**
     * プレイヤー参加時のキャッシュ読み込み (互換用)
     */
    public void loadPlayerCache(Player player) {
        UUID playerId = player.getUniqueId();
        getRankAsync(playerId).thenAccept(rank -> {
            plugin.getTaskScheduler().runEntity(player, () -> {
                if (!player.isOnline()) {
                    return;
                }
                String divisionDisplay = plugin.getDivisionManager().getDivisionDisplay(playerId);
                TabNametagUtil.updatePlayer(player, rank, divisionDisplay);
            });
        }).exceptionally(ex -> {
            plugin.getLogger().warning("Failed to load rank cache for " + player.getName() + ": " + ex.getMessage());
            return null;
        });
    }

    /**
     * プレイヤー退出時のキャッシュクリア
     */
    public void unloadPlayerCache(UUID playerId) {
        rankCache.remove(playerId);
    }
}
