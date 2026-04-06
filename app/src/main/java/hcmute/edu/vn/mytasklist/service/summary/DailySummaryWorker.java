package hcmute.edu.vn.mytasklist.service.summary;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;
import java.util.List;

import hcmute.edu.vn.mytasklist.MainActivity;
import hcmute.edu.vn.mytasklist.data.AppDatabase;
import hcmute.edu.vn.mytasklist.data.entity.TaskEntity;
import hcmute.edu.vn.mytasklist.service.NotificationHelper;

public class DailySummaryWorker extends Worker {

    private static final String TAG = "DailySummaryWorker";

    public DailySummaryWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "DailySummaryWorker started");

        try {
            // Tính toán khoảng thời gian của ngày HÔM QUA
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -1); // Quay ngược lại hôm qua

            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long startOfYesterday = calendar.getTimeInMillis();

            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            long endOfYesterday = calendar.getTimeInMillis();

            // Truy vấn các task có hạn vào hôm qua
            List<TaskEntity> yesterdaysTasks = AppDatabase.getDatabase(getApplicationContext())
                    .taskDao().getTasksByDateRange(startOfYesterday, endOfYesterday);

            int completedCount = 0;
            int incompleteCount = 0;

            for (TaskEntity t : yesterdaysTasks) {
                if (t.isCompleted()) completedCount++;
                else incompleteCount++;
            }

            // Nếu hôm qua có task thì mới hiện báo cáo
            if (completedCount > 0 || incompleteCount > 0) {
                showNotification(completedCount, incompleteCount);
            }

            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "DailySummaryWorker failed", e);
            return Result.failure();
        }
    }

    private void showNotification(int completedCount, int incompleteCount) {
        Context context = getApplicationContext();

        // Intent chính khi người dùng nhấn vào notification
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Nút Action để "Accept" và chuyển sang báo cáo hôm nay (Notification 2)
        Intent actionIntent = new Intent(context, SummaryActionReceiver.class);
        actionIntent.setAction("ACTION_SHOW_TODAY_SUMMARY");
        PendingIntent actionPendingIntent = PendingIntent.getBroadcast(
                context, 100, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String message = "Hôm qua bạn đã hoàn thành " + completedCount + " công việc, còn " + incompleteCount + " công việc chưa hoàn thành.";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_SYSTEM_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Báo cáo Năng suất hôm qua \uD83D\uDCCA")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_media_play, "Bắt đầu ngày mới \u2794", actionPendingIntent);

        try {
            // ID = 2001 (Daily Summary)
            NotificationManagerCompat.from(context).notify(2001, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing", e);
        }
    }
}
