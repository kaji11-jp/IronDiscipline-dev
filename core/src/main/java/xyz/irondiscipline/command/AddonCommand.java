package xyz.irondiscipline.command;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import xyz.irondiscipline.IronDiscipline;
import xyz.irondiscipline.manager.AddonManager;
import xyz.irondiscipline.manager.AddonManager.AddonDescriptor;
import xyz.irondiscipline.manager.AddonManager.AddonRegistryEntry;
import xyz.irondiscipline.manager.AddonManager.InstallResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * /iron addon サブコマンドハンドラ
 * <p>
 * {@code /iron addon install <id|owner/repo|URL>} - アドオンインストール<br>
 * {@code /iron addon list} - インストール済みアドオン一覧<br>
 * {@code /iron addon certified} - 公認アドオン一覧（IrDi チーム審査済み）<br>
 * {@code /iron addon remove <id>} - アドオンアンインストール
 * </p>
 */
public class AddonCommand {

    private final IronDiscipline plugin;

    public AddonCommand(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    /**
     * addonサブコマンドを処理します。args[0]は "addon" です。
     */
    public void handle(CommandSender sender, String[] args) {
        if (args.length < 2) {
            showAddonHelp(sender);
            return;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "install", "i" -> handleInstall(sender, args);
            case "list", "ls" -> handleList(sender);
            case "certified", "cert", "available", "av" -> handleCertified(sender);
            case "remove", "rm", "uninstall" -> handleRemove(sender, args);
            case "refresh" -> handleRefresh(sender);
            default -> showAddonHelp(sender);
        }
    }

    /**
     * タブ補完
     */
    public List<String> tabComplete(String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 2) {
            String prefix = args[1].toLowerCase();
            for (String sub : new String[]{"install", "list", "certified", "remove", "refresh"}) {
                if (sub.startsWith(prefix)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 3) {
            String action = args[1].toLowerCase();
            String prefix = args[2].toLowerCase();

            if ("install".equals(action) || "i".equals(action)) {
                // 公認アドオンID候補
                for (String id : plugin.getAddonManager().getCertifiedAddons().keySet()) {
                    if (id.startsWith(prefix)) {
                        completions.add(id);
                    }
                }
            } else if ("remove".equals(action) || "rm".equals(action) || "uninstall".equals(action)) {
                // インストール済みアドオンID候補
                for (AddonDescriptor desc : plugin.getAddonManager().scanInstalledAddons()) {
                    if (desc.id().toLowerCase().startsWith(prefix)) {
                        completions.add(desc.id());
                    }
                }
            }
        }

        return completions;
    }

    // ========== サブコマンドハンドラ ==========

    private void handleInstall(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getConfigManager().getMessage("addon_install_usage"));
            return;
        }

        String target = args[2];
        AddonManager addonManager = plugin.getAddonManager();

        sender.sendMessage(plugin.getConfigManager().getMessage("addon_installing", "%target%", target));

        // URL 形式かどうか判定
        if (target.startsWith("http://") || target.startsWith("https://")) {
            // 直接 URL ダウンロード（非公認アドオンも可）
            addonManager.installFromUrl(target).thenAccept(result -> {
                sendResult(sender, result);
            });
        } else if (target.contains("/")) {
            // "owner/repo" 形式→ GitHub Releases から
            addonManager.installFromGitHub(target).thenAccept(result -> {
                sendResult(sender, result);
            });
        } else {
            // 公認レジストリから
            addonManager.installCertified(target).thenAccept(result -> {
                sendResult(sender, result);
            });
        }
    }

    private void handleList(CommandSender sender) {
        List<AddonDescriptor> addons = plugin.getAddonManager().scanInstalledAddons();

        if (addons.isEmpty()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("addon_list_empty"));
            return;
        }

        sender.sendMessage(plugin.getConfigManager().getMessage("addon_list_header",
                "%count%", String.valueOf(addons.size())));

        for (AddonDescriptor addon : addons) {
            String certBadge = plugin.getAddonManager().isCertified(addon.id())
                    ? "&2[公認] " : "&6[非公認] ";
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&7- " + certBadge + "&a" + addon.name() + " &7v" + addon.version()
                    + " &8[&e" + addon.id() + "&8]"
                    + (addon.description().isEmpty() ? "" : " &7" + addon.description())));
        }
    }

    private void handleCertified(CommandSender sender) {
        Map<String, AddonRegistryEntry> certified = plugin.getAddonManager().getCertifiedAddons();
        List<AddonDescriptor> installed = plugin.getAddonManager().scanInstalledAddons();

        sender.sendMessage(plugin.getConfigManager().getMessage("addon_certified_header",
                "%count%", String.valueOf(certified.size())));

        for (Map.Entry<String, AddonRegistryEntry> entry : certified.entrySet()) {
            AddonRegistryEntry addon = entry.getValue();
            boolean isInstalled = installed.stream()
                    .anyMatch(d -> d.id().equalsIgnoreCase(addon.id()));

            String status = isInstalled
                    ? ChatColor.GREEN + "[導入済]"
                    : ChatColor.YELLOW + "[未導入]";

            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&7- &b" + addon.name() + " " + status
                    + " &8(&7" + entry.getKey() + "&8)"
                    + "\n  &7" + addon.description()
                    + "\n  &8GitHub: &9" + addon.githubRepo()));
        }
    }

    private void handleRemove(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.getConfigManager().getMessage("addon_remove_usage"));
            return;
        }

        String addonId = args[2];
        sender.sendMessage(plugin.getConfigManager().getMessage("addon_removing", "%id%", addonId));

        plugin.getAddonManager().uninstall(addonId).thenAccept(result -> {
            sendResult(sender, result);
        });
    }

    private void handleRefresh(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getMessage("addon_refreshing"));
        plugin.getAddonManager().forceRefreshRegistry().thenRun(() -> {
            int count = plugin.getAddonManager().getCertifiedAddons().size();
            sender.sendMessage(plugin.getConfigManager().getMessage("addon_refreshed",
                    "%count%", String.valueOf(count)));
        });
    }

    private void sendResult(CommandSender sender, InstallResult result) {
        // Folia ではメインスレッド以外からメッセージ送信可能（CommandSender.sendMessage はスレッドセーフ）
        if (result.success()) {
            sender.sendMessage(plugin.getConfigManager().getMessage("addon_success",
                    "%message%", result.message()));
        } else {
            sender.sendMessage(plugin.getConfigManager().getMessage("addon_failed",
                    "%message%", result.message()));
        }
    }

    private void showAddonHelp(CommandSender sender) {
        sender.sendMessage(plugin.getConfigManager().getMessage("addon_help_header"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&e/iron addon install <id|owner/repo|URL> &7- アドオンをインストール"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&e/iron addon list &7- インストール済みアドオン一覧"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&e/iron addon certified &7- 公認アドオン一覧"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&e/iron addon remove <id> &7- アドオンをアンインストール"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&e/iron addon refresh &7- 公認レジストリを再取得"));
    }
}
