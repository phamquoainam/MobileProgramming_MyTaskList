package hcmute.edu.vn.mytasklist.service.reminder;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import hcmute.edu.vn.mytasklist.MainActivity;
import hcmute.edu.vn.mytasklist.R;
import hcmute.edu.vn.mytasklist.data.AppDatabase;
import hcmute.edu.vn.mytasklist.data.dao.TaskDao;
import hcmute.edu.vn.mytasklist.data.entity.TaskEntity;
import hcmute.edu.vn.mytasklist.service.NotificationHelper;

public class TaskReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "TaskReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Handle BOOT_COMPLETED to reschedule all alarms if device restarts
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            rescheduleAllAlarms(context);
            return;
        }

        // Handle specific action intents like Mark Done or Snooze
        String action = intent.getAction();
        if ("ACTION_MARK_DONE".equals(action)) {
            long taskId = intent.getLongExtra("TASK_ID", -1);
            if (taskId != -1) markTaskDone(context, taskId);
            return;
        } else if ("ACTION_SNOOZE".equals(action)) {
            long taskId = intent.getLongExtra("TASK_ID", -1);
            if (taskId != -1) snoozeTask(context, taskId);
            return;
        }

        long taskId = intent.getLongExtra("TASK_ID", -1);
        String taskTitle = intent.getStringExtra("TASK_TITLE");

        Log.d(TAG, "Alarm fired for task: " + taskTitle);

        if (taskId == -1 || taskTitle == null) return;

        showNotification(context, taskId, taskTitle);
    }

    private void showNotification(Context context, long taskId, String taskTitle) {
        // Open App Intent
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(
                context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Mark Done Action Intent
        Intent doneIntent = new Intent(context, TaskReminderReceiver.class);
        doneIntent.setAction("ACTION_MARK_DONE");
        doneIntent.putExtra("TASK_ID", taskId);
        PendingIntent donePendingIntent = PendingIntent.getBroadcast(
                context, (int) taskId, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Snooze Action Intent
        Intent snoozeIntent = new Intent(context, TaskReminderReceiver.class);
        snoozeIntent.setAction("ACTION_SNOOZE");
        snoozeIntent.putExtra("TASK_ID", taskId);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                context, (int) taskId, snoozeIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_REMINDER_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // using default icon for now
                .setContentTitle("Task Reminder")
                .setContentText(taskTitle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(mainPendingIntent)
                .addAction(android.R.drawable.ic_menu_edit, "Mark Done", donePendingIntent)
                .addAction(android.R.drawable.ic_popup_sync, "Snooze 10m", snoozePendingIntent);

        try {
            NotificationManagerCompat.from(context).notify((int) taskId, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing", e);
        }
    }

    private void markTaskDone(Context context, long taskId) {
        NotificationManagerCompat.from(context).cancel((int) taskId);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            TaskDao dao = AppDatabase.getDatabase(context).taskDao();
            dao.updateCompleted(taskId, true);
        });
    }

    private void snoozeTask(Context context, long taskId) {
        NotificationManagerCompat.from(context).cancel((int) taskId);
        AppDatabase.databaseWriteExecutor.execute(() -> {
            TaskDao dao = AppDatabase.getDatabase(context).taskDao();
            TaskEntity task = dao.getTaskById(taskId);
            if (task != null) {
                // Snooze for 10 minutes
                task.setReminderTime(System.currentTimeMillis() + 10 * 60 * 1000);
                dao.update(task);
                TaskReminderHelper.scheduleReminder(context, task);
            }
        });
    }

    private void rescheduleAllAlarms(Context context) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            TaskDao dao = AppDatabase.getDatabase(context).taskDao();
            for (TaskEntity task : dao.getAllTasks()) {
                if (!task.isCompleted() && task.getReminderTime() != null && task.getReminderTime() > System.currentTimeMillis()) {
                    TaskReminderHelper.scheduleReminder(context, task);
                }
            }
        });
    }
}
