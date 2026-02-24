package xyz.irondiscipline.manager;

import xyz.irondiscipline.IronDiscipline;
import xyz.irondiscipline.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExamManager テスト
 * 試験システムのテスト
 */
class ExamManagerTest {

    @Mock
    private IronDiscipline plugin;
    @Mock
    private ConfigManager configManager;
    @Mock
    private RankManager rankManager;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private Player instructor;
    @Mock
    private Player target;
    @Mock
    private Player nearbyPlayer;
    @Mock
    private World world;
    @Mock
    private Location instructorLoc;
    @Mock
    private Location nearbyLoc;

    private ExamManager examManager;
    private AutoCloseable mocks;
    private MockedStatic<Bukkit> bukkitMock;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        bukkitMock = mockStatic(Bukkit.class);

        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getRankManager()).thenReturn(rankManager);
        
        bukkitMock.when(Bukkit::getPluginManager).thenReturn(pluginManager);
        bukkitMock.when(() -> Bukkit.broadcastMessage(anyString())).then(invocation -> null);

        when(instructor.getName()).thenReturn("Instructor");
        when(instructor.getWorld()).thenReturn(world);
        when(instructor.getLocation()).thenReturn(instructorLoc);
        when(instructorLoc.distance(instructorLoc)).thenReturn(0.0); // Instructor is at their own location
        
        when(target.getName()).thenReturn("Target");
        
        when(nearbyPlayer.getLocation()).thenReturn(nearbyLoc);
        
        when(nearbyLoc.distance(instructorLoc)).thenReturn(30.0); // Within 50 blocks
        when(world.getPlayers()).thenReturn(Arrays.asList(instructor, nearbyPlayer));

        when(configManager.getMessage(anyString())).thenReturn("Test Message");
        when(configManager.getMessage(anyString(), anyString(), anyString())).thenReturn("Test Message");
        when(configManager.getMessage(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("Exam Broadcast");
        when(configManager.getMessage(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn("Exam Broadcast");

        examManager = new ExamManager(plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (bukkitMock != null) bukkitMock.close();
        if (mocks != null) mocks.close();
    }

    @Test
    void testConstructor() {
        // Verify it registers as a listener
        verify(pluginManager).registerEvents(any(ExamManager.class), eq(plugin));
    }

    @Test
    void testStartSTS() {
        examManager.startSTS(instructor);

        // Verify message sent to nearby players
        verify(nearbyPlayer, times(2)).sendMessage(anyString());
        verify(nearbyPlayer).playSound(any(Location.class), eq(Sound.BLOCK_NOTE_BLOCK_PLING), anyFloat(), anyFloat());
    }

    @Test
    void testStartSTS_OnlyNearbyPlayers() {
        Player farPlayer = mock(Player.class);
        Location farLoc = mock(Location.class);
        when(farPlayer.getLocation()).thenReturn(farLoc);
        when(farLoc.distance(instructorLoc)).thenReturn(100.0); // Beyond 50 blocks
        when(world.getPlayers()).thenReturn(Arrays.asList(instructor, nearbyPlayer, farPlayer));

        examManager.startSTS(instructor);

        // Nearby player should receive
        verify(nearbyPlayer, times(2)).sendMessage(anyString());
        
        // Far player should not receive
        verify(farPlayer, never()).sendMessage(anyString());
    }

    @Test
    void testStartExamSession() {
        bukkitMock.when(() -> Bukkit.broadcastMessage(anyString())).thenReturn(1);

        examManager.startExamSession(instructor, target, "Marksmanship");

        // Verify broadcast was sent
        bukkitMock.verify(() -> Bukkit.broadcastMessage(anyString()));
        verify(configManager).getMessage(eq("exam_start_broadcast"), anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void testPassExam() {
        when(rankManager.promote(target)).thenReturn(CompletableFuture.completedFuture(Rank.PRIVATE_FIRST_CLASS));
        when(configManager.getMessage(eq("exam_promotion_congrats"), anyString(), anyString()))
                .thenReturn("Congratulations!");
        
        bukkitMock.when(() -> Bukkit.broadcastMessage(anyString())).thenReturn(1);

        examManager.passExam(instructor, target);

        // Verify broadcast
        bukkitMock.verify(() -> Bukkit.broadcastMessage(anyString()), times(1));
        
        // Verify promotion was triggered
        verify(rankManager).promote(target);
    }

    @Test
    void testPassExam_PromotionMessage() {
        Rank newRank = Rank.CORPORAL;
        when(rankManager.promote(target)).thenReturn(CompletableFuture.completedFuture(newRank));
        when(configManager.getMessage(eq("exam_promotion_congrats"), anyString(), anyString()))
                .thenReturn("Congratulations on " + newRank.getDisplay());
        
        bukkitMock.when(() -> Bukkit.broadcastMessage(anyString())).thenReturn(1);

        examManager.passExam(instructor, target);

        // Wait for async completion
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Target should receive promotion message
        verify(target, timeout(500)).sendMessage(anyString());
    }

    @Test
    void testPassExam_MaxRank() {
        // If already max rank, promotion returns null
        when(rankManager.promote(target)).thenReturn(CompletableFuture.completedFuture(null));
        
        bukkitMock.when(() -> Bukkit.broadcastMessage(anyString())).thenReturn(1);

        examManager.passExam(instructor, target);

        // Should still broadcast pass message
        bukkitMock.verify(() -> Bukkit.broadcastMessage(anyString()), atLeastOnce());
        
        // But no promotion congratulations sent
        verify(configManager, never()).getMessage(eq("exam_promotion_congrats"), anyString(), anyString());
    }

    @Test
    void testFailExam() {
        bukkitMock.when(() -> Bukkit.broadcastMessage(anyString())).thenReturn(1);
        when(configManager.getMessage(eq("exam_fail_broadcast"), anyString(), anyString()))
                .thenReturn("Exam failed");

        examManager.failExam(instructor, target);

        // Verify broadcast
        bukkitMock.verify(() -> Bukkit.broadcastMessage(anyString()), times(1));
        
        // Should not trigger promotion
        verify(rankManager, never()).promote(any());
    }

    @Test
    void testMultipleExamSessions() {
        Player target2 = mock(Player.class);
        when(target2.getName()).thenReturn("Target2");
        
        bukkitMock.when(() -> Bukkit.broadcastMessage(anyString())).thenReturn(1);

        examManager.startExamSession(instructor, target, "Physical");
        examManager.startExamSession(instructor, target2, "Written");

        // Both should broadcast
        bukkitMock.verify(() -> Bukkit.broadcastMessage(anyString()), times(2));
    }

    @Test
    void testSTSNearbyDistance() {
        // Test the 50-block distance limit for STS
        Location edgeLoc = mock(Location.class);
        Player edgePlayer = mock(Player.class);
        when(edgePlayer.getLocation()).thenReturn(edgeLoc);
        
        // Exactly at 50 blocks (edge case)
        when(edgeLoc.distance(instructorLoc)).thenReturn(50.0);
        when(world.getPlayers()).thenReturn(Arrays.asList(instructor, edgePlayer));

        examManager.startSTS(instructor);

        // At exactly 50, should NOT receive (< 50)
        verify(edgePlayer, never()).sendMessage(anyString());
    }

    @Test
    void testSTSAtExactBoundary() {
        Location boundaryLoc = mock(Location.class);
        Player boundaryPlayer = mock(Player.class);
        when(boundaryPlayer.getLocation()).thenReturn(boundaryLoc);
        
        // Just inside 50 blocks
        when(boundaryLoc.distance(instructorLoc)).thenReturn(49.9);
        when(world.getPlayers()).thenReturn(Arrays.asList(instructor, boundaryPlayer));

        examManager.startSTS(instructor);

        // Should receive at 49.9
        verify(boundaryPlayer, times(2)).sendMessage(anyString());
        verify(instructor, times(2)).sendMessage(anyString());
        verify(instructor, times(2)).sendMessage(anyString());
    }
}
