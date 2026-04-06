package hcmute.edu.vn.mytasklist.service.report;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;

import hcmute.edu.vn.mytasklist.MainActivity;
import hcmute.edu.vn.mytasklist.R;
import hcmute.edu.vn.mytasklist.data.AppDatabase;
import hcmute.edu.vn.mytasklist.service.NotificationHelper;

public class WeeklyReportWorker extends Worker {

    public WeeklyReportWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());

        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.add(Calendar.DAY_OF_YEAR, -7);
        long weekAgo = cal.getTimeInMillis();

        int completedTasks = db.taskDao().getCompletedTaskCountByDateRange(weekAgo, now);
        int incompleteTasks = db.taskDao().getIncompleteTaskCountByDateRange(weekAgo, now);
        Long focusMs = db.pomodoroDao().getCompletedPomodoroDuration(weekAgo, now);
        if (focusMs == null) focusMs = 0L;

        long focusHours = focusMs / (1000 * 60 * 60);
        long focusMins = (focusMs / (1000 * 60)) % 60;
        
        String timeStr = focusHours > 0 ? focusHours + "h " + focusMins + "m" : focusMins + "m";

        String message = "Tuần này bạn đã hoàn thành " + completedTasks + " công việc, còn " + incompleteTasks + " việc tồn đọng. \nThời gian tập trung Pomodoro: " + timeStr + ".";

        showNotification(message);

        return Result.success();
    }

    private void showNotification(String message) {
        android.content.Intent intent = new android.content.Intent(getApplicationContext(), MainActivity.class);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                getApplicationContext(), 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(getApplicationContext(), NotificationHelper.CHANNEL_SYSTEM_ID)
                .setSmallIcon(R.drawable.ic_week)
                .setContentTitle("Báo cáo Tuần của bạn")
                .setContentText(message)
                .setStyle(new androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            androidx.core.app.NotificationManagerCompat.from(getApplicationContext()).notify((int) System.currentTimeMillis() + 2, builder.build());
        } catch (SecurityException e) {
            // Ignored
        }
    }
}
