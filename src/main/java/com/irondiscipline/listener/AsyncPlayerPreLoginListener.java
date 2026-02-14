package com.irondiscipline.listener;

import com.irondiscipline.IronDiscipline;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.UUID;

public class AsyncPlayerPreLoginListener implements Listener {

    private final IronDiscipline plugin;

    public AsyncPlayerPreLoginListener(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        UUID playerId = event.getUniqueId();

        // 階級データを事前ロード (同期的に待機)
        plugin.getRankManager().loadPlayerCache(playerId);

        // 隔離データを事前ロード (同期的に待機)
        plugin.getJailManager().loadJailStatusSync(playerId);
    }
}
