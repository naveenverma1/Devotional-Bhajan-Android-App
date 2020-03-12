package com.nv.user.sunderkand;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.nv.user.sunderkand.R;
import com.squareup.picasso.Picasso;

import org.json.JSONObject;

import java.util.List;

public class chalisaadapter extends BaseAdapter {
    Activity mactivity;
    List<JSONObject> mlist;

    chalisaadapter(Activity mactivity, List<JSONObject> mlist){


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

        View view =inflater.inflate(R.layout.custumchalisalist,null);

        TextView textView =view.findViewById(R.id.text);
        ImageView image=view.findViewById(R.id.img);

        JSONObject jsonObject = mlist.get(position);
        String data =jsonObject.optString("name");
        String imagedata=jsonObject.optString("image");

        textView.setText(data);
        Picasso.with(mactivity).load(imagedata).error(R.mipmap.ic_launcher).into(image);
        return view;
    }


    public void updateView(List<JSONObject> list) {
        mlist.clear();
        mlist.addAll(list);
        notifyDataSetChanged();
    }
}
