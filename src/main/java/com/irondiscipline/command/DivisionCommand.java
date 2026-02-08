package com.irondiscipline.command;

import com.irondiscipline.IronDiscipline;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * /division コマンド
 * 部隊管理
 */
public class DivisionCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;

    public DivisionCommand(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // 階級チェック
        if (!plugin.getRankUtil().canExecuteCommand(sender)) {
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "set" -> handleSet(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "members" -> handleMembers(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_division_set_usage"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found_simple"));
            return;
        }

        String division = args[2].toLowerCase();
        plugin.getDivisionManager().setDivision(target.getUniqueId(), division);
        
        String displayName = plugin.getDivisionManager().getDivisionDisplayName(division);
        sender.sendMessage(plugin.getConfigManager().getMessage("division_set_sender",
            "%player%", target.getName(),
            "%division%", displayName));
        target.sendMessage(plugin.getConfigManager().getMessage("division_set_target",
            "%division%", displayName));
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_division_remove_usage"));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found_simple"));
            return;
        }

        plugin.getDivisionManager().removeDivision(target.getUniqueId());
        sender.sendMessage(plugin.getConfigManager().getMessage("division_remove_sender", "%player%", target.getName()));
        target.sendMessage(plugin.getConfigManager().getMessage("division_remove_target"));
    }

    private void handleList(CommandSender sender) {
        Set<String> divisions = plugin.getDivisionManager().getAllDivisions();
        
        sender.sendMessage(plugin.getConfigManager().getMessage("division_list_header"));
        for (String div : divisions) {
            String display = plugin.getDivisionManager().getDivisionDisplayName(div);
            int count = plugin.getDivisionManager().getDivisionMembers(div).size();
            sender.sendMessage(plugin.getConfigManager().getMessage("division_list_entry",
                "%division%", display,
                "%count%", String.valueOf(count)));
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found_simple"));
                return;
            }
        } else if (sender instanceof Player) {
            target = (Player) sender;
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessage("error_specify_player"));
            return;
        }

        String division = plugin.getDivisionManager().getDivision(target.getUniqueId());
        if (division == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("division_info_none", "%player%", target.getName()));
        } else {
            String display = plugin.getDivisionManager().getDivisionDisplayName(division);
            sender.sendMessage(plugin.getConfigManager().getMessage("division_info_display",
                "%player%", target.getName(),
                "%division%", display));
        }
    }

    private void handleMembers(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_division_members_usage"));
            return;
        }

        String division = args[1].toLowerCase();
        Set<UUID> members = plugin.getDivisionManager().getDivisionMembers(division);
        
        String display = plugin.getDivisionManager().getDivisionDisplayName(division);
        sender.sendMessage(plugin.getConfigManager().getMessage("division_members_header", "%division%", display));
        
        if (members.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("division_members_empty"));
            return;
        }

        for (UUID uuid : members) {
            Player p = Bukkit.getPlayer(uuid);
            String name = p != null ? p.getName() : Bukkit.getOfflinePlayer(uuid).getName();
            String online = p != null && p.isOnline() ? "§a●" : "§c○";
            sender.sendMessage(online + " §f" + name);
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getMessage("division_help_header"));
        sender.sendMessage(plugin.getConfigManager().getMessage("division_help_set"));
        sender.sendMessage(plugin.getConfigManager().getMessage("division_help_remove"));
        sender.sendMessage(plugin.getConfigManager().getMessage("division_help_list"));
        sender.sendMessage(plugin.getConfigManager().getMessage("division_help_info"));
        sender.sendMessage(plugin.getConfigManager().getMessage("division_help_members"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subs = Arrays.asList("set", "remove", "list", "info", "members");
            for (String s : subs) {
                if (s.startsWith(args[0].toLowerCase())) {
                    completions.add(s);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("set") || sub.equals("remove") || sub.equals("info")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            } else if (sub.equals("members")) {
                for (String div : plugin.getDivisionManager().getAllDivisions()) {
                    if (div.startsWith(args[1].toLowerCase())) {
                        completions.add(div);
                    }
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            for (String div : plugin.getDivisionManager().getAllDivisions()) {
                if (div.startsWith(args[2].toLowerCase())) {
                    completions.add(div);
                }
            }
        }

        return completions;
    }
}
