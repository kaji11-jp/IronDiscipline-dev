package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.Rank;
import com.irondiscipline.util.TabNametagUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 階級マネージャー
 * RankStorageManagerを使用した階級管理（LuckPerms非依存）
 */
public class RankManager {

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

        // キャッシュにない場合 (通常はあり得ないが、Reload直後など)
        // ここで同期ロードするとメインスレッドブロックのリスクがあるが、
        // 階級なしで進むよりはマシ。
        // ただし頻繁に呼ばれるメソッドなので、ログを出してデフォルトを返すか、
        // あるいは `rankStorage.getRank` を同期呼び出しするか。
        // ここではデフォルトを返しつつ非同期ロードをキックする戦略をとる。

        plugin.getLogger().warning("Rank cache miss for online player: " + player.getName());
        loadPlayerCache(player.getUniqueId()); // 同期ロード (PreLoginと同じ)

        return rankCache.getOrDefault(player.getUniqueId(), Rank.PRIVATE);
    }

    /**
     * UUIDで階級取得（オフラインプレイヤー対応）
     */
    public CompletableFuture<Rank> getRankAsync(UUID playerId) {
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
        return setRankByUUID(player.getUniqueId(), player.getName(), newRank).thenApply(success -> {
            if (success) {
                // キャッシュ更新
                rankCache.put(player.getUniqueId(), newRank);

                // Tab/ネームタグ即時更新
                plugin.getTaskScheduler().runEntity(player, () -> {
                    if (!player.isOnline())
                        return;

                    TabNametagUtil.updatePlayer(player, newRank);

                    // 本人に通知
                    player.sendMessage(plugin.getConfigManager().getMessage("rank_changed_self",
                            "%rank%", newRank.getDisplay()));
                });
            }
            return success;
        });
    }

    /**
     * UUIDで階級設定
     */
    public CompletableFuture<Boolean> setRankByUUID(UUID playerId, String playerName, Rank newRank) {
        return rankStorage.setRank(playerId, playerName, newRank).thenApply(success -> {
            if (success) {
                rankCache.put(playerId, newRank);
            }
            return success;
        });
    }

    /**
     * 昇進
     */
    public CompletableFuture<Rank> promote(Player player) {
        Rank current = getRank(player);
        Rank next = current.getNextRank();

        if (next == null) {
            return CompletableFuture.completedFuture(null); // 最高階級
        }

        return setRank(player, next).thenApply(success -> success ? next : null);
    }

    /**
     * 降格
     */
    public CompletableFuture<Rank> demote(Player player) {
        Rank current = getRank(player);
        Rank prev = current.getPreviousRank();

        if (prev == null) {
            return CompletableFuture.completedFuture(null); // 最低階級
        }

        return setRank(player, prev).thenApply(success -> success ? prev : null);
    }

    /**
     * PTSが必要かどうか
     */
    public boolean requiresPTS(Player player) {
        int threshold = plugin.getConfigManager().getPTSRequireBelowWeight();
        return getRank(player).getWeight() <= threshold;
    }

    /**
     * 対象より上位階級かどうか
     */
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
                        TabNametagUtil.updatePlayer(player, rank);
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
        getRankAsync(playerId).join();
    }

    /**
     * プレイヤー参加時のキャッシュ読み込み (互換用)
     */
    public void loadPlayerCache(Player player) {
        loadPlayerCache(player.getUniqueId());
        // Tab/Nametag更新は別途JoinEventで行う
        plugin.getTaskScheduler().runEntity(player, () -> {
            TabNametagUtil.updatePlayer(player, getRank(player));
        });
    }

    /**
     * プレイヤー退出時のキャッシュクリア
     */
    public void unloadPlayerCache(UUID playerId) {
        rankCache.remove(playerId);
    }
}
