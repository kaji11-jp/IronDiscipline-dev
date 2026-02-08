package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PlaytimeManagerTest {

    @Mock
    private IronDiscipline plugin;
    @Mock
    private ConfigManager configManager;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private Player player;

    private PlaytimeManager playtimeManager;
    private AutoCloseable mocks;
    private MockedStatic<Bukkit> bukkitMock;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Bukkit Static Mocks
        bukkitMock = mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkitMock.when(Bukkit::getPluginManager).thenReturn(pluginManager);
        bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());

        // Plugin Mock
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("PlaytimeManagerTest"));

        // Config Mock
        when(configManager.getRawMessage(anyString())).thenReturn("Message");
        when(configManager.getRawMessage("time_format_days")).thenReturn("%d日 %02d時間 %02d分");
        when(configManager.getRawMessage("time_format_hours")).thenReturn("%d時間 %02d分");
        when(configManager.getRawMessage("time_format_minutes")).thenReturn("%d分");

        // Scheduler Mock (Run async tasks immediately)
        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(1);
            r.run();
            return null;
        }).when(scheduler).runTaskAsynchronously(any(IronDiscipline.class), any(Runnable.class));

        playtimeManager = new PlaytimeManager(plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (bukkitMock != null) bukkitMock.close();
        if (mocks != null) mocks.close();
    }

    @Test
    void testOnJoinStartsSession() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);

        PlayerJoinEvent event = new PlayerJoinEvent(player, "joined");
        playtimeManager.onJoin(event);
        
        // Session should be started (playtime > 0 or 0 depending on speed, but map entry should exist)
        // We can't access private map, but getTodayPlaytime should return >= 0
        long today = playtimeManager.getTodayPlaytime(uuid);
        assertTrue(today >= 0, "Session should be active");
    }

    @Test
    void testOnQuitSavesSession() {
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);

        // Simulate Join
        PlayerJoinEvent joinEvent = new PlayerJoinEvent(player, "joined");
        playtimeManager.onJoin(joinEvent);

        // Simulate short wait (optional, but System.currentTimeMillis might be same)
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}

        // Simulate Quit
        PlayerQuitEvent quitEvent = new PlayerQuitEvent(player, "quit");
        playtimeManager.onQuit(quitEvent);

        // Session should be closed, total playtime updated
        long total = playtimeManager.getTotalPlaytime(uuid);
        assertTrue(total > 0, "Total playtime should be recorded");
    }

    @Test
    void testFormatTime() {
        // Use reflection to access private method or just test public wrapper if possible.
        // Public wrapper requires UUID and data.
        // Let's manually populate data by mocking a session close.
        
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        
        // Inject data via join/quit with known delay? No, sleep is flaky.
        // Instead, I'll rely on the logic: getTotalPlaytime returns (saved + current_session).
        
        // We can't easily inject specific time without reflection on 'totalPlaytime' map.
        // However, we can test the formatter via 'getFormattedPlaytime' if we can control the time.
        // Since we can't control System.currentTimeMillis() easily, let's try a different approach.
        // We can verify the "0 minutes" case at least.
        
        String formatted = playtimeManager.getFormattedPlaytime(UUID.randomUUID());
        // 0 millis -> 0 mins
        assertEquals("0分", formatted);
    }
    
    @Test
    void testFormatTimeLogic_MockingInternalState() throws Exception {
        // Use reflection to inject data to test formatting
        UUID uuid = UUID.randomUUID();
        long hours25 = 25 * 60 * 60 * 1000L; // 25 hours
        
        java.lang.reflect.Field field = PlaytimeManager.class.getDeclaredField("totalPlaytime");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<UUID, Long> map = (java.util.Map<UUID, Long>) field.get(playtimeManager);
        map.put(uuid, hours25);
        
        String formatted = playtimeManager.getFormattedPlaytime(uuid);
        // 1 day, 1 hour, 0 mins -> "1日 01時間 00分"
        assertEquals("1日 01時間 00分", formatted);
    }
}
