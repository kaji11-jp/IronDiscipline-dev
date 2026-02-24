package xyz.irondiscipline.command;

import xyz.irondiscipline.IronDiscipline;
import xyz.irondiscipline.manager.WarningManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * /warn, /warnings, /clearwarnings コマンド
 */
public class WarnCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;

    public WarnCommand(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmdName = command.getName().toLowerCase();

        switch (cmdName) {
            case "warn" -> handleWarn(sender, args);
            case "warnings" -> handleWarnings(sender, args);
            case "clearwarnings" -> handleClearWarnings(sender, args);
            case "unwarn" -> handleUnwarn(sender, args);
        }

        return true;
    }

    private void handleWarn(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_usage_warn"));
            return;
        }

        // メインスレッドでオンラインプレイヤー検索
        OfflinePlayer target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            // オフライン検索もUUID計算が速ければ同期で良いが、安全のため非同期で行うならここだけ分離
            // しかしBukkit.getOfflinePlayer(name)はUUID解決のためにWebリクエストを飛ばす可能性があるため非同期推奨
            // ただしBukkit.getPlayer()はメインスレッド必須
        } else {
            // オンラインなら即時実行
            executeWarn(sender, target, args);
            return;
        }

        // オフライン検索のみ非同期
        CompletableFuture.supplyAsync(() -> {
            @SuppressWarnings("deprecation")
            OfflinePlayer p = Bukkit.getOfflinePlayer(args[0]);
            return p;
        }).thenAccept(offlineTarget -> {
            plugin.getTaskScheduler().runGlobal(() -> executeWarn(sender, offlineTarget, args));
        });
    }

    private void executeWarn(CommandSender sender, OfflinePlayer target, String[] args) {
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found", "%player%", target.getName()));
            return;
        }

        // 権限チェック（憲兵 or 少尉以上）
        if (!canWarn(sender, target)) {
            return;
        }

        // 理由を結合
        StringBuilder reason = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            if (i > 1)
                reason.append(" ");
            reason.append(args[i]);
        }

        String reasonStr = reason.toString();

        // 警告追加 (非同期)
        plugin.getWarningManager().addWarning(
                target.getUniqueId(),
                target.getName(),
                reasonStr,
                sender instanceof Player ? ((Player) sender).getUniqueId() : null
        ).thenAccept(count -> {
            plugin.getTaskScheduler().runGlobal(() -> {
                // 通知
                sender.sendMessage(plugin.getConfigManager().getMessage("warn_success", "%player%", target.getName(), "%count%", String.valueOf(count)));

                if (target.isOnline() && target.getPlayer() != null) {
                    target.getPlayer().sendMessage(plugin.getConfigManager().getRawMessage("warn_received").replace("%reason%", reasonStr).replace("%count%", String.valueOf(count)));
                }

                // 自動処分の通知
                if (count >= 5) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("warn_kick_broadcast", "%player%", target.getName(), "%count%", String.valueOf(count)));
                } else if (count >= 3) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("warn_jail_broadcast", "%player%", target.getName(), "%count%", String.valueOf(count)));
                }
            });
        });
    }

    private void handleWarnings(CommandSender sender, String[] args) {
        if (args.length > 0) {
            // オフライン検索非同期
            OfflinePlayer onlineTarget = Bukkit.getPlayer(args[0]);
            if (onlineTarget != null) {
                showWarnings(sender, onlineTarget);
            } else {
                CompletableFuture.supplyAsync(() -> {
                    @SuppressWarnings("deprecation")
                    OfflinePlayer p = Bukkit.getOfflinePlayer(args[0]);
                    return p;
                }).thenAccept(target -> plugin.getTaskScheduler().runGlobal(() -> showWarnings(sender, target)));
            }
        } else if (sender instanceof Player) {
            showWarnings(sender, (Player) sender);
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found", "%player%", "null"));
        }
    }

    private void showWarnings(CommandSender sender, OfflinePlayer target) {
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("error_specify_player"));
            return;
        }

        plugin.getWarningManager().getWarnings(target.getUniqueId()).thenAccept(warnings -> {
            plugin.getTaskScheduler().runGlobal(() -> {
                if (warnings.isEmpty()) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("warn_none", "%player%", target.getName()));
                    return;
                }

                sender.sendMessage(plugin.getConfigManager().getMessage("warn_history_header", "%player%", target.getName(), "%count%", String.valueOf(warnings.size())));
                int i = 1;
                for (WarningManager.Warning w : warnings) {
                    sender.sendMessage(plugin.getConfigManager().getRawMessage("warn_history_entry").replace("%index%", String.valueOf(i)).replace("%reason%", w.reason).replace("%date%", w.getFormattedDate()));
                    i++;
                }
            });
        });
    }

    private void handleClearWarnings(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_usage_clearwarnings"));
            return;
        }

        OfflinePlayer onlineTarget = Bukkit.getPlayer(args[0]);
        if (onlineTarget != null) {
            executeClearWarnings(sender, onlineTarget);
        } else {
            CompletableFuture.supplyAsync(() -> {
                @SuppressWarnings("deprecation")
                OfflinePlayer p = Bukkit.getOfflinePlayer(args[0]);
                return p;
            }).thenAccept(target -> plugin.getTaskScheduler().runGlobal(() -> executeClearWarnings(sender, target)));
        }
    }

    private void executeClearWarnings(CommandSender sender, OfflinePlayer target) {
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found", "%player%", target.getName()));
            return;
        }

        plugin.getWarningManager().clearWarnings(target.getUniqueId()).thenRun(() -> {
            plugin.getTaskScheduler().runGlobal(() -> {
                sender.sendMessage(plugin.getConfigManager().getMessage("warn_clear_success", "%player%", target.getName()));
            });
        });
    }

    private void handleUnwarn(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_usage_unwarn"));
            return;
        }

        OfflinePlayer onlineTarget = Bukkit.getPlayer(args[0]);
        if (onlineTarget != null) {
            executeUnwarn(sender, onlineTarget);
        } else {
            CompletableFuture.supplyAsync(() -> {
                @SuppressWarnings("deprecation")
                OfflinePlayer p = Bukkit.getOfflinePlayer(args[0]);
                return p;
            }).thenAccept(target -> plugin.getTaskScheduler().runGlobal(() -> executeUnwarn(sender, target)));
        }
    }

    private void executeUnwarn(CommandSender sender, OfflinePlayer target) {
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player_not_found", "%player%", target.getName()));
            return;
        }

        plugin.getWarningManager().removeLastWarning(target.getUniqueId()).thenAccept(success -> {
            plugin.getTaskScheduler().runGlobal(() -> {
                if (success) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("warn_remove_success", "%player%", target.getName()));
                } else {
                    sender.sendMessage(plugin.getConfigManager().getMessage("warn_none", "%player%", target.getName()));
                }
            });
        });
    }

    private boolean canWarn(CommandSender sender, OfflinePlayer target) {
        if (!(sender instanceof Player executor)) {
            return true; // コンソール
        }

        // 憲兵チェック
        if (plugin.getDivisionManager().isMP(executor)) {
            // 憲兵は士官未満のみ
            if (target instanceof Player) {
                if (plugin.getRankManager().getRank((Player) target)
                        .getWeight() >= xyz.irondiscipline.model.Rank.LIEUTENANT
                                .getWeight()) {
                    sender.sendMessage(plugin.getConfigManager().getMessage("warn_cannot_warn_officer"));
                    return false;
                }
            }
            return true;
        }

        // 通常の階級チェック (オンライン時のみ厳密チェック、オフラインは一旦許可)
        if (target instanceof Player) {
            return plugin.getRankUtil().checkAll(sender, (Player) target);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(p.getName());
                }
            }
        }

        return completions;
    }
}
