package com.irondiscipline;

import com.irondiscipline.command.*;
import com.irondiscipline.listener.*;
import com.irondiscipline.listener.AsyncPlayerPreLoginListener;
import com.irondiscipline.listener.JailListener;
import com.irondiscipline.manager.*;
import com.irondiscipline.util.RankUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.File;
import java.util.logging.Level;

/**
 * IronDiscipline-dev (鉄の規律)
 * 軍事RP向け規律システムプラグイン
 * LuckPerms非依存版
 */
public class IronDiscipline extends JavaPlugin {

    private static IronDiscipline instance;

    // Managers
    private ConfigManager configManager;
    private StorageManager storageManager;
    private RankStorageManager rankStorageManager;
    private RankManager rankManager;
    private PTSManager ptsManager;
    private JailManager jailManager;
    private RadioManager radioManager;
    private ExamManager examManager;
    private DivisionManager divisionManager;
    private WarningManager warningManager;
    private PlaytimeManager playtimeManager;
    private ExamQuestionManager examQuestionManager;
    private LinkManager linkManager;
    private DiscordManager discordManager;
    private AutoPromotionManager autoPromotionManager;

    // Utilities
    private com.irondiscipline.util.TaskScheduler taskScheduler;
    private RankUtil rankUtil;

    // Database connection
    private Connection dbConnection;

    @Override
    public void onEnable() {
        instance = this;

        // バナー表示
        logBanner();

        // 設定読み込み
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        // データベース接続
        if (!initDatabase()) {
            getLogger().severe("データベース接続に失敗しました！");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        // Manager初期化
        initializeManagers();

        // リスナー登録
        registerListeners();

        // コマンド登録
        registerCommands();

        getLogger().info(
                configManager.getRawMessage("log_startup_success").replace("%version%", getDescription().getVersion()));
    }

    @Override
    public void onDisable() {
        // タスク停止
        if (ptsManager != null) {
            ptsManager.shutdown();
        }
        if (autoPromotionManager != null) {
            autoPromotionManager.shutdown();
        }

        // データ保存
        if (storageManager != null) {
            storageManager.shutdown();
        }
        if (rankStorageManager != null) {
            rankStorageManager.shutdown();
        }
        if (jailManager != null) {
            jailManager.saveAll();
        }
        if (playtimeManager != null) {
            playtimeManager.saveAll();
        }
        if (discordManager != null) {
            discordManager.shutdown();
        }
        if (linkManager != null) {
            linkManager.shutdown();
        }

        // DB接続クローズ
        if (dbConnection != null) {
            try {
                dbConnection.close();
            } catch (SQLException ignored) {
            }
        }

        getLogger().info(configManager.getRawMessage("log_shutdown_complete"));
    }

    private void logBanner() {
        getLogger().info("========================================");
        getLogger().info("   鉄の規律 (IronDiscipline-dev)");
        getLogger().info("   Military RP Discipline System");
        getLogger().info("   LuckPerms非依存版");
        getLogger().info("   Version: " + getDescription().getVersion());
        getLogger().info("========================================");
    }

    private boolean initDatabase() {
        try {
            String dbType = configManager.getDatabaseType();
            if ("mysql".equalsIgnoreCase(dbType)) {
                initMySQL();
            } else {
                initH2();
            }
            getLogger().info("データベース接続成功 [" + dbType.toUpperCase() + "]");
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "データベース接続失敗", e);
            return false;
        }
    }

    private void initH2() throws SQLException {
        try {
            Class.forName("com.irondiscipline.lib.h2.Driver");
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("org.h2.Driver");
            } catch (ClassNotFoundException e2) {
                throw new SQLException("H2ドライバーが見つかりません", e2);
            }
        }

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        File dbFile = new File(getDataFolder(), "irondiscipline");
        String url = "jdbc:h2:" + dbFile.getAbsolutePath() + ";MODE=MySQL";
        dbConnection = DriverManager.getConnection(url, "sa", "");
    }

    private void initMySQL() throws SQLException {
        String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true",
                configManager.getMySQLHost(),
                configManager.getMySQLPort(),
                configManager.getMySQLDatabase());
        dbConnection = DriverManager.getConnection(url,
                configManager.getMySQLUsername(),
                configManager.getMySQLPassword());
    }

    private void initializeManagers() {
        this.taskScheduler = new com.irondiscipline.util.TaskScheduler(this);
        this.rankUtil = new RankUtil(this);

        this.storageManager = new StorageManager(this);
        this.rankStorageManager = new RankStorageManager(this, dbConnection);
        this.rankManager = new RankManager(this, rankStorageManager);
        this.ptsManager = new PTSManager(this);
        this.jailManager = new JailManager(this);
        this.radioManager = new RadioManager(this);
        this.examManager = new ExamManager(this);
        this.divisionManager = new DivisionManager(this);
        this.warningManager = new WarningManager(this);
        this.playtimeManager = new PlaytimeManager(this);
        this.examQuestionManager = new ExamQuestionManager(this);
        this.linkManager = new LinkManager(this);
        this.discordManager = new DiscordManager(this);
        this.autoPromotionManager = new AutoPromotionManager(this, rankManager);

        // Start auto-promotion task
        this.autoPromotionManager.startTask();

        // Discord Bot 起動
        initDiscord();

        getLogger().info(configManager.getRawMessage("log_managers_initialized"));
    }

    private void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new AsyncPlayerPreLoginListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JailListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinQuitListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GestureListener(this), this);

        getLogger().info(configManager.getRawMessage("log_listeners_registered"));
    }

    private void registerCommands() {
        getCommand("promote").setExecutor(new PromoteCommand(this));
        getCommand("demote").setExecutor(new DemoteCommand(this));
        getCommand("grant").setExecutor(new GrantCommand(this));
        getCommand("radio").setExecutor(new RadioCommand(this));
        getCommand("radiobroadcast").setExecutor(new RadioBroadcastCommand(this));
        getCommand("jail").setExecutor(new JailCommand(this));
        getCommand("unjail").setExecutor(new UnjailCommand(this));
        getCommand("setjail").setExecutor(new SetJailCommand(this));
        getCommand("killlog").setExecutor(new KillLogCommand(this));
        getCommand("irondiscipline").setExecutor(new IronDisciplineCommand(this));

        // irondevコマンド（移行用）
        getCommand("irondev").setExecutor(new IronDevCommand(this));

        ExamCommand examCmd = new ExamCommand(this);
        getCommand("exam").setExecutor(examCmd);
        getCommand("exam").setTabCompleter(examCmd);

        // Phase 3 commands
        WarnCommand warnCmd = new WarnCommand(this);
        getCommand("warn").setExecutor(warnCmd);
        getCommand("warn").setTabCompleter(warnCmd);
        getCommand("warnings").setExecutor(warnCmd);
        getCommand("warnings").setTabCompleter(warnCmd);
        getCommand("clearwarnings").setExecutor(warnCmd);
        getCommand("unwarn").setExecutor(warnCmd);

        PlaytimeCommand playtimeCmd = new PlaytimeCommand(this);
        getCommand("playtime").setExecutor(playtimeCmd);
        getCommand("playtime").setTabCompleter(playtimeCmd);

        DivisionCommand divCmd = new DivisionCommand(this);
        getCommand("division").setExecutor(divCmd);
        getCommand("division").setTabCompleter(divCmd);

        // Phase 4 - Discord連携
        getCommand("link").setExecutor(new LinkCommand(this));

        getLogger().info(configManager.getRawMessage("log_commands_registered"));
    }

    private void initDiscord() {
        String botToken = configManager.getDiscordBotToken();
        String channelId = configManager.getDiscordNotificationChannel();
        String guildId = configManager.getDiscordGuildId();
        String unverifiedRoleId = configManager.getDiscordUnverifiedRoleId();
        String verifiedRoleId = configManager.getDiscordVerifiedRoleId();
        String adminRoleId = configManager.getDiscordAdminRoleId();

        if (botToken != null && !botToken.isEmpty() && configManager.isDiscordEnabled()) {
            discordManager.start(botToken, channelId, guildId, unverifiedRoleId, verifiedRoleId, adminRoleId);
        } else {
            getLogger().info(configManager.getRawMessage("log_discord_disabled"));
        }
    }

    /**
     * 設定をリロードする
     */
    public void reload() {
        reloadConfig();
        configManager.reload();
        getLogger().info(configManager.getRawMessage("log_reload_complete"));
    }

    // ===== Getters =====

    public static IronDiscipline getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public StorageManager getStorageManager() {
        return storageManager;
    }

    public RankStorageManager getRankStorageManager() {
        return rankStorageManager;
    }

    public RankManager getRankManager() {
        return rankManager;
    }

    public PTSManager getPTSManager() {
        return ptsManager;
    }

    public JailManager getJailManager() {
        return jailManager;
    }

    public RadioManager getRadioManager() {
        return radioManager;
    }

    public ExamManager getExamManager() {
        return examManager;
    }

    public RankUtil getRankUtil() {
        return rankUtil;
    }

    public DivisionManager getDivisionManager() {
        return divisionManager;
    }

    public WarningManager getWarningManager() {
        return warningManager;
    }

    public PlaytimeManager getPlaytimeManager() {
        return playtimeManager;
    }

    public ExamQuestionManager getExamQuestionManager() {
        return examQuestionManager;
    }

    public LinkManager getLinkManager() {
        return linkManager;
    }

    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    public AutoPromotionManager getAutoPromotionManager() {
        return autoPromotionManager;
    }

    public com.irondiscipline.util.TaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    public Connection getDbConnection() {
        return dbConnection;
    }
}
