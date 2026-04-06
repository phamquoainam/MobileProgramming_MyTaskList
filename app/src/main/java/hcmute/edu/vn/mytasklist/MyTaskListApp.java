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

        WorkManager workManager = WorkManager.getInstance(this);

        workManager.enqueueUniquePeriodicWork("SyncWork", ExistingPeriodicWorkPolicy.KEEP, syncRequest);
        workManager.enqueueUniquePeriodicWork("DailySummaryWork", ExistingPeriodicWorkPolicy.KEEP, summaryRequest);
        workManager.enqueueUniquePeriodicWork("AutoCleanupWork", ExistingPeriodicWorkPolicy.KEEP, cleanupRequest);
    }

    public AppDatabase getDatabase() {
        return database;
    }
}
