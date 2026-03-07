package xyz.irondiscipline.model;

import org.bukkit.ChatColor;
import xyz.irondiscipline.api.rank.IRank;
import xyz.irondiscipline.api.rank.RankRegistry;

/**
 * 軍階級enum
 * 重みが低いほど下位階級
 * <p>
 * {@link IRank} インターフェースを実装しているため、API 経由でも使用可能です。
 * 新規コードでは {@link xyz.irondiscipline.api.rank.CoreRanks} の定数または
 * {@link RankRegistry} を使用することを推奨します。
 * </p>
 */
public enum Rank implements IRank {
    
    PRIVATE("PRIVATE", "&7[二等兵]", 10),
    PRIVATE_FIRST_CLASS("PRIVATE_FIRST_CLASS", "&7[一等兵]", 15),
    CORPORAL("CORPORAL", "&7[伍長]", 20),
    SERGEANT("SERGEANT", "&e[軍曹]", 30),
    LIEUTENANT("LIEUTENANT", "&6[少尉]", 40),
    CAPTAIN("CAPTAIN", "&6[大尉]", 50),
    MAJOR("MAJOR", "&c[少佐]", 60),
    COLONEL("COLONEL", "&c[大佐]", 70),
    COMMANDER("COMMANDER", "&4&l[司令官]", 100);

    private final String id;
    private final String displayRaw;
    private final int weight;

    Rank(String id, String displayRaw, int weight) {
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
    public String getDisplay() {
        return ChatColor.translateAlternateColorCodes('&', displayRaw);
    }

    @Override
    public int getWeight() {
        return weight;
    }

    /**
     * この階級が対象より上位かどうか。
     * IRank 型も受け付けるオーバーロードが追加されています。
     */
    public boolean isHigherThan(Rank other) {
        return this.weight > other.weight;
    }

    /**
     * この階級が対象より下位かどうか。
     * IRank 型も受け付けるオーバーロードが追加されています。
     */
    public boolean isLowerThan(Rank other) {
        return this.weight < other.weight;
    }

    // IRank のデフォルトメソッドと同じシグネチャ (IRank引数版)
    @Override
    public boolean isHigherThan(IRank other) {
        return this.weight > other.getWeight();
    }

    @Override
    public boolean isLowerThan(IRank other) {
        return this.weight < other.getWeight();
    }

    /**
     * PTSが必要かどうか (重み25以下)
     */
    public boolean requiresPTS() {
        return this.weight <= 25;
    }

    /**
     * 次の階級を取得 (昇進)
     */
    public Rank getNextRank() {
        Rank[] ranks = values();
        int next = ordinal() + 1;
        return next < ranks.length ? ranks[next] : null;
    }

    /**
     * 前の階級を取得 (降格)
     */
    public Rank getPreviousRank() {
        int prev = ordinal() - 1;
        return prev >= 0 ? values()[prev] : null;
    }

    /**
     * IDから階級を取得
     */
    public static Rank fromId(String id) {
        if (id == null) return PRIVATE;
        for (Rank rank : values()) {
            if (rank.id.equalsIgnoreCase(id)) {
                return rank;
            }
        }
        return PRIVATE;
    }

    /**
     * 重みから階級を取得
     */
    public static Rank fromWeight(int weight) {
        Rank result = PRIVATE;
        for (Rank rank : values()) {
            if (rank.weight <= weight && rank.weight > result.weight) {
                result = rank;
            }
        }
        return result;
    }

    /**
     * IRank から Rank enum への変換。
     * IRank のIDに一致する Rank enum 定数を返します。
     * 一致しない場合は PRIVATE を返します。
     *
     * @param iRank IRank インスタンス
     * @return 対応する Rank enum
     */
    public static Rank fromIRank(IRank iRank) {
        if (iRank == null) return PRIVATE;
        if (iRank instanceof Rank) return (Rank) iRank;
        return fromId(iRank.getId());
    }

    @Override
    public String getNamespace() {
        return "core";
    }
}
