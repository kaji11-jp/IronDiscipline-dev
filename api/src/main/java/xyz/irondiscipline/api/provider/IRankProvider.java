package xyz.irondiscipline.api.provider;

import org.bukkit.entity.Player;
import xyz.irondiscipline.api.rank.IRank;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 階級管理プロバイダ。
 * <p>
 * プレイヤーの階級取得・設定・昇降格を提供します。
 * Core の {@code RankManager} がこのインターフェースを実装します。
 * </p>
 *
 * <h3>アドオンでの取得方法</h3>
 * <pre>{@code
 * IRankProvider ranks = getServer().getServicesManager().load(IRankProvider.class);
 * IRank rank = ranks.getRank(player);
 * }</pre>
 */
public interface IRankProvider {

    /**
     * オンラインプレイヤーの現在階級を同期的に取得します。
     * キャッシュから取得するため、メインスレッドから安全に呼び出せます。
     * キャッシュミス時は {@code CoreRanks.PRIVATE} を返します。
     *
     * @param player オンラインプレイヤー
     * @return 現在の階級（非null）
     */
    IRank getRank(Player player);

    /**
     * UUID で階級を非同期取得します（オフラインプレイヤー対応）。
     *
     * @param playerId プレイヤー UUID
     * @return 階級の CompletableFuture
     */
    CompletableFuture<IRank> getRankAsync(UUID playerId);

    /**
     * プレイヤーの階級を設定します。
     * Tab/ネームタグの更新と本人への通知も行います。
     *
     * @param player 対象プレイヤー
     * @param newRank 新しい階級
     * @return 成功した場合 true の CompletableFuture
     */
    CompletableFuture<Boolean> setRank(Player player, IRank newRank);

    /**
     * UUID で階級を設定します（オフラインプレイヤー対応）。
     *
     * @param playerId プレイヤー UUID
     * @param playerName プレイヤー名
     * @param newRank 新しい階級
     * @return 成功した場合 true の CompletableFuture
     */
    CompletableFuture<Boolean> setRankByUUID(UUID playerId, String playerName, IRank newRank);

    /**
     * プレイヤーを昇進させます。
     *
     * @param player 対象プレイヤー
     * @return 昇進後の階級（最高階級の場合は null）の CompletableFuture
     */
    CompletableFuture<IRank> promote(Player player);

    /**
     * プレイヤーを降格させます。
     *
     * @param player 対象プレイヤー
     * @return 降格後の階級（最低階級の場合は null）の CompletableFuture
     */
    CompletableFuture<IRank> demote(Player player);

    /**
     * プレイヤーが PTS (発言許可) を必要とするかどうかを判定します。
     *
     * @param player 対象プレイヤー
     * @return PTS が必要な場合 true
     */
    boolean requiresPTS(Player player);

    /**
     * officer が target より上位階級かどうかを判定します。
     *
     * @param officer 上位候補プレイヤー
     * @param target 比較対象プレイヤー
     * @return officer の方が上位の場合 true
     */
    boolean isHigherRank(Player officer, Player target);
}
