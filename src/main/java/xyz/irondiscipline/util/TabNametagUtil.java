package xyz.irondiscipline.util;

import xyz.irondiscipline.model.Rank;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Tab & ネームタグユーティリティ
 * 階級に応じたTabリストとネームタグの即時更新
 */
public class TabNametagUtil {

    private static final String TEAM_PREFIX = "iron_";

    /**
     * プレイヤーのTab/ネームタグを更新
     */
    public static void updatePlayer(Player player, Rank rank, String divisionDisplay) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        
        // チーム名（ソート順のため重みを使用）
        String teamName = TEAM_PREFIX + String.format("%03d", 100 - rank.getWeight());
        
        // 既存チームから削除
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.removeEntry(player.getName());
            }
        }
        
        // チーム取得または作成
        Team team = scoreboard.getTeam(teamName);
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName);
        }
        
        // プレフィックス設定（階級のみ、スペースなし、RESETで色リセット）
        String prefix = rank.getDisplay() + ChatColor.RESET;
        if (prefix.length() > 64) {
            prefix = prefix.substring(0, 64);
        }
        team.setPrefix(prefix);
        
        // サフィックス設定（部隊表示）
        String suffix = "";
        if (divisionDisplay != null && !divisionDisplay.isEmpty()) {
            suffix = divisionDisplay;
            if (suffix.length() > 64) {
                suffix = suffix.substring(0, 64);
            }
        }
        team.setSuffix(suffix);
        
        // チームにプレイヤーを追加
        team.addEntry(player.getName());
        
        // Tabリストのヘッダー表示名も更新
        // フォーマット: [階級]ユーザーネーム[部隊所属]
        String displayName = rank.getDisplay() + ChatColor.WHITE + player.getName();
        if (divisionDisplay != null && !divisionDisplay.isEmpty()) {
            displayName += divisionDisplay;
        }
        player.setPlayerListName(displayName);
    }
    
    /**
     * プレイヤーのTab/ネームタグを更新（部隊なしの互換用）
     * divisionDisplayにnullを渡して呼び出し、部隊表示なしでTab/ネームタグを更新
     */
    public static void updatePlayer(Player player, Rank rank) {
        updatePlayer(player, rank, null);
    }

    /**
     * 全プレイヤーのTab/ネームタグを再構築
     */
    public static void refreshAll() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        
        // 既存チームをクリア
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.unregister();
            }
        }
    }

    /**
     * プレイヤー退出時のクリーンアップ
     */
    public static void cleanup(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team team : scoreboard.getTeams()) {
            if (team.getName().startsWith(TEAM_PREFIX)) {
                team.removeEntry(player.getName());
            }
        }
    }
}
