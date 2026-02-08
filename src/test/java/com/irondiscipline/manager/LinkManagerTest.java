package com.irondiscipline.manager;

import com.irondiscipline.IronDiscipline;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LinkManagerTest {

    @Mock
    private IronDiscipline plugin;

    private LinkManager linkManager;
    private AutoCloseable mocks;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);

        // Plugin Mock
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("LinkManagerTest"));

        linkManager = new LinkManager(plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (linkManager != null)
            linkManager.shutdown();
        if (mocks != null)
            mocks.close();
    }

    @Test
    void testGenerateLinkCode() {
        long discordId = 123456789L;
        String code = linkManager.generateLinkCode(discordId);

        assertNotNull(code);
        assertEquals(8, code.length());

        // Re-generating should give a different code and invalidate old
        String code2 = linkManager.generateLinkCode(discordId);
        assertNotEquals(code, code2);
    }

    @Test
    void testAttemptLinkSuccess() {
        long discordId = 123456789L;
        UUID mcId = UUID.randomUUID();
        String code = linkManager.generateLinkCode(discordId);

        LinkManager.LinkResult result = linkManager.attemptLink(mcId, code);

        assertEquals(LinkManager.LinkResult.SUCCESS, result);
        assertTrue(linkManager.isLinked(discordId));
        assertTrue(linkManager.isLinked(mcId));
        assertEquals(mcId, linkManager.getMinecraftId(discordId));
        assertEquals(discordId, linkManager.getDiscordId(mcId));
    }

    @Test
    void testAttemptLinkInvalidCode() {
        UUID mcId = UUID.randomUUID();
        LinkManager.LinkResult result = linkManager.attemptLink(mcId, "INVALID");

        assertEquals(LinkManager.LinkResult.INVALID_CODE, result);
    }

    @Test
    void testUnlink() {
        long discordId = 123456789L;
        UUID mcId = UUID.randomUUID();
        String code = linkManager.generateLinkCode(discordId);
        linkManager.attemptLink(mcId, code);

        assertTrue(linkManager.unlink(mcId));
        assertFalse(linkManager.isLinked(mcId));
        assertFalse(linkManager.isLinked(discordId));
    }

    @Test
    void testPersistence() throws InterruptedException {
        long discordId = 987654321L;
        UUID mcId = UUID.randomUUID();
        String code = linkManager.generateLinkCode(discordId);
        linkManager.attemptLink(mcId, code);

        // 非同期保存の完了を待つ
        Thread.sleep(500);

        // Create new instance to test loading
        LinkManager newManager = new LinkManager(plugin);
        try {
            assertTrue(newManager.isLinked(discordId));
            assertEquals(mcId, newManager.getMinecraftId(discordId));
        } finally {
            newManager.shutdown();
        }
    }
}
