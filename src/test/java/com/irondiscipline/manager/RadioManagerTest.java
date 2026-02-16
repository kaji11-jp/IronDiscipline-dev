package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.RadioChannel;
import com.irondiscipline.model.Rank;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RadioManager テスト
 * 無線通信システムのテスト
 */
class RadioManagerTest {

    @Mock
    private IronDiscipline plugin;
    @Mock
    private ConfigManager configManager;
    @Mock
    private Player player1;
    @Mock
    private Player player2;
    @Mock
    private Location location;

    private RadioManager radioManager;
    private AutoCloseable mocks;
    private UUID player1Id;
    private UUID player2Id;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        
        player1Id = UUID.randomUUID();
        player2Id = UUID.randomUUID();
        
        when(player1.getUniqueId()).thenReturn(player1Id);
        when(player1.getName()).thenReturn("Player1");
        when(player1.getLocation()).thenReturn(location);
        
        when(player2.getUniqueId()).thenReturn(player2Id);
        when(player2.getName()).thenReturn("Player2");
        when(player2.getLocation()).thenReturn(location);
        
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(configManager.getMessage(anyString())).thenReturn("Test Message");
        when(configManager.getMessage(anyString(), anyString(), anyString())).thenReturn("Test Message");
        
        radioManager = new RadioManager(plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) mocks.close();
    }

    @Test
    void testJoinFrequency() {
        radioManager.joinFrequency(player1, "100.5");
        
        assertTrue(radioManager.isInFrequency(player1Id));
        assertEquals("100.5", radioManager.getPlayerFrequency(player1Id));
        verify(player1).sendMessage(anyString());
        verify(player1).playSound(any(Location.class), any(Sound.class), anyFloat(), anyFloat());
    }

    @Test
    void testJoinFrequency_NormalizedFormat() {
        // Test that frequencies are normalized - 100.5 should become "100.5"
        radioManager.joinFrequency(player1, "100.5");
        
        assertEquals("100.5", radioManager.getPlayerFrequency(player1Id));
        assertEquals(1, radioManager.getChannelMemberCount("100.5"));
    }

    @Test
    void testLeaveFrequency() {
        radioManager.joinFrequency(player1, "100.5");
        assertTrue(radioManager.isInFrequency(player1Id));
        
        radioManager.leaveFrequency(player1);
        
        assertFalse(radioManager.isInFrequency(player1Id));
        assertNull(radioManager.getPlayerFrequency(player1Id));
        verify(player1, atLeast(2)).sendMessage(anyString()); // Join + Leave messages
    }

    @Test
    void testLeaveFrequency_NotOnAnyFrequency() {
        // Should not throw exception
        assertDoesNotThrow(() -> radioManager.leaveFrequency(player1));
    }

    @Test
    void testSwitchFrequency() {
        radioManager.joinFrequency(player1, "100.5");
        assertEquals("100.5", radioManager.getPlayerFrequency(player1Id));
        
        // Join another frequency - should automatically leave previous
        radioManager.joinFrequency(player1, "200.0");
        
        assertEquals("200.0", radioManager.getPlayerFrequency(player1Id));
        assertEquals(0, radioManager.getChannelMemberCount("100.5")); // Old channel empty
        assertEquals(1, radioManager.getChannelMemberCount("200.0")); // New channel has 1
    }

    @Test
    void testMultiplePlayersOnSameFrequency() {
        radioManager.joinFrequency(player1, "100.5");
        radioManager.joinFrequency(player2, "100.5");
        
        assertTrue(radioManager.isInFrequency(player1Id));
        assertTrue(radioManager.isInFrequency(player2Id));
        assertEquals(2, radioManager.getChannelMemberCount("100.5"));
    }

    @Test
    void testMultiplePlayersOnDifferentFrequencies() {
        radioManager.joinFrequency(player1, "100.5");
        radioManager.joinFrequency(player2, "200.0");
        
        assertEquals("100.5", radioManager.getPlayerFrequency(player1Id));
        assertEquals("200.0", radioManager.getPlayerFrequency(player2Id));
        assertEquals(1, radioManager.getChannelMemberCount("100.5"));
        assertEquals(1, radioManager.getChannelMemberCount("200.0"));
    }

    @Test
    void testGetPlayerFrequency() {
        radioManager.joinFrequency(player1, "100.5");
        
        String freq = radioManager.getPlayerFrequency(player1Id);
        assertNotNull(freq);
        assertEquals("100.5", freq);
    }

    @Test
    void testGetPlayerFrequency_NotOnAny() {
        String freq = radioManager.getPlayerFrequency(player1Id);
        assertNull(freq);
    }

    @Test
    void testCleanup() {
        radioManager.joinFrequency(player1, "100.5");
        assertTrue(radioManager.isInFrequency(player1Id));
        
        radioManager.cleanup(player1Id);
        
        assertFalse(radioManager.isInFrequency(player1Id));
        assertEquals(0, radioManager.getChannelMemberCount("100.5"));
    }

    @Test
    void testEmptyChannelRemoval() {
        // When last player leaves, channel should be cleaned up internally
        radioManager.joinFrequency(player1, "100.5");
        radioManager.joinFrequency(player2, "100.5");
        assertEquals(2, radioManager.getChannelMemberCount("100.5"));
        
        radioManager.leaveFrequency(player1);
        assertTrue(radioManager.isInFrequency(player2Id));
        assertEquals(1, radioManager.getChannelMemberCount("100.5"));
        
        radioManager.leaveFrequency(player2);
        assertFalse(radioManager.isInFrequency(player2Id));
        assertEquals(0, radioManager.getChannelMemberCount("100.5")); // Channel removed
        
        // Both players off - channel should be empty/removed
        // Re-joining should work fine
        radioManager.joinFrequency(player1, "100.5");
        assertTrue(radioManager.isInFrequency(player1Id));
        assertEquals(1, radioManager.getChannelMemberCount("100.5"));
    }

    @Test
    void testGetChannelMemberCount() {
        assertEquals(0, radioManager.getChannelMemberCount("100.5"));
        
        radioManager.joinFrequency(player1, "100.5");
        assertEquals(1, radioManager.getChannelMemberCount("100.5"));
        
        radioManager.joinFrequency(player2, "100.5");
        assertEquals(2, radioManager.getChannelMemberCount("100.5"));
        
        radioManager.leaveFrequency(player1);
        assertEquals(1, radioManager.getChannelMemberCount("100.5"));
    }
}
