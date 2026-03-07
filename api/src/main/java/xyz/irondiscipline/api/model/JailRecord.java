package xyz.irondiscipline.api.model;

import java.util.UUID;

/**
 * 隔離（収監）記録。
 * <p>
 * プレイヤーが隔離された際の詳細情報を保持する不変オブジェクトです。
 * </p>
 */
public class JailRecord {

    private final UUID playerId;
    private final String playerName;
    private final String reason;
    private final long jailedAt;
    private final UUID jailedBy;
    private final String originalLocation;
    private final String inventoryBackup;
    private final String armorBackup;

    public JailRecord(UUID playerId, String playerName, String reason, long jailedAt,
                      UUID jailedBy, String originalLocation,
                      String inventoryBackup, String armorBackup) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.reason = reason;
        this.jailedAt = jailedAt;
        this.jailedBy = jailedBy;
        this.originalLocation = originalLocation;
        this.inventoryBackup = inventoryBackup;
        this.armorBackup = armorBackup;
    }

    public UUID getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public String getReason() { return reason; }
    public long getJailedAt() { return jailedAt; }
    public UUID getJailedBy() { return jailedBy; }
    public String getOriginalLocation() { return originalLocation; }
    public String getInventoryBackup() { return inventoryBackup; }
    public String getArmorBackup() { return armorBackup; }
}
