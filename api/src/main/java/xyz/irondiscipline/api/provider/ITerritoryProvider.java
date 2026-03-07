package xyz.irondiscipline.api.provider;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 領土管理プロバイダ。
 * <p>
 * チャンク（土地）の購入・所有権管理を提供します。
 * <strong>Phase 2</strong> の IrDi-Territory アドオンがこのインターフェースを実装します。
 * </p>
 *
 * <p>Nations アドオンは所有チャンク数を監視し、
 * 100チャンク超で国家樹立可能にするといった連携が可能です。</p>
 */
public interface ITerritoryProvider {

    /**
     * チャンクの所有者 UUID を取得します。
     *
     * @param chunk 対象チャンク
     * @return 所有者 UUID、未所有の場合は null
     */
    CompletableFuture<UUID> getOwner(Chunk chunk);

    /**
     * プレイヤーがチャンクを請求（購入）します。
     *
     * @param player 請求プレイヤー
     * @param chunk 対象チャンク
     * @return 成功した場合 true の CompletableFuture
     */
    CompletableFuture<Boolean> claim(Player player, Chunk chunk);

    /**
     * チャンクの所有権を放棄します。
     *
     * @param chunk 対象チャンク
     * @return 成功した場合 true の CompletableFuture
     */
    CompletableFuture<Boolean> unclaim(Chunk chunk);

    /**
     * プレイヤーの請求チャンク数を取得します。
     *
     * @param playerId プレイヤー UUID
     * @return チャンク数の CompletableFuture
     */
    CompletableFuture<Integer> getClaimCount(UUID playerId);

    /**
     * プレイヤーが所有する全チャンクの座標を取得します。
     *
     * @param playerId プレイヤー UUID
     * @return チャンクキー のセットの CompletableFuture
     */
    CompletableFuture<Set<Long>> getClaimedChunks(UUID playerId);

    /**
     * 指定チャンクが請求済みかどうかを判定します。
     *
     * @param chunk 対象チャンク
     * @return 請求済みの場合 true の CompletableFuture
     */
    CompletableFuture<Boolean> isClaimed(Chunk chunk);
}
