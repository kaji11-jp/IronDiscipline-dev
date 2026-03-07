package xyz.irondiscipline.model;

/**
 * 戦闘ログエントリ。
 * <p>
 * API モジュールの {@link xyz.irondiscipline.api.model.KillLog} を継承しています。
 * Core 内部では引き続きこのクラスを使用できます。
 * </p>
 *
 * @see xyz.irondiscipline.api.model.KillLog
 */
public class KillLog extends xyz.irondiscipline.api.model.KillLog {

    public KillLog(long id, long timestamp, java.util.UUID killerId, String killerName,
                   java.util.UUID victimId, String victimName, String weapon, double distance,
                   String world, double x, double y, double z) {
        super(id, timestamp, killerId, killerName, victimId, victimName, weapon, distance,
              world, x, y, z);
    }
}
