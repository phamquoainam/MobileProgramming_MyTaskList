package hcmute.edu.vn.mytasklist.service.pomodoro;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.ArrayList;

import hcmute.edu.vn.mytasklist.MainActivity;
import hcmute.edu.vn.mytasklist.data.AppDatabase;
import hcmute.edu.vn.mytasklist.data.entity.PomodoroSession;
import hcmute.edu.vn.mytasklist.service.NotificationHelper;

public class PomodoroForegroundService extends Service {

    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_PAUSE = "ACTION_PAUSE";
    public static final String ACTION_RESUME = "ACTION_RESUME";
    public static final String ACTION_STOP = "ACTION_STOP";
    public static final String ACTION_STOP_MUSIC = "ACTION_STOP_MUSIC";

    public static final String EXTRA_TASK_ID = "EXTRA_TASK_ID";
    public static final String EXTRA_TASK_TITLE = "EXTRA_TASK_TITLE";
    public static final String EXTRA_DURATION = "EXTRA_DURATION"; // focus duration in ms

    private static final int NOTIFICATION_ID = 1001;

    private enum SessionMode {
        FOCUS, BREAK
    }

    private long taskId = -1;
    private String taskTitle = "Pomodoro Task";
    
    private SessionMode currentMode = SessionMode.FOCUS;
    private long focusDurationMs = 25 * 60 * 1000;
    private long breakDurationMs = 5 * 60 * 1000;
    private long durationMs = focusDurationMs;
    private long timeLeftMs = durationMs;
    private long startTimeMs = 0;
    private long totalFocusTimeMs = 0; 

    private CountDownTimer timer;
    private boolean isRunning = false;
    
    // Music Player Variables
    private MediaPlayer mediaPlayer;
    private ArrayList<String> playlistUris = new ArrayList<>();
    private ArrayList<String> playlistTitles = new ArrayList<>();
    private int currentTrackIndex = 0;
    private boolean isMusicActive = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_START:
                    handleStart(intent);
                    break;
                case ACTION_PAUSE:
                    handlePause();
                    break;
                case ACTION_RESUME:
                    handleResume();
                    break;
                case ACTION_STOP_MUSIC:
                    stopMusic();
                    updateNotification("Music Stopped", true);
                    break;
                case ACTION_STOP:
                    handleStop("INTERRUPTED");
                    break;
            }
        }
        return START_STICKY;
    }

    private void handleStart(Intent intent) {
        taskId = intent.getLongExtra(EXTRA_TASK_ID, -1);
        taskTitle = intent.getStringExtra(EXTRA_TASK_TITLE);
        
        focusDurationMs = intent.getLongExtra(EXTRA_DURATION, 25 * 60 * 1000);
        breakDurationMs = intent.getLongExtra("EXTRA_BREAK_DURATION", 5 * 60 * 1000);
        
        ArrayList<String> uris = intent.getStringArrayListExtra("EXTRA_MUSIC_URIS");
        ArrayList<String> titles = intent.getStringArrayListExtra("EXTRA_MUSIC_TITLES");
        
        if (uris != null && !uris.isEmpty()) {
            playlistUris = uris;
            if (titles != null) {
                playlistTitles = titles;
            } else {
                for (String uri : uris) playlistTitles.add("Bản nhạc");
            }
            startMusic();
        }

        currentMode = SessionMode.FOCUS;
        durationMs = focusDurationMs;
        timeLeftMs = durationMs;
        startTimeMs = System.currentTimeMillis();

        startForeground(NOTIFICATION_ID, getNotification("Focusing...", true));
        startTimer();
    }

    private void handlePause() {
        if (timer != null) timer.cancel();
        isRunning = false;
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        String status = (currentMode == SessionMode.FOCUS) ? "Focus Paused" : "Break Paused";
        updateNotification(status, false);
    }

    private void handleResume() {
        String status = (currentMode == SessionMode.FOCUS) ? "Focusing..." : "Resting...";
        if (mediaPlayer != null && !mediaPlayer.isPlaying() && isMusicActive) {
            mediaPlayer.start();
        }
        startForeground(NOTIFICATION_ID, getNotification(status, true));
        startTimer();
    }

    private void handleStop(String status) {
        if (timer != null) timer.cancel();
        isRunning = false;
        
        stopMusic();
        
        if (currentMode == SessionMode.FOCUS) {
            long sessionTime = System.currentTimeMillis() - startTimeMs;
            totalFocusTimeMs += sessionTime;
            saveSession(status);
        }
        
        showSummaryNotification();
        Intent stopIntent = new Intent("ACTION_POMODORO_FINISHED");
        stopIntent.setPackage(getPackageName());
        sendBroadcast(stopIntent);
        stopForeground(true);
        stopSelf();
    }

    private void startTimer() {
        isRunning = true;
        timer = new CountDownTimer(timeLeftMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftMs = millisUntilFinished;
                String status = (currentMode == SessionMode.FOCUS) ? "Focusing..." : "Resting...";
                updateNotification(status, true);
                broadcastTick();
            }

            @Override
            public void onFinish() {
                playChime();
                isRunning = false;
                
                if (currentMode == SessionMode.FOCUS) {
                    saveSession("COMPLETED");
                    totalFocusTimeMs += focusDurationMs; 
                    
                    currentMode = SessionMode.BREAK;
                    durationMs = breakDurationMs;
                    timeLeftMs = durationMs;
                    startTimeMs = System.currentTimeMillis();
                    
                    updateNotification("Resting...", true);
                    startTimer();
                } else {
                    currentMode = SessionMode.FOCUS;
                    durationMs = focusDurationMs;
                    timeLeftMs = durationMs;
                    startTimeMs = System.currentTimeMillis();
                    
                    updateNotification("Focusing...", true);
                    startTimer();
                }
            }
        }.start();
    }
    
    private void startMusic() {
        if (playlistUris.isEmpty()) return;
        isMusicActive = true;
        playTrack(currentTrackIndex);
    }
    
    private void playTrack(int index) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, Uri.parse(playlistUris.get(index)));
            mediaPlayer.prepare();
            mediaPlayer.start();
            
            mediaPlayer.setOnCompletionListener(mp -> {
                currentTrackIndex++;
                if (currentTrackIndex >= playlistUris.size()) {
                    currentTrackIndex = 0; // Loop playlist
                }
                playTrack(currentTrackIndex);
            });
            
        } catch (Exception e) {
            e.printStackTrace();
            // Try next track if current fails
            currentTrackIndex++;
            if (currentTrackIndex < playlistUris.size()) {
                playTrack(currentTrackIndex);
            } else {
                isMusicActive = false;
            }
        }
    }
    
    private void stopMusic() {
        isMusicActive = false;
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    
    private void playChime() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
            r.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Notification getNotification(String statusText, boolean includePauseAction) {
        Intent mainIntent = new Intent(this, hcmute.edu.vn.mytasklist.PomodoroActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        long minutes = timeLeftMs / 60000;
        long seconds = (timeLeftMs % 60000) / 1000;
        String timeStr = String.format("%02d:%02d", minutes, seconds);
        
        String modeEmoji = (currentMode == SessionMode.FOCUS) ? "🍅" : "☕";
        
        String contentText = statusText + " - " + timeStr;
        if (isMusicActive && mediaPlayer != null && mediaPlayer.isPlaying()) {
            contentText += "  |  🎵 Đang phát nhạc";
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_POMODORO_ID)
                .setSmallIcon(android.R.drawable.ic_menu_recent_history)
                .setContentTitle(modeEmoji + " " + taskTitle)
                .setContentText(contentText)
                .setOnlyAlertOnce(true)
                .setOngoing(isRunning)
                .setContentIntent(pendingIntent);

        if (isRunning) {
            builder.addAction(android.R.drawable.ic_media_pause, "Pause", getPendingIntent(ACTION_PAUSE));
        } else if (timeLeftMs > 0 && timeLeftMs < durationMs) {
            builder.addAction(android.R.drawable.ic_media_play, "Resume", getPendingIntent(ACTION_RESUME));
        }
        
        if (isMusicActive) {
            builder.addAction(android.R.drawable.ic_lock_silent_mode, "Tắt nhạc \uD83D\uDD07", getPendingIntent(ACTION_STOP_MUSIC));
        }
        
        builder.addAction(android.R.drawable.ic_delete, "Stop", getPendingIntent(ACTION_STOP));

        return builder.build();
    }

    private void showSummaryNotification() {
        int totalMinutes = (int) (totalFocusTimeMs / 60000);
        if (totalMinutes <= 0) return; 

        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String message = "Chúc mừng bạn đã dành " + totalMinutes + " phút tập trung cho công việc \"" + taskTitle + "\"";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NotificationHelper.CHANNEL_POMODORO_ID)
                .setSmallIcon(android.R.drawable.star_on)
                .setContentTitle("Hoàn thành Pomodoro 🍅")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            // Permission missing
        }
    }

    private void updateNotification(String statusText, boolean includePauseAction) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        try {
            notificationManager.notify(NOTIFICATION_ID, getNotification(statusText, includePauseAction));
        } catch (SecurityException e) {
            // Permission missing
        }
    }

    private PendingIntent getPendingIntent(String action) {
        Intent intent = new Intent(this, PomodoroForegroundService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void saveSession(String status) {
        if (taskId == -1) return;
        AppDatabase.databaseWriteExecutor.execute(() -> {
            long endTime = System.currentTimeMillis();
            long actualDuration = endTime - startTimeMs;
            PomodoroSession session = new PomodoroSession(taskId, startTimeMs, endTime, actualDuration, status);
            AppDatabase.getDatabase(this).pomodoroDao().insert(session);
        });
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMusic();
        if (timer != null) timer.cancel();
    }

    private void broadcastTick() {
        Intent intent = new Intent("ACTION_POMODORO_TICK");
        intent.setPackage(getPackageName());
        intent.putExtra("TIME_LEFT", timeLeftMs);
        intent.putExtra("IS_RUNNING", isRunning);
        intent.putExtra("MODE", currentMode.name());
        intent.putExtra("IS_MUSIC_ACTIVE", isMusicActive && mediaPlayer != null && mediaPlayer.isPlaying());
        
        if (isMusicActive && mediaPlayer != null && mediaPlayer.isPlaying()) {
            if (!playlistUris.isEmpty() && currentTrackIndex < playlistUris.size()) {
                intent.putExtra("TRACK_URI", playlistUris.get(currentTrackIndex));
            }
            if (!playlistTitles.isEmpty() && currentTrackIndex < playlistTitles.size()) {
                intent.putExtra("TRACK_TITLE", playlistTitles.get(currentTrackIndex));
            }
            try {
                intent.putExtra("TRACK_PROGRESS", (long) mediaPlayer.getCurrentPosition());
                intent.putExtra("TRACK_DURATION", (long) mediaPlayer.getDuration());
            } catch (Exception e) {
                // Ignore illegal state
            }
        }
        sendBroadcast(intent);
    }
}
