package hcmute.edu.vn.mytasklist;

import android.app.Application;

import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

import hcmute.edu.vn.mytasklist.data.AppDatabase;
import hcmute.edu.vn.mytasklist.service.NotificationHelper;
import hcmute.edu.vn.mytasklist.service.cleanup.AutoCleanupWorker;
import hcmute.edu.vn.mytasklist.service.summary.DailySummaryWorker;
import hcmute.edu.vn.mytasklist.service.sync.SyncWorker;
import hcmute.edu.vn.mytasklist.service.reschedule.SmartRescheduleWorker;
import hcmute.edu.vn.mytasklist.service.report.WeeklyReportWorker;
import hcmute.edu.vn.mytasklist.service.idle.IdleReminderWorker;
import java.util.Calendar;

public class MyTaskListApp extends Application {

    private AppDatabase database;

    @Override
    public void onCreate() {
        super.onCreate();

        // 1. Initialize Notification Channels
        NotificationHelper.createNotificationChannels(this);

        // 2. Initialize Room Database eagerly
        database = AppDatabase.getDatabase(this);

        // 3. Schedule Background Work
        scheduleBackgroundJobs();
    }

    private void scheduleBackgroundJobs() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build();

        // Sync Worker - runs periodically every 15 minutes (minimum interval) when
        // network is available
        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build();

        // Daily Summary Worker - runs once a day
        PeriodicWorkRequest summaryRequest = new PeriodicWorkRequest.Builder(DailySummaryWorker.class, 1, TimeUnit.DAYS)
                .build();

        // Auto Cleanup Worker - runs once a day to clear old done tasks
        PeriodicWorkRequest cleanupRequest = new PeriodicWorkRequest.Builder(AutoCleanupWorker.class, 1, TimeUnit.DAYS)
                .build();

        // Calculate initial delay for Smart Reschedule (7:00 AM)
        Calendar calReschedule = Calendar.getInstance();
        long now = calReschedule.getTimeInMillis();
        calReschedule.set(Calendar.HOUR_OF_DAY, 7);
        calReschedule.set(Calendar.MINUTE, 0);
        calReschedule.set(Calendar.SECOND, 0);
        calReschedule.set(Calendar.MILLISECOND, 0);
        if (calReschedule.getTimeInMillis() < now) {
            calReschedule.add(Calendar.DAY_OF_YEAR, 1);
        }
        long rescheduleDelay = calReschedule.getTimeInMillis() - now;

        PeriodicWorkRequest rescheduleRequest = new PeriodicWorkRequest.Builder(SmartRescheduleWorker.class, 1, TimeUnit.DAYS)
                .setInitialDelay(rescheduleDelay, TimeUnit.MILLISECONDS)
                .build();

        // Calculate initial delay for Weekly Report (Sunday 20:00)
        Calendar calWeekly = Calendar.getInstance();
        calWeekly.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        calWeekly.set(Calendar.HOUR_OF_DAY, 20);
        calWeekly.set(Calendar.MINUTE, 0);
        calWeekly.set(Calendar.SECOND, 0);
        calWeekly.set(Calendar.MILLISECOND, 0);
        if (calWeekly.getTimeInMillis() < now) {
            calWeekly.add(Calendar.WEEK_OF_YEAR, 1);
        }
        long weeklyDelay = calWeekly.getTimeInMillis() - now;

        PeriodicWorkRequest weeklyReportRequest = new PeriodicWorkRequest.Builder(WeeklyReportWorker.class, 7, TimeUnit.DAYS)
                .setInitialDelay(weeklyDelay, TimeUnit.MILLISECONDS)
                .build();

        // Idle Reminder Worker - periodic check every 2 hours
        PeriodicWorkRequest idleReminderRequest = new PeriodicWorkRequest.Builder(IdleReminderWorker.class, 2, TimeUnit.HOURS)
                .build();

        WorkManager workManager = WorkManager.getInstance(this);

        workManager.enqueueUniquePeriodicWork("SyncWork", ExistingPeriodicWorkPolicy.KEEP, syncRequest);
        workManager.enqueueUniquePeriodicWork("DailySummaryWork", ExistingPeriodicWorkPolicy.KEEP, summaryRequest);
        workManager.enqueueUniquePeriodicWork("AutoCleanupWork", ExistingPeriodicWorkPolicy.KEEP, cleanupRequest);
        workManager.enqueueUniquePeriodicWork("SmartRescheduleWork", ExistingPeriodicWorkPolicy.KEEP, rescheduleRequest);
        workManager.enqueueUniquePeriodicWork("WeeklyReportWork", ExistingPeriodicWorkPolicy.KEEP, weeklyReportRequest);
        workManager.enqueueUniquePeriodicWork("IdleReminderWork", ExistingPeriodicWorkPolicy.KEEP, idleReminderRequest);
    }

    public AppDatabase getDatabase() {
        return database;
    }
}
