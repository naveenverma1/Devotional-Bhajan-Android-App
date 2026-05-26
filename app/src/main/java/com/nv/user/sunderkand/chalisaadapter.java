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
        return mlist.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = mactivity.getLayoutInflater();
            convertView = inflater.inflate(R.layout.custumchalisalist, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.text);
        ImageView image = convertView.findViewById(R.id.img);

        JSONObject jsonObject = mlist.get(position);
        String data = jsonObject.optString("name");
        String imagedata = jsonObject.optString("image");

        textView.setText(data);
        Picasso.get()
                .load(imagedata)
                .placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(image);
        return convertView;
    }


    public void updateView(List<JSONObject> list) {
        mlist.clear();
        mlist.addAll(list);
        notifyDataSetChanged();
    }
}
