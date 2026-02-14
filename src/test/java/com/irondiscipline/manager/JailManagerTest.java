package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.util.TaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JailManagerTest {

    @Mock
    private IronDiscipline plugin;
    @Mock
    private ConfigManager configManager;
    @Mock
    private StorageManager storageManager;
    @Mock
    private TaskScheduler taskScheduler;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private Player player;
    @Mock
    private Player jailer;
    @Mock
    private PlayerInventory inventory;
    @Mock
    private World world;

    private JailManager jailManager;
    private AutoCloseable mocks;
    private MockedStatic<Bukkit> bukkitMock;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Bukkit Static Mocks
        bukkitMock = mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);
        bukkitMock.when(Bukkit::getOnlinePlayers).thenReturn(Collections.emptyList());
        bukkitMock.when(Bukkit::getWorlds).thenReturn(Collections.singletonList(world));

        // Plugin Mock
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getStorageManager()).thenReturn(storageManager);
        when(plugin.getTaskScheduler()).thenReturn(taskScheduler);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("JailManagerTest"));

        // Config Mock
        when(configManager.getJailLocation()).thenReturn(new Location(world, 100, 64, 100));
        when(configManager.getRawMessage(anyString())).thenReturn("Message");
        when(configManager.getMessage(anyString(), any())).thenReturn("Message");

        // Storage Mock
        when(storageManager.getJailedPlayerIdsAsync()).thenReturn(CompletableFuture.completedFuture(Collections.emptyList()));
        when(storageManager.saveJailedPlayerAsync(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(true));

        // Player Mock
        UUID uuid = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(uuid);
        when(player.getName()).thenReturn("TestPlayer");
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(player.getInventory()).thenReturn(inventory);
        when(player.isOnline()).thenReturn(true);
        
        when(inventory.getContents()).thenReturn(new ItemStack[0]);
        when(inventory.getArmorContents()).thenReturn(new ItemStack[0]);
        
        // TaskScheduler Mock
        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(1);
            r.run();
            return null;
        }).when(taskScheduler).runEntity(any(), any(Runnable.class));

        jailManager = new JailManager(plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (bukkitMock != null) bukkitMock.close();
        if (mocks != null) mocks.close();
    }

    @Test
    void testJailAddsToCache() {
        boolean result = jailManager.jail(player, jailer, "Reason");
        
        assertTrue(result, "Jail command should start");
        assertTrue(jailManager.isJailed(player), "Player should be in jailed cache immediately");
        
        // Verify DB save called (async, so wait up to 1 second)
        verify(storageManager, timeout(1000)).saveJailedPlayerAsync(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void testUnjailRemovesFromCache() {
        // Pre-jail
        jailManager.jailOffline(player.getUniqueId(), "TestPlayer", null, "Reason");
        assertTrue(jailManager.isJailed(player));

        // Mock DB calls for unjail
        when(storageManager.getJailRecordAsync(any())).thenReturn(CompletableFuture.completedFuture(null));
        when(storageManager.removeJailedPlayerAsync(any())).thenReturn(CompletableFuture.completedFuture(null));

        boolean result = jailManager.unjail(player);
        
        assertTrue(result);
        assertFalse(jailManager.isJailed(player), "Player should be removed from cache");
    }

    @Test
    void testPreventEscape() {
        // Not jailed -> No teleport
        jailManager.preventEscape(player);
        verify(player, never()).teleport(any(Location.class));

        // Jailed & Far away -> Teleport
        jailManager.jailOffline(player.getUniqueId(), "TestPlayer", null, "Reason");
        
        Location farLoc = new Location(world, 200, 64, 200); // Jail is at 100,64,100
        when(player.getLocation()).thenReturn(farLoc);
        
        jailManager.preventEscape(player);
        verify(player).teleport(any(Location.class));
    }
}
