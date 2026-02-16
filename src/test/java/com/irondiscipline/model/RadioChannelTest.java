package com.irondiscipline.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RadioChannel モデルクラステスト
 */
class RadioChannelTest {

    private RadioChannel channel;
    private UUID player1;
    private UUID player2;
    private UUID player3;

    @BeforeEach
    void setUp() {
        channel = new RadioChannel("100.5");
        player1 = UUID.randomUUID();
        player2 = UUID.randomUUID();
        player3 = UUID.randomUUID();
    }

    @Test
    void testConstructor() {
        RadioChannel ch = new RadioChannel("123.45");
        assertEquals("123.45", ch.getFrequency());
        assertTrue(ch.isEmpty());
        assertEquals(0, ch.getMemberCount());
    }

    @Test
    void testAddMember() {
        channel.addMember(player1);
        
        assertTrue(channel.hasMember(player1));
        assertFalse(channel.isEmpty());
        assertEquals(1, channel.getMemberCount());
    }

    @Test
    void testAddMultipleMembers() {
        channel.addMember(player1);
        channel.addMember(player2);
        channel.addMember(player3);
        
        assertTrue(channel.hasMember(player1));
        assertTrue(channel.hasMember(player2));
        assertTrue(channel.hasMember(player3));
        assertEquals(3, channel.getMemberCount());
    }

    @Test
    void testAddSameMemberTwice() {
        channel.addMember(player1);
        channel.addMember(player1); // Add again
        
        // Should only count once (Set behavior)
        assertEquals(1, channel.getMemberCount());
        assertTrue(channel.hasMember(player1));
    }

    @Test
    void testRemoveMember() {
        channel.addMember(player1);
        channel.addMember(player2);
        
        channel.removeMember(player1);
        
        assertFalse(channel.hasMember(player1));
        assertTrue(channel.hasMember(player2));
        assertEquals(1, channel.getMemberCount());
    }

    @Test
    void testRemoveNonExistentMember() {
        channel.addMember(player1);
        
        // Should not throw exception
        channel.removeMember(player2);
        
        assertTrue(channel.hasMember(player1));
        assertEquals(1, channel.getMemberCount());
    }

    @Test
    void testIsEmpty() {
        assertTrue(channel.isEmpty());
        
        channel.addMember(player1);
        assertFalse(channel.isEmpty());
        
        channel.removeMember(player1);
        assertTrue(channel.isEmpty());
    }

    @Test
    void testGetMembersReturnsDefensiveCopy() {
        channel.addMember(player1);
        
        Set<UUID> members = channel.getMembers();
        assertEquals(1, members.size());
        
        // Modify the returned set
        members.add(player2);
        
        // Original channel should be unaffected
        assertEquals(1, channel.getMemberCount());
        assertFalse(channel.hasMember(player2));
    }

    @Test
    void testGetFrequency() {
        RadioChannel ch1 = new RadioChannel("100.0");
        RadioChannel ch2 = new RadioChannel("FM-51");
        
        assertEquals("100.0", ch1.getFrequency());
        assertEquals("FM-51", ch2.getFrequency());
    }

    @Test
    void testMultipleChannelsSeparately() {
        RadioChannel channel1 = new RadioChannel("100.5");
        RadioChannel channel2 = new RadioChannel("200.0");
        
        channel1.addMember(player1);
        channel2.addMember(player2);
        
        assertTrue(channel1.hasMember(player1));
        assertFalse(channel1.hasMember(player2));
        
        assertTrue(channel2.hasMember(player2));
        assertFalse(channel2.hasMember(player1));
    }

    @Test
    void testClearAllMembers() {
        channel.addMember(player1);
        channel.addMember(player2);
        channel.addMember(player3);
        
        assertEquals(3, channel.getMemberCount());
        
        // Remove all
        channel.removeMember(player1);
        channel.removeMember(player2);
        channel.removeMember(player3);
        
        assertTrue(channel.isEmpty());
        assertEquals(0, channel.getMemberCount());
    }
}
