package xyz.irondiscipline.api.rank;

import org.bukkit.ChatColor;

/**
 * Core 軍階級の組み込み実装。
 * <p>
 * 既存の {@code Rank} enum と同じ9段階を {@link IRank} インターフェースで提供します。
 * アドオンプラグインは {@code CoreRanks.PRIVATE} のように定数参照できます。
 * </p>
 *
 * <p>このクラスのインスタンスは不変であり、{@link RankRegistry} 起動時に自動登録されます。</p>
 */
public final class CoreRanks {

    private CoreRanks() {
        // インスタンス化を防止
    }

    public static final IRank PRIVATE = new CoreRank("PRIVATE", "&7[二等兵]", 10);
    public static final IRank PRIVATE_FIRST_CLASS = new CoreRank("PRIVATE_FIRST_CLASS", "&7[一等兵]", 15);
    public static final IRank CORPORAL = new CoreRank("CORPORAL", "&7[伍長]", 20);
    public static final IRank SERGEANT = new CoreRank("SERGEANT", "&e[軍曹]", 30);
    public static final IRank LIEUTENANT = new CoreRank("LIEUTENANT", "&6[少尉]", 40);
    public static final IRank CAPTAIN = new CoreRank("CAPTAIN", "&6[大尉]", 50);
    public static final IRank MAJOR = new CoreRank("MAJOR", "&c[少佐]", 60);
    public static final IRank COLONEL = new CoreRank("COLONEL", "&c[大佐]", 70);
    public static final IRank COMMANDER = new CoreRank("COMMANDER", "&4&l[司令官]", 100);

    /**
     * Core 階級の全定数を配列で返します（weight 昇順）。
     */
    public static IRank[] values() {
        return new IRank[]{
                PRIVATE, PRIVATE_FIRST_CLASS, CORPORAL, SERGEANT,
                LIEUTENANT, CAPTAIN, MAJOR, COLONEL, COMMANDER
        };
    }

    /**
     * Core 階級の組み込み実装 (不変レコード的クラス)
     */
    static final class CoreRank implements IRank {
        private final String id;
        private final String displayRaw;
        private final int weight;

        CoreRank(String id, String displayRaw, int weight) {
            this.id = id;
            this.displayRaw = displayRaw;
            this.weight = weight;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getDisplayRaw() {
            return displayRaw;
        }

        @Override
        public int getWeight() {
            return weight;
        }

        @Override
        public String getNamespace() {
            return "core";
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof IRank other)) return false;
            return id.equals(other.getId());
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        @Override
        public String toString() {
            return "CoreRank{" + id + ", weight=" + weight + "}";
        }
    }
}
