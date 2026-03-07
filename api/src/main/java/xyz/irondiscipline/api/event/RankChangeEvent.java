package xyz.irondiscipline.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import xyz.irondiscipline.api.rank.IRank;

/**
 * プレイヤーの階級が変更された際に発火するイベント。
 * <p>
 * 昇進・降格・手動設定・自動昇進のいずれの場合もこのイベントが発火します。
 * アドオンプラグインはこのイベントをリスンして、階級変動に応じた処理を実行できます。
 * </p>
 *
 * <h3>リスナー例</h3>
 * <pre>{@code
 * @EventHandler
 * public void onRankChange(RankChangeEvent event) {
 *     if (event.getNewRank().getWeight() >= 100) {
 *         // 司令官に昇進！
 *     }
 * }
 * }</pre>
 */
public class RankChangeEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final IRank oldRank;
    private final IRank newRank;
    private final Cause cause;

    /**
     * @param player 階級変更されたプレイヤー
     * @param oldRank 変更前の階級
     * @param newRank 変更後の階級
     * @param cause 変更原因
     */
    public RankChangeEvent(Player player, IRank oldRank, IRank newRank, Cause cause) {
        super(true); // async = true (DB操作後に呼ばれるため)
        this.player = player;
        this.oldRank = oldRank;
        this.newRank = newRank;
        this.cause = cause;
    }

    /** 階級変更されたプレイヤー */
    public Player getPlayer() {
        return player;
    }

    /** 変更前の階級 */
    public IRank getOldRank() {
        return oldRank;
    }

    /** 変更後の階級 */
    public IRank getNewRank() {
        return newRank;
    }

    /** 変更原因 */
    public Cause getCause() {
        return cause;
    }

    /** 昇進かどうか */
    public boolean isPromotion() {
        return newRank.isHigherThan(oldRank);
    }

    /** 降格かどうか */
    public boolean isDemotion() {
        return newRank.isLowerThan(oldRank);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    /**
     * 階級変更の原因
     */
    public enum Cause {
        /** /promote コマンドによる昇進 */
        PROMOTE,
        /** /demote コマンドによる降格 */
        DEMOTE,
        /** 手動設定 (/irondev setrank 等) */
        SET,
        /** AutoPromotionManager による自動昇進 */
        AUTO_PROMOTE,
        /** プラグインAPI経由 */
        API,
        /** その他 */
        OTHER
    }
}
