package xyz.irondiscipline.api.provider;

import java.util.Set;
import java.util.UUID;

/**
 * 部隊管理プロバイダ。
 * <p>
 * プレイヤーの部隊所属の取得・設定を提供します。
 * Core の {@code DivisionManager} がこのインターフェースを実装します。
 * </p>
 */
public interface IDivisionProvider {

    /**
     * プレイヤーの所属部隊IDを取得します。
     *
     * @param playerId プレイヤー UUID
     * @return 部隊ID、未所属の場合は null
     */
    String getDivision(UUID playerId);

    /**
     * プレイヤーの部隊表示名を取得します。
     *
     * @param playerId プレイヤー UUID
     * @return 部隊表示名、未所属の場合は空文字列
     */
    String getDivisionDisplay(UUID playerId);

    /**
     * プレイヤーの所属部隊を設定します。
     *
     * @param playerId プレイヤー UUID
     * @param division 部隊ID
     */
    void setDivision(UUID playerId, String division);

    /**
     * プレイヤーの部隊所属を解除します。
     *
     * @param playerId プレイヤー UUID
     */
    void removeDivision(UUID playerId);

    /**
     * プレイヤーが MP（憲兵）部隊に所属しているかどうかを判定します。
     *
     * @param playerId プレイヤー UUID
     * @return MP 所属の場合 true
     */
    boolean isMP(UUID playerId);

    /**
     * 部隊が存在するかどうかを判定します。
     *
     * @param division 部隊ID
     * @return 存在する場合 true
     */
    boolean divisionExists(String division);

    /**
     * 全ての部隊IDを取得します。
     *
     * @return 部隊ID のセット
     */
    Set<String> getAllDivisions();

    /**
     * 指定部隊のメンバー UUID を取得します。
     *
     * @param division 部隊ID
     * @return メンバー UUID のセット
     */
    Set<UUID> getDivisionMembers(String division);
}
