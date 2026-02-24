package xyz.irondiscipline.manager;

import xyz.irondiscipline.IronDiscipline;
import xyz.irondiscipline.model.Rank;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * 階級データストレージマネージャー
 * LuckPermsを使用せず、独自DBに階級データを保存
 */
public class RankStorageManager {

    private final IronDiscipline plugin;
    private final Connection connection;
    private final ExecutorService dbExecutor = Executors.newCachedThreadPool();

    public RankStorageManager(IronDiscipline plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
        initializeTables();
    }

    /**
     * テーブル初期化
     */
    private void initializeTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                        CREATE TABLE IF NOT EXISTS player_ranks (
                            player_id VARCHAR(36) PRIMARY KEY,
                            player_name VARCHAR(32),
                            rank_id VARCHAR(32) NOT NULL DEFAULT 'PRIVATE',
                            updated_at BIGINT NOT NULL
                        )
                    """);
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_player_ranks_rank ON player_ranks(rank_id)");
            plugin.getLogger().info("階級テーブル初期化完了");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "階級テーブル初期化失敗", e);
        }
    }

    /**
     * 階級をDBから取得
     */
    public CompletableFuture<Rank> getRank(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql = "SELECT rank_id FROM player_ranks WHERE player_id = ?";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, playerId.toString());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return Rank.fromId(rs.getString("rank_id"));
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "階級取得失敗: " + playerId, e);
            }
            // デフォルト階級
            return Rank.PRIVATE;
        }, dbExecutor);
    }

    /**
     * 階級を設定
     */
    public CompletableFuture<Boolean> setRank(UUID playerId, String playerName, Rank rank) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql;
                if (isMySql()) {
                    sql = """
                                INSERT INTO player_ranks (player_id, player_name, rank_id, updated_at)
                                VALUES (?, ?, ?, ?)
                                ON DUPLICATE KEY UPDATE
                                player_name = VALUES(player_name),
                                rank_id = VALUES(rank_id),
                                updated_at = VALUES(updated_at)
                            """;
                } else {
                    sql = """
                                MERGE INTO player_ranks (player_id, player_name, rank_id, updated_at)
                                KEY (player_id)
                                VALUES (?, ?, ?, ?)
                            """;
                }

                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, playerId.toString());
                    ps.setString(2, playerName);
                    ps.setString(3, rank.getId());
                    ps.setLong(4, System.currentTimeMillis());
                    ps.executeUpdate();
                    return true;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "階級設定失敗: " + playerId, e);
                return false;
            }
        }, dbExecutor);
    }

    /**
     * 全階級データを取得（移行用）
     */
    public CompletableFuture<Map<UUID, Rank>> getAllRanks() {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, Rank> ranks = new HashMap<>();
            try {
                String sql = "SELECT player_id, rank_id FROM player_ranks";
                try (PreparedStatement ps = connection.prepareStatement(sql);
                        ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        try {
                            UUID playerId = UUID.fromString(rs.getString("player_id"));
                            Rank rank = Rank.fromId(rs.getString("rank_id"));
                            ranks.put(playerId, rank);
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "全階級データ取得失敗", e);
            }
            return ranks;
        }, dbExecutor);
    }

    /**
     * MySQL使用かどうか
     */
    private boolean isMySql() {
        return "mysql".equalsIgnoreCase(plugin.getConfigManager().getDatabaseType());
    }

    /**
     * シャットダウン
     */
    public void shutdown() {
        dbExecutor.shutdown();
    }
}
