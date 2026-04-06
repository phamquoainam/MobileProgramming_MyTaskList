package hcmute.edu.vn.mytasklist.service.sync;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import hcmute.edu.vn.mytasklist.MainActivity;
import hcmute.edu.vn.mytasklist.data.AppDatabase;
import hcmute.edu.vn.mytasklist.data.entity.TaskEntity;
import hcmute.edu.vn.mytasklist.service.NotificationHelper;

public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "SyncWorker started");

        try {
            // Mock offline-first sync approach: read from Room and save to local JSON backup
            List<TaskEntity> allTasks = AppDatabase.getDatabase(getApplicationContext()).taskDao().getAllTasks();

            JSONArray jsonArray = new JSONArray();
            for (TaskEntity task : allTasks) {
                JSONObject obj = new JSONObject();
                obj.put("id", task.getId());
                obj.put("title", task.getTitle());
                obj.put("completed", task.isCompleted());
                obj.put("due_date", task.getDueDate() == null ? JSONObject.NULL : task.getDueDate());
                obj.put("priority", task.getPriority());
                obj.put("category", task.getCategory());
                jsonArray.put(obj);
            }

            File backupFile = new File(getApplicationContext().getFilesDir(), "tasks_backup.json");
            FileWriter writer = new FileWriter(backupFile);
            writer.write(jsonArray.toString(4));
            writer.flush();
            writer.close();

            Log.d(TAG, "SyncWorker completed successfully. Data synced to: " + backupFile.getAbsolutePath());

            // Show notification on successful sync
            showNotification(allTasks.size());

            return Result.success(new Data.Builder()
                    .putString("SYNC_STATUS", "SUCCESS")
                    .build());

        } catch (JSONException | IOException e) {
            Log.e(TAG, "SyncWorker failed", e);
            return Result.retry();
        } catch (Exception e) {
            Log.e(TAG, "SyncWorker unexpected failure", e);
            return Result.failure();
        }
    }

    private void showNotification(int taskCount) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), NotificationHelper.CHANNEL_SYSTEM_ID)
                .setSmallIcon(android.R.drawable.ic_popup_sync)
                .setContentTitle("Sync Completed")
                .setContentText("Đã sao lưu " + taskCount + " tasks thành công.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat.from(getApplicationContext()).notify(2002, builder.build());
        } catch (SecurityException e) {
            Log.e(TAG, "Notification permission missing", e);
        }
    }
}
