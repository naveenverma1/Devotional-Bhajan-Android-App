package com.nv.user.sunderkand;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.SeekBar;

/**
 * Hanuman Chalisa reader: WebView for the chaupais (k.html in assets) +
 * MediaPlayer for the recitation (R.raw.hanumanchalisa) with a SeekBar.
 *
 * Lifecycle:
 *  - MediaPlayer is created on view-create and released on view-destroy,
 *    so it never outlives the visible fragment.
 *  - SeekBar updates run on the main thread via Handler.postDelayed and
 *    the callback is removed on view-destroy (replaces the leaky Timer).
 *  - WebView text-zoom + scroll position are persisted across visits via
 *    {@link ReaderPrefs} / {@link ReaderUi}.
 */
public class chalisadata extends Fragment {

    private static final String URL = "file:///android_asset/k.html";

    private Button play;
    private SeekBar seekBar;
    private MediaPlayer mediaPlayer;
    private WebView webView;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable seekBarTicker = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && seekBar != null) {
                try {
                    seekBar.setProgress(mediaPlayer.getCurrentPosition());
                } catch (IllegalStateException ignored) {
                    // Player was released between checks; stop ticking.
                    return;
                }
                mainHandler.postDelayed(this, 500);
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragchalisadata, container, false);

        play = view.findViewById(R.id.play);
        seekBar = view.findViewById(R.id.Seekbarmedia);
        webView = view.findViewById(R.id.Webchalisa);

        mediaPlayer = MediaPlayer.create(getContext(), R.raw.hanumanchalisa);
        if (mediaPlayer != null) {
            seekBar.setMax(mediaPlayer.getDuration());
            mediaPlayer.setOnCompletionListener(mp -> {
                play.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                seekBar.setProgress(0);
                mainHandler.removeCallbacks(seekBarTicker);
            });
        } else {
            play.setEnabled(false);
            seekBar.setEnabled(false);
        }

        play.setOnClickListener(v -> {
            if (mediaPlayer == null) return;
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                play.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                mainHandler.removeCallbacks(seekBarTicker);
            } else {
                mediaPlayer.start();
                play.setBackgroundResource(R.drawable.ic_pause_circle_outline_black_24dp);
                mainHandler.post(seekBarTicker);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });

        ReaderUi.wireReader(view, webView, URL);
        webView.loadUrl(URL);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            play.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
        }
        mainHandler.removeCallbacks(seekBarTicker);
        ReaderUi.saveScroll(getContext(), webView, URL);
    }

    @Override
    public void onDestroyView() {
        mainHandler.removeCallbacks(seekBarTicker);
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
            } catch (IllegalStateException ignored) { }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroyView();
    }
}
