package xyz.irondiscipline.api.rank;

import org.bukkit.ChatColor;

/**
 * 階級を表すインターフェース。
 * <p>
 * Core の軍階級 ({@link CoreRanks}) はこのインターフェースの組み込み実装です。
 * アドオンプラグイン（Nations 等）は独自の {@code IRank} 実装を作成し、
 * {@link RankRegistry} に登録することでカスタム階級を追加できます。
 * </p>
 *
 * <p>階級は {@code weight} で比較されます。weight が大きいほど上位階級です。</p>
 */
public interface IRank {

    /**
     * 階級の一意識別子（例: "PRIVATE", "COMMANDER"）
     * 大文字スネークケース推奨。DB保存やコマンド引数に使用されます。
     *
     * @return 階級ID（非null）
     */
    String getId();

    /**
     * 色コード付きの表示名（未変換）。
     * {@code &} プレフィックスの色コードを含みます（例: "&7[二等兵]"）。
     *
     * @return 生の表示名（非null）
     */
    String getDisplayRaw();

    /**
     * 色コード変換済みの表示名。
     * デフォルト実装は {@link ChatColor#translateAlternateColorCodes(char, String)} を使用します。
     *
     * @return 変換済み表示名（非null）
     */
    default String getDisplay() {
        return ChatColor.translateAlternateColorCodes('&', getDisplayRaw());
    }

    /**
     * 階級の重み。大きいほど上位。
     * Core 階級は 10（二等兵）〜 100（司令官）の範囲を使用します。
     * アドオン階級は 100 超の値を使用できます。
     *
     * @return 階級の重み
     */
    int getWeight();

    /**
     * この階級が対象より上位かどうかを判定します。
     *
     * @param other 比較対象の階級
     * @return この階級の weight が other より大きい場合 true
     */
    default boolean isHigherThan(IRank other) {
        return this.getWeight() > other.getWeight();
    }

    /**
     * この階級が対象より下位かどうかを判定します。
     *
     * @param other 比較対象の階級
     * @return この階級の weight が other より小さい場合 true
     */
    default boolean isLowerThan(IRank other) {
        return this.getWeight() < other.getWeight();
    }

    /**
     * この階級の所属名前空間を返します。
     * Core 階級は "core"、アドオン階級はプラグイン名等を返します。
     * 名前空間は {@link RankRegistry} 内でのグループ化に使用されます。
     *
     * @return 名前空間（非null）
     */
    default String getNamespace() {
        return "core";
    }
}
