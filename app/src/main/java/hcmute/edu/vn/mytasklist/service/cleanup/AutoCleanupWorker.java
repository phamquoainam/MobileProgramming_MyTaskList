package hcmute.edu.vn.mytasklist.service.cleanup;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import hcmute.edu.vn.mytasklist.MainActivity;
import hcmute.edu.vn.mytasklist.data.AppDatabase;
import hcmute.edu.vn.mytasklist.data.dao.TaskDao;
import hcmute.edu.vn.mytasklist.service.NotificationHelper;

public class AutoCleanupWorker extends Worker {

    private static final String TAG = "AutoCleanupWorker";

    public AutoCleanupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "AutoCleanupWorker started");

        try {
            // Delete completed tasks older than 7 days
            long sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000;
            long thresholdTime = System.currentTimeMillis() - sevenDaysInMillis;

            TaskDao dao = AppDatabase.getDatabase(getApplicationContext()).taskDao();

            // Count completed tasks before deleting to report in notification
            int completedCount = dao.getCompletedTasks().size();
            dao.deleteOldCompletedTasks(thresholdTime);
            int remainingCount = dao.getCompletedTasks().size();
            int deletedCount = completedCount - remainingCount;

            Log.d(TAG, "AutoCleanupWorker deleted " + deletedCount + " old tasks successfully");

            // Show notification with cleanup result
            showNotification(deletedCount);

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "AutoCleanupWorker failed", e);
            return Result.failure();
        }
    }

    private void showNotification(int deletedCount) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String message = deletedCount > 0
                ? "Đã dọn dẹp " + deletedCount + " task cũ đã hoàn thành."
                : "Không có task cũ nào cần dọn dẹp.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), NotificationHelper.CHANNEL_SYSTEM_ID)
                .setSmallIcon(android.R.drawable.ic_menu_delete)
                .setContentTitle("Auto Cleanup")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(getApplicationContext()).notify(2003, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing", e);
        }
    }
}
