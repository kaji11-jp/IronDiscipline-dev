package com.irondiscipline.command;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.KillLog;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * /killlog コマンド
 * 戦闘ログを閲覧
 */
public class KillLogCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm:ss");

    public KillLogCommand(IronDiscipline plugin) {
        this.plugin = plugin;
        plugin.getCommand("killlog").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("iron.killlog.view")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }

        int limit = 10;
        String targetName = null;

        // 引数解析
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            try {
                limit = Integer.parseInt(arg);
            } catch (NumberFormatException e) {
                targetName = arg;
            }
        }

        final int displayLimit = Math.min(limit, 50);
        final String finalTargetName = targetName;

        if (targetName != null) {
            // 特定プレイヤーのログ
            Player target = Bukkit.getPlayer(targetName);
            UUID targetId = target != null ? target.getUniqueId() : null;
            
            if (targetId == null) {
                // オフラインプレイヤーを検索（簡易実装）
                sender.sendMessage(plugin.getConfigManager().getMessage("killlog_searching", "%player%", targetName));
                // 全ログから名前でフィルタ
                plugin.getStorageManager().getAllKillLogsAsync(100).thenAccept(logs -> {
                    plugin.getTaskScheduler().runGlobal(() -> {
                        sender.sendMessage(plugin.getConfigManager().getMessage("killlog_header_player", "%player%", finalTargetName));
                        int count = 0;
                        for (KillLog log : logs) {
                            if (count >= displayLimit) break;
                            if (log.getKillerName() != null && log.getKillerName().equalsIgnoreCase(finalTargetName) ||
                                log.getVictimName().equalsIgnoreCase(finalTargetName)) {
                                sendLogEntry(sender, log);
                                count++;
                            }
                        }
                        if (count == 0) {
                            sender.sendMessage(plugin.getConfigManager().getMessage("killlog_not_found"));
                        }
                    });
                });
            } else {
                plugin.getStorageManager().getKillLogsAsync(targetId, displayLimit).thenAccept(logs -> {
                    plugin.getTaskScheduler().runGlobal(() -> {
                        sender.sendMessage(plugin.getConfigManager().getMessage("killlog_header_player", "%player%", finalTargetName));
                        for (KillLog log : logs) {
                            sendLogEntry(sender, log);
                        }
                        if (logs.isEmpty()) {
                            sender.sendMessage(plugin.getConfigManager().getMessage("killlog_not_found"));
                        }
                    });
                });
            }
        } else {
            // 全ログ
            plugin.getStorageManager().getAllKillLogsAsync(displayLimit).thenAccept(logs -> {
                plugin.getTaskScheduler().runGlobal(() -> {
                    sender.sendMessage(plugin.getConfigManager().getMessage("killlog_header_latest"));
                    for (KillLog log : logs) {
                        sendLogEntry(sender, log);
                    }
                    if (logs.isEmpty()) {
                        sender.sendMessage(plugin.getConfigManager().getMessage("killlog_not_found"));
                    }
                });
            });
        }

        return true;
    }

    /**
     * ログエントリを整形して送信
     */
    private void sendLogEntry(CommandSender sender, KillLog log) {
        String date = DATE_FORMAT.format(new Date(log.getTimestamp()));
        String killer = log.getKillerName() != null ? log.getKillerName() : plugin.getConfigManager().getRawMessage("killlog_environment");
        
        sender.sendMessage(plugin.getConfigManager().getMessage("killlog_entry_format",
            "%date%", date,
            "%killer_color%", ChatColor.RED.toString(),
            "%killer%", killer,
            "%victim%", log.getVictimName(),
            "%weapon%", log.getWeapon(),
            "%distance%", log.getFormattedDistance()
        ));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(player.getName());
                }
            }
            // 件数サジェスト
            completions.add("5");
            completions.add("10");
            completions.add("20");
        }
        return completions;
    }
}
