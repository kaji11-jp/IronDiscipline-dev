package xyz.irondiscipline.api.rank;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 階級レジストリ。
 * <p>
 * 全ての {@link IRank} インスタンスを管理する中央レジストリです。
 * Core 起動時にデフォルトの軍階級 ({@link CoreRanks}) が登録され、
 * アドオンプラグインは独自の階級を追加登録できます。
 * </p>
 *
 * <h3>使用例 (アドオン側)</h3>
 * <pre>{@code
 * // Nations プラグインでカスタム階級を登録
 * RankRegistry.register(new NationRank("PRESIDENT", "&6[大統領]", 200, "nations"));
 * RankRegistry.register(new NationRank("CITIZEN", "&a[国民]", 5, "nations"));
 *
 * // 階級を取得
 * IRank rank = RankRegistry.fromId("PRESIDENT");
 * }</pre>
 */
public final class RankRegistry {

    /** ID → IRank のマッピング (case-insensitive lookup は fromId で対応) */
    private static final Map<String, IRank> REGISTRY = new ConcurrentHashMap<>();

    /** weight 昇順のソート済みリスト (変更時に再構築) */
    private static volatile List<IRank> sortedRanks = Collections.emptyList();

    private RankRegistry() {
        // インスタンス化を防止
    }

    /*
     * Static initializer: Core 階級を自動登録
     */
    static {
        for (IRank rank : CoreRanks.values()) {
            registerInternal(rank);
        }
        rebuildSortedList();
    }

    /**
     * 階級を登録します。同一IDが既存の場合は上書きされます。
     *
     * @param rank 登録する階級（非null）
     * @throws IllegalArgumentException rank が null、または ID が空の場合
     */
    public static void register(IRank rank) {
        if (rank == null) throw new IllegalArgumentException("Rank cannot be null");
        if (rank.getId() == null || rank.getId().isEmpty()) {
            throw new IllegalArgumentException("Rank ID cannot be null or empty");
        }
        registerInternal(rank);
        rebuildSortedList();
    }

    /**
     * 複数の階級を一括登録します。
     *
     * @param ranks 登録する階級の配列
     */
    public static void registerAll(IRank... ranks) {
        for (IRank rank : ranks) {
            if (rank == null) continue;
            registerInternal(rank);
        }
        rebuildSortedList();
    }

    /**
     * 指定 ID の階級を登録解除します。
     * Core 階級の登録解除は推奨されません。
     *
     * @param id 登録解除する階級ID
     * @return 登録解除された階級、存在しない場合 null
     */
    public static IRank unregister(String id) {
        IRank removed = REGISTRY.remove(id.toUpperCase());
        if (removed != null) {
            rebuildSortedList();
        }
        return removed;
    }

    /**
     * IDから階級を取得します (case-insensitive)。
     * 見つからない場合は {@link CoreRanks#PRIVATE} を返します。
     *
     * @param id 階級ID
     * @return 対応する階級、未登録の場合は PRIVATE
     */
    public static IRank fromId(String id) {
        if (id == null) return CoreRanks.PRIVATE;
        IRank rank = REGISTRY.get(id.toUpperCase());
        return rank != null ? rank : CoreRanks.PRIVATE;
    }

    /**
     * IDから階級を取得します。見つからない場合は null を返します。
     *
     * @param id 階級ID
     * @return 対応する階級、未登録の場合は null
     */
    public static IRank fromIdOrNull(String id) {
        if (id == null) return null;
        return REGISTRY.get(id.toUpperCase());
    }

    /**
     * 指定 weight 以下で最も weight が大きい階級を返します。
     * 該当がない場合は PRIVATE を返します。
     *
     * @param weight 基準 weight
     * @return 最も近い下位の階級
     */
    public static IRank fromWeight(int weight) {
        IRank result = CoreRanks.PRIVATE;
        for (IRank rank : sortedRanks) {
            if (rank.getWeight() <= weight) {
                result = rank;
            } else {
                break;
            }
        }
        return result;
    }

    /**
     * 指定の階級の次の階級 (weight が1段階上) を返します。
     * 最高階級の場合は null を返します。
     *
     * @param current 現在の階級
     * @return 次の階級、または null
     */
    public static IRank getNextRank(IRank current) {
        List<IRank> sorted = sortedRanks;
        for (int i = 0; i < sorted.size() - 1; i++) {
            if (sorted.get(i).getId().equals(current.getId())) {
                return sorted.get(i + 1);
            }
        }
        return null;
    }

    /**
     * 指定の階級の前の階級 (weight が1段階下) を返します。
     * 最低階級の場合は null を返します。
     *
     * @param current 現在の階級
     * @return 前の階級、または null
     */
    public static IRank getPreviousRank(IRank current) {
        List<IRank> sorted = sortedRanks;
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i).getId().equals(current.getId())) {
                return sorted.get(i - 1);
            }
        }
        return null;
    }

    /**
     * 登録されている全階級を weight 昇順で返します。
     *
     * @return 全階級の不変リスト
     */
    public static List<IRank> values() {
        return sortedRanks;
    }

    /**
     * 指定の名前空間に属する階級のみを weight 昇順で返します。
     *
     * @param namespace 名前空間 (例: "core", "nations")
     * @return 該当階級のリスト
     */
    public static List<IRank> valuesByNamespace(String namespace) {
        List<IRank> result = new ArrayList<>();
        for (IRank rank : sortedRanks) {
            if (rank.getNamespace().equals(namespace)) {
                result.add(rank);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * 指定 ID の階級が登録済みかどうかを返します。
     *
     * @param id 階級ID
     * @return 登録済みなら true
     */
    public static boolean isRegistered(String id) {
        if (id == null) return false;
        return REGISTRY.containsKey(id.toUpperCase());
    }

    /**
     * 登録済み階級の数を返します。
     *
     * @return 登録数
     */
    public static int size() {
        return REGISTRY.size();
    }

    // ===== Internal =====

    private static void registerInternal(IRank rank) {
        REGISTRY.put(rank.getId().toUpperCase(), rank);
    }

    private static void rebuildSortedList() {
        List<IRank> list = new ArrayList<>(REGISTRY.values());
        list.sort(Comparator.comparingInt(IRank::getWeight));
        sortedRanks = Collections.unmodifiableList(list);
    }
}
