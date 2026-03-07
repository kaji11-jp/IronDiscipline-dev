package xyz.irondiscipline.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import xyz.irondiscipline.api.model.KillLog;

/**
 * PvP キルが発生した際に発火するイベント。
 * <p>
 * {@link KillLog} が DB 保存された後に発火します。
 * Wars アドオンはこのイベントをリスンしてキルスコアの集計等を行えます。
 * </p>
 */
public class PlayerKillEvent extends Event {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player killer;
    private final Player victim;
    private final KillLog killLog;

    /**
     * @param killer キルしたプレイヤー
     * @param victim 死亡したプレイヤー
     * @param killLog 保存されたキルログ
     */
    public PlayerKillEvent(Player killer, Player victim, KillLog killLog) {
        super(true); // async = true
        this.killer = killer;
        this.victim = victim;
        this.killLog = killLog;
    }

    /** キルしたプレイヤー */
    public Player getKiller() {
        return killer;
    }

    /** 死亡したプレイヤー */
    public Player getVictim() {
        return victim;
    }

    /** 保存されたキルログ */
    public KillLog getKillLog() {
        return killLog;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
