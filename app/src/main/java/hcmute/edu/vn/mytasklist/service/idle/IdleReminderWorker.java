package hcmute.edu.vn.mytasklist.service.idle;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import hcmute.edu.vn.mytasklist.MainActivity;
import hcmute.edu.vn.mytasklist.R;
import hcmute.edu.vn.mytasklist.data.AppDatabase;
import hcmute.edu.vn.mytasklist.service.NotificationHelper;

public class IdleReminderWorker extends Worker {

    public IdleReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE);
        long lastActiveTime = prefs.getLong("last_active_time", 0);
        long now = System.currentTimeMillis();

        if (lastActiveTime > 0) {
            long hoursIdle = (now - lastActiveTime) / (1000 * 60 * 60);

            if (hoursIdle >= 6) {
                AppDatabase db = AppDatabase.getDatabase(getApplicationContext());
                int pendingTasks = db.taskDao().getPendingTaskCount();

                if (pendingTasks > 0) {
                    showNotification(pendingTasks);
                    
                    // Reset or forward the time to avoid immediate spamming
                    // so it asks again in another 6 hours minimum
                    prefs.edit().putLong("last_active_time", now).apply();
                }
            }
        }

        return Result.success();
    }

    private void showNotification(int pendingTasks) {
        android.content.Intent intent = new android.content.Intent(getApplicationContext(), MainActivity.class);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                getApplicationContext(), 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(getApplicationContext(), NotificationHelper.CHANNEL_SYSTEM_ID)
                .setSmallIcon(R.drawable.ic_inbox)
                .setContentTitle("Nhắc nhở")
                .setContentText("Bạn vẫn còn " + pendingTasks + " công việc chưa hoàn thành, hãy quay lại tiếp tục nhé!")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            androidx.core.app.NotificationManagerCompat.from(getApplicationContext()).notify((int) System.currentTimeMillis() + 3, builder.build());
        } catch (SecurityException e) {
            // Ignored
        }
    }
}
