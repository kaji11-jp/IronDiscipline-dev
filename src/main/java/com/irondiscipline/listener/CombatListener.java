package com.irondiscipline.listener;

import com.irondiscipline.IronDiscipline;
import com.irondiscipline.model.KillLog;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * 戦闘リスナー
 * 詳細な戦闘ログ（距離・武器）を記録
 */
public class CombatListener implements Listener {

    private final IronDiscipline plugin;

    public CombatListener(IronDiscipline plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        // 戦闘によるデスのみログ
        if (killer == null) {
            return;
        }
        
        // 距離計算
        double distance = calculateDistance(killer.getLocation(), victim.getLocation());
        
        // 使用武器取得
        String weapon = getWeaponName(killer);
        
        // ログエントリ作成
        KillLog log = KillLog.builder()
            .killer(killer.getUniqueId(), killer.getName())
            .victim(victim.getUniqueId(), victim.getName())
            .weapon(weapon)
            .distance(distance)
            .location(
                victim.getLocation().getWorld().getName(),
                victim.getLocation().getX(),
                victim.getLocation().getY(),
                victim.getLocation().getZ()
            )
            .build();
        
        // 非同期でDB保存
        plugin.getStorageManager().saveKillLogAsync(log);
        
        // デバッグログ
        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info(String.format(
                "[KillLog] %s killed %s with %s at %.1fm",
                killer.getName(), victim.getName(), weapon, distance
            ));
        }
    }

    /**
     * 2点間の距離を計算
     */
    private double calculateDistance(Location loc1, Location loc2) {
        if (!loc1.getWorld().equals(loc2.getWorld())) {
            return -1; // 異なるワールド
        }
        return loc1.distance(loc2);
    }

    /**
     * 使用武器の名前を取得
     */
    private String getWeaponName(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType() == Material.AIR) {
            return plugin.getConfigManager().getRawMessage("weapon_hand");
        }
        
        // カスタム名がある場合はそれを使用
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                return meta.getDisplayName();
            }
        }
        
        // マテリアル名を日本語化
        return translateWeaponName(item.getType());
    }

    /**
     * 武器名を日本語に翻訳
     */
    private String translateWeaponName(Material material) {
        String name = material.name();
        
        // よく使われる武器の翻訳
        return switch (name) {
            case "DIAMOND_SWORD" -> plugin.getConfigManager().getRawMessage("weapon_diamond_sword");
            case "IRON_SWORD" -> plugin.getConfigManager().getRawMessage("weapon_iron_sword");
            case "GOLDEN_SWORD" -> plugin.getConfigManager().getRawMessage("weapon_golden_sword");
            case "STONE_SWORD" -> plugin.getConfigManager().getRawMessage("weapon_stone_sword");
            case "WOODEN_SWORD" -> plugin.getConfigManager().getRawMessage("weapon_wooden_sword");
            case "NETHERITE_SWORD" -> plugin.getConfigManager().getRawMessage("weapon_netherite_sword");
            case "BOW" -> plugin.getConfigManager().getRawMessage("weapon_bow");
            case "CROSSBOW" -> plugin.getConfigManager().getRawMessage("weapon_crossbow");
            case "TRIDENT" -> plugin.getConfigManager().getRawMessage("weapon_trident");
            case "DIAMOND_AXE" -> plugin.getConfigManager().getRawMessage("weapon_diamond_axe");
            case "IRON_AXE" -> plugin.getConfigManager().getRawMessage("weapon_iron_axe");
            case "NETHERITE_AXE" -> plugin.getConfigManager().getRawMessage("weapon_netherite_axe");
            default -> name.replace("_", " ").toLowerCase();
        };
    }
}
