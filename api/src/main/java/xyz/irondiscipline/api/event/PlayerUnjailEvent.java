package xyz.irondiscipline.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * プレイヤーが釈放された際に発火するイベント。
 * <p>
 * {@link Cancellable} を実装しているため、アドオンプラグインは
 * {@code setCancelled(true)} を呼ぶことで釈放をキャンセルできます。
 * </p>
 */
public class PlayerUnjailEvent extends Event implements Cancellable {

    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private boolean cancelled = false;

    /**
     * @param player 釈放されるプレイヤー
     */
    public PlayerUnjailEvent(Player player) {
        super(false);
        this.player = player;
    }

    /** 釈放されるプレイヤー */
    public Player getPlayer() {
        return player;
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
