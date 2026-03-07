package xyz.irondiscipline.command;

import xyz.irondiscipline.IronDiscipline;
import xyz.irondiscipline.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IronDiscipline-dev 管理コマンド
 * /irondev migrate - LuckPermsからデータを移行
 * /irondev status - ステータス表示
 */
public class IronDevCommand implements CommandExecutor, TabCompleter {

    private final IronDiscipline plugin;
    private boolean isMigrating = false;

    public IronDevCommand(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("iron.admin")) {
            sender.sendMessage(ChatColor.RED + "権限がありません。");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "migrate" -> handleMigrate(sender);
            case "status" -> handleStatus(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== IronDiscipline-dev ===");
        sender.sendMessage(ChatColor.YELLOW + "/irondev migrate" + ChatColor.GRAY + " - LuckPermsからデータを移行");
        sender.sendMessage(ChatColor.YELLOW + "/irondev status" + ChatColor.GRAY + " - ステータス表示");
    }

    private void handleStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== IronDiscipline-dev ステータス ===");
        sender.sendMessage(ChatColor.YELLOW + "バージョン: " + ChatColor.WHITE + plugin.getDescription().getVersion());
        sender.sendMessage(ChatColor.YELLOW + "LuckPerms非依存: " + ChatColor.GREEN + "はい");

        // LuckPerms検出チェック
        boolean lpAvailable = Bukkit.getPluginManager().getPlugin("LuckPerms") != null;
        sender.sendMessage(ChatColor.YELLOW + "LuckPerms検出: " +
                (lpAvailable ? ChatColor.GREEN + "あり (移行可能)" : ChatColor.GRAY + "なし"));

        // 階級データ件数
        plugin.getRankStorageManager().getAllRanks().thenAccept(ranks -> {
            sender.sendMessage(ChatColor.YELLOW + "階級データ件数: " + ChatColor.WHITE + ranks.size());
        });
    }

    private void handleMigrate(CommandSender sender) {
        if (isMigrating) {
            sender.sendMessage(ChatColor.RED + "移行処理が既に実行中です。");
            return;
        }

        // LuckPerms存在チェック
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            sender.sendMessage(ChatColor.RED + "LuckPermsが見つかりません！");
            sender.sendMessage(ChatColor.GRAY + "移行にはLuckPermsプラグインが必要です。");
            return;
        }

        sender.sendMessage(ChatColor.YELLOW + "LuckPermsからのデータ移行を開始します...");
        isMigrating = true;

        // 非同期でLuckPermsからデータを読み取り、独自DBに保存
        plugin.getTaskScheduler().runAsync(() -> {
            try {
                migratFromLuckPerms(sender);
            } finally {
                isMigrating = false;
            }
        });
    }

    private void migratFromLuckPerms(CommandSender sender) {
        try {
            // LuckPerms APIを取得
            net.luckperms.api.LuckPerms luckPerms = net.luckperms.api.LuckPermsProvider.get();
            String metaKey = plugin.getConfigManager().getRankMetaKey();

            // 全ユーザーをロード
            sender.sendMessage(ChatColor.GRAY + "ユーザーデータを取得中...");

            // LuckPermsからユニークユーザーを取得
            luckPerms.getUserManager().getUniqueUsers().thenAccept(uuids -> {
                int total = uuids.size();
                AtomicInteger processed = new AtomicInteger(0);
                AtomicInteger migrated = new AtomicInteger(0);
                int reportInterval = Math.max(total / 10, 1);

                sender.sendMessage(ChatColor.YELLOW + "対象ユーザー数: " + total);

                for (UUID uuid : uuids) {
                    // ユーザーデータをロード
                    luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
                        // メタデータから階級を取得
                        String rankId = user.getCachedData().getMetaData().getMetaValue(metaKey);

                        if (rankId != null) {
                            Rank rank = Rank.fromId(rankId);
                            String userName = user.getUsername() != null ? user.getUsername() : "Unknown";

                            // 独自DBに保存
                            plugin.getRankStorageManager().setRank(uuid, userName, rank);
                            migrated.incrementAndGet();
                        }

                        int current = processed.incrementAndGet();
                        if (current % reportInterval == 0 || current == total) {
                            int percent = (int) ((current * 100.0) / total);
                            plugin.getTaskScheduler().runGlobal(() -> {
                                sender.sendMessage(
                                        ChatColor.GREEN + "進捗: " + percent + "% (" + current + "/" + total + ")");
                            });
                        }

                        // 完了チェック
                        if (current == total) {
                            plugin.getTaskScheduler().runGlobal(() -> {
                                sender.sendMessage(ChatColor.GREEN + "===========================");
                                sender.sendMessage(ChatColor.GREEN + "移行完了！");
                                sender.sendMessage(ChatColor.YELLOW + "移行したユーザー数: " + ChatColor.WHITE + migrated.get());
                                sender.sendMessage(ChatColor.GREEN + "===========================");
                            });
                        }
                    });
                }
            });

        } catch (IllegalStateException e) {
            sender.sendMessage(ChatColor.RED + "LuckPerms APIの取得に失敗しました: " + e.getMessage());
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "移行中にエラーが発生しました: " + e.getMessage());
            plugin.getLogger().warning("移行エラー: " + e.getMessage());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (String sub : List.of("migrate", "status")) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        }

        return completions;
    }
}
