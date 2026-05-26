package com.nv.user.sunderkand;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

/**
 * Splash screen. Shows the brand image + a short temple chime, then routes
 * to Main2Activity. The whole thing is bounded by SPLASH_TIME_OUT.
 *
 * The previous implementation posted a Runnable that called
 * {@code mediaPlayerl.start()} BEFORE the player was created in fade(),
 * could NPE if MediaPlayer.create() returned null, and never released the
 * MediaPlayer or removed Handler callbacks if the user backed out early.
 * All of that is handled here.
 */
public class MainActivity extends AppCompatActivity {

    private static final long SPLASH_TIME_OUT = 2000L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private MediaPlayer chime;
    private Runnable advanceRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Splash: let the brand image extend edge-to-edge under the
        // transparent system bars. No content-padding required because
        // the foreground ImageView is centered.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        // Brand image pulse.
        ImageView imageView = findViewById(R.id.imageView);
        if (imageView != null) {
            Animation animation = AnimationUtils.loadAnimation(this, R.anim.blink);
            animation.setInterpolator(new LinearInterpolator());
            animation.setRepeatCount(Animation.INFINITE);
            animation.setDuration(1000);
            imageView.startAnimation(animation);
        }

        // Temple chime (best-effort; never crash the splash if it fails).
        chime = MediaPlayer.create(this, R.raw.temple);
        if (chime != null) {
            try {
                chime.start();
            } catch (IllegalStateException ignored) { }
        }

        advanceRunnable = () -> {
            startActivity(new Intent(MainActivity.this, Main2Activity.class));
            finish();
        };
        mainHandler.postDelayed(advanceRunnable, SPLASH_TIME_OUT);
    }

    @Override
    protected void onDestroy() {
        if (advanceRunnable != null) {
            mainHandler.removeCallbacks(advanceRunnable);
        }
        if (chime != null) {
            try {
                if (chime.isPlaying()) chime.stop();
            } catch (IllegalStateException ignored) { }
            chime.release();
            chime = null;
        }
        super.onDestroy();
    }
}
