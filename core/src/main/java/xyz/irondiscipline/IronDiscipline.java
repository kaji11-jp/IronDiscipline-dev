package xyz.irondiscipline;

import xyz.irondiscipline.api.provider.*;
import xyz.irondiscipline.command.*;
import xyz.irondiscipline.listener.*;
import xyz.irondiscipline.manager.*;
import xyz.irondiscipline.util.RankUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * IronDiscipline-dev (鉄の規律)
 * 軍事RP向け規律システムプラグイン
 * Folia専用版（LuckPerms非依存）
 */
public class IronDiscipline extends JavaPlugin implements IStorageProvider {

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
    private WebDashboardManager webDashboardManager;
    private AddonManager addonManager;

    // Utilities
    private xyz.irondiscipline.util.TaskScheduler taskScheduler;
    private RankUtil rankUtil;

    // Database connection
    private Connection dbConnection;
    private ExecutorService sharedDbExecutor;

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

        // APIサービス登録
        registerApiServices();

        // リスナー登録
        registerListeners();

        // コマンド登録
        registerCommands();

        getLogger().info(
                configManager.getRawMessage("log_startup_success").replace("%version%", getDescription().getVersion()));
    }

    @Override
    public void onDisable() {
        // APIサービス登録解除
        getServer().getServicesManager().unregisterAll(this);

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
        if (sharedDbExecutor != null) {
            sharedDbExecutor.shutdown();
        }
        if (jailManager != null) {
            jailManager.saveAll();
        }
        if (playtimeManager != null) {
            playtimeManager.saveAll();
        }
        if (webDashboardManager != null) {
            webDashboardManager.shutdown();
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
        getLogger().info("   Folia専用版");
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
            Class.forName("xyz.irondiscipline.lib.h2.Driver");
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
        this.taskScheduler = new xyz.irondiscipline.util.TaskScheduler(this);
        this.rankUtil = new RankUtil(this);
        this.sharedDbExecutor = Executors.newSingleThreadExecutor();

        this.storageManager = new StorageManager(this, dbConnection, sharedDbExecutor);
        this.rankStorageManager = new RankStorageManager(this, dbConnection, sharedDbExecutor);
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
        this.addonManager = new AddonManager(this);

        // Start auto-promotion task
        this.autoPromotionManager.startTask();

        // Discord Bot 起動
        initDiscord();

        // Web Dashboard 起動
        initWebDashboard();

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

    private void initWebDashboard() {
        if (getConfig().getBoolean("dashboard.enabled", false)) {
            this.webDashboardManager = new WebDashboardManager(this);
            webDashboardManager.start();
        } else {
            getLogger().info("Web Dashboard is disabled. Set dashboard.enabled=true in config.yml to enable.");
        }
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

    /**
     * API プロバイダを Bukkit ServicesManager に登録する。
     * アドオンプラグインは ServicesManager 経由で各プロバイダにアクセスできる。
     */
    private void registerApiServices() {
        ServicesManager sm = getServer().getServicesManager();

        sm.register(IRankProvider.class, rankManager, this, ServicePriority.Normal);
        sm.register(IStorageProvider.class, this, this, ServicePriority.Normal);
        sm.register(IJailProvider.class, jailManager, this, ServicePriority.Normal);
        sm.register(IDivisionProvider.class, divisionManager, this, ServicePriority.Normal);
        sm.register(IKillLogProvider.class, storageManager, this, ServicePriority.Normal);

        getLogger().info("API services registered to ServicesManager");
    }

    // ===== IStorageProvider 実装 =====

    @Override
    public Connection getConnection() {
        return dbConnection;
    }

    @Override
    public ExecutorService getDbExecutor() {
        return sharedDbExecutor;
    }

    @Override
    public String getDatabaseType() {
        return configManager.getDatabaseType();
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

    public WebDashboardManager getWebDashboardManager() {
        return webDashboardManager;
    }

    public AutoPromotionManager getAutoPromotionManager() {
        return autoPromotionManager;
    }

    public AddonManager getAddonManager() {
        return addonManager;
    }

    public xyz.irondiscipline.util.TaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    public Connection getDbConnection() {
        return dbConnection;
    }
}
