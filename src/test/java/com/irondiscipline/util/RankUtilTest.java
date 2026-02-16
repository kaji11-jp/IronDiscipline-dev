package com.irondiscipline.util;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.manager.ConfigManager;
import com.irondiscipline.manager.RankManager;
import com.irondiscipline.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * RankUtil テスト
 * 階級ベースの権限チェックロジックをテスト
 */
class RankUtilTest {

    @Mock
    private IronDiscipline plugin;
    @Mock
    private ConfigManager configManager;
    @Mock
    private RankManager rankManager;
    @Mock
    private Player executor;
    @Mock
    private Player target;
    @Mock
    private ConsoleCommandSender console;

    private RankUtil rankUtil;
    private AutoCloseable mocks;
    private MockedStatic<Bukkit> bukkitMock;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        bukkitMock = mockStatic(Bukkit.class);

        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getRankManager()).thenReturn(rankManager);
        when(configManager.getMessage(anyString())).thenReturn("Permission denied");

        rankUtil = new RankUtil(plugin);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (bukkitMock != null) bukkitMock.close();
        if (mocks != null) mocks.close();
    }

    @Test
    void testCanExecuteCommand_Console() {
        // Console should always be able to execute
        assertTrue(rankUtil.canExecuteCommand(console));
        
        // Should not check rank for console
        verify(rankManager, never()).getRank(any(Player.class));
    }

    @Test
    void testCanExecuteCommand_SufficientRank() {
        when(rankManager.getRank(executor)).thenReturn(Rank.LIEUTENANT);
        
        assertTrue(rankUtil.canExecuteCommand(executor));
        verify(executor, never()).sendMessage(anyString());
    }

    @Test
    void testCanExecuteCommand_HighRank() {
        when(rankManager.getRank(executor)).thenReturn(Rank.COMMANDER);
        
        assertTrue(rankUtil.canExecuteCommand(executor));
    }

    @Test
    void testCanExecuteCommand_InsufficientRank() {
        when(rankManager.getRank(executor)).thenReturn(Rank.PRIVATE);
        
        assertFalse(rankUtil.canExecuteCommand(executor));
        verify(executor).sendMessage(anyString());
    }

    @Test
    void testCanExecuteCommand_Corporal() {
        // Corporal (weight 20) is below Lieutenant (weight 40)
        when(rankManager.getRank(executor)).thenReturn(Rank.CORPORAL);
        
        assertFalse(rankUtil.canExecuteCommand(executor));
    }

    @Test
    void testCanOperateOn_Console() {
        when(target.getName()).thenReturn("Target");
        
        // Console can operate on anyone
        assertTrue(rankUtil.canOperateOn(console, target));
    }

    @Test
    void testCanOperateOn_HigherRank() {
        when(rankManager.getRank(executor)).thenReturn(Rank.MAJOR);
        when(rankManager.getRank(target)).thenReturn(Rank.SERGEANT);
        
        assertTrue(rankUtil.canOperateOn(executor, target));
        verify(executor, never()).sendMessage(anyString());
    }

    @Test
    void testCanOperateOn_SameRank() {
        when(rankManager.getRank(executor)).thenReturn(Rank.CAPTAIN);
        when(rankManager.getRank(target)).thenReturn(Rank.CAPTAIN);
        when(target.getName()).thenReturn("Target");
        
        // Same rank = cannot operate
        assertFalse(rankUtil.canOperateOn(executor, target));
        verify(executor).sendMessage(anyString());
    }

    @Test
    void testCanOperateOn_LowerRank() {
        when(rankManager.getRank(executor)).thenReturn(Rank.PRIVATE);
        when(rankManager.getRank(target)).thenReturn(Rank.COLONEL);
        when(target.getName()).thenReturn("Target");
        
        assertFalse(rankUtil.canOperateOn(executor, target));
        verify(executor).sendMessage(anyString());
    }

    @Test
    void testCanOperateOn_UUID_Online() {
        UUID targetId = UUID.randomUUID();
        when(target.getUniqueId()).thenReturn(targetId);
        when(rankManager.getRank(executor)).thenReturn(Rank.MAJOR);
        when(rankManager.getRank(target)).thenReturn(Rank.SERGEANT);
        
        bukkitMock.when(() -> Bukkit.getPlayer(targetId)).thenReturn(target);
        
        assertTrue(rankUtil.canOperateOn(executor, targetId));
    }

    @Test
    void testCanOperateOn_UUID_Offline() {
        UUID targetId = UUID.randomUUID();
        
        bukkitMock.when(() -> Bukkit.getPlayer(targetId)).thenReturn(null);
        
        // Offline players should be operable (can't check rank)
        assertTrue(rankUtil.canOperateOn(executor, targetId));
    }

    @Test
    void testCheckAll_BothPass() {
        when(rankManager.getRank(executor)).thenReturn(Rank.LIEUTENANT);
        when(rankManager.getRank(target)).thenReturn(Rank.PRIVATE);
        
        assertTrue(rankUtil.checkAll(executor, target));
    }

    @Test
    void testCheckAll_CommandFails() {
        when(rankManager.getRank(executor)).thenReturn(Rank.CORPORAL); // Below Lieutenant
        when(rankManager.getRank(target)).thenReturn(Rank.PRIVATE);
        
        assertFalse(rankUtil.checkAll(executor, target));
    }

    @Test
    void testCheckAll_OperateFails() {
        when(rankManager.getRank(executor)).thenReturn(Rank.LIEUTENANT);
        when(rankManager.getRank(target)).thenReturn(Rank.MAJOR); // Higher than executor
        when(target.getName()).thenReturn("Target");
        
        assertFalse(rankUtil.checkAll(executor, target));
    }

    @Test
    void testCheckAll_BothFail() {
        when(rankManager.getRank(executor)).thenReturn(Rank.PRIVATE);
        when(rankManager.getRank(target)).thenReturn(Rank.COMMANDER);
        when(target.getName()).thenReturn("Target");
        
        assertFalse(rankUtil.checkAll(executor, target));
    }

    @Test
    void testExactlyLieutenantCanExecute() {
        // Test the boundary - Lieutenant should be able to execute
        when(rankManager.getRank(executor)).thenReturn(Rank.LIEUTENANT);
        
        assertTrue(rankUtil.canExecuteCommand(executor));
    }

    @Test
    void testJustBelowLieutenantCannotExecute() {
        // Sergeant (weight 30) is below Lieutenant (weight 40)
        when(rankManager.getRank(executor)).thenReturn(Rank.SERGEANT);
        
        assertFalse(rankUtil.canExecuteCommand(executor));
    }
}
