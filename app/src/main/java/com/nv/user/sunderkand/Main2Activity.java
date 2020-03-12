package com.nv.user.sunderkand;

import android.media.MediaPlayer;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;



public class Main2Activity extends AppCompatActivity implements View.OnClickListener {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);


        fraghome mfraghome = new fraghome();
        getSupportFragmentManager().beginTransaction().replace(R.id.Framelay,mfraghome).commit();
    }

    @Override
    public void onClick(View v) {

    }
}
