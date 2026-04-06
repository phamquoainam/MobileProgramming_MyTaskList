package hcmute.edu.vn.mytasklist;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.AnimationSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

@SuppressLint("CustomSplashScreen")
public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY_MS = 2200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Animate logo: scale + fade in
        FrameLayout logoFrame = findViewById(R.id.logoFrame);
        ScaleAnimation scale = new ScaleAnimation(
                0.5f, 1f, 0.5f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(600);

        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(600);

        AnimationSet anim = new AnimationSet(true);
        anim.addAnimation(scale);
        anim.addAnimation(fadeIn);
        anim.setFillAfter(true);
        logoFrame.startAnimation(anim);

        // Animate tagline: fade in with delay
        TextView tvTagline = findViewById(R.id.tvTagline);
        AlphaAnimation taglineFade = new AlphaAnimation(0f, 1f);
        taglineFade.setDuration(800);
        taglineFade.setStartOffset(500);
        taglineFade.setFillAfter(true);
        tvTagline.startAnimation(taglineFade);

        // Navigate to MainActivity after delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // đóng SplashActivity, không cho back lại
        }, SPLASH_DELAY_MS);
    }
}
