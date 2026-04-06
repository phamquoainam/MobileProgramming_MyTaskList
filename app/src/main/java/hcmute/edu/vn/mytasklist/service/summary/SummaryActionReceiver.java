package hcmute.edu.vn.mytasklist.service.summary;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;
import java.util.List;

import hcmute.edu.vn.mytasklist.MainActivity;
import hcmute.edu.vn.mytasklist.data.AppDatabase;
import hcmute.edu.vn.mytasklist.data.entity.TaskEntity;
import hcmute.edu.vn.mytasklist.service.NotificationHelper;

public class SummaryActionReceiver extends BroadcastReceiver {

    private static final String TAG = "SummaryActionReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && "ACTION_SHOW_TODAY_SUMMARY".equals(intent.getAction())) {
            
            // Xóa Notification 1 (Báo cáo hôm qua)
            try {
                NotificationManagerCompat.from(context).cancel(2001);
            } catch (Exception e) {
                Log.e(TAG, "Failed to cancel previous notification", e);
            }

            // Chạy query ngầm do BroadcastReceiver chạy trên main thread
            AppDatabase.databaseWriteExecutor.execute(() -> {
                showMorningSummary(context);
            });
        }
    }

    private void showMorningSummary(Context context) {
        Calendar cal = Calendar.getInstance();
        
        // Hôm nay
        cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        long todayStart = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999);
        long todayEnd = cal.getTimeInMillis();

        AppDatabase db = AppDatabase.getDatabase(context);
        List<TaskEntity> todayTasks = db.taskDao().getIncompleteTasksDueToday(todayStart, todayEnd);

        String message = "";
        
        if (todayTasks != null && todayTasks.size() > 0) {
            message = "Chào buổi sáng ☀️! Hôm nay bạn có " + todayTasks.size() + " việc cần phải hoàn thành. Quyết tâm nhé!";
        } else {
            // Không có task hôm nay, tra cứu 7 ngày tới
            long tmrStart = todayEnd + 1; // Start of tomorrow
            cal.setTimeInMillis(tmrStart);
            cal.add(Calendar.DAY_OF_YEAR, 6); // Up to day 7
            cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59); cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999);
            long endOf7Days = cal.getTimeInMillis();

            List<TaskEntity> next7DaysTasks = db.taskDao().getTasksByDateRange(tmrStart, endOf7Days);
            
            int upcomingIncomplete = 0;
            for (TaskEntity t : next7DaysTasks) {
                if (!t.isCompleted()) upcomingIncomplete++;
            }

            if (upcomingIncomplete > 0) {
                message = "Chào buổi sáng \uD83D\uDE0E! Hôm nay thật thảnh thơi, nhưng trong 7 ngày tới bạn có " + upcomingIncomplete + " việc đang chờ đấy nhé!";
            } else {
                message = "Chào buổi sáng \uD83C\uDFDD️! Tuần này bạn hoàn toàn trống lịch, hãy tranh thủ nghỉ ngơi và tận hưởng trọn vẹn nhé!";
            }
        }

        // Bắn Notification 2
        Intent mainIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NotificationHelper.CHANNEL_SYSTEM_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Bắt đầu ngày mới!")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(context).notify(2005, builder.build()); // ID=2005 cho Morning Summary
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing", e);
        }
    }
}
