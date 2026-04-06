package hcmute.edu.vn.mytasklist.service.deadline;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import hcmute.edu.vn.mytasklist.data.entity.TaskEntity;

public class DeadlineWarningHelper {

    private static final String TAG = "DeadlineWarningHelper";

    public static void scheduleDeadlineWarnings(Context context, TaskEntity task) {
        if (task.getDueDate() == null || task.isCompleted() || task.getReminderOffsets() == null) {
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarms. Permission denied.");
                return;
            }
        }

        try {
            JSONArray offsets = new JSONArray(task.getReminderOffsets());
            for (int i = 0; i < offsets.length(); i++) {
                long offsetMs = offsets.getLong(i);
                long warningTime = task.getDueDate() - offsetMs;

                if (warningTime > System.currentTimeMillis()) {
                    Intent intent = new Intent(context, DeadlineWarningReceiver.class);
                    intent.putExtra("TASK_ID", task.getId());
                    intent.putExtra("TASK_TITLE", task.getTitle());
                    intent.putExtra("OFFSET_MS", offsetMs);

                    // Unique request code per task and offset
                    int requestCode = (int) (task.getId() * 100 + i);

                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            context,
                            requestCode,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );

                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            warningTime,
                            pendingIntent
                    );
                    Log.d(TAG, "Scheduled deadline warning for task " + task.getId() + " at " + warningTime);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse reminder offsets", e);
        }
    }

    public static void cancelDeadlineWarnings(Context context, TaskEntity task) {
        if (task.getReminderOffsets() == null) return;

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        try {
            JSONArray offsets = new JSONArray(task.getReminderOffsets());
            for (int i = 0; i < offsets.length(); i++) {
                Intent intent = new Intent(context, DeadlineWarningReceiver.class);
                int requestCode = (int) (task.getId() * 100 + i);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                alarmManager.cancel(pendingIntent);
            }
            Log.d(TAG, "Cancelled all deadline warnings for task " + task.getId());
        } catch (JSONException e) {
            Log.e(TAG, "Failed to parse reminder offsets on cancel", e);
        }
    }
}
