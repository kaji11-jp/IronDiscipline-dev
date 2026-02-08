package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.manager.WarningManager.Warning;
import com.irondiscipline.util.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class WarningManagerTest {

    @Mock
    private IronDiscipline plugin;
    @Mock
    private StorageManager storageManager;
    @Mock
    private ConfigManager configManager;
    @Mock
    private JailManager jailManager;
    @Mock
    private Server server;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private Player player;
    @Mock
    private TaskScheduler taskScheduler;

    private WarningManager warningManager;
    private AutoCloseable mocks;
    private MockedStatic<Bukkit> bukkitMock;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        
        // Mock Bukkit statics
        bukkitMock = mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getServer).thenReturn(server);
        bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkitMock.when(() -> Bukkit.getPlayer(any(UUID.class))).thenReturn(player);

        // Basic plugin mocks
        when(plugin.getStorageManager()).thenReturn(storageManager);
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getJailManager()).thenReturn(jailManager);
        when(plugin.getJailManager()).thenReturn(jailManager);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("TestLogger"));
        when(plugin.getTaskScheduler()).thenReturn(taskScheduler);

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

        // Scheduler mock - immediate execution for runTask
        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(1);
            r.run();
            return null;
        }).when(scheduler).runTask(any(IronDiscipline.class), any(Runnable.class));

        when(configManager.getRawMessage("warn_punish_kick_reason")).thenReturn("警告が%limit%回に達したため、キックされました。");
        when(configManager.getRawMessage("warn_punish_jail_reason")).thenReturn("警告%count%回による自動隔離");

        warningManager = new WarningManager(plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (bukkitMock != null) {
            bukkitMock.close();
        }
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void testAddWarning_BelowThreshold() {
        UUID playerId = UUID.randomUUID();
        String playerName = "TestPlayer";
        String reason = "Griefing";
        UUID officerId = UUID.randomUUID();

        // Config thresholds
        when(configManager.getWarningKickThreshold()).thenReturn(3);
        when(configManager.getWarningJailThreshold()).thenReturn(5);

        // Storage mocks
        when(storageManager.addWarningAsync(any(), any(), any(), any(), anyLong()))
                .thenReturn(CompletableFuture.completedFuture(null));
        
        // Return 1 warning
        List<Warning> warnings = new ArrayList<>();
        warnings.add(new Warning());
        when(storageManager.getWarningsAsync(playerId))
                .thenReturn(CompletableFuture.completedFuture(warnings));

        when(player.isOnline()).thenReturn(true);

        // Execute
        int count = warningManager.addWarning(playerId, playerName, reason, officerId).join();

        assertEquals(1, count);
        
        // Verify no punishment triggered
        verify(player, never()).kickPlayer(anyString());
        verify(jailManager, never()).jail(any(), any(), any());
    }

    @Test
    void testAddWarning_KickThreshold() {
        UUID playerId = UUID.randomUUID();
        
        // Thresholds
        when(configManager.getWarningKickThreshold()).thenReturn(3);
        when(configManager.getWarningJailThreshold()).thenReturn(5);

        when(storageManager.addWarningAsync(any(), any(), any(), any(), anyLong()))
                .thenReturn(CompletableFuture.completedFuture(null));
        
        // Return 3 warnings
        List<Warning> warnings = new ArrayList<>();
        warnings.add(new Warning());
        warnings.add(new Warning());
        warnings.add(new Warning());
        when(storageManager.getWarningsAsync(playerId))
                .thenReturn(CompletableFuture.completedFuture(warnings));

        when(player.isOnline()).thenReturn(true);

        // Execute
        warningManager.addWarning(playerId, "Test", "Reason", null).join();

        // Verify Kick
        verify(player).kickPlayer(contains("警告が3回に達したため"));
    }
    
    @Test
    void testAddWarning_JailThreshold() {
        UUID playerId = UUID.randomUUID();
        
        // Thresholds
        when(configManager.getWarningKickThreshold()).thenReturn(50); // High kick
        when(configManager.getWarningJailThreshold()).thenReturn(5);  // Low jail

        when(storageManager.addWarningAsync(any(), any(), any(), any(), anyLong()))
                .thenReturn(CompletableFuture.completedFuture(null));
        
        // Return 5 warnings
        List<Warning> warnings = new ArrayList<>();
        for (int i=0; i<5; i++) warnings.add(new Warning());
        
        when(storageManager.getWarningsAsync(playerId))
                .thenReturn(CompletableFuture.completedFuture(warnings));

        when(player.isOnline()).thenReturn(true);

        // Execute
        warningManager.addWarning(playerId, "Test", "Reason", null).join();

        // Verify Jail
        verify(jailManager).jail(eq(player), any(), contains("警告5回による自動隔離"));
        verify(player, never()).kickPlayer(anyString());
    }

    @Test
    void testClearWarnings() {
        UUID playerId = UUID.randomUUID();
        when(storageManager.clearWarningsAsync(playerId))
                .thenReturn(CompletableFuture.completedFuture(null));

        warningManager.clearWarnings(playerId).join();

        verify(storageManager).clearWarningsAsync(playerId);
    }
}
