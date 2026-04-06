package hcmute.edu.vn.mytasklist.service.reschedule;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Calendar;
import java.util.List;

import hcmute.edu.vn.mytasklist.MainActivity;
import hcmute.edu.vn.mytasklist.R;
import hcmute.edu.vn.mytasklist.data.AppDatabase;
import hcmute.edu.vn.mytasklist.data.entity.TaskEntity;
import hcmute.edu.vn.mytasklist.service.NotificationHelper;

public class SmartRescheduleWorker extends Worker {

    public SmartRescheduleWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        AppDatabase db = AppDatabase.getDatabase(getApplicationContext());

        // Get time range for yesterday
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startOfYesterday = cal.getTimeInMillis();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        long endOfYesterday = cal.getTimeInMillis();

        // Get incomplete tasks due yesterday
        List<TaskEntity> incompleteTasks = db.taskDao().getIncompleteTasksDueToday(startOfYesterday, endOfYesterday);

        if (incompleteTasks != null && !incompleteTasks.isEmpty()) {
            Calendar today = Calendar.getInstance();
            
            for (TaskEntity task : incompleteTasks) {
                if (task.getDueDate() != null) {
                    Calendar taskTime = Calendar.getInstance();
                    taskTime.setTimeInMillis(task.getDueDate());
                    
                    // Keep the hour/minute but set to today
                    today.set(Calendar.HOUR_OF_DAY, taskTime.get(Calendar.HOUR_OF_DAY));
                    today.set(Calendar.MINUTE, taskTime.get(Calendar.MINUTE));
                    today.set(Calendar.SECOND, taskTime.get(Calendar.SECOND));
                    
                    task.setDueDate(today.getTimeInMillis());
                    db.taskDao().update(task);
                }
            }

            // Show notification
            showNotification(incompleteTasks.size());
        }

        return Result.success();
    }

    private void showNotification(int count) {
        android.content.Intent intent = new android.content.Intent(getApplicationContext(), MainActivity.class);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                getApplicationContext(), 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);

        androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(getApplicationContext(), NotificationHelper.CHANNEL_SYSTEM_ID)
                .setSmallIcon(R.drawable.ic_week)
                .setContentTitle("Smart Reschedule")
                .setContentText(count + " công việc chưa hoàn thành từ hôm qua đã được dời sang hôm nay.")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            androidx.core.app.NotificationManagerCompat.from(getApplicationContext()).notify((int) System.currentTimeMillis() + 1, builder.build());
        } catch (SecurityException e) {
            // Ignored
        }
    }
}
