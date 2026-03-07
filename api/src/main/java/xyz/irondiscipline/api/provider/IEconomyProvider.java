package xyz.irondiscipline.api.provider;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 経済プロバイダ。
 * <p>
 * プレイヤーの残高管理を提供します。
 * <strong>Phase 2</strong> の IrDi-Economy アドオンがこのインターフェースを実装します。
 * </p>
 *
 * <h3>アドオンでの取得方法</h3>
 * <pre>{@code
 * IEconomyProvider economy = getServer().getServicesManager().load(IEconomyProvider.class);
 * if (economy != null) {
 *     double balance = economy.getBalance(player.getUniqueId()).join();
 * }
 * }</pre>
 */
public interface IEconomyProvider {

    /**
     * プレイヤーの残高を非同期で取得します。
     *
     * @param playerId プレイヤー UUID
     * @return 残高の CompletableFuture
     */
    CompletableFuture<Double> getBalance(UUID playerId);

    /**
     * プレイヤーの残高から引き出します。
     *
     * @param playerId プレイヤー UUID
     * @param amount 引き出し額 (正数)
     * @return 成功した場合 true の CompletableFuture
     */
    CompletableFuture<Boolean> withdraw(UUID playerId, double amount);

    /**
     * プレイヤーの残高に入金します。
     *
     * @param playerId プレイヤー UUID
     * @param amount 入金額 (正数)
     * @return 成功した場合 true の CompletableFuture
     */
    CompletableFuture<Boolean> deposit(UUID playerId, double amount);

    /**
     * プレイヤー間で送金します。
     *
     * @param fromId 送金元プレイヤー UUID
     * @param toId 送金先プレイヤー UUID
     * @param amount 送金額 (正数)
     * @return 成功した場合 true の CompletableFuture
     */
    CompletableFuture<Boolean> transfer(UUID fromId, UUID toId, double amount);

    /**
     * プレイヤーが指定金額を所持しているかどうか確認します。
     *
     * @param playerId プレイヤー UUID
     * @param amount 確認する金額
     * @return 所持している場合 true の CompletableFuture
     */
    CompletableFuture<Boolean> has(UUID playerId, double amount);
}
