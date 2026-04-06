package hcmute.edu.vn.mytasklist.service.deadline;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import hcmute.edu.vn.mytasklist.MainActivity;
import hcmute.edu.vn.mytasklist.data.AppDatabase;
import hcmute.edu.vn.mytasklist.data.dao.TaskDao;
import hcmute.edu.vn.mytasklist.data.entity.TaskEntity;
import hcmute.edu.vn.mytasklist.service.NotificationHelper;

public class DeadlineWarningReceiver extends BroadcastReceiver {

    private static final String TAG = "DeadlineWarningReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            rescheduleAllWarnings(context);
            return;
        }

        long taskId = intent.getLongExtra("TASK_ID", -1);
        String taskTitle = intent.getStringExtra("TASK_TITLE");
        long offsetMs = intent.getLongExtra("OFFSET_MS", 0);

        if (taskId == -1 || taskTitle == null) return;

        String offsetText = getOffsetText(offsetMs);

        // Intent to open the app
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(
                context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_REMINDER_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Deadline Approaching!")
                .setContentText("Task '" + taskTitle + "' is due in " + offsetText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(mainPendingIntent);

        try {
            NotificationManagerCompat.from(context).notify((int) (taskId + offsetMs), builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing", e);
        }
    }

    private String getOffsetText(long offsetMs) {
        if (offsetMs == 60 * 60 * 1000) return "1 hour";
        if (offsetMs == 10 * 60 * 1000) return "10 minutes";
        long minutes = offsetMs / 60000;
        return minutes + " minutes";
    }

    private void rescheduleAllWarnings(Context context) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            TaskDao dao = AppDatabase.getDatabase(context).taskDao();
            for (TaskEntity task : dao.getAllTasks()) {
                DeadlineWarningHelper.scheduleDeadlineWarnings(context, task);
            }
        });
    }
}
