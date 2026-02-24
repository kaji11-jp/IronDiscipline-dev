package xyz.irondiscipline.command;

import xyz.irondiscipline.IronDiscipline;
import org.bukkit.Bukkit;
import org.bukkit.BanList;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * /irondiscipline (iron, id) コマンド
 * プラグイン管理 + Adonis風管理コマンド
 */
public class IronDisciplineCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;
    
    // freeze中のプレイヤー
    private final Set<UUID> frozenPlayers = ConcurrentHashMap.newKeySet();

    public IronDisciplineCommand(IronDiscipline plugin) {
        this.plugin = plugin;
        plugin.getCommand("irondiscipline").setTabCompleter(this);
        
        // Freeze中の移動を防ぐ
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {
            @org.bukkit.event.EventHandler
            public void onMove(org.bukkit.event.player.PlayerMoveEvent e) {
                if (frozenPlayers.contains(e.getPlayer().getUniqueId())) {
                    if (e.getFrom().getX() != e.getTo().getX() || 
                        e.getFrom().getZ() != e.getTo().getZ()) {
                        e.setTo(e.getFrom());
                    }
                }
            }
        }, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("iron.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no_permission"));
            return true;
        }

        if (args.length < 1) {
            showHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "reload" -> {
                plugin.reload();
                sender.sendMessage(plugin.getConfigManager().getMessage("config_reloaded"));
            }
            case "version", "ver" -> {
                sender.sendMessage(plugin.getConfigManager().getMessage("command_iron_version", "%version%", plugin.getDescription().getVersion()));
            }
            case "cleanup" -> {
                plugin.getStorageManager().cleanupOldLogs();
                sender.sendMessage(plugin.getConfigManager().getMessage("command_iron_cleanup_started"));
            }
            // Adonis風コマンド
            case "kick" -> handleKick(sender, args);
            case "ban" -> handleBan(sender, args);
            case "tp" -> handleTp(sender, args);
            case "bring" -> handleBring(sender, args);
            case "freeze" -> handleFreeze(sender, args);
            case "unfreeze" -> handleUnfreeze(sender, args);
            case "announce", "ann" -> handleAnnounce(sender, args);
            default -> showHelp(sender);
        }

        return true;
    }

    private void handleKick(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_usage_iron_kick"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found_simple"));
            return;
        }
        
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "規律違反";
        target.kickPlayer(plugin.getConfigManager().getRawMessage("kick_reason_prefix") + reason);
        
        Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("kick_broadcast", "%player%", target.getName(), "%reason%", reason));
        sender.sendMessage(plugin.getConfigManager().getMessage("kick_success", "%player%", target.getName()));
    }

    private void handleBan(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_usage_iron_ban"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found_simple"));
            return;
        }
        
        String reason = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "規律違反";
        
        @SuppressWarnings("deprecation")
        BanList<org.bukkit.BanEntry<String>> banList = (BanList<org.bukkit.BanEntry<String>>) Bukkit.getBanList(BanList.Type.NAME);
        if (banList != null) {
            banList.addBan(target.getName(), reason, null, sender.getName());
        }
        target.kickPlayer(plugin.getConfigManager().getRawMessage("ban_reason_prefix") + reason);
        
        Bukkit.broadcastMessage(plugin.getConfigManager().getMessage("ban_broadcast", "%player%", target.getName(), "%reason%", reason));
        sender.sendMessage(plugin.getConfigManager().getMessage("ban_success", "%player%", target.getName()));
    }

    private void handleTp(CommandSender sender, String[] args) {
        if (!(sender instanceof Player executor)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_player_only"));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_iron_tp_usage"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found_simple"));
            return;
        }
        
        executor.teleport(target.getLocation());
        sender.sendMessage(plugin.getConfigManager().getMessage("iron_tp_success", "%player%", target.getName()));
    }

    private void handleBring(CommandSender sender, String[] args) {
        if (!(sender instanceof Player executor)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_player_only"));
            return;
        }
        
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_iron_bring_usage"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found_simple"));
            return;
        }
        
        target.teleport(executor.getLocation());
        sender.sendMessage(plugin.getConfigManager().getMessage("iron_bring_success_sender", "%player%", target.getName()));
        target.sendMessage(plugin.getConfigManager().getMessage("iron_bring_success_target"));
    }

    private void handleFreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_iron_freeze_usage"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found_simple"));
            return;
        }
        
        frozenPlayers.add(target.getUniqueId());
        sender.sendMessage(plugin.getConfigManager().getMessage("iron_freeze_success_sender", "%player%", target.getName()));
        target.sendMessage(plugin.getConfigManager().getMessage("iron_freeze_success_target"));
    }

    private void handleUnfreeze(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_iron_unfreeze_usage"));
            return;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found_simple"));
            return;
        }
        
        if (frozenPlayers.remove(target.getUniqueId())) {
            sender.sendMessage(plugin.getConfigManager().getMessage("iron_unfreeze_success_sender", "%player%", target.getName()));
            target.sendMessage(plugin.getConfigManager().getMessage("iron_unfreeze_success_target"));
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessage("iron_not_frozen", "%player%", target.getName()));
        }
    }

    private void handleAnnounce(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_iron_announce_usage"));
            return;
        }
        
        String message = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        
        // タイトル表示と全体ブロードキャスト
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(plugin.getConfigManager().getRawMessage("iron_announce_title"), ChatColor.WHITE + message, 10, 100, 20);
            p.sendMessage(plugin.getConfigManager().getMessage("iron_announce_chat", "%message%", message));
        }
        
        sender.sendMessage(plugin.getConfigManager().getMessage("iron_announce_sent"));
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getMessage("iron_help_header"));
        sender.sendMessage(plugin.getConfigManager().getMessage("iron_help_reload"));
        sender.sendMessage(plugin.getConfigManager().getMessage("iron_help_version"));
        sender.sendMessage(plugin.getConfigManager().getMessage("iron_help_cleanup"));
        sender.sendMessage(plugin.getConfigManager().getMessage("iron_help_admin_header"));
        sender.sendMessage(plugin.getConfigManager().getMessage("iron_help_kick"));
        sender.sendMessage(plugin.getConfigManager().getMessage("iron_help_ban"));
        sender.sendMessage(plugin.getConfigManager().getMessage("iron_help_tp"));
        sender.sendMessage(plugin.getConfigManager().getMessage("iron_help_bring"));
        sender.sendMessage(plugin.getConfigManager().getMessage("iron_help_freeze"));
        sender.sendMessage(plugin.getConfigManager().getMessage("iron_help_unfreeze"));
        sender.sendMessage(plugin.getConfigManager().getMessage("iron_help_announce"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            for (String sub : new String[]{"reload", "version", "cleanup", "kick", "ban", "tp", "bring", "freeze", "unfreeze", "announce"}) {
                if (sub.startsWith(prefix)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("kick", "ban", "tp", "bring", "freeze", "unfreeze").contains(sub)) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(p.getName());
                    }
                }
            }
        }
        return completions;
    }
    
    /**
     * 凍結中かチェック
     */
    public boolean isFrozen(UUID playerId) {
        return frozenPlayers.contains(playerId);
    }
}
