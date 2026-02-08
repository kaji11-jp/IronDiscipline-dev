package com.irondiscipline.command;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.manager.LinkManager;
import com.irondiscipline.model.Rank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /link コマンド
 * Discordアカウントとの連携
 */
public class LinkCommand implements CommandExecutor {

    private final IronDiscipline plugin;

    public LinkCommand(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("command_link_usage"));
            return true;
        }

        if (args.length < 1) {
            // 連携状況確認
            if (plugin.getLinkManager().isLinked(player.getUniqueId())) {
                sender.sendMessage(plugin.getConfigManager().getMessage("link_already_linked"));
                sender.sendMessage(plugin.getConfigManager().getMessage("link_how_to_unlink"));
            } else {
                sender.sendMessage(plugin.getConfigManager().getMessage("link_not_linked"));
                sender.sendMessage(plugin.getConfigManager().getMessage("link_instructions_header"));
                sender.sendMessage(plugin.getConfigManager().getMessage("link_instructions_1"));
                sender.sendMessage(plugin.getConfigManager().getMessage("link_instructions_2"));
            }
            return true;
        }

        String code = args[0].toUpperCase();
        LinkManager.LinkResult result = plugin.getLinkManager().attemptLink(player.getUniqueId(), code);

        switch (result) {
            case SUCCESS -> {
                player.sendMessage(plugin.getConfigManager().getMessage("link_success_title"));
                player.sendMessage(plugin.getConfigManager().getMessage("link_success_message"));
                
                // Discord側ロール・ニックネーム変更
                Long discordId = plugin.getLinkManager().getDiscordId(player.getUniqueId());
                if (discordId != null && plugin.getDiscordManager().isEnabled()) {
                    Rank rank = plugin.getRankManager().getRank(player);
                    plugin.getDiscordManager().onLinkComplete(discordId, player.getName(), rank);
                    
                    plugin.getDiscordManager().sendNotification(
                        plugin.getConfigManager().getRawMessage("link_success_notification_title"),
                        plugin.getConfigManager().getRawMessage("link_success_broadcast").replace("%player%", player.getName()),
                        java.awt.Color.GREEN
                    );
                }
            }
            case INVALID_CODE -> {
                player.sendMessage(plugin.getConfigManager().getMessage("link_invalid_code"));
                player.sendMessage(plugin.getConfigManager().getMessage("link_reacquire_code"));
            }
            case EXPIRED -> {
                player.sendMessage(plugin.getConfigManager().getMessage("link_code_expired"));
                player.sendMessage(plugin.getConfigManager().getMessage("link_reacquire_code"));
            }
        }

        return true;
    }
}
