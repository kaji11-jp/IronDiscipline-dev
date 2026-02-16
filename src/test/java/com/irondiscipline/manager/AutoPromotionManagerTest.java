package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.Rank;
import com.irondiscipline.util.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AutoPromotionManager テスト
 * 自動昇進システムのテスト
 */
class AutoPromotionManagerTest {

    @Mock
    private IronDiscipline plugin;
    @Mock
    private ConfigManager configManager;
    @Mock
    private RankManager rankManager;
    @Mock
    private TaskScheduler taskScheduler;
    @Mock
    private Player player;
    @Mock
    private ScheduledTask scheduledTask;

    private AutoPromotionManager autoPromotionManager;
    private AutoCloseable mocks;
    private MockedStatic<Bukkit> bukkitMock;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        bukkitMock = mockStatic(Bukkit.class);

        playerId = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(playerId);
        when(player.getName()).thenReturn("TestPlayer");

        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getRankManager()).thenReturn(rankManager);
        when(plugin.getTaskScheduler()).thenReturn(taskScheduler);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("AutoPromotionManagerTest"));

        when(rankManager.getRank(player)).thenReturn(Rank.PRIVATE);

        when(configManager.getMessage(anyString(), anyString(), anyString())).thenReturn("Promotion Message");
        when(configManager.isTimeBasedPromotionEnabled()).thenReturn(true);
        when(configManager.getTimeBasedPromotionInterval()).thenReturn(60);

        when(taskScheduler.runGlobalTimer(any(Runnable.class), anyLong(), anyLong()))
                .thenAnswer(invocation -> {
                    Runnable r = invocation.getArgument(0);
                    r.run(); // Execute immediately for testing
                    return scheduledTask;
                });

        bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.singletonList(player));

        autoPromotionManager = new AutoPromotionManager(plugin, rankManager);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (bukkitMock != null) bukkitMock.close();
        if (mocks != null) mocks.close();
    }

    @Test
    void testStartTask_Enabled() {
        when(configManager.isTimeBasedPromotionEnabled()).thenReturn(true);
        when(configManager.getTimeBasedPromotionInterval()).thenReturn(120);

        autoPromotionManager.startTask();

        // Should have started a timer task
        verify(taskScheduler).runGlobalTimer(any(Runnable.class), eq(120L * 20), eq(120L * 20));
    }

    @Test
    void testStartTask_Disabled() {
        when(configManager.isTimeBasedPromotionEnabled()).thenReturn(false);

        autoPromotionManager.startTask();

        // Should not start task
        verify(taskScheduler, never()).runGlobalTimer(any(Runnable.class), anyLong(), anyLong());
    }

    @Test
    void testStopTask() {
        autoPromotionManager.startTask();
        autoPromotionManager.stopTask();

        verify(scheduledTask).cancel();
    }

    @Test
    void testShutdown() {
        autoPromotionManager.startTask();
        autoPromotionManager.shutdown();

        verify(scheduledTask).cancel();
    }

    @Test
    void testAutoPromotion_SufficientPlaytime() {
        // Setup: Player is PRIVATE with sufficient playtime for PRIVATE_FIRST_CLASS
        when(rankManager.getRank(player)).thenReturn(Rank.PRIVATE);
        when(configManager.getServerPlaytimeRequirement("PRIVATE_FIRST_CLASS")).thenReturn(60); // 60 minutes required
        
        // Player has 90 minutes of playtime (90 * 1200 = 108000 ticks)
        when(player.getStatistic(Statistic.PLAY_ONE_MINUTE)).thenReturn(108000);
        
        when(rankManager.promote(player)).thenReturn(CompletableFuture.completedFuture(Rank.PRIVATE_FIRST_CLASS));

        // Manually trigger check (simulate timer firing)
        autoPromotionManager.startTask();
        
        // Verify promotion was attempted
        verify(rankManager, timeout(1000)).promote(player);
    }

    @Test
    void testAutoPromotion_InsufficientPlaytime() {
        // Setup: Player is PRIVATE without sufficient playtime
        when(rankManager.getRank(player)).thenReturn(Rank.PRIVATE);
        when(configManager.getServerPlaytimeRequirement("PRIVATE_FIRST_CLASS")).thenReturn(100); // 100 minutes required
        
        // Player has only 30 minutes (30 * 1200 = 36000 ticks)
        when(player.getStatistic(Statistic.PLAY_ONE_MINUTE)).thenReturn(36000);

        autoPromotionManager.startTask();
        
        // Should not promote
        verify(rankManager, never()).promote(player);
    }

    @Test
    void testAutoPromotion_MaxRank() {
        // Commander is max rank - should not attempt promotion
        when(rankManager.getRank(player)).thenReturn(Rank.COMMANDER);
        when(player.getStatistic(Statistic.PLAY_ONE_MINUTE)).thenReturn(1000000);

        autoPromotionManager.startTask();

        verify(rankManager, never()).promote(player);
        verify(configManager, never()).getServerPlaytimeRequirement(anyString());
    }

    @Test
    void testAutoPromotion_NoRequirement() {
        // If the next rank has no playtime requirement (returns 0), skip
        when(rankManager.getRank(player)).thenReturn(Rank.CORPORAL);
        when(configManager.getServerPlaytimeRequirement("SERGEANT")).thenReturn(0); // No auto-promotion
        when(player.getStatistic(Statistic.PLAY_ONE_MINUTE)).thenReturn(100000);

        autoPromotionManager.startTask();

        verify(rankManager, never()).promote(player);
    }

    @Test
    void testAutoPromotion_NegativeRequirement() {
        // Negative values should also disable auto-promotion
        when(rankManager.getRank(player)).thenReturn(Rank.SERGEANT);
        when(configManager.getServerPlaytimeRequirement("LIEUTENANT")).thenReturn(-1);
        when(player.getStatistic(Statistic.PLAY_ONE_MINUTE)).thenReturn(200000);

        autoPromotionManager.startTask();

        verify(rankManager, never()).promote(player);
    }

    @Test
    void testMultiplePlayersChecked() {
        Player player2 = mock(Player.class);
        UUID player2Id = UUID.randomUUID();
        when(player2.getUniqueId()).thenReturn(player2Id);
        when(player2.getName()).thenReturn("Player2");
        when(player2.getStatistic(Statistic.PLAY_ONE_MINUTE)).thenReturn(50000);

        bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(java.util.Arrays.asList(player, player2));

        when(rankManager.getRank(player)).thenReturn(Rank.PRIVATE);
        when(rankManager.getRank(player2)).thenReturn(Rank.PRIVATE);
        
        when(configManager.getServerPlaytimeRequirement("PRIVATE_FIRST_CLASS")).thenReturn(10); // Low requirement
        when(player.getStatistic(Statistic.PLAY_ONE_MINUTE)).thenReturn(20000); // Sufficient
        when(player2.getStatistic(Statistic.PLAY_ONE_MINUTE)).thenReturn(20000); // Sufficient

        when(rankManager.promote(any())).thenReturn(CompletableFuture.completedFuture(Rank.PRIVATE_FIRST_CLASS));

        autoPromotionManager.startTask();

        // Both players should be checked
        verify(rankManager, timeout(1000).atLeast(2)).getRank(any(Player.class));
    }

    @Test
    void testRestartTask() {
        autoPromotionManager.startTask();
        verify(taskScheduler, times(1)).runGlobalTimer(any(Runnable.class), anyLong(), anyLong());

        // Restart should cancel old task and start new one
        autoPromotionManager.startTask();
        
        verify(scheduledTask).cancel();
        verify(taskScheduler, times(2)).runGlobalTimer(any(Runnable.class), anyLong(), anyLong());
    }
}
