package com.irondiscipline.util;

import com.irondiscipline.IronDiscipline;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import space.arim.morepaperlib.MorePaperLib;
import space.arim.morepaperlib.scheduling.ScheduledTask;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Folia 専用タスクスケジューラー
 * MorePaperLib を使用した Folia ネイティブ実装
 */
public class TaskScheduler {

    private final MorePaperLib morePaperLib;

    public TaskScheduler(IronDiscipline plugin) {
        this.morePaperLib = new MorePaperLib(plugin);
    }

    /**
     * 非同期タスクを実行
     */
    public ScheduledTask runAsync(Runnable runnable) {
        return morePaperLib.scheduling().asyncScheduler().run(runnable);
    }

    /**
     * グローバルタスクを実行 (Foliaでは非推奨だが、全体に関わる処理に使用)
     */
    public ScheduledTask runGlobal(Runnable runnable) {
        return morePaperLib.scheduling().globalRegionalScheduler().run(runnable);
    }
    
    /**
     * 遅延タスク (グローバル)
     * @param ticks 遅延Tick
     */
    public ScheduledTask runGlobalLater(Runnable runnable, long ticks) {
        return morePaperLib.scheduling().globalRegionalScheduler().runDelayed(runnable, ticks);
    }

    /**
     * 定期タスク (グローバル)
     * @param delayTicks 開始遅延
     * @param periodTicks 周期
     */
    public ScheduledTask runGlobalTimer(Runnable runnable, long delayTicks, long periodTicks) {
        return morePaperLib.scheduling().globalRegionalScheduler().runAtFixedRate(runnable, delayTicks, periodTicks);
    }

    /**
     * 定期タスク (グローバル) - タスク自己参照可能
     */
    public void runGlobalTimer(java.util.function.Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        morePaperLib.scheduling().globalRegionalScheduler().runAtFixedRate(task, delayTicks, periodTicks);
    }

    /**
     * エンティティに関連付けられたタスクを実行 (Folia対応)
     */
    public ScheduledTask runEntity(Entity entity, Runnable runnable) {
        return morePaperLib.scheduling().entitySpecificScheduler(entity).run(runnable, null);
    }
    
    /**
     * エンティティに関連付けられた遅延タスクを実行 (Folia対応)
     */
    public ScheduledTask runEntityLater(Entity entity, Runnable runnable, long delayTicks) {
         // EntityScheduler in MorePaperLib wraps Folia's runDelayed.
        return morePaperLib.scheduling().entitySpecificScheduler(entity).runDelayed(runnable, null, delayTicks);
    }

    /**
     * エンティティに関連付けられた定期タスクを実行 (Folia対応)
     */
    public ScheduledTask runEntityTimer(Entity entity, Runnable runnable, long delayTicks, long periodTicks) {
        return morePaperLib.scheduling().entitySpecificScheduler(entity).runAtFixedRate(runnable, null, delayTicks, periodTicks);
    }

    /**
     * エンティティに関連付けられた定期タスクを実行 (Folia対応) - タスク自己参照可能
     */
    public void runEntityTimer(Entity entity, java.util.function.Consumer<ScheduledTask> task, long delayTicks, long periodTicks) {
        morePaperLib.scheduling().entitySpecificScheduler(entity).runAtFixedRate(task, null, delayTicks, periodTicks);
    }

    /**
     * 特定の場所でタスクを実行 (Folia対応)
     */
    public ScheduledTask runRegion(Location location, Runnable runnable) {
        return morePaperLib.scheduling().regionSpecificScheduler(location).run(runnable);
    }

    /**
     * スケジューラーを停止 (キャンセルはTaskオブジェクトから行う)
     */
    public void cancel(ScheduledTask task) {
        if (task != null) {
            task.cancel();
        }
    }
}
