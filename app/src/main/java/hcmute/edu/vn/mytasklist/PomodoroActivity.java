package hcmute.edu.vn.mytasklist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import hcmute.edu.vn.mytasklist.service.pomodoro.PomodoroForegroundService;

public class PomodoroActivity extends AppCompatActivity {

    private TextView tvModeEmoji;
    private TextView tvTimer;
    private TextView tvTrackName;
    private TextView tvTrackTime;
    private FloatingActionButton fabPauseResume;
    private FloatingActionButton fabStop;

    private boolean isRunning = true;
    private BroadcastReceiver pomodoroReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Keep screen on while focusing
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.activity_pomodoro);

        tvModeEmoji = findViewById(R.id.tvModeEmoji);
        tvTimer = findViewById(R.id.tvTimer);
        tvTrackName = findViewById(R.id.tvTrackName);
        tvTrackTime = findViewById(R.id.tvTrackTime);
        fabPauseResume = findViewById(R.id.fabPauseResume);
        fabStop = findViewById(R.id.fabStop);

        fabPauseResume.setOnClickListener(v -> {
            Intent intent = new Intent(this, PomodoroForegroundService.class);
            intent.setAction(isRunning ? PomodoroForegroundService.ACTION_PAUSE : PomodoroForegroundService.ACTION_RESUME);
            startService(intent);
        });

        fabStop.setOnClickListener(v -> {
            Intent intent = new Intent(this, PomodoroForegroundService.class);
            intent.setAction(PomodoroForegroundService.ACTION_STOP);
            startService(intent);
            finish();
        });

        setupBroadcastReceiver();
    }

    private void setupBroadcastReceiver() {
        pomodoroReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent == null || intent.getAction() == null) return;
                
                if ("ACTION_POMODORO_TICK".equals(intent.getAction())) {
                    long timeLeftMs = intent.getLongExtra("TIME_LEFT", 0);
                    boolean running = intent.getBooleanExtra("IS_RUNNING", false);
                    String mode = intent.getStringExtra("MODE");
                    
                    isRunning = running;

                    long minutes = timeLeftMs / 60000;
                    long seconds = (timeLeftMs % 60000) / 1000;
                    tvTimer.setText(String.format("%02d:%02d", minutes, seconds));

                    tvModeEmoji.setText("FOCUS".equals(mode) ? "🍅" : "☕");
                    
                    if (isRunning) {
                        fabPauseResume.setImageResource(android.R.drawable.ic_media_pause);
                    } else {
                        fabPauseResume.setImageResource(android.R.drawable.ic_media_play);
                    }

                    boolean isMusicActive = intent.getBooleanExtra("IS_MUSIC_ACTIVE", false);
                    if (isMusicActive) {
                        String trackTitle = intent.getStringExtra("TRACK_TITLE");
                        if (trackTitle == null || trackTitle.isEmpty()) trackTitle = "Đang phát nhạc...";
                        
                        long trackProgress = intent.getLongExtra("TRACK_PROGRESS", 0);
                        long trackDuration = intent.getLongExtra("TRACK_DURATION", 1);
                        
                        tvTrackName.setText("🎵 " + trackTitle);
                        
                        tvTrackTime.setText(formatTime(trackProgress) + " / " + formatTime(trackDuration));
                        tvTrackName.setVisibility(View.VISIBLE);
                        tvTrackTime.setVisibility(View.VISIBLE);
                    } else {
                        tvTrackName.setText("🎵 Đang không phát nhạc");
                        tvTrackTime.setVisibility(View.GONE);
                    }
                } else if ("ACTION_POMODORO_FINISHED".equals(intent.getAction())) {
                    finish(); // Automatically close when service signals complete stop
                }
            }
        };
    }

    private String formatTime(long ms) {
        long m = ms / 60000;
        long s = (ms % 60000) / 1000;
        return String.format("%02d:%02d", m, s);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction("ACTION_POMODORO_TICK");
        filter.addAction("ACTION_POMODORO_FINISHED");
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(pomodoroReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(pomodoroReceiver, filter);
        }
        
        // Also just in case, we can manually request an update or start service to ping us.
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(pomodoroReceiver);
        } catch (IllegalArgumentException e) {
            // Unregistered
        }
    }
}
