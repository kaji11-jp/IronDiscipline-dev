package xyz.irondiscipline.manager;

import xyz.irondiscipline.IronDiscipline;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ConfigManagerTest {

    @Mock
    private IronDiscipline plugin;
    @Mock
    private FileConfiguration config;

    private ConfigManager configManager;
    private AutoCloseable mocks;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        mocks = MockitoAnnotations.openMocks(this);

        when(plugin.getConfig()).thenReturn(config);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ConfigManagerTest"));

        // Default config values
        when(config.getString("general.locale", "ja_JP")).thenReturn("en_US");
        when(config.getBoolean("general.debug", false)).thenReturn(true);
        when(config.getString("database.type", "h2")).thenReturn("mysql");
        
        // Setup dummy locale file
        File localeDir = new File(tempDir.toFile(), "locales");
        localeDir.mkdirs();
        File enFile = new File(localeDir, "messages_en_US.yml");
        
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("prefix", "&8[&cTest&8] ");
        yaml.set("test_message", "&aHello World");
        yaml.set("test_placeholder", "&eValue: %value%");
        yaml.save(enFile);

        configManager = new ConfigManager(plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) mocks.close();
    }

    @Test
    void testGetters() {
        assertTrue(configManager.isDebug());
        assertEquals("en_US", configManager.getLocale());
        assertEquals("mysql", configManager.getDatabaseType());
    }

    @Test
    void testGetMessage() {
        // "prefix" is "&8[&cTest&8] " -> Colorized
        // "test_message" is "&aHello World"
        
        String msg = configManager.getMessage("test_message");
        // Expect: Prefix + Message (Colorized)
        String expected = ChatColor.translateAlternateColorCodes('&', "&8[&cTest&8] &aHello World");
        
        assertEquals(expected, msg);
    }

    @Test
    void testGetMessageWithReplacements() {
        String msg = configManager.getMessage("test_placeholder", "%value%", "100");
        
        String expected = ChatColor.translateAlternateColorCodes('&', "&8[&cTest&8] &eValue: 100");
        assertEquals(expected, msg);
    }

    @Test
    void testGetRawMessage() {
        String msg = configManager.getRawMessage("test_message");
        // No prefix
        String expected = ChatColor.translateAlternateColorCodes('&', "&aHello World");
        assertEquals(expected, msg);
    }
    
    @Test
    void testFallbackToDefaultConfig() {
        // Mock a key that doesn't exist in locale file but exists in main config
        when(config.getString("messages.missing_key", "missing_key")).thenReturn("&cFallback");
        
        String msg = configManager.getMessage("missing_key");
        String expected = ChatColor.translateAlternateColorCodes('&', "&8[&cTest&8] &cFallback");
        
        assertEquals(expected, msg);
    }

    @Test
    void testGetMessageWithMultipleReplacements() {
        // Test multiple replacements in one message
        when(config.getString(anyString(), anyString())).thenReturn("&aPlayer: %player%, Value: %value%");
        
        String msg = configManager.getRawMessage("test");
        // Replace manually for test
        msg = msg.replace("%player%", "Alice").replace("%value%", "100");
        
        String expected = ChatColor.translateAlternateColorCodes('&', "&aPlayer: Alice, Value: 100");
        assertEquals(expected, msg);
    }

    @Test
    void testColorCodeTranslation() {
        when(config.getString(anyString(), anyString())).thenReturn("&c&lRED BOLD &r&aGreen");
        
        String msg = configManager.getRawMessage("colored");
        
        assertNotEquals("&c&lRED BOLD &r&aGreen", msg); // Should be translated
        assertTrue(msg.contains("§")); // Contains color code section symbol
    }

    @Test
    void testEmptyMessage() {
        when(config.getString(anyString(), anyString())).thenReturn("");
        
        String msg = configManager.getMessage("empty");
        
        // Should have prefix even if message is empty
        assertFalse(msg.isEmpty());
    }

    @Test
    void testNullSafetyInReplacements() {
        // getMessage with null replacement values should not crash
        assertDoesNotThrow(() -> {
            configManager.getMessage("test_placeholder", "%value%", null);
        });
    }
}
