package xyz.irondiscipline.util;

import xyz.irondiscipline.IronDiscipline;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * アクションバーユーティリティ
 * プレイヤーへの持続的な情報表示
 */
public class ActionBarUtil {

    private static final Map<UUID, ScheduledTask> activeTasks = new ConcurrentHashMap<>();

    /**
     * アクションバーにメッセージを表示
     */
    public static void send(Player player, String message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
            TextComponent.fromLegacyText(message));
    }

    /**
     * 一定時間アクションバーにメッセージを表示
     */
    public static void sendTimed(Plugin plugin, Player player, String message, int seconds) {
        if (!(plugin instanceof IronDiscipline)) {
            throw new IllegalArgumentException("Plugin must be instance of IronDiscipline");
        }
        IronDiscipline ironDiscipline = (IronDiscipline) plugin;
        UUID playerId = player.getUniqueId();
        
        // 既存タスクをキャンセル
        cancelTask(playerId);
        
        // 定期的に送信（アクションバーは自動で消えるため）
        ScheduledTask task = ironDiscipline.getTaskScheduler().runEntityTimer(player, new Runnable() {
            private int remaining = seconds * 2; // 0.5秒ごとなので2倍
            
            @Override
            public void run() {
                // プレイヤーが無効ならキャンセル
                if (!player.isValid() || !player.isOnline() || remaining <= 0) {
                    cancelTask(playerId);
                    return;
                }
                
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    TextComponent.fromLegacyText(message));
                remaining--;
            }
        }, 0L, 10L); // 0.5秒ごと
        
        activeTasks.put(playerId, task);
    }

    /**
     * アクションバー表示をキャンセル
     */
    public static void cancelTask(UUID playerId) {
        ScheduledTask task = activeTasks.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * 全タスクをキャンセル
     */
    public static void cancelAll() {
        for (ScheduledTask task : activeTasks.values()) {
            task.cancel();
        }
        activeTasks.clear();
    }
}
