package com.nv.user.sunderkand;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.SeekBar;


import java.util.Timer;
import java.util.TimerTask;


public class sunderkanddata extends Fragment implements sunderkanddatam {
    private Handler mHandler = new Handler();
    Button play;
    SeekBar seekBar;
    MediaPlayer mediaPlayer;
    WebView webView;
    //  MediaStore mAudio;

   /* InterstitialAd mInterstitialAd;
    private InterstitialAd interstitial;
    private AdView mAdView;
*/

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sunderkanddata, container, false);


        play = (Button) view.findViewById(R.id.play);
        //  interstitial = new InterstitialAd(getContext());
      /*  mAdView = view.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/
       /* interstitial.setAdUnitId(getString(R.string.admob_interstitial_id));
        AdRequest adReuest = new AdRequest.Builder().build();
        interstitial.loadAd(adRequest);
        interstitial.setAdListener(new AdListener() {

            public void onAdLoaded() {
                displayInterstitial();
            }
        });*/
        mediaPlayer = MediaPlayer.create(getContext(), R.raw.sunder);
        play.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mediaPlayer.isPlaying()) {
                    if (mediaPlayer != null) {
                        mediaPlayer.pause();
                        play.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                    }
                } else {
                    if (mediaPlayer != null) {

                        mediaPlayer.start();
                        play.setBackgroundResource(R.drawable.ic_pause_circle_outline_black_24dp);
                    }
                }

            }
        });

        seekBar = (SeekBar) view.findViewById(R.id.Seekbarmedia);
        seekBar.setMax(mediaPlayer.getDuration());
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
            }
        }, 0, 1000);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean userTouch;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (mediaPlayer.isPlaying() && fromUser) mediaPlayer.seekTo(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userTouch = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userTouch = false;


            }
        });


        webView = view.findViewById(R.id.websunderkand);
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.setInitialScale(1);
        webView.loadUrl("file:///android_asset/n.html");
        return view;


    }

   /* public void displayInterstitial() {

        if (interstitial.isLoaded()) {
            interstitial.show();
        }
*/
//}
    @Override
    public void onBackPressed()
    {
        if (mediaPlayer != null)
            mediaPlayer.stop();

    }
    @Override
    public void onPause ()
    {
        if (mediaPlayer != null)
        {
            mediaPlayer.pause();
            mediaPlayer.stop();
        }
        super.onPause();
    }

    }


