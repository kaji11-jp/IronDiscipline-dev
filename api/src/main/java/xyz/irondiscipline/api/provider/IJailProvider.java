package xyz.irondiscipline.api.provider;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 隔離（Jail）管理プロバイダ。
 * <p>
 * プレイヤーの隔離・釈放とステータス確認を提供します。
 * Core の {@code JailManager} がこのインターフェースを実装します。
 * </p>
 */
public interface IJailProvider {

    /**
     * プレイヤーが隔離中かどうかを判定します。
     *
     * @param playerId プレイヤー UUID
     * @return 隔離中の場合 true
     */
    boolean isJailed(UUID playerId);

    /**
     * プレイヤーが隔離中かどうかを非同期で判定します。
     *
     * @param playerId プレイヤー UUID
     * @return 判定結果の CompletableFuture
     */
    CompletableFuture<Boolean> isJailedAsync(UUID playerId);

    /**
     * プレイヤーを隔離します。
     *
     * @param target 隔離対象
     * @param jailer 隔離実施者（システムの場合は null）
     * @param reason 理由
     * @return 成功した場合 true
     */
    boolean jail(Player target, Player jailer, String reason);

    /**
     * プレイヤーを釈放します。
     *
     * @param target 釈放対象
     * @return 成功した場合 true
     */
    boolean unjail(Player target);
}
