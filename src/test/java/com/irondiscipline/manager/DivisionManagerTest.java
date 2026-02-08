package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DivisionManagerTest {

    @Mock
    private IronDiscipline plugin;
    @Mock
    private BukkitScheduler scheduler;
    
    private DivisionManager divisionManager;
    private AutoCloseable mocks;
    private MockedStatic<Bukkit> bukkitMock;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Bukkit Static Mock for Scheduler
        bukkitMock = mockStatic(Bukkit.class);
        bukkitMock.when(Bukkit::getScheduler).thenReturn(scheduler);

        // Plugin Mock
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("DivisionManagerTest"));

        // Scheduler Mock (Run async tasks immediately on the same thread for testing)
        doAnswer(invocation -> {
            Runnable r = invocation.getArgument(1);
            r.run();
            return null;
        }).when(scheduler).runTaskAsynchronously(any(IronDiscipline.class), any(Runnable.class));

        // Initialize Manager
        divisionManager = new DivisionManager(plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (bukkitMock != null) bukkitMock.close();
        if (mocks != null) mocks.close();
    }

    @Test
    void testInitDefaultDivisions() {
        Set<String> divisions = divisionManager.getAllDivisions();
        assertTrue(divisions.contains("mp"), "Should contain default MP division");
        assertTrue(divisions.contains("infantry"), "Should contain default infantry division");
        assertTrue(divisions.contains("command"), "Should contain default command division");
    }

    @Test
    void testSetAndGetDivision() {
        UUID playerId = UUID.randomUUID();
        String division = "infantry";

        divisionManager.setDivision(playerId, division);

        assertEquals(division, divisionManager.getDivision(playerId));
        assertTrue(divisionManager.getDivisionMembers(division).contains(playerId));
    }

    @Test
    void testRemoveDivision() {
        UUID playerId = UUID.randomUUID();
        String division = "medical";

        divisionManager.setDivision(playerId, division);
        assertNotNull(divisionManager.getDivision(playerId));

        divisionManager.removeDivision(playerId);
        assertNull(divisionManager.getDivision(playerId));
        assertFalse(divisionManager.getDivisionMembers(division).contains(playerId));
    }

    @Test
    void testIsMP() {
        UUID playerId = UUID.randomUUID();
        divisionManager.setDivision(playerId, "mp");
        assertTrue(divisionManager.isMP(playerId));

        divisionManager.setDivision(playerId, "infantry");
        assertFalse(divisionManager.isMP(playerId));
    }

    @Test
    void testDivisionDisplayName() {
        assertEquals("§c[憲兵]", divisionManager.getDivisionDisplayName("mp"));
        assertEquals("§a[歩兵]", divisionManager.getDivisionDisplayName("infantry"));
        assertEquals("§7[custom]", divisionManager.getDivisionDisplayName("custom"));
    }

    @Test
    void testCreateDivision() {
        String newDiv = "special_ops";
        assertFalse(divisionManager.divisionExists(newDiv));

        divisionManager.createDivision(newDiv);
        assertTrue(divisionManager.divisionExists(newDiv));
    }
    
    @Test
    void testPersistence() {
        // Create a new manager instance to verify data loading
        UUID playerId = UUID.randomUUID();
        divisionManager.setDivision(playerId, "aviation");
        
        // Re-initialize manager to simulate server restart
        DivisionManager newManager = new DivisionManager(plugin);
        
        assertEquals("aviation", newManager.getDivision(playerId), "Data should persist across reloads");
    }
}
