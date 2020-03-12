package com.nv.user.sunderkand;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class chalisadata extends Fragment implements chalisadatam {
    // Media Player
    // private MediaPlayer mp;
    // Handler to update UI timer, progress bar etc,.
    private Handler mHandler = new Handler();

  Button  play;
  Button pause;
  SeekBar seekBar;
 // String url ="https://sunderkand-5b024.firebaseio.com/hanuman%20chalisa.json";
//  List<JSONObject> objectList;
/* InterstitialAd mInterstitialAd;*/

    StorageReference storageReference;
    DatabaseReference databaseReference;

    // private Utilities utils;
    // private int seekForwardTime = 5000; // 5000 milliseconds
    // private int seekBackwardTime = 5000; // 5000 milliseconds
   //  ImageButton play;
    MediaPlayer mediaPlayer;
    //SeekBar seekbar;
    WebView webView;

   // List<Integer> mp3 =new ArrayList<>();

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragchalisadata, container, false);

      /*  interstitial = new InterstitialAd(getContext());

        interstitial.setAdUnitId(getString(R.string.admob_interstitial_id));
        AdRequest adRequest = new AdRequest.Builder().build();
        interstitial.loadAd(adRequest);
        interstitial.setAdListener(new AdListener() {

            public void onAdLoaded() {
                displayInterstitial();
            }
        });

        mAdView = view.findViewById(R.id.adView);
        adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/
        play   = (Button)view.findViewById(R.id.play);
       // pause =(Button)view.findViewById(R.id.pause) ;
        mediaPlayer = MediaPlayer.create(getContext(),R.raw.hanumanchalisa);
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                    // check for already playing
                    if(mediaPlayer.isPlaying()){
                        if(mediaPlayer!=null){
                            mediaPlayer.pause();
                            // Changing button image to play button
                            play.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                        }
                    }else{
                        // Resume song
                        if(mediaPlayer!=null){
                            mediaPlayer.start();
                            // Changing button image to pause button
                            play.setBackgroundResource(R.drawable.ic_pause_circle_outline_black_24dp);
                        }
                    }

                }
            });
                /*if (mediaPlayer.isPlaying()){
                    mediaPlayer.pause();
                    play.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);

                }else {mediaPlayer.start();
                    play.setBackgroundResource(R.drawable.ic_pause_circle_outline_black_24dp);
                }
            }});*/
        seekBar=(SeekBar)view.findViewById(R.id.Seekbarmedia);
        seekBar.setMax(mediaPlayer.getDuration());
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                seekBar.setProgress(mediaPlayer.getCurrentPosition());
            }
        },0,1000);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean userTouch;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if(mediaPlayer.isPlaying() && fromUser)
                    mediaPlayer.seekTo(progress);
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

    /*    mediaPlayer = new MediaPlayer();
        getAmazonProducts();

    //    storageReference = FirebaseStorage.getInstance().getReference();
    //    databaseReference = FirebaseDatabase.getInstance().getReference("https://sunderkand-5b024.firebaseio.com/hanuman chalisa/abc/music");

                    play.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                        playSong();

                                        *//*if (mediaPlayer.isPlaying()){
                                            mediaPlayer.pause();
                                            play.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);

                                        }else {mediaPlayer.start();
                                        play.setBackgroundResource(R.drawable.ic_pause_circle_outline_black_24dp);
                                    }*//*
                                }});
    *//*    pause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mediaPlayer.pause();
            }
        });*//*

       *//* Button stopButton = (Button)view.findViewById(R.id.stop);
        stopButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mediaPlayer.stop();
                mediaPlayer.prepareAsync();
            }
        });*//*

        seekBar=(SeekBar)view.findViewById(R.id.Seekbarmedia);
        *//*    seekBar.setMax(mediaPlayer.getDuration());
           new Timer().scheduleAtFixedRate(new TimerTask() {
    @Override
    public void run() {
               seekBar.setProgress(mediaPlayer.getCurrentPosition());
      }
},0,1000);*//*
seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
   // boolean userTouch;
   int seeked_progess;
    @Override
    public void onProgressChanged(final SeekBar seekBar, int progress, boolean fromUser) {
//mediaPlayer.seekTo(progress);
       seeked_progess = progress;
       seeked_progess = seeked_progess * 1000;

        if (fromUser) {
          //  seekBar.setProgress();// To set initial progress, i.e zero in starting of the song
           seekBar.setMax(583);// To set the max progress, i.e duration of the song
            Runnable mRunnable = new Runnable() {
                @Override
                public void run() {
                    int min, sec;
                    if (mediaPlayer != null *//*Checking if the
                       music player is null or not otherwise it
                       may throw an exception*//*) {
                        int mCurrentPosition;
                        mCurrentPosition = seekBar.getProgress();

                        min = mCurrentPosition / 60;
                        sec = mCurrentPosition % 60;

                        Log.e("Music Player Activity", "Minutes : "+min +" Seconds : " + sec);

                        *//*currentime_mm.setText("" + min);
                        currentime_ss.setText("" + sec);*//*
                    }
                    mHandler.postDelayed(this, 1000);
                }
            };
            mRunnable.run();}
        }
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mediaPlayer.seekTo(seeked_progess);
    }
});*/

      /*  if(mediaPlayer.isPlaying() && fromUser)
           ;
            mediaPlayer.seekTo(progress);*/
  /*  }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        userTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        userTouch = false;


    }
});*/
                //  seekbar = view.findViewById(R.id.Seekbarmedia);

                // play = view.findViewById(R.id.play);
                webView = view.findViewById(R.id.Webchalisa);
        webView.setWebViewClient(new WebViewClient());

        webView.loadUrl("file:///android_asset/k.html");




        //utils = new Utilities();
      //  mp3.add(R.raw.hanumanchalisa);
        /* play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // check for already playing
                // mp3.add(R.raw.hanumanchalisa);

                   // mp = MediaPlayer.create(getActivity(), R.raw.hanumanchalisa);
                   // mp.start();
                    // Displaying Song title
                    //  String songTitle = songsList.get(songIndex).get("songTitle");


                    // Changing Button Image to pause image
                    //   btnPlay.setImageResource(R.drawable.btn_pause);

                    // set Progress bar values
                   // seekbar.setProgress(0);
                  //  seekbar.setMax(100);
                  //  updateProgressBar();


                    if (mp.isPlaying()) {
                        if (mp != null) {
                            mp.pause();
                          //  Changing button image to play button*//*
                             play.setImageResource(R.drawable.ic_fast_forward_black_24dp);
                        }
                   } else {
                       *//*  Resume song*//*
                        if (mp != null) {
                            mp.start();
                            // Changing button image to pause button
                            // btnPlay.setImageResource(R.drawable.btn_pause);
                        }
    }
}

        });*/
//playSong(0);
       return view;
   }

   /* private void runOnUiThread(Runnable runnable) {
        chalisadata.this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if(mediaPlayer != null){
                    int mCurrentPosition = mediaPlayer.getCurrentPosition() / 1000;
                    seekBar.setProgress(mCurrentPosition);
                }
                mHandler.postDelayed(this, 1000);
            }
        });
    }*/

   /* private void getAmazonProducts() {

        StringRequest stringRequest = new StringRequest(Request.Method.GET,url , new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                 objectList = new ArrayList<>();
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if(jsonObject != null){
                        Iterator<String> keys = jsonObject.keys();
                        while(keys.hasNext()){
                            String key = String.valueOf(keys.next()); // this will be your JsonObject key
                            JSONObject childObj = jsonObject.getJSONObject(key);
                            if(childObj != null){
                                objectList.add(childObj);
                               // play.setText(childObj.optString("abc"));
                            }
                           // mHasMore = itemDetailsAdapter.mDataList.size() < objectList.size();
                           // itemDetailsAdapter.updateView(objectList);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

               System.out.println(objectList.size());

            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                //    mBar.hide();
                if (volleyError.networkResponse != null && volleyError.networkResponse.data != null) {
                    VolleyError error = new VolleyError(new String(volleyError.networkResponse.data));
                    try {
                        JSONObject jsonObject = new JSONObject(error.getMessage());
                        Toast.makeText(getActivity(), jsonObject.optString("error"), Toast.LENGTH_LONG).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (volleyError instanceof NoConnectionError)
                    Toast.makeText(getActivity(), getResources().getString(R.string.app_name), Toast.LENGTH_LONG).show();
            }
        });
        stringRequest.setRetryPolicy(new RetryPolicy() {
            @Override
            public int getCurrentTimeout() {
                return 50000;
            }

            @Override
            public int getCurrentRetryCount() {
                return 50000;
            }

            @Override
            public void retry(VolleyError error) throws VolleyError {

            }
        });
        RequestQueue requestQueue = Volley.newRequestQueue(getActivity());
        requestQueue.add(stringRequest);
    }



    *//* public void updateProgressBar() {
        mHandler.postDelayed(mUpdateTimeTask, 100);
    }


    *//**//**
     * Background Runnable thread
     *//**//*
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long totalDuration = mp.getDuration();
            long currentDuration = mp.getCurrentPosition();

            // Displaying Total Duration time
            // songTotalDurationLabel.setText(""+utils.milliSecondsToTimer(totalDuration));
            // Displaying time completed playing
            // songCurrentDurationLabel.setText(""+utils.milliSecondsToTimer(currentDuration));

            // Updating progress bar
            int progress = (int) (utils.getProgressPercentage(currentDuration, totalDuration));
            //Log.d("Progress", ""+progress);
            seekbar.setProgress(progress);

            // Running this thread after 100 milliseconds
            mHandler.postDelayed(this, 100);
        }
    };*//*



     *//*  @Override
    public void onDestroy(){
        super.onDestroy();
        mp.release();
    }
*//*


    public void  playSong(){
        // Play song
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(objectList.get(0).optString("music"));
            mediaPlayer.prepare();
            mediaPlayer.start();


            if(mediaPlayer.isPlaying()){
                mediaPlayer.pause();
            } else {
                mediaPlayer.start();
            }


            // Displaying Song title
            //String songTitle = songsList.get(songIndex).get("songTitle");
           // songTitleLabel.setText(songTitle);

         *//* *//**//*  play.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        play.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                    } else {

                        mediaPlayer.start();
                        play.setBackgroundResource(R.drawable.ic_pause_circle_outline_black_24dp);
                    }
*//**//*
                }
            }); *//*// Changing Button Image to pause image
           //play.setBackgroundResource(R.drawable.ic_pause_circle_outline_black_24dp);

            // set Progress bar values
         //   seekbar.setProgress(0);
           // seekbar.setMax(100);

            // Updating progress bar
           // updateProgressBar();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
*//* public void displayInterstitial() {

        if (interstitial.isLoaded()) {
            interstitial.show();
        }
    }*/
    @Override
    public void onBackPressed()
    {
        if (mediaPlayer.isPlaying())
            mediaPlayer.stop();

    }



    @Override
    public void onPause ()
    {
        if (mediaPlayer.isPlaying())
        {
            mediaPlayer.pause();
            mediaPlayer.stop();
        }
        super.onPause();
    }

}




