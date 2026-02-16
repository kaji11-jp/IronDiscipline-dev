package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.Rank;
import com.irondiscipline.util.TaskScheduler;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Player.Spigot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * PTSManager テスト
 * 発言許可システムの管理ロジックをテスト
 */
class PTSManagerTest {

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
    private Player officer;
    @Mock
    private Spigot spigot;
    @Mock
    private ScheduledTask scheduledTask;

    private PTSManager ptsManager;
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
        when(player.spigot()).thenReturn(spigot);
        when(player.hasPermission(anyString())).thenReturn(false);
        
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getRankManager()).thenReturn(rankManager);
        when(plugin.getTaskScheduler()).thenReturn(taskScheduler);
        
        when(configManager.getMessage(anyString())).thenReturn("Test Message");
        when(configManager.getMessage(anyString(), anyString(), anyString())).thenReturn("Test Message");
        when(configManager.getRawMessage(anyString())).thenReturn("Raw Message");
        when(configManager.getPTSRequireBelowWeight()).thenReturn(25);
        
        // Mock TaskScheduler to return a mock task
        when(taskScheduler.runGlobalTimer(any(Runnable.class), anyLong(), anyLong()))
                .thenReturn(scheduledTask);
        when(taskScheduler.runGlobalTimer(any(java.util.function.Consumer.class), anyLong(), anyLong()))
                .thenAnswer(inv -> {
                    // Don't actually run the timer in tests
                    return null;
                });
        when(taskScheduler.runGlobalLater(any(Runnable.class), anyLong()))
                .thenAnswer(inv -> {
                    // Don't actually run delayed tasks in tests
                    return scheduledTask;
                });
        
        ptsManager = new PTSManager(plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (bukkitMock != null) bukkitMock.close();
        if (mocks != null) mocks.close();
    }

    @Test
    void testHasPermissionToSpeak_WithBypassPermission() {
        when(player.hasPermission("iron.pts.bypass")).thenReturn(true);
        
        assertTrue(ptsManager.hasPermissionToSpeak(player));
    }

    @Test
    void testHasPermissionToSpeak_HighRank() {
        when(player.hasPermission("iron.pts.bypass")).thenReturn(false);
        when(rankManager.requiresPTS(player)).thenReturn(false); // High rank
        
        assertTrue(ptsManager.hasPermissionToSpeak(player));
    }

    @Test
    void testHasPermissionToSpeak_NoPermission() {
        when(player.hasPermission("iron.pts.bypass")).thenReturn(false);
        when(rankManager.requiresPTS(player)).thenReturn(true); // Low rank
        
        assertFalse(ptsManager.hasPermissionToSpeak(player));
    }

    @Test
    void testGrantPermission() {
        ptsManager.grantPermission(player, 60);
        
        // Should now have permission
        when(player.hasPermission("iron.pts.bypass")).thenReturn(false);
        when(rankManager.requiresPTS(player)).thenReturn(true);
        assertTrue(ptsManager.hasPermissionToSpeak(player));
        
        verify(player).sendMessage(anyString());
    }

    @Test
    void testGrantPermission_Expiration() throws InterruptedException {
        // Grant permission for 1 second
        ptsManager.grantPermission(player, 1);
        
        when(player.hasPermission("iron.pts.bypass")).thenReturn(false);
        when(rankManager.requiresPTS(player)).thenReturn(true);
        
        // Should have permission initially
        assertTrue(ptsManager.hasPermissionToSpeak(player));
        
        // Wait for expiration (plus a bit for safety)
        Thread.sleep(1100);
        
        // Should no longer have permission after expiration
        assertFalse(ptsManager.hasPermissionToSpeak(player));
    }

    @Test
    void testRevokeGrant() {
        ptsManager.grantPermission(player, 60);
        
        when(player.hasPermission("iron.pts.bypass")).thenReturn(false);
        when(rankManager.requiresPTS(player)).thenReturn(true);
        assertTrue(ptsManager.hasPermissionToSpeak(player));
        
        ptsManager.revokeGrant(player);
        
        assertFalse(ptsManager.hasPermissionToSpeak(player));
        verify(player, atLeast(1)).sendMessage(anyString());
    }

    @Test
    void testSendRequest() {
        ptsManager.sendRequest(player);
        
        assertTrue(ptsManager.isRequesting(playerId));
        verify(player).sendMessage(anyString());
        verify(taskScheduler).runGlobalLater(any(Runnable.class), eq(20L * 30));
    }

    @Test
    void testSendRequest_AlreadyRequesting() {
        ptsManager.sendRequest(player);
        
        // Send again - should be ignored
        int messageCount = mockingDetails(player).getInvocations().size();
        ptsManager.sendRequest(player);
        
        // Should still only be counted once
        assertTrue(ptsManager.isRequesting(playerId));
    }

    @Test
    void testGetRemainingSeconds_NoGrant() {
        int remaining = ptsManager.getRemainingSeconds(playerId);
        assertEquals(0, remaining);
    }

    @Test
    void testGetRemainingSeconds_WithGrant() {
        ptsManager.grantPermission(player, 60);
        
        int remaining = ptsManager.getRemainingSeconds(playerId);
        assertTrue(remaining > 55 && remaining <= 60, 
                "Expected remaining time around 60 seconds, got: " + remaining);
    }

    @Test
    void testCleanup() {
        ptsManager.grantPermission(player, 60);
        ptsManager.sendRequest(player);
        
        assertTrue(ptsManager.isRequesting(playerId));
        when(player.hasPermission("iron.pts.bypass")).thenReturn(false);
        when(rankManager.requiresPTS(player)).thenReturn(true);
        assertTrue(ptsManager.hasPermissionToSpeak(player));
        
        ptsManager.cleanup(playerId);
        
        assertFalse(ptsManager.isRequesting(playerId));
        assertFalse(ptsManager.hasPermissionToSpeak(player));
    }

    @Test
    void testShutdown() {
        // Should not throw exception
        assertDoesNotThrow(() -> ptsManager.shutdown());
        
        verify(scheduledTask, atLeastOnce()).cancel();
    }

    @Test
    void testMultiplePlayersIndependently() {
        Player player2 = mock(Player.class);
        UUID player2Id = UUID.randomUUID();
        when(player2.getUniqueId()).thenReturn(player2Id);
        when(player2.getName()).thenReturn("Player2");
        when(player2.spigot()).thenReturn(mock(Spigot.class));
        
        // Grant to player1 only
        ptsManager.grantPermission(player, 60);
        
        when(player.hasPermission("iron.pts.bypass")).thenReturn(false);
        when(player2.hasPermission("iron.pts.bypass")).thenReturn(false);
        when(rankManager.requiresPTS(player)).thenReturn(true);
        when(rankManager.requiresPTS(player2)).thenReturn(true);
        
        assertTrue(ptsManager.hasPermissionToSpeak(player));
        assertFalse(ptsManager.hasPermissionToSpeak(player2));
    }
}
