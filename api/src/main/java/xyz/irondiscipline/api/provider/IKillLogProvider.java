package xyz.irondiscipline.api.provider;

import xyz.irondiscipline.api.model.KillLog;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * キルログプロバイダ。
 * <p>
 * 戦闘ログの保存・取得を提供します。
 * Core の {@code StorageManager} がこのインターフェースを実装します。
 * Wars アドオンはこのプロバイダを使用してキルスコアを集計できます。
 * </p>
 */
public interface IKillLogProvider {

    /**
     * キルログを非同期で保存します。
     *
     * @param log 保存するキルログ
     * @return 完了の CompletableFuture
     */
    CompletableFuture<Void> saveKillLogAsync(KillLog log);

    /**
     * 指定プレイヤーのキルログを非同期で取得します。
     *
     * @param playerId プレイヤー UUID（killer または victim として）
     * @param limit 最大取得件数
     * @return キルログリストの CompletableFuture
     */
    CompletableFuture<List<KillLog>> getKillLogsAsync(UUID playerId, int limit);

    /**
     * 全てのキルログを非同期で取得します。
     *
     * @param limit 最大取得件数
     * @return キルログリストの CompletableFuture
     */
    CompletableFuture<List<KillLog>> getAllKillLogsAsync(int limit);
}
