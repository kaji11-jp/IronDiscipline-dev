package com.irondiscipline.util;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * InventoryUtil テスト
 * インベントリのシリアライズ・デシリアライズをテスト
 */
class InventoryUtilTest {

    private MockedStatic<Bukkit> bukkitMock;

    @BeforeEach
    void setUp() {
        bukkitMock = mockStatic(Bukkit.class);
        // Note: Unsafe is not available in Spigot API, so these tests are disabled
    }

    @AfterEach
    void tearDown() {
        if (bukkitMock != null) bukkitMock.close();
    }

    @Disabled("Unsafe not available in Spigot API")
    @Test
    void testToBase64_EmptyArray() {
        ItemStack[] empty = new ItemStack[0];
        
        String encoded = InventoryUtil.toBase64(empty);
        
        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());
    }

    @Disabled("Unsafe not available in Spigot API")
    @Test
    void testToBase64_SingleItem() {
        ItemStack[] items = new ItemStack[] {
            new ItemStack(Material.DIAMOND_SWORD, 1)
        };
        
        String encoded = InventoryUtil.toBase64(items);
        
        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());
        // Base64 should only contain valid characters
        assertTrue(encoded.matches("^[A-Za-z0-9+/=]+$"));
    }

    @Disabled("Unsafe not available in Spigot API")
    @Test
    void testToBase64_MultipleItems() {
        ItemStack[] items = new ItemStack[] {
            new ItemStack(Material.DIAMOND_SWORD, 1),
            new ItemStack(Material.IRON_CHESTPLATE, 1),
            new ItemStack(Material.GOLDEN_APPLE, 5)
        };
        
        String encoded = InventoryUtil.toBase64(items);
        
        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());
    }

    @Disabled("Unsafe not available in Spigot API")
    @Test
    void testToBase64_WithNulls() {
        ItemStack[] items = new ItemStack[] {
            new ItemStack(Material.DIAMOND, 10),
            null,
            new ItemStack(Material.GOLD_INGOT, 5),
            null
        };
        
        String encoded = InventoryUtil.toBase64(items);
        
        assertNotNull(encoded);
        assertFalse(encoded.isEmpty());
    }

    @Disabled("Unsafe not available in Spigot API")
    @Test
    void testFromBase64_Null() {
        ItemStack[] result = InventoryUtil.fromBase64(null);
        
        assertNull(result);
    }

    @Disabled("Unsafe not available in Spigot API")
    @Test
    void testFromBase64_EmptyString() {
        ItemStack[] result = InventoryUtil.fromBase64("");
        
        assertNull(result);
    }

    @Disabled("Unsafe not available in Spigot API")
    @Test
    void testRoundTrip_EmptyArray() {
        ItemStack[] original = new ItemStack[0];
        
        String encoded = InventoryUtil.toBase64(original);
        ItemStack[] decoded = InventoryUtil.fromBase64(encoded);
        
        assertNotNull(decoded);
        assertEquals(original.length, decoded.length);
    }

    @Disabled("Unsafe not available in Spigot API")
    @Test
    void testRoundTrip_SingleItem() {
        ItemStack[] original = new ItemStack[] {
            new ItemStack(Material.DIAMOND, 64)
        };
        
        String encoded = InventoryUtil.toBase64(original);
        ItemStack[] decoded = InventoryUtil.fromBase64(encoded);
        
        assertNotNull(decoded);
        assertEquals(original.length, decoded.length);
        assertEquals(original[0].getType(), decoded[0].getType());
        assertEquals(original[0].getAmount(), decoded[0].getAmount());
    }

    @Disabled("Unsafe not available in Spigot API")
    @Test
    void testRoundTrip_MultipleItems() {
        ItemStack[] original = new ItemStack[] {
            new ItemStack(Material.DIAMOND_SWORD, 1),
            new ItemStack(Material.IRON_HELMET, 1),
            new ItemStack(Material.COOKED_BEEF, 32)
        };
        
        String encoded = InventoryUtil.toBase64(original);
        ItemStack[] decoded = InventoryUtil.fromBase64(encoded);
        
        assertNotNull(decoded);
        assertEquals(original.length, decoded.length);
        
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i].getType(), decoded[i].getType());
            assertEquals(original[i].getAmount(), decoded[i].getAmount());
        }
    }

    @Disabled("Unsafe not available in Spigot API")
    @Test
    void testRoundTrip_WithNulls() {
        ItemStack[] original = new ItemStack[] {
            new ItemStack(Material.DIAMOND, 10),
            null,
            new ItemStack(Material.EMERALD, 5),
            null,
            new ItemStack(Material.GOLD_INGOT, 20)
        };
        
        String encoded = InventoryUtil.toBase64(original);
        ItemStack[] decoded = InventoryUtil.fromBase64(encoded);
        
        assertNotNull(decoded);
        assertEquals(original.length, decoded.length);
        
        for (int i = 0; i < original.length; i++) {
            if (original[i] == null) {
                assertNull(decoded[i]);
            } else {
                assertNotNull(decoded[i]);
                assertEquals(original[i].getType(), decoded[i].getType());
                assertEquals(original[i].getAmount(), decoded[i].getAmount());
            }
        }
    }

    @Disabled("Unsafe not available in Spigot API")
    @Test
    void testRoundTrip_FullInventory() {
        // Simulate a full player inventory (36 slots)
        ItemStack[] original = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            if (i % 3 == 0) {
                original[i] = null; // Some empty slots
            } else if (i % 3 == 1) {
                original[i] = new ItemStack(Material.DIRT, i + 1);
            } else {
                original[i] = new ItemStack(Material.STONE, 64);
            }
        }
        
        String encoded = InventoryUtil.toBase64(original);
        ItemStack[] decoded = InventoryUtil.fromBase64(encoded);
        
        assertNotNull(decoded);
        assertEquals(original.length, decoded.length);
        
        for (int i = 0; i < original.length; i++) {
            if (original[i] == null) {
                assertNull(decoded[i], "Slot " + i + " should be null");
            } else {
                assertNotNull(decoded[i], "Slot " + i + " should not be null");
                assertEquals(original[i].getType(), decoded[i].getType());
                assertEquals(original[i].getAmount(), decoded[i].getAmount());
            }
        }
    }

    @Disabled("Unsafe not available in Spigot API")
    @Test
    void testFromBase64_InvalidData() {
        String invalidBase64 = "InvalidData!!!";
        
        // Should handle invalid data gracefully
        ItemStack[] result = InventoryUtil.fromBase64(invalidBase64);
        
        assertNull(result);
    }

    @Disabled("Unsafe not available in Spigot API")
    @Test
    void testFromBase64_CorruptedData() {
        // Create valid data then corrupt it
        ItemStack[] original = new ItemStack[] { new ItemStack(Material.DIAMOND, 1) };
        String encoded = InventoryUtil.toBase64(original);
        
        // Corrupt the encoded string
        String corrupted = encoded.substring(0, encoded.length() / 2);
        
        // Should handle corruption gracefully
        ItemStack[] result = InventoryUtil.fromBase64(corrupted);
        
        assertNull(result);
    }

    @Disabled("Unsafe not available in Spigot API")
    @Test
    void testDifferentStackSizes() {
        ItemStack[] original = new ItemStack[] {
            new ItemStack(Material.ARROW, 1),
            new ItemStack(Material.ARROW, 16),
            new ItemStack(Material.ARROW, 32),
            new ItemStack(Material.ARROW, 64)
        };
        
        String encoded = InventoryUtil.toBase64(original);
        ItemStack[] decoded = InventoryUtil.fromBase64(encoded);
        
        assertNotNull(decoded);
        assertEquals(4, decoded.length);
        assertEquals(1, decoded[0].getAmount());
        assertEquals(16, decoded[1].getAmount());
        assertEquals(32, decoded[2].getAmount());
        assertEquals(64, decoded[3].getAmount());
    }

    @Disabled("Unsafe not available in Spigot API")
    @Test
    void testAllNullArray() {
        ItemStack[] original = new ItemStack[] { null, null, null };
        
        String encoded = InventoryUtil.toBase64(original);
        ItemStack[] decoded = InventoryUtil.fromBase64(encoded);
        
        assertNotNull(decoded);
        assertEquals(3, decoded.length);
        assertNull(decoded[0]);
        assertNull(decoded[1]);
        assertNull(decoded[2]);
    }

    @Disabled("Unsafe not available in Spigot API")
    @Test
    void testLargeInventory() {
        // Test with a very large inventory (e.g., double chest = 54 slots)
        ItemStack[] original = new ItemStack[54];
        for (int i = 0; i < 54; i++) {
            original[i] = new ItemStack(Material.values()[i % Material.values().length], (i % 64) + 1);
        }
        
        String encoded = InventoryUtil.toBase64(original);
        ItemStack[] decoded = InventoryUtil.fromBase64(encoded);
        
        assertNotNull(decoded);
        assertEquals(54, decoded.length);
    }
}
