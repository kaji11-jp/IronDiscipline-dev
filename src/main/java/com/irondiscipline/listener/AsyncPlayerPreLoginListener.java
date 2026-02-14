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

        try {
            // 階級データを事前ロード (同期的に待機)
            plugin.getRankManager().loadPlayerCache(playerId);

            // 隔離データを事前ロード (同期的に待機)
            plugin.getJailManager().loadJailStatusSync(playerId);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load player data for " + event.getName() + " (" + playerId + "): " + e.getMessage());
            e.printStackTrace();
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                "Data loading error. Please try again later or contact an administrator.");
        }
    }
}
