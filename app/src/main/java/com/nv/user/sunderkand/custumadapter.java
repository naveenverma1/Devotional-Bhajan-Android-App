package com.nv.user.sunderkand;

import android.app.Activity;
import android.media.MediaActionSound;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class custumadapter extends BaseAdapter {
    Activity mactivity;
    List<JSONObject> mlist;
    String url="https://sunderkand-5b024.firebaseio.com/hanumanchalisa.json";
    String music;

    custumadapter(Activity mactivity, List<JSONObject> mlist){


        this.mactivity =mactivity;
        this.mlist =mlist;
    }

    @Override
    public int getCount() {
        return mlist.size();
    }

    @Override
    public Object getItem(int position) {
        return mlist.size();
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = mactivity.getLayoutInflater();

        View view =inflater.inflate(R.layout.fragment_chalisadata,null);
        ToggleButton playdoogle =view.findViewById(R.id.play);
        TextView textView =view.findViewById(R.id.TVchopae);
        final JSONObject jsonObject = mlist.get(position);
        String data =jsonObject.optString("doheone");
        textView.setText(data);

     playdoogle.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
           String chalisa =  jsonObject.optString("music");
           MediaPlayer mediaPlayer =MediaPlayer.create(mactivity,Uri.parse(chalisa));
           mediaPlayer.start();
         }
     });
        return view;
    }



    public void updateView(List<JSONObject> list) {
        mlist.clear();
        mlist.addAll(list);
        notifyDataSetChanged();
    }




}
