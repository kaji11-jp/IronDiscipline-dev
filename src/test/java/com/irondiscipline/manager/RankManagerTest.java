package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.Rank;
import com.irondiscipline.util.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RankManager テスト (LuckPerms非依存版)
 */
class RankManagerTest {

    @Mock
    private IronDiscipline plugin;
    @Mock
    private RankStorageManager rankStorage;
    @Mock
    private ConfigManager configManager;
    @Mock
    private Player player;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private TaskScheduler taskScheduler;

    private RankManager rankManager;
    private AutoCloseable mocks;
    private MockedStatic<Bukkit> bukkitMock;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Bukkit Static Mocks
        bukkitMock = mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);

        ScoreboardManager scoreboardManager = mock(ScoreboardManager.class);
        Scoreboard scoreboard = mock(Scoreboard.class);
        when(scoreboardManager.getMainScoreboard()).thenReturn(scoreboard);
        Team team = mock(Team.class);
        when(scoreboard.getTeam(anyString())).thenReturn(team);
        when(scoreboard.registerNewTeam(anyString())).thenReturn(team);

        bukkitMock.when(Bukkit::getScoreboardManager).thenReturn(scoreboardManager);

        // Basic Plugin Setup
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getTaskScheduler()).thenReturn(taskScheduler);
        when(configManager.getRankMetaKey()).thenReturn("rank");

        // TaskScheduler mocks
        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(0);
            r.run();
            return null;
        }).when(taskScheduler).runGlobal(any(Runnable.class));

        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(1);
            r.run();
            return null;
        }).when(taskScheduler).runEntity(any(Entity.class), any(Runnable.class));

        // Logger mock
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("RankManagerTest"));

        rankManager = new RankManager(plugin, rankStorage);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (bukkitMock != null)
            bukkitMock.close();
        if (mocks != null)
            mocks.close();
    }

    @Test
    void testGetRank_DefaultPrivate() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(rankStorage.getRank(uuid)).thenReturn(CompletableFuture.completedFuture(Rank.PRIVATE));

        // キャッシュにないのでロードが走る
        Rank rank = rankManager.getRank(player);
        assertEquals(Rank.PRIVATE, rank, "デフォルト階級はPRIVATEであるべき");
    }

    @Test
    void testGetRank_Specific() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(rankStorage.getRank(uuid)).thenReturn(CompletableFuture.completedFuture(Rank.MAJOR));

        // 事前にキャッシュロードが必要 (getRankはキャッシュミス時にPRIVATEを返し、非同期ロードをキックする仕様になったため)
        rankManager.loadPlayerCache(uuid);

        Rank rank = rankManager.getRank(player);
        assertEquals(Rank.MAJOR, rank);
    }

    @Test
    void testPromote() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.isOnline()).thenReturn(true);
        when(configManager.getMessage(anyString(), anyString(), anyString())).thenReturn("Message");

        // PRIVATEから開始 (事前にキャッシュロード)
        when(rankStorage.getRank(uuid)).thenReturn(CompletableFuture.completedFuture(Rank.PRIVATE));
        rankManager.loadPlayerCache(uuid);

        when(rankStorage.setRank(eq(uuid), anyString(), eq(Rank.PRIVATE_FIRST_CLASS)))
                .thenReturn(CompletableFuture.completedFuture(true));

        Rank newRank = rankManager.promote(player).join();

        assertNotNull(newRank);
        assertEquals(Rank.PRIVATE_FIRST_CLASS, newRank);

        // ストレージへの保存が呼ばれたことを確認
        verify(rankStorage).setRank(eq(uuid), anyString(), eq(Rank.PRIVATE_FIRST_CLASS));
    }

    @Test
    void testDemote() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.isOnline()).thenReturn(true);
        when(configManager.getMessage(anyString(), anyString(), anyString())).thenReturn("Message");

        // CORPORALから開始
        when(rankStorage.getRank(uuid)).thenReturn(CompletableFuture.completedFuture(Rank.CORPORAL));
        rankManager.loadPlayerCache(uuid);

        when(rankStorage.setRank(eq(uuid), anyString(), eq(Rank.PRIVATE_FIRST_CLASS)))
                .thenReturn(CompletableFuture.completedFuture(true));

        Rank newRank = rankManager.demote(player).join();

        assertNotNull(newRank);
        assertEquals(Rank.PRIVATE_FIRST_CLASS, newRank);
    }

    @Test
    void testPromote_MaxRank() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        // COMMANDERは最高階級
        when(rankStorage.getRank(uuid)).thenReturn(CompletableFuture.completedFuture(Rank.COMMANDER));
        rankManager.loadPlayerCache(uuid);

        Rank newRank = rankManager.promote(player).join();

        assertNull(newRank, "最高階級からは昇進できないはず");
        verify(rankStorage, never()).setRank(any(), anyString(), any());
    }

    @Test
    void testDemote_MinRank() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        // PRIVATEは最低階級
        when(rankStorage.getRank(uuid)).thenReturn(CompletableFuture.completedFuture(Rank.PRIVATE));
        rankManager.loadPlayerCache(uuid);

        Rank newRank = rankManager.demote(player).join();

        assertNull(newRank, "最低階級からは降格できないはず");
        verify(rankStorage, never()).setRank(any(), anyString(), any());
    }

    @Test
    void testSetRankDirectly() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.isOnline()).thenReturn(true);
        
        when(rankStorage.setRank(eq(uuid), anyString(), eq(Rank.COLONEL)))
                .thenReturn(CompletableFuture.completedFuture(true));

        Rank result = rankManager.setRank(player, Rank.COLONEL).join();

        assertEquals(Rank.COLONEL, result);
        verify(rankStorage).setRank(eq(uuid), anyString(), eq(Rank.COLONEL));
    }

    @Test
    void testMultiplePlayersIndependently() {
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        
        Player player2 = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid1);
        when(player2.getUniqueId()).thenReturn(uuid2);
        
        when(rankStorage.getRank(uuid1)).thenReturn(CompletableFuture.completedFuture(Rank.PRIVATE));
        when(rankStorage.getRank(uuid2)).thenReturn(CompletableFuture.completedFuture(Rank.MAJOR));
        
        rankManager.loadPlayerCache(uuid1);
        rankManager.loadPlayerCache(uuid2);
        
        assertEquals(Rank.PRIVATE, rankManager.getRank(player));
        assertEquals(Rank.MAJOR, rankManager.getRank(player2));
    }

    @Test
    void testPromoteThroughMultipleLevels() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.isOnline()).thenReturn(true);
        when(configManager.getMessage(anyString(), anyString(), anyString())).thenReturn("Message");
        
        // Start at PRIVATE
        when(rankStorage.getRank(uuid)).thenReturn(CompletableFuture.completedFuture(Rank.PRIVATE));
        rankManager.loadPlayerCache(uuid);
        
        // Promote to PRIVATE_FIRST_CLASS
        when(rankStorage.setRank(eq(uuid), anyString(), eq(Rank.PRIVATE_FIRST_CLASS)))
                .thenReturn(CompletableFuture.completedFuture(true));
        Rank rank1 = rankManager.promote(player).join();
        assertEquals(Rank.PRIVATE_FIRST_CLASS, rank1);
        
        // Promote to CORPORAL
        when(rankStorage.setRank(eq(uuid), anyString(), eq(Rank.CORPORAL)))
                .thenReturn(CompletableFuture.completedFuture(true));
        Rank rank2 = rankManager.promote(player).join();
        assertEquals(Rank.CORPORAL, rank2);
    }

    @Test
    void testRequiresPTS_LowRanks() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        
        // CORPORAL requires PTS (weight 20 <= 25)
        when(rankStorage.getRank(uuid)).thenReturn(CompletableFuture.completedFuture(Rank.CORPORAL));
        rankManager.loadPlayerCache(uuid);
        
        assertTrue(rankManager.requiresPTS(player));
    }

    @Test
    void testRequiresPTS_HighRanks() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        
        // SERGEANT does not require PTS (weight 30 > 25)
        when(rankStorage.getRank(uuid)).thenReturn(CompletableFuture.completedFuture(Rank.SERGEANT));
        rankManager.loadPlayerCache(uuid);
        
        assertFalse(rankManager.requiresPTS(player));
    }

    @Test
    void testCacheInvalidation() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        
        // Load initial rank
        when(rankStorage.getRank(uuid)).thenReturn(CompletableFuture.completedFuture(Rank.PRIVATE));
        rankManager.loadPlayerCache(uuid);
        assertEquals(Rank.PRIVATE, rankManager.getRank(player));
        
        // Clear cache
        rankManager.clearCache(uuid);
        
        // Should reload from storage
        when(rankStorage.getRank(uuid)).thenReturn(CompletableFuture.completedFuture(Rank.CAPTAIN));
        rankManager.loadPlayerCache(uuid);
        assertEquals(Rank.CAPTAIN, rankManager.getRank(player));
    }
}
