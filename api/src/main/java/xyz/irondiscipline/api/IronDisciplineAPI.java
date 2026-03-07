package xyz.irondiscipline.api;

import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicesManager;
import xyz.irondiscipline.api.provider.*;

/**
 * IronDiscipline API のエントリポイント。
 * <p>
 * アドオンプラグインはこのクラスを通じて各プロバイダに簡単にアクセスできます。
 * 内部的には Bukkit {@link ServicesManager} を使用しています。
 * </p>
 *
 * <h3>使用例</h3>
 * <pre>{@code
 * IRankProvider ranks = IronDisciplineAPI.getRankProvider();
 * IJailProvider jail = IronDisciplineAPI.getJailProvider();
 * }</pre>
 */
public final class IronDisciplineAPI {

    private IronDisciplineAPI() {
        // インスタンス化を防止
    }

    /**
     * 階級管理プロバイダを取得します。
     *
     * @return IRankProvider、Core が未ロードの場合は null
     */
    public static IRankProvider getRankProvider() {
        return getService(IRankProvider.class);
    }

    /**
     * 隔離管理プロバイダを取得します。
     *
     * @return IJailProvider、Core が未ロードの場合は null
     */
    public static IJailProvider getJailProvider() {
        return getService(IJailProvider.class);
    }

    /**
     * 部隊管理プロバイダを取得します。
     *
     * @return IDivisionProvider、Core が未ロードの場合は null
     */
    public static IDivisionProvider getDivisionProvider() {
        return getService(IDivisionProvider.class);
    }

    /**
     * キルログプロバイダを取得します。
     *
     * @return IKillLogProvider、Core が未ロードの場合は null
     */
    public static IKillLogProvider getKillLogProvider() {
        return getService(IKillLogProvider.class);
    }

    /**
     * ストレージプロバイダを取得します。
     *
     * @return IStorageProvider、Core が未ロードの場合は null
     */
    public static IStorageProvider getStorageProvider() {
        return getService(IStorageProvider.class);
    }

    /**
     * 経済プロバイダを取得します。
     * IrDi-Economy アドオンがロードされている場合のみ非 null を返します。
     *
     * @return IEconomyProvider、Economy アドオンが未ロードの場合は null
     */
    public static IEconomyProvider getEconomyProvider() {
        return getService(IEconomyProvider.class);
    }

    /**
     * 領土管理プロバイダを取得します。
     * IrDi-Territory アドオンがロードされている場合のみ非 null を返します。
     *
     * @return ITerritoryProvider、Territory アドオンが未ロードの場合は null
     */
    public static ITerritoryProvider getTerritoryProvider() {
        return getService(ITerritoryProvider.class);
    }

    private static <T> T getService(Class<T> clazz) {
        return Bukkit.getServicesManager().load(clazz);
    }
}
