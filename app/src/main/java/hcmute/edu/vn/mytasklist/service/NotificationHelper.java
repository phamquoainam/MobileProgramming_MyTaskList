package hcmute.edu.vn.mytasklist.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

public class NotificationHelper {

    public static final String CHANNEL_REMINDER_ID = "channel_task_reminder";
    public static final String CHANNEL_POMODORO_ID = "channel_pomodoro";
    public static final String CHANNEL_SYSTEM_ID = "channel_system_services";

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

            // 1. Task Reminder Channel
            NotificationChannel reminderChannel = new NotificationChannel(
                    CHANNEL_REMINDER_ID,
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            reminderChannel.setDescription("Notifications for expected tasks and deadlines");
            reminderChannel.enableLights(true);
            reminderChannel.setLightColor(Color.RED);
            reminderChannel.enableVibration(true);

            // 2. Pomodoro Channel (Foreground Service)
            NotificationChannel pomodoroChannel = new NotificationChannel(
                    CHANNEL_POMODORO_ID,
                    "Pomodoro Timer",
                    NotificationManager.IMPORTANCE_LOW // Low so it doesn't pop up and make sound every second
            );
            pomodoroChannel.setDescription("Persistent notification for Pomodoro timer");

            // 3. System Services Channel (Sync, Summary, Cleanup)
            NotificationChannel systemChannel = new NotificationChannel(
                    CHANNEL_SYSTEM_ID,
                    "System Events",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            systemChannel.setDescription("Background jobs, daily summaries, etc.");

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(reminderChannel);
                notificationManager.createNotificationChannel(pomodoroChannel);
                notificationManager.createNotificationChannel(systemChannel);
            }
        }
    }
}
