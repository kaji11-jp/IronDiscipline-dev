package xyz.irondiscipline.command;

import xyz.irondiscipline.IronDiscipline;
import xyz.irondiscipline.manager.ExamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ExamCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;

    public ExamCommand(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_player_only"));
            return true;
        }

        Player player = (Player) sender;
        ExamManager examManager = plugin.getExamManager();

        // 権限チェック (簡易的に少尉以上とする、本来はRankManagerでチェックすべき)
        if (!plugin.getRankManager().getRank(player).isHigherThan(xyz.irondiscipline.model.Rank.SERGEANT)) {
            player.sendMessage(plugin.getConfigManager().getMessage("exam_permission_denied"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "start":
                if (args.length < 3) {
                    player.sendMessage(plugin.getConfigManager().getMessage("command_exam_start_usage"));
                    return true;
                }
                handleStart(player, args[1], args[2]);
                break;
            case "pass":
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfigManager().getMessage("command_exam_pass_usage"));
                    return true;
                }
                handlePass(player, args[1]);
                break;
            case "fail":
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfigManager().getMessage("command_exam_fail_usage"));
                    return true;
                }
                handleFail(player, args[1]);
                break;
            case "quiz":
                if (args.length < 2) {
                    player.sendMessage(plugin.getConfigManager().getMessage("command_exam_quiz_usage"));
                    return true;
                }
                handleQuiz(player, args[1]);
                break;
            case "sts":
                examManager.startSTS(player);
                outputSTS(player);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void handleStart(Player instructor, String targetName, String type) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            instructor.sendMessage(plugin.getConfigManager().getMessage("player_not_found", "%player%", targetName));
            return;
        }
        plugin.getExamManager().startExamSession(instructor, target, type);
    }

    private void handlePass(Player instructor, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            instructor.sendMessage(plugin.getConfigManager().getMessage("player_not_found", "%player%", targetName));
            return;
        }
        plugin.getExamManager().passExam(instructor, target);
    }

    private void handleFail(Player instructor, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            instructor.sendMessage(plugin.getConfigManager().getMessage("player_not_found", "%player%", targetName));
            return;
        }
        plugin.getExamManager().failExam(instructor, target);
    }

    private void handleQuiz(Player instructor, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            instructor.sendMessage(plugin.getConfigManager().getMessage("player_not_found", "%player%", targetName));
            return;
        }
        plugin.getExamManager().startQuiz(instructor, target);
    }

    private void outputSTS(Player instructor) {
        // Chat broadcasting is handled in ExamManager, added local confirm if needed
    }

    private void sendHelp(Player player) {
        player.sendMessage(plugin.getConfigManager().getMessage("exam_help_header"));
        player.sendMessage(plugin.getConfigManager().getMessage("exam_help_start"));
        player.sendMessage(plugin.getConfigManager().getMessage("exam_help_pass"));
        player.sendMessage(plugin.getConfigManager().getMessage("exam_help_fail"));
        player.sendMessage(plugin.getConfigManager().getMessage("exam_help_quiz"));
        player.sendMessage(plugin.getConfigManager().getMessage("exam_help_sts"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("start", "pass", "fail", "quiz", "sts");
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("sts")) {
            return null; // Player list
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("start")) {
            return Arrays.asList("Basic", "Advanced", "Officer", "Driving", "Shooting");
        }
        return Collections.emptyList();
    }
}
