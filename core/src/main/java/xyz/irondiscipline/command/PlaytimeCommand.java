package xyz.irondiscipline.command;

import xyz.irondiscipline.IronDiscipline;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * /playtime コマンド
 * 勤務時間の表示
 */
public class PlaytimeCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;

    public PlaytimeCommand(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("top")) {
            showTopPlaytime(sender);
            return true;
        }
        
        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found_simple"));
                return true;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessage("error_specify_player"));
            return true;
        }
        
        String totalTime = plugin.getPlaytimeManager().getFormattedPlaytime(target.getUniqueId());
        String todayTime = plugin.getPlaytimeManager().getFormattedTodayPlaytime(target.getUniqueId());
        
        sender.sendMessage(plugin.getConfigManager().getMessage("playtime_header", "%player%", target.getName()));
        sender.sendMessage(plugin.getConfigManager().getMessage("playtime_total", "%time%", totalTime));
        sender.sendMessage(plugin.getConfigManager().getMessage("playtime_today", "%time%", todayTime));
        
        return true;
    }

    private void showTopPlaytime(CommandSender sender) {
        List<Map.Entry<UUID, Long>> top = plugin.getPlaytimeManager().getTopPlaytime(10);
        
        sender.sendMessage(plugin.getConfigManager().getMessage("playtime_ranking_header"));
        
        int rank = 1;
        for (Map.Entry<UUID, Long> entry : top) {
            String name = Bukkit.getOfflinePlayer(entry.getKey()).getName();
            if (name == null) name = entry.getKey().toString().substring(0, 8);
            
            String time = plugin.getPlaytimeManager().getFormattedPlaytime(entry.getKey());
            
            String prefix = switch (rank) {
                case 1 -> plugin.getConfigManager().getRawMessage("playtime_rank_1");
                case 2 -> plugin.getConfigManager().getRawMessage("playtime_rank_2");
                case 3 -> plugin.getConfigManager().getRawMessage("playtime_rank_3");
                default -> plugin.getConfigManager().getRawMessage("playtime_rank_other").replace("%rank%", String.valueOf(rank));
            };
            
            sender.sendMessage(prefix + " §f" + name + " §7- §f" + time);
            rank++;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            completions.add("top");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
        }
        
        return completions;
    }
}
