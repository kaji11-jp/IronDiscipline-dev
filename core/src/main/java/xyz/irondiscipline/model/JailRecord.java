package xyz.irondiscipline.model;

import java.util.UUID;

/**
 * 隔離記録。
 * <p>
 * API モジュールの {@link xyz.irondiscipline.api.model.JailRecord} を継承しています。
 * </p>
 */
public class JailRecord extends xyz.irondiscipline.api.model.JailRecord {

    public JailRecord(UUID playerId, String playerName, String reason, long jailedAt,
                      UUID jailedBy, String originalLocation,
                      String inventoryBackup, String armorBackup) {
        super(playerId, playerName, reason, jailedAt, jailedBy, originalLocation,
              inventoryBackup, armorBackup);
    }
}
