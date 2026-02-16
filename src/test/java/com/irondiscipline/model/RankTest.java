package com.irondiscipline.model;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rank enum テスト
 * 階級の比較ロジック、昇進・降格、PTS要件などをテスト
 */
class RankTest {

    @Test
    void testGetters() {
        Rank rank = Rank.SERGEANT;
        assertEquals("SERGEANT", rank.getId());
        assertEquals("&e[軍曹]", rank.getDisplayRaw());
        assertEquals(30, rank.getWeight());
        
        // Display should translate color codes
        String display = rank.getDisplay();
        assertTrue(display.contains("軍曹"));
        assertNotEquals(rank.getDisplayRaw(), display);
    }

    @Test
    void testIsHigherThan() {
        assertTrue(Rank.MAJOR.isHigherThan(Rank.SERGEANT));
        assertFalse(Rank.PRIVATE.isHigherThan(Rank.CORPORAL));
        assertFalse(Rank.CAPTAIN.isHigherThan(Rank.CAPTAIN)); // Same rank
    }

    @Test
    void testIsLowerThan() {
        assertTrue(Rank.PRIVATE.isLowerThan(Rank.LIEUTENANT));
        assertFalse(Rank.COLONEL.isLowerThan(Rank.MAJOR));
        assertFalse(Rank.SERGEANT.isLowerThan(Rank.SERGEANT)); // Same rank
    }

    @Test
    void testRequiresPTS() {
        // Weight <= 25 requires PTS
        assertTrue(Rank.PRIVATE.requiresPTS()); // weight 10
        assertTrue(Rank.PRIVATE_FIRST_CLASS.requiresPTS()); // weight 15
        assertTrue(Rank.CORPORAL.requiresPTS()); // weight 20
        
        // Weight > 25 does not require PTS
        assertFalse(Rank.SERGEANT.requiresPTS()); // weight 30
        assertFalse(Rank.LIEUTENANT.requiresPTS()); // weight 40
        assertFalse(Rank.COMMANDER.requiresPTS()); // weight 100
    }

    @Test
    void testGetNextRank() {
        assertEquals(Rank.PRIVATE_FIRST_CLASS, Rank.PRIVATE.getNextRank());
        assertEquals(Rank.CORPORAL, Rank.PRIVATE_FIRST_CLASS.getNextRank());
        assertEquals(Rank.SERGEANT, Rank.CORPORAL.getNextRank());
        assertEquals(Rank.LIEUTENANT, Rank.SERGEANT.getNextRank());
        assertEquals(Rank.CAPTAIN, Rank.LIEUTENANT.getNextRank());
        assertEquals(Rank.MAJOR, Rank.CAPTAIN.getNextRank());
        assertEquals(Rank.COLONEL, Rank.MAJOR.getNextRank());
        assertEquals(Rank.COMMANDER, Rank.COLONEL.getNextRank());
        
        // Commander is max rank
        assertNull(Rank.COMMANDER.getNextRank());
    }

    @Test
    void testGetPreviousRank() {
        assertEquals(Rank.COLONEL, Rank.COMMANDER.getPreviousRank());
        assertEquals(Rank.MAJOR, Rank.COLONEL.getPreviousRank());
        assertEquals(Rank.CAPTAIN, Rank.MAJOR.getPreviousRank());
        assertEquals(Rank.LIEUTENANT, Rank.CAPTAIN.getPreviousRank());
        assertEquals(Rank.SERGEANT, Rank.LIEUTENANT.getPreviousRank());
        assertEquals(Rank.CORPORAL, Rank.SERGEANT.getPreviousRank());
        assertEquals(Rank.PRIVATE_FIRST_CLASS, Rank.CORPORAL.getPreviousRank());
        assertEquals(Rank.PRIVATE, Rank.PRIVATE_FIRST_CLASS.getPreviousRank());
        
        // Private is min rank
        assertNull(Rank.PRIVATE.getPreviousRank());
    }

    @Test
    void testFromId() {
        assertEquals(Rank.PRIVATE, Rank.fromId("PRIVATE"));
        assertEquals(Rank.SERGEANT, Rank.fromId("SERGEANT"));
        assertEquals(Rank.COMMANDER, Rank.fromId("COMMANDER"));
        
        // Case insensitive
        assertEquals(Rank.MAJOR, Rank.fromId("major"));
        assertEquals(Rank.CORPORAL, Rank.fromId("CoRpOrAl"));
        
        // Null or unknown returns default (PRIVATE)
        assertEquals(Rank.PRIVATE, Rank.fromId(null));
        assertEquals(Rank.PRIVATE, Rank.fromId("UNKNOWN_RANK"));
        assertEquals(Rank.PRIVATE, Rank.fromId(""));
    }

    @Test
    void testRankOrdering() {
        Rank[] ranks = Rank.values();
        
        // Verify weights are in ascending order
        for (int i = 0; i < ranks.length - 1; i++) {
            assertTrue(ranks[i].getWeight() < ranks[i + 1].getWeight(),
                    ranks[i] + " should have lower weight than " + ranks[i + 1]);
        }
    }

    @Test
    void testPromotionDemotionSymmetry() {
        // For any rank (except min/max), next.previous should equal current
        for (Rank rank : Rank.values()) {
            Rank next = rank.getNextRank();
            if (next != null) {
                assertEquals(rank, next.getPreviousRank());
            }
            
            Rank prev = rank.getPreviousRank();
            if (prev != null) {
                assertEquals(rank, prev.getNextRank());
            }
        }
    }

    @Test
    void testWeightBasedComparison() {
        // Verify weight directly correlates with rank comparison
        Rank lower = Rank.PRIVATE;
        Rank higher = Rank.COLONEL;
        
        assertTrue(higher.isHigherThan(lower));
        assertFalse(lower.isHigherThan(higher));
        assertTrue(lower.isLowerThan(higher));
        assertFalse(higher.isLowerThan(lower));
    }

    @Test
    void testAllRanksHaveUniqueWeights() {
        Rank[] ranks = Rank.values();
        for (int i = 0; i < ranks.length; i++) {
            for (int j = i + 1; j < ranks.length; j++) {
                assertNotEquals(ranks[i].getWeight(), ranks[j].getWeight(),
                        ranks[i] + " and " + ranks[j] + " should have different weights");
            }
        }
    }

    @Test
    void testAllRanksHaveUniqueIds() {
        Rank[] ranks = Rank.values();
        for (int i = 0; i < ranks.length; i++) {
            for (int j = i + 1; j < ranks.length; j++) {
                assertNotEquals(ranks[i].getId(), ranks[j].getId(),
                        ranks[i] + " and " + ranks[j] + " should have different IDs");
            }
        }
    }
}
