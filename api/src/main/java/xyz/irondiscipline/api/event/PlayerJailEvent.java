package xyz.irondiscipline.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.UUID;

/**
 * プレイヤーが隔離された際に発火するイベント。
 * <p>
 * {@link Cancellable} を実装しているため、アドオンプラグインは
 * {@code setCancelled(true)} を呼ぶことで隔離をキャンセルできます。
 * </p>
 */
public class PlayerJailEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final String reason;
    private final UUID jailedBy;
    private boolean cancelled = false;

    /**
     * @param player 隔離されるプレイヤー
     * @param reason 隔離理由
     * @param jailedBy 隔離実施者の UUID（システムの場合は null）
     */
    public PlayerJailEvent(Player player, String reason, UUID jailedBy) {
        super(false);
        this.player = player;
        this.reason = reason;
        this.jailedBy = jailedBy;
    }

    /** 隔離されるプレイヤー */
    public Player getPlayer() {
        return player;
    }

    /** 隔離理由 */
    public String getReason() {
        return reason;
    }

    /** 隔離実施者の UUID（システムの場合は null） */
    public UUID getJailedBy() {
        return jailedBy;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }
}
