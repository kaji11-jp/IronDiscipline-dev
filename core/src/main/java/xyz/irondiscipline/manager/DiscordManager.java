package xyz.irondiscipline.manager;

import xyz.irondiscipline.IronDiscipline;
import xyz.irondiscipline.api.rank.IRank;
import xyz.irondiscipline.api.rank.RankRegistry;
import xyz.irondiscipline.model.Rank;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.EntitySelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.awt.Color;
import java.util.UUID;

/**
 * Discord Bot マネージャー
 */
public class DiscordManager extends ListenerAdapter {

    private final IronDiscipline plugin;
    private JDA jda;
    private String notificationChannelId;
    private String guildId;
    private String unverifiedRoleId;
    private String verifiedRoleId;
    private String adminRoleId;
    private boolean enabled = false;

    // 寄付システム
    private int donationGoal = 5000; // 月間目標（円）
    private int donationCurrent = 0; // 現在の寄付額

    public DiscordManager(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    /**
     * Botを起動
     */
    public boolean start(String botToken, String channelId, String guildId, String unverifiedRoleId,
            String verifiedRoleId, String adminRoleId) {
        if (botToken == null || botToken.isEmpty()) {
            plugin.getLogger().warning(rawMsg("discord_log_token_not_set"));
            return false;
        }

        this.notificationChannelId = channelId;
        this.guildId = guildId;
        this.unverifiedRoleId = unverifiedRoleId;
        this.verifiedRoleId = verifiedRoleId;
        this.adminRoleId = adminRoleId;

        try {
            jda = JDABuilder.createDefault(botToken)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS)
                    .setActivity(Activity.playing(rawMsg("discord_bot_activity")))
                    .addEventListeners(this)
                    .build();

            // コマンド登録は onGuildReady で行う
            plugin.getLogger().info(rawMsg("discord_log_login_complete"));

            return true;

        } catch (Exception e) {
            plugin.getLogger().severe(rawMsg("discord_log_start_failed", "%error%", e.getMessage()));
            return false;
        }
    }

    private boolean isAdmin(Member member) {
        if (member == null)
            return false;
        if (member.hasPermission(Permission.ADMINISTRATOR))
            return true;
        if (adminRoleId != null && !adminRoleId.isEmpty()) {
            Role adminRole = member.getGuild().getRoleById(adminRoleId);
            return adminRole != null && member.getRoles().contains(adminRole);
        }
        return false;
    }

    @Override
    public void onGuildReady(net.dv8tion.jda.api.events.guild.GuildReadyEvent event) {
        String configGuildId = this.guildId;
        if (configGuildId != null && !configGuildId.isEmpty() && !event.getGuild().getId().equals(configGuildId)) {
            return; // 指定されたサーバー以外は無視
        }

        plugin.getLogger()
            .info(rawMsg("discord_log_registering_commands", "%guild%", event.getGuild().getName(), "%id%",
                event.getGuild().getId()));

        event.getGuild().updateCommands().addCommands(
            Commands.slash("link", rawMsg("discord_cmd_link_desc")),
            Commands.slash("unlink", rawMsg("discord_cmd_unlink_desc")),
            Commands.slash("status", rawMsg("discord_cmd_status_desc")),
            Commands.slash("players", rawMsg("discord_cmd_players_desc")),
            Commands.slash("playtime", rawMsg("discord_cmd_playtime_desc")),
            Commands.slash("rank", rawMsg("discord_cmd_rank_desc")),
            Commands.slash("warn", rawMsg("discord_cmd_warn_desc"))
                .addOption(OptionType.USER, "user", rawMsg("discord_opt_user_desc"), true)
                .addOption(OptionType.STRING, "reason", rawMsg("discord_opt_reason_desc"), true),
            Commands.slash("announce", rawMsg("discord_cmd_announce_desc"))
                .addOption(OptionType.STRING, "message", rawMsg("discord_opt_message_desc"), true),
            Commands.slash("donate", rawMsg("discord_cmd_donate_desc")),
            Commands.slash("setgoal", rawMsg("discord_cmd_setgoal_desc"))
                .addOption(OptionType.INTEGER, "goal", rawMsg("discord_opt_goal_desc"), true)
                .addOption(OptionType.INTEGER, "current", rawMsg("discord_opt_current_desc"), true),

                // === New Commands ===
                Commands.slash("settings", rawMsg("discord_cmd_settings_desc"))
                    .addOption(OptionType.STRING, "action", rawMsg("discord_opt_action_desc"), true)
                    .addOption(OptionType.STRING, "key", rawMsg("discord_opt_key_desc"), false)
                    .addOption(OptionType.STRING, "value", rawMsg("discord_opt_value_desc"), false),

                Commands.slash("panel", rawMsg("discord_cmd_panel_desc"))
                    .addOption(OptionType.STRING, "type", rawMsg("discord_opt_panel_type_desc"), true),

                Commands.slash("division", rawMsg("discord_cmd_division_desc"))
                    .addOption(OptionType.STRING, "action", rawMsg("discord_opt_division_action_desc"), true)
                    .addOption(OptionType.STRING, "arg1", rawMsg("discord_opt_arg1_desc"), false)
                    .addOption(OptionType.STRING, "arg2", rawMsg("discord_opt_arg2_desc"), false),

                Commands.slash("promote", rawMsg("discord_cmd_promote_desc"))
                    .addOption(OptionType.USER, "user", rawMsg("discord_opt_user_desc"), true),

                Commands.slash("demote", rawMsg("discord_cmd_demote_desc"))
                    .addOption(OptionType.USER, "user", rawMsg("discord_opt_user_desc"), true),

                Commands.slash("setrank", rawMsg("discord_cmd_setrank_desc"))
                    .addOption(OptionType.USER, "user", rawMsg("discord_opt_user_desc"), true)
                    .addOption(OptionType.STRING, "rank", rawMsg("discord_opt_rank_id_desc"), true),

                Commands.slash("kick", rawMsg("discord_cmd_kick_desc"))
                    .addOption(OptionType.USER, "user", rawMsg("discord_opt_user_desc"), true)
                    .addOption(OptionType.STRING, "reason", rawMsg("discord_opt_reason_desc"), true),

                Commands.slash("ban", rawMsg("discord_cmd_ban_desc"))
                    .addOption(OptionType.USER, "user", rawMsg("discord_opt_user_desc"), true)
                    .addOption(OptionType.STRING, "reason", rawMsg("discord_opt_reason_desc"), true))
                .queue(
                    success -> plugin.getLogger().info(rawMsg("discord_log_register_success", "%count%",
                        String.valueOf(success.size()))),
                    error -> plugin.getLogger().severe(rawMsg("discord_log_register_failed", "%error%",
                        error.getMessage())));
    }

    /**
     * Botを停止
     */
    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            plugin.getLogger().info(rawMsg("discord_log_shutdown"));
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        String cmd = event.getName();

        switch (cmd) {
            case "link" -> handleLink(event);
            case "unlink" -> handleUnlink(event);
            case "status" -> handleStatus(event);
            case "players" -> handlePlayers(event);
            case "playtime" -> handlePlaytime(event);
            case "rank" -> handleRank(event);
            case "warn" -> handleWarn(event);
            case "announce" -> handleAnnounce(event);
            case "donate" -> handleDonate(event);
            case "setgoal" -> handleSetGoal(event);

            // New Handlers
            case "settings" -> handleSettings(event);
            case "panel" -> handlePanel(event);
            case "division" -> handleDivision(event);
            case "promote" -> handleAdminRank(event, true);
            case "demote" -> handleAdminRank(event, false);
            case "setrank" -> handleSetRank(event);
            case "kick" -> handlePunish(event, "kick");
            case "ban" -> handlePunish(event, "ban");
        }
    }

    private void handleLink(SlashCommandInteractionEvent event) {
        long discordId = event.getUser().getIdLong();

        if (plugin.getLinkManager().isLinked(discordId)) {
            event.reply(rawMsg("discord_link_already_linked_unlink")).setEphemeral(true).queue();
            return;
        }

        String code = plugin.getLinkManager().generateLinkCode(discordId);

        EmbedBuilder eb = new EmbedBuilder()
            .setTitle(rawMsg("discord_embed_link_title"))
            .setDescription(rawMsg("discord_embed_link_desc"))
            .addField(rawMsg("discord_embed_link_field_command"), "`/link " + code + "`", false)
            .addField(rawMsg("discord_embed_link_field_expiry"),
                rawMsg("discord_embed_link_expiry_value", "%minutes%", "5"), false)
                .setColor(Color.BLUE)
            .setFooter(rawMsg("discord_embed_footer_brand"));

        event.replyEmbeds(eb.build()).setEphemeral(true).queue();
    }

    private void handleUnlink(SlashCommandInteractionEvent event) {
        long discordId = event.getUser().getIdLong();

        if (plugin.getLinkManager().unlinkByDiscord(discordId)) {
            event.reply(rawMsg("discord_unlink_success")).setEphemeral(true).queue();
        } else {
            event.reply(rawMsg("discord_unlink_not_linked")).setEphemeral(true).queue();
        }
    }

    private void handleStatus(SlashCommandInteractionEvent event) {
        plugin.getTaskScheduler().runGlobal(() -> {
            int online = Bukkit.getOnlinePlayers().size();
            int max = Bukkit.getMaxPlayers();
            int linked = plugin.getLinkManager().getLinkCount();

            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle(rawMsg("discord_embed_status_title"))
                    .addField(rawMsg("discord_embed_status_online"), online + " / " + max, true)
                    .addField(rawMsg("discord_embed_status_linked"),
                        rawMsg("discord_person_count", "%count%", String.valueOf(linked)), true)
                    .setColor(Color.GREEN)
                    .setFooter(rawMsg("discord_embed_footer_brand"));

            event.replyEmbeds(eb.build()).queue();
        });
    }

    private void handlePlayers(SlashCommandInteractionEvent event) {
        plugin.getTaskScheduler().runGlobal(() -> {
            StringBuilder sb = new StringBuilder();

            for (Player p : Bukkit.getOnlinePlayers()) {
                Rank rank = plugin.getRankManager().getRank(p);
                String div = plugin.getDivisionManager().getDivision(p.getUniqueId());
                String divDisplay = div != null ? plugin.getDivisionManager().getDivisionDisplayName(div) : "";

                sb.append("**").append(p.getName()).append("** - ")
                        .append(rank.getId()).append(" ").append(divDisplay).append("\n");
            }

            if (sb.length() == 0) {
                sb.append(rawMsg("discord_no_online_players"));
            }

            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle(rawMsg("discord_embed_players_title"))
                    .setDescription(sb.toString())
                    .setColor(Color.CYAN)
                    .setFooter(rawMsg("discord_embed_footer_brand"));

            event.replyEmbeds(eb.build()).queue();
        });
    }

    private void handlePlaytime(SlashCommandInteractionEvent event) {
        long discordId = event.getUser().getIdLong();
        UUID minecraftId = plugin.getLinkManager().getMinecraftId(discordId);

        if (minecraftId == null) {
            event.reply(rawMsg("discord_account_not_linked_use_link")).setEphemeral(true).queue();
            return;
        }

        // Defer reply to prevent timeout and indicate processing
        event.deferReply(true).queue();

        plugin.getTaskScheduler().runGlobal(() -> {
            String playtime = plugin.getPlaytimeManager().getFormattedPlaytime(minecraftId);
            String playerName = Bukkit.getOfflinePlayer(minecraftId).getName();

            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle(rawMsg("discord_embed_playtime_title"))
                    .addField(playerName != null ? playerName : rawMsg("discord_unknown_player_name"), playtime, false)
                    .setColor(Color.ORANGE)
                    .setFooter(rawMsg("discord_embed_footer_brand"));

            event.getHook().editOriginalEmbeds(eb.build()).queue();
        });
    }

    private void handleRank(SlashCommandInteractionEvent event) {
        long discordId = event.getUser().getIdLong();
        UUID minecraftId = plugin.getLinkManager().getMinecraftId(discordId);

        if (minecraftId == null) {
            event.reply(rawMsg("discord_account_not_linked")).setEphemeral(true).queue();
            return;
        }

        plugin.getTaskScheduler().runGlobal(() -> {
            Player player = Bukkit.getPlayer(minecraftId);
            Rank rank = player != null ? plugin.getRankManager().getRank(player) : Rank.PRIVATE;
            String div = plugin.getDivisionManager().getDivision(minecraftId);

            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle(rawMsg("discord_embed_rank_title"))
                    .addField(rawMsg("discord_embed_rank_field_rank"), rank.getId(), true)
                    .addField(rawMsg("discord_embed_rank_field_division"),
                        div != null ? div : rawMsg("discord_none"), true)
                    .setColor(Color.YELLOW)
                    .setFooter(rawMsg("discord_embed_footer_brand"));

            event.replyEmbeds(eb.build()).setEphemeral(true).queue();
        });
    }

    private void handleWarn(SlashCommandInteractionEvent event) {
        var targetOption = event.getOption("user");
        var reasonOption = event.getOption("reason");

        if (targetOption == null || reasonOption == null) {
            event.reply(rawMsg("discord_param_missing")).setEphemeral(true).queue();
            return;
        }

        long targetDiscordId = targetOption.getAsUser().getIdLong();
        String reason = reasonOption.getAsString();

        UUID targetMinecraft = plugin.getLinkManager().getMinecraftId(targetDiscordId);
        if (targetMinecraft == null) {
            event.reply(rawMsg("discord_target_not_linked")).setEphemeral(true).queue();
            return;
        }

        plugin.getTaskScheduler().runGlobal(() -> {
            Player target = Bukkit.getPlayer(targetMinecraft);
            if (target == null || !target.isOnline()) {
                event.reply(rawMsg("discord_target_offline")).setEphemeral(true).queue();
                return;
            }

            // 警告実行
            plugin.getWarningManager().addWarning(targetMinecraft, target.getName(), reason, null).thenAccept(count -> {
                plugin.getTaskScheduler().runEntity(target, () -> {
                    if (!target.isOnline()) {
                        return;
                    }
                    target.sendMessage(plugin.getConfigManager().getRawMessage("warn_received")
                            .replace("%reason%", reason)
                            .replace("%count%", String.valueOf(count)));
                });
            });

            event.reply(rawMsg("discord_warn_success", "%player%", target.getName(), "%reason%", reason)).queue();
        });
    }

    private void handleAnnounce(SlashCommandInteractionEvent event) {
        var msgOption = event.getOption("message");
        if (msgOption == null)
            return;

        String message = msgOption.getAsString();

        plugin.getTaskScheduler().runGlobal(() -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.getTaskScheduler().runEntity(p, () -> {
                    if (!p.isOnline()) {
                        return;
                    }
                    p.sendTitle(
                            plugin.getConfigManager().getRawMessage("iron_announce_title"),
                            plugin.getConfigManager().getRawMessage("discord_announce_title_sub").replace("%message%", message),
                            10, 100, 20);
                    p.sendMessage(plugin.getConfigManager().getRawMessage("discord_announce_chat")
                            .replace("%message%", message));
                });
            }
        });

        event.reply(rawMsg("discord_announce_sent", "%message%", message)).queue();
    }

    private void handleDonate(SlashCommandInteractionEvent event) {
        int percent = donationGoal > 0 ? (donationCurrent * 100 / donationGoal) : 0;
        if (percent > 100)
            percent = 100;

        // プログレスバー生成
        int bars = 20;
        int filled = (percent * bars) / 100;
        StringBuilder progressBar = new StringBuilder();
        for (int i = 0; i < bars; i++) {
            progressBar.append(i < filled ? "█" : "░");
        }

        EmbedBuilder eb = new EmbedBuilder()
            .setTitle(rawMsg("discord_embed_donate_title"))
            .setDescription(rawMsg("discord_embed_donate_desc"))
            .addField(rawMsg("discord_embed_donate_goal"), "¥" + String.format("%,d", donationGoal), true)
            .addField(rawMsg("discord_embed_donate_current"), "¥" + String.format("%,d", donationCurrent), true)
            .addField(rawMsg("discord_embed_donate_percent"), percent + "%", true)
            .addField(rawMsg("discord_embed_donate_progress"), "`" + progressBar.toString() + "` " + percent + "%",
                false)
                .setColor(percent >= 100 ? Color.GREEN : (percent >= 50 ? Color.YELLOW : Color.RED))
            .setFooter(rawMsg("discord_embed_donate_footer"));

        // 寄付先情報があれば追加
        String info = plugin.getConfigManager().getDonationInfo();
        if (info != null && !info.isEmpty()) {
            eb.addField(rawMsg("discord_embed_donate_method"), info, false);
        }

        event.replyEmbeds(eb.build()).queue();
    }

    private void handleSetGoal(SlashCommandInteractionEvent event) {
        // 管理者権限チェック
        if (!isAdmin(event.getMember())) {
            event.reply(rawMsg("discord_admin_only")).setEphemeral(true).queue();
            return;
        }

        var goalOption = event.getOption("goal");
        var currentOption = event.getOption("current");

        if (goalOption == null || currentOption == null) {
            event.reply(rawMsg("discord_param_missing")).setEphemeral(true).queue();
            return;
        }

        donationGoal = goalOption.getAsInt();
        donationCurrent = currentOption.getAsInt();

        int percent = donationGoal > 0 ? (donationCurrent * 100 / donationGoal) : 0;

        event.reply(rawMsg("discord_setgoal_updated",
            "%goal%", String.format("%,d", donationGoal),
            "%current%", String.format("%,d", donationCurrent),
            "%percent%", String.valueOf(percent))).queue();
    }

    // ===== 通知機能 =====

    /**
     * 通知チャンネルにメッセージ送信
     */
    public void sendNotification(String title, String message, Color color) {
        if (!enabled || jda == null || notificationChannelId == null || notificationChannelId.isEmpty()) {
            return;
        }

        TextChannel channel = jda.getTextChannelById(notificationChannelId);
        if (channel == null)
            return;

        EmbedBuilder eb = new EmbedBuilder()
                .setTitle(title)
                .setDescription(message)
                .setColor(color)
                .setTimestamp(java.time.Instant.now())
                .setFooter(rawMsg("discord_embed_footer_brand"));

        channel.sendMessageEmbeds(eb.build()).queue();
    }

    public void notifyJoin(Player player) {
        sendNotification(rawMsg("discord_notify_title_join"),
            rawMsg("discord_notify_message_join", "%player%", player.getName()), Color.GREEN);
    }

    public void notifyQuit(Player player) {
        sendNotification(rawMsg("discord_notify_title_quit"),
            rawMsg("discord_notify_message_quit", "%player%", player.getName()), Color.GRAY);
    }

    public void notifyWarning(String playerName, String reason, int count) {
        sendNotification(rawMsg("discord_notify_title_warning"),
            rawMsg("discord_notify_message_warning", "%player%", playerName, "%count%", String.valueOf(count),
                "%reason%", reason),
            Color.ORANGE);
    }

    public void notifyJail(String playerName, String reason) {
        sendNotification(rawMsg("discord_notify_title_jail"),
            rawMsg("discord_notify_message_jail", "%player%", playerName, "%reason%", reason), Color.RED);
    }

    public void notifyUnjail(String playerName) {
        sendNotification(rawMsg("discord_notify_title_unjail"),
            rawMsg("discord_notify_message_unjail", "%player%", playerName), Color.GREEN);
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ===== ロール管理 =====

    /**
     * Discordサーバーに参加した時に未認証ロールを付与
     */
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (unverifiedRoleId == null || unverifiedRoleId.isEmpty())
            return;

        Role unverifiedRole = event.getGuild().getRoleById(unverifiedRoleId);
        if (unverifiedRole != null) {
            event.getGuild().addRoleToMember(event.getMember(), unverifiedRole).queue();
            plugin.getLogger().info(rawMsg("discord_log_unverified_role_granted", "%user%", event.getUser().getName()));
        }
    }

    /**
     * 連携完了時に認証済みロールを付与し、ニックネームを変更
     */
    public void onLinkComplete(long discordId, String minecraftName, Rank rank) {
        if (!enabled || jda == null || guildId == null || guildId.isEmpty())
            return;

        Guild guild = jda.getGuildById(guildId);
        if (guild == null)
            return;

        guild.retrieveMemberById(discordId).queue(member -> {
            if (member == null)
                return;

            // 未認証ロールを削除
            if (unverifiedRoleId != null && !unverifiedRoleId.isEmpty()) {
                Role unverifiedRole = guild.getRoleById(unverifiedRoleId);
                if (unverifiedRole != null) {
                    guild.removeRoleFromMember(member, unverifiedRole).queue();
                }
            }

            // 認証済みロールを付与
            if (verifiedRoleId != null && !verifiedRoleId.isEmpty()) {
                Role verifiedRole = guild.getRoleById(verifiedRoleId);
                if (verifiedRole != null) {
                    guild.addRoleToMember(member, verifiedRole).queue();
                }
            }

            // ニックネーム変更 [階級]MinecraftName
            String nickname = "[" + rank.getId() + "]" + minecraftName;
            if (nickname.length() > 32) {
                nickname = nickname.substring(0, 32);
            }
            member.modifyNickname(nickname).queue(
                    success -> plugin.getLogger().info(rawMsg("discord_log_nickname_changed", "%player%", minecraftName)),
                    error -> plugin.getLogger().warning(
                        rawMsg("discord_log_nickname_change_failed", "%error%", error.getMessage())));

        }, error -> {
        });
    }

    /**
     * 階級変更時にニックネームを更新
     */
    public void updateNickname(long discordId, String minecraftName, IRank rank) {
        if (!enabled || jda == null || guildId == null || guildId.isEmpty())
            return;

        Guild guild = jda.getGuildById(guildId);
        if (guild == null)
            return;

        guild.retrieveMemberById(discordId).queue(member -> {
            if (member == null)
                return;

            String nickname = "[" + rank.getId() + "]" + minecraftName;
            if (nickname.length() > 32) {
                nickname = nickname.substring(0, 32);
            }
            member.modifyNickname(nickname).queue();
        }, error -> {
        });
    }

    /**
     * 連携解除時にロールとニックネームをリセット
     */
    private void handleSettings(SlashCommandInteractionEvent event) {
        // 管理者権限チェック
        if (!isAdmin(event.getMember())) {
            event.reply(rawMsg("discord_admin_only")).setEphemeral(true).queue();
            return;
        }

        String action = event.getOption("action").getAsString();
        String key = event.getOption("key") != null ? event.getOption("key").getAsString() : null;
        String value = event.getOption("value") != null ? event.getOption("value").getAsString() : null;

        if (action.equalsIgnoreCase("get")) {
            // 現在の設定を表示
            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle(rawMsg("discord_embed_settings_title"))
                    .setColor(Color.GRAY)
                    .addField(rawMsg("discord_embed_settings_channel"),
                        plugin.getConfigManager().getDiscordNotificationChannel(), false)
                    .addField(rawMsg("discord_embed_settings_unverified"),
                        plugin.getConfigManager().getDiscordUnverifiedRoleId(), true)
                    .addField(rawMsg("discord_embed_settings_verified"),
                        plugin.getConfigManager().getDiscordVerifiedRoleId(), true)
                    .addField(rawMsg("discord_embed_settings_notify"),
                        plugin.getConfigManager().getDiscordNotificationRoleId(), true)
                    .addField(rawMsg("discord_embed_settings_console"),
                        plugin.getConfigManager().getDiscordConsoleRoleId(), true);

            event.replyEmbeds(eb.build()).setEphemeral(true).queue();

        } else if (action.equalsIgnoreCase("set")) {
            if (key == null || value == null) {
                event.reply(rawMsg("discord_settings_set_requires"))
                        .setEphemeral(true).queue();
                return;
            }

            switch (key.toLowerCase()) {
                case "channel", "notification_channel" -> {
                    plugin.getConfigManager().setDiscordSetting("notification_channel_id", value);
                    event.reply(rawMsg("discord_settings_channel_updated", "%value%", value)).setEphemeral(true).queue();
                }
                case "role_unverified", "unverified" -> {
                    plugin.getConfigManager().setDiscordSetting("unverified_role_id", value);
                    event.reply(rawMsg("discord_settings_unverified_updated", "%value%", value)).setEphemeral(true).queue();
                }
                case "role_verified", "verified" -> {
                    plugin.getConfigManager().setDiscordSetting("verified_role_id", value);
                    event.reply(rawMsg("discord_settings_verified_updated", "%value%", value)).setEphemeral(true).queue();
                }
                case "role_notification", "notification" -> {
                    plugin.getConfigManager().setDiscordSetting("notification_role_id", value);
                    event.reply(rawMsg("discord_settings_notify_updated", "%value%", value)).setEphemeral(true).queue();
                }
                case "role_console", "console" -> {
                    plugin.getConfigManager().setDiscordSetting("console_role_id", value);
                    event.reply(rawMsg("discord_settings_console_updated", "%value%", value)).setEphemeral(true).queue();
                }
                default -> event.reply(rawMsg("discord_settings_unknown_key", "%key%", key)).setEphemeral(true).queue();
            }

            // 設定再読み込み
            plugin.getConfigManager().reload();

        } else if (action.equalsIgnoreCase("role")) {
            if (key == null || value == null) {
                event.reply(rawMsg("discord_settings_role_requires"))
                        .setEphemeral(true).queue();
                return;
            }
            // 階級ロール設定
            plugin.getConfigManager().setDiscordRankRole(key, value);
            event.reply(rawMsg("discord_settings_role_updated", "%rank%", key.toUpperCase(), "%value%", value))
                    .setEphemeral(true).queue();
            plugin.getConfigManager().reload();

        } else {
            event.reply(rawMsg("discord_settings_unknown_action", "%action%", action)).setEphemeral(true).queue();
        }
    }

    private void handlePanel(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) {
            event.reply(rawMsg("discord_admin_only")).setEphemeral(true).queue();
            return;
        }

        String type = event.getOption("type").getAsString();

        if (type.equalsIgnoreCase("auth")) {
            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle(rawMsg("discord_embed_panel_auth_title"))
                    .setDescription(rawMsg("discord_embed_panel_auth_desc"))
                    .setColor(Color.BLUE)
                    .setFooter(rawMsg("discord_embed_panel_auth_footer"));

            event.getChannel().sendMessageEmbeds(eb.build())
                    .setActionRow(Button.primary("auth_start", rawMsg("discord_button_auth_start")))
                    .queue();

            event.reply(rawMsg("discord_panel_auth_installed")).setEphemeral(true).queue();

        } else if (type.equalsIgnoreCase("roles")) {
            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle(rawMsg("discord_embed_panel_roles_title"))
                    .setDescription(rawMsg("discord_embed_panel_roles_desc"))
                    .setColor(Color.CYAN)
                    .addField(rawMsg("discord_embed_panel_roles_field_sync_title"),
                        rawMsg("discord_embed_panel_roles_field_sync_desc"), false)
                    .addField(rawMsg("discord_embed_panel_roles_field_notify_title"),
                        rawMsg("discord_embed_panel_roles_field_notify_desc"), false);

            event.getChannel().sendMessageEmbeds(eb.build())
                    .setActionRow(
                        Button.success("role_sync", rawMsg("discord_button_role_sync")),
                        Button.secondary("role_toggle_notify", rawMsg("discord_button_role_toggle_notify")))
                    .queue();

            event.reply(rawMsg("discord_panel_roles_installed")).setEphemeral(true).queue();
        } else if (type.equalsIgnoreCase("setup")) {
            sendSetupPanel(event);
            event.reply(rawMsg("discord_panel_setup_opened")).setEphemeral(true).queue();

        } else {
            event.reply(rawMsg("discord_panel_unknown_type")).setEphemeral(true).queue();
        }
    }

    // ===== Setup Panel Logic (Phase 8) =====

    /**
     * 設定パネル（メインメニュー）を送信
     */
    private void sendSetupPanel(SlashCommandInteractionEvent event) {
        StringSelectMenu menu = StringSelectMenu.create("setup_category")
            .setPlaceholder(rawMsg("discord_setup_menu_placeholder"))
            .addOption(rawMsg("discord_setup_option_basic_title"), "basic", rawMsg("discord_setup_option_basic_desc"))
            .addOption(rawMsg("discord_setup_option_ranks_title"), "ranks", rawMsg("discord_setup_option_ranks_desc"))
                .build();

        EmbedBuilder eb = new EmbedBuilder()
            .setTitle(rawMsg("discord_embed_setup_title"))
            .setDescription(rawMsg("discord_embed_setup_desc"))
                .setColor(Color.LIGHT_GRAY);

        event.getChannel().sendMessageEmbeds(eb.build())
                .setActionRow(menu)
                .queue();
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String id = event.getComponentId();

        // カテゴリ選択
        if (id.equals("setup_category")) {
            String selected = event.getValues().get(0);

            if (selected.equals("basic")) {
                // 基本設定メニュー
                EntitySelectMenu channelMenu = EntitySelectMenu
                        .create("setup_channel", EntitySelectMenu.SelectTarget.CHANNEL)
                    .setPlaceholder(rawMsg("discord_setup_channel_placeholder"))
                        .setMinValues(0) // 選択解除用
                        .setMaxValues(1)
                        .build();

                EntitySelectMenu notifyRoleMenu = EntitySelectMenu
                        .create("setup_role_notify", EntitySelectMenu.SelectTarget.ROLE)
                    .setPlaceholder(rawMsg("discord_setup_notify_role_placeholder"))
                        .setMinValues(0)
                        .setMaxValues(1)
                        .build();

                EntitySelectMenu verifiedRoleMenu = EntitySelectMenu
                        .create("setup_role_verified", EntitySelectMenu.SelectTarget.ROLE)
                    .setPlaceholder(rawMsg("discord_setup_verified_role_placeholder"))
                        .setMinValues(0)
                        .setMaxValues(1)
                        .build();

                EntitySelectMenu unverifyRoleMenu = EntitySelectMenu
                        .create("setup_role_unverified", EntitySelectMenu.SelectTarget.ROLE)
                    .setPlaceholder(rawMsg("discord_setup_unverified_role_placeholder"))
                        .setMinValues(0)
                        .setMaxValues(1)
                        .build();

                EmbedBuilder eb = new EmbedBuilder()
                    .setTitle(rawMsg("discord_embed_setup_basic_title"))
                    .setDescription(rawMsg("discord_embed_setup_basic_desc"))
                        .setColor(Color.BLUE);

                event.editMessageEmbeds(eb.build())
                        .setComponents(
                                event.getMessage().getActionRows().get(0), // カテゴリメニュー維持
                                net.dv8tion.jda.api.interactions.components.ActionRow.of(channelMenu),
                                net.dv8tion.jda.api.interactions.components.ActionRow.of(notifyRoleMenu),
                                net.dv8tion.jda.api.interactions.components.ActionRow.of(verifiedRoleMenu),
                                net.dv8tion.jda.api.interactions.components.ActionRow.of(unverifyRoleMenu))
                        .queue();

            } else if (selected.equals("ranks")) {
                // 階級選択メニュー
                StringSelectMenu rankMenu = StringSelectMenu.create("setup_rank_select")
                    .setPlaceholder(rawMsg("discord_setup_rank_select_placeholder"))
                    .addOption(rawMsg("discord_rank_label_private"), "PRIVATE")
                    .addOption(rawMsg("discord_rank_label_private_first_class"), "PRIVATE_FIRST_CLASS")
                    .addOption(rawMsg("discord_rank_label_corporal"), "CORPORAL")
                    .addOption(rawMsg("discord_rank_label_sergeant"), "SERGEANT")
                    .addOption(rawMsg("discord_rank_label_sergeant_major"), "SERGEANT_MAJOR")
                    .addOption(rawMsg("discord_rank_label_warrant_officer"), "WARRANT_OFFICER")
                    .addOption(rawMsg("discord_rank_label_lieutenant"), "LIEUTENANT")
                    .addOption(rawMsg("discord_rank_label_first_lieutenant"), "FIRST_LIEUTENANT")
                    .addOption(rawMsg("discord_rank_label_captain"), "CAPTAIN")
                    .addOption(rawMsg("discord_rank_label_major"), "MAJOR")
                    .addOption(rawMsg("discord_rank_label_lieutenant_colonel"), "LIEUTENANT_COLONEL")
                    .addOption(rawMsg("discord_rank_label_colonel"), "COLONEL")
                        .build();

                EmbedBuilder eb = new EmbedBuilder()
                    .setTitle(rawMsg("discord_embed_setup_ranks_title"))
                    .setDescription(rawMsg("discord_embed_setup_ranks_desc"))
                        .setColor(Color.YELLOW);

                event.editMessageEmbeds(eb.build())
                        .setComponents(
                                event.getMessage().getActionRows().get(0),
                                net.dv8tion.jda.api.interactions.components.ActionRow.of(rankMenu))
                        .queue();
            }

            // 階級選択後のロール選択表示
        } else if (id.equals("setup_rank_select")) {
            String rank = event.getValues().get(0);

            EntitySelectMenu roleMenu = EntitySelectMenu
                    .create("setup_rank_role_" + rank, EntitySelectMenu.SelectTarget.ROLE)
                    .setPlaceholder(rawMsg("discord_setup_rank_role_placeholder", "%rank%", rank))
                    .setMinValues(0)
                    .setMaxValues(1)
                    .build();

            EmbedBuilder eb = new EmbedBuilder()
                    .setTitle(rawMsg("discord_embed_setup_rank_title", "%rank%", rank))
                    .setDescription(rawMsg("discord_embed_setup_rank_desc"))
                    .setColor(Color.ORANGE);

            event.editMessageEmbeds(eb.build())
                    .setComponents(
                            event.getMessage().getActionRows().get(0), // カテゴリ
                            event.getMessage().getActionRows().get(1), // 階級選択
                            net.dv8tion.jda.api.interactions.components.ActionRow.of(roleMenu))
                    .queue();
        }
    }

    @Override
    public void onEntitySelectInteraction(EntitySelectInteractionEvent event) {
        String id = event.getComponentId();
        String value = event.getValues().isEmpty() ? "" : event.getValues().get(0).getId();
        String name = event.getValues().isEmpty() ? rawMsg("discord_none") : event.getValues().get(0).getAsMention(); // Channel or Role
                                                                                                    // mention

        if (id.equals("setup_channel")) {
            plugin.getConfigManager().setDiscordSetting("notification_channel_id", value);
            event.reply(rawMsg("discord_setup_channel_updated", "%name%", name)).setEphemeral(true).queue();

        } else if (id.equals("setup_role_notify")) {
            plugin.getConfigManager().setDiscordSetting("notification_role_id", value);
            event.reply(rawMsg("discord_setup_notify_role_updated", "%name%", name)).setEphemeral(true).queue();

        } else if (id.equals("setup_role_verified")) {
            plugin.getConfigManager().setDiscordSetting("verified_role_id", value);
            event.reply(rawMsg("discord_setup_verified_role_updated", "%name%", name)).setEphemeral(true).queue();

        } else if (id.equals("setup_role_unverified")) {
            plugin.getConfigManager().setDiscordSetting("unverified_role_id", value);
            event.reply(rawMsg("discord_setup_unverified_role_updated", "%name%", name)).setEphemeral(true).queue();

        } else if (id.startsWith("setup_rank_role_")) {
            String rankId = id.replace("setup_rank_role_", "");
            plugin.getConfigManager().setDiscordRankRole(rankId, value);
                event.reply(rawMsg("discord_setup_rank_role_updated", "%rank%", rankId, "%name%", name)).setEphemeral(true)
                    .queue();
        }

        // 設定を保存
        plugin.getConfigManager().reload();
    }

    public void onUnlink(long discordId) {
        if (!enabled || jda == null || guildId == null || guildId.isEmpty())
            return;

        Guild guild = jda.getGuildById(guildId);
        if (guild == null)
            return;

        guild.retrieveMemberById(discordId).queue(member -> {
            if (member == null)
                return;

            // 認証済みロールを削除
            if (verifiedRoleId != null && !verifiedRoleId.isEmpty()) {
                Role verifiedRole = guild.getRoleById(verifiedRoleId);
                if (verifiedRole != null) {
                    guild.removeRoleFromMember(member, verifiedRole).queue();
                }
            }

            // 未認証ロールを付与
            if (unverifiedRoleId != null && !unverifiedRoleId.isEmpty()) {
                Role unverifiedRole = guild.getRoleById(unverifiedRoleId);
                if (unverifiedRole != null) {
                    guild.addRoleToMember(member, unverifiedRole).queue();
                }
            }

            // ニックネームをリセット
            member.modifyNickname(null).queue();
        }, error -> {
        });
    }

    private void handleDivision(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) {
            event.reply(rawMsg("discord_admin_only")).setEphemeral(true).queue();
            return;
        }

        String action = event.getOption("action").getAsString();
        String arg1 = event.getOption("arg1") != null ? event.getOption("arg1").getAsString() : null;
        String arg2 = event.getOption("arg2") != null ? event.getOption("arg2").getAsString() : null;

        if (action.equalsIgnoreCase("list")) {
            StringBuilder sb = new StringBuilder();
            for (String div : plugin.getDivisionManager().getAllDivisions()) {
                String display = plugin.getDivisionManager().getDivisionDisplayName(div);
                int count = plugin.getDivisionManager().getDivisionMembers(div).size();
                sb.append(display).append(": ").append(count).append("人\n");
            }
            event.reply(rawMsg("discord_division_list_header") + "\n" + sb).setEphemeral(true).queue();

        } else if (action.equalsIgnoreCase("create")) {
            if (arg1 == null) {
                event.reply(rawMsg("discord_division_name_required")).setEphemeral(true).queue();
                return;
            }
            plugin.getDivisionManager().createDivision(arg1);
            event.reply(rawMsg("discord_division_created", "%division%", arg1)).setEphemeral(true).queue();

        } else if (action.equalsIgnoreCase("add")) {
            // arg1: ユーザー, arg2: 部隊
            if (arg1 == null || arg2 == null) {
                event.reply(rawMsg("discord_division_add_requires"))
                        .setEphemeral(true).queue();
                return;
            }
            // メンションからユーザーID抽出 (<@123456> -> 123456)
            long discordId = parseDiscordId(arg1);
            UUID uuid = plugin.getLinkManager().getMinecraftId(discordId);

            if (uuid == null) {
                event.reply(rawMsg("discord_target_not_linked")).setEphemeral(true).queue();
                return;
            }

            if (!plugin.getDivisionManager().divisionExists(arg2)) {
                event.reply(rawMsg("discord_division_not_found")).setEphemeral(true).queue();
                return;
            }

            plugin.getDivisionManager().setDivision(uuid, arg2);
                event.reply(rawMsg("discord_division_assigned", "%discordId%", String.valueOf(discordId), "%division%", arg2))
                    .setEphemeral(true).queue();

            // 権限やロール更新のために即時反映処理があれば呼ぶ (今回はロール同期ボタン推奨)

        } else if (action.equalsIgnoreCase("remove")) {
            if (arg1 == null) {
                event.reply(rawMsg("discord_division_user_required")).setEphemeral(true).queue();
                return;
            }
            long discordId = parseDiscordId(arg1);
            UUID uuid = plugin.getLinkManager().getMinecraftId(discordId);

            if (uuid == null) {
                event.reply(rawMsg("discord_target_not_linked")).setEphemeral(true).queue();
                return;
            }

            plugin.getDivisionManager().removeDivision(uuid);
                event.reply(rawMsg("discord_division_removed", "%discordId%", String.valueOf(discordId))).setEphemeral(true)
                    .queue();

        } else {
            event.reply(rawMsg("discord_division_unknown_action")).setEphemeral(true).queue();
        }
    }

    private void handleAdminRank(SlashCommandInteractionEvent event, boolean promote) {
        if (!isAdmin(event.getMember())) {
            event.reply(rawMsg("discord_admin_only")).setEphemeral(true).queue();
            return;
        }

        long targetDiscordId = event.getOption("user").getAsUser().getIdLong();
        UUID targetUUID = plugin.getLinkManager().getMinecraftId(targetDiscordId);

        if (targetUUID == null) {
            event.reply(rawMsg("discord_target_not_linked")).setEphemeral(true).queue();
            return;
        }

        // RankManager requires Player object currently, need to fix if offline support
        // RankManager handles offline players via getRankAsync
        plugin.getRankManager().getRankAsync(targetUUID).thenAccept(current -> {
            IRank next = promote ? RankRegistry.getNextRank(current) : RankRegistry.getPreviousRank(current);

            if (next == null) {
                    event.reply(rawMsg("discord_rank_cannot_change_more", "%current%", current.getId())).setEphemeral(true)
                        .queue();
                return;
            }

            plugin.getRankManager().setRankByUUID(targetUUID, Bukkit.getOfflinePlayer(targetUUID).getName(), next)
                    .thenAccept(success -> {
                        if (success) {
                            event.getHook().sendMessage(rawMsg("discord_rank_changed",
                                "%action%", promote ? rawMsg("discord_rank_action_promote")
                                    : rawMsg("discord_rank_action_demote"),
                                "%from%", current.getId(),
                                "%to%", next.getId())).queue();
                            updateNickname(targetDiscordId, Bukkit.getOfflinePlayer(targetUUID).getName(), next);
                        } else {
                            event.getHook().sendMessage(rawMsg("discord_rank_change_failed")).queue();
                        }
                    });
        });

        event.deferReply().queue();
    }

    private void handleSetRank(SlashCommandInteractionEvent event) {
        if (!isAdmin(event.getMember())) {
            event.reply(rawMsg("discord_admin_only")).setEphemeral(true).queue();
            return;
        }

        long targetDiscordId = event.getOption("user").getAsUser().getIdLong();
        String rankId = event.getOption("rank").getAsString();
        UUID targetUUID = plugin.getLinkManager().getMinecraftId(targetDiscordId);

        if (targetUUID == null) {
            event.reply(rawMsg("discord_target_not_linked")).setEphemeral(true).queue();
            return;
        }

        try {
            Rank rank = Rank.valueOf(rankId.toUpperCase());
            plugin.getRankManager().setRankByUUID(targetUUID, Bukkit.getOfflinePlayer(targetUUID).getName(), rank)
                    .thenAccept(success -> {
                        if (success) {
                            event.getHook().sendMessage(rawMsg("discord_setrank_success", "%rank%", rank.getId())).queue();
                            updateNickname(targetDiscordId, Bukkit.getOfflinePlayer(targetUUID).getName(), rank);
                        } else {
                            event.getHook().sendMessage(rawMsg("discord_setrank_failed")).queue();
                        }
                    });
            event.deferReply().queue();
        } catch (IllegalArgumentException e) {
            event.reply(rawMsg("discord_invalid_rank")).setEphemeral(true).queue();
        }
    }

    private void handlePunish(SlashCommandInteractionEvent event, String type) {
        if (!isAdmin(event.getMember())
                && (event.getMember() == null || !event.getMember().hasPermission(Permission.KICK_MEMBERS))) {
            event.reply(rawMsg("discord_no_permission")).setEphemeral(true).queue();
            return;
        }

        long targetDiscordId = event.getOption("user").getAsUser().getIdLong();
        String reason = event.getOption("reason").getAsString();
        UUID targetUUID = plugin.getLinkManager().getMinecraftId(targetDiscordId);

        if (targetUUID == null) {
            event.reply(rawMsg("discord_target_not_linked")).setEphemeral(true).queue();
            return;
        }

        plugin.getTaskScheduler().runGlobal(() -> {
            if (type.equals("kick")) {
                Player target = Bukkit.getPlayer(targetUUID);
                if (target != null) {
                    plugin.getTaskScheduler().runEntity(target, () -> {
                        if (!target.isOnline()) {
                            return;
                        }
                        target.kickPlayer(plugin.getConfigManager().getRawMessage("kick_reason_prefix") + reason);
                    });
                }
            } else if (type.equals("ban")) {
                @SuppressWarnings("deprecation")
                org.bukkit.BanList<org.bukkit.BanEntry<String>> banList = (org.bukkit.BanList<org.bukkit.BanEntry<String>>) Bukkit
                        .getBanList(org.bukkit.BanList.Type.NAME);
                if (banList != null) {
                    @SuppressWarnings("deprecation")
                    String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();
                    banList.addBan(targetName, reason, null, "Console(Discord)");
                }

                Player target = Bukkit.getPlayer(targetUUID);
                if (target != null) {
                    plugin.getTaskScheduler().runEntity(target, () -> {
                        if (!target.isOnline()) {
                            return;
                        }
                        target.kickPlayer(plugin.getConfigManager().getRawMessage("ban_reason_prefix") + reason);
                    });
                }
            }
        });

        event.reply(rawMsg("discord_punish_executed", "%type%", type, "%reason%", reason)).queue();
    }

    // Helper to parse <@12345> style mentions or raw IDs
    private long parseDiscordId(String input) {
        if (input.startsWith("<@") && input.endsWith(">")) {
            input = input.replaceAll("[^0-9]", "");
        }
        try {
            return Long.parseLong(input);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ===== Button Interactions =====

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.equals("auth_start")) {
            long discordId = event.getUser().getIdLong();
            if (plugin.getLinkManager().isLinked(discordId)) {
                event.reply(rawMsg("discord_already_linked")).setEphemeral(true).queue();
                return;
            }

            String code = plugin.getLinkManager().generateLinkCode(discordId);
            event.reply(rawMsg("discord_auth_start_instruction", "%code%", code, "%minutes%", "5"))
                    .setEphemeral(true).queue();

        } else if (id.equals("role_sync")) {
            long discordId = event.getUser().getIdLong();
            UUID uuid = plugin.getLinkManager().getMinecraftId(discordId);

            if (uuid == null) {
                event.reply(rawMsg("discord_target_not_linked_minecraft")).setEphemeral(true).queue();
                return;
            }

            // Sync logic
            event.deferReply(true).queue();

            plugin.getRankManager().getRankAsync(uuid).thenAccept(rank -> {
                String rankRoleId = plugin.getConfigManager().getDiscordRankRoleId(rank.getId());
                String verifiedRoleId = plugin.getConfigManager().getDiscordVerifiedRoleId();

                Guild guild = event.getGuild();
                Member member = event.getMember();

                if (guild != null && member != null) {
                    // 認証済みロールチェック
                    if (verifiedRoleId != null && !verifiedRoleId.isEmpty()) {
                        Role vRole = guild.getRoleById(verifiedRoleId);
                        if (vRole != null && !member.getRoles().contains(vRole)) {
                            guild.addRoleToMember(member, vRole).queue();
                        }
                    }

                    // 階級ロールチェック
                    if (rankRoleId != null && !rankRoleId.isEmpty()) {
                        Role rRole = guild.getRoleById(rankRoleId);
                        if (rRole != null && !member.getRoles().contains(rRole)) {
                            guild.addRoleToMember(member, rRole).queue();
                        }
                    }

                    // ニックネーム更新
                    updateNickname(discordId, Bukkit.getOfflinePlayer(uuid).getName(), rank);

                    event.getHook().sendMessage(rawMsg("discord_role_sync_success")).queue();
                } else {
                    event.getHook().sendMessage(rawMsg("discord_role_sync_failed")).queue();
                }
            });

        } else if (id.equals("role_toggle_notify")) {
            String notifyRoleId = plugin.getConfigManager().getDiscordNotificationRoleId();
            if (notifyRoleId == null || notifyRoleId.isEmpty()) {
                event.reply(rawMsg("discord_notify_role_not_set")).setEphemeral(true).queue();
                return;
            }

            Guild guild = event.getGuild();
            Member member = event.getMember();
            Role notifyRole = guild.getRoleById(notifyRoleId);

            if (notifyRole == null) {
                event.reply(rawMsg("discord_notify_role_not_found")).setEphemeral(true).queue();
                return;
            }

            if (member.getRoles().contains(notifyRole)) {
                guild.removeRoleFromMember(member, notifyRole).queue();
                event.reply(rawMsg("discord_notify_off")).setEphemeral(true).queue();
            } else {
                guild.addRoleToMember(member, notifyRole).queue();
                event.reply(rawMsg("discord_notify_on")).setEphemeral(true).queue();
            }
        }
    }

    private String rawMsg(String key, String... replacements) {
        String message = plugin.getConfigManager().getRawMessage(key);
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            message = message.replace(replacements[index], replacements[index + 1]);
        }
        return message;
    }
}
