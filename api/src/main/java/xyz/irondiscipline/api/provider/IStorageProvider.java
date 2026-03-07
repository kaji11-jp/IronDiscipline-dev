package xyz.irondiscipline.api.provider;

import java.sql.Connection;
import java.util.concurrent.ExecutorService;

/**
 * ストレージプロバイダ。
 * <p>
 * Core の DB 接続と DB スレッドプールを提供します。
 * アドオンプラグインはこのインターフェースを通じて同一データベースに
 * 独自テーブルを作成し、データを管理できます。
 * </p>
 *
 * <h3>アドオンでの使用例</h3>
 * <pre>{@code
 * IStorageProvider storage = getServer().getServicesManager().load(IStorageProvider.class);
 * Connection conn = storage.getConnection();
 * // 独自テーブルを作成
 * try (Statement stmt = conn.createStatement()) {
 *     stmt.execute("CREATE TABLE IF NOT EXISTS economy_accounts (...)");
 * }
 * }</pre>
 *
 * <p><strong>注意</strong>: 全 DB 操作は {@link #getDbExecutor()} が返す
 * ExecutorService 上で実行してください。接続はシングルスレッドで管理されています。</p>
 */
public interface IStorageProvider {

    /**
     * 共有 DB 接続を取得します。
     * <p>
     * この接続は Core と全アドオンで共有されます。
     * 直接 close() しないでください。
     * </p>
     *
     * @return 共有 DB 接続
     */
    Connection getConnection();

    /**
     * DB 操作用のシングルスレッド ExecutorService を取得します。
     * <p>
     * 全 DB 操作はこの ExecutorService 上で実行する必要があります。
     * </p>
     *
     * @return 共有 ExecutorService
     */
    ExecutorService getDbExecutor();

    /**
     * データベースの種類を返します。
     *
     * @return "h2" または "mysql"
     */
    String getDatabaseType();
}
