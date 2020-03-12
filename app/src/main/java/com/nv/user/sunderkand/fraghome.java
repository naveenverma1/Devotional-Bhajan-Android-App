package com.nv.user.sunderkand;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class fraghome extends Fragment  implements View.OnClickListener {

    Button btsundarkand,btchlisha,btaarti,btabout,btshare;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =inflater.inflate(R.layout.frag_home, container, false);

btshare=view.findViewById(R.id.share);
btshare.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {

        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "My application name");
            String sAux = "\nLet me recommend you this application\n\n";
            sAux = sAux + "https://play.google.com/store/apps/details?id=com.nv.user.sunderkand \n\n";
            i.putExtra(Intent.EXTRA_TEXT, sAux);
            startActivity(Intent.createChooser(i, "choose one"));
        } catch (Exception e) {
            //e.toString();
        }
    }
});


        btsundarkand =view.findViewById(R.id.sunderkandb);
        btsundarkand.setOnClickListener(this);


        btchlisha =view.findViewById(R.id.hanumanb);
        btchlisha.setOnClickListener(this);

        btaarti = view.findViewById(R.id.aaarti);
        btaarti.setOnClickListener(this);

        btabout =view.findViewById(R.id.about);
        btabout.setOnClickListener(this);

return view;
    }
    @Override
    public void onClick(View v) {

        switch (v.getId()){
          case  R.id.sunderkandb :

              sunderkanddata fragg = new sunderkanddata();
              FragmentManager fm = getFragmentManager();
              if(fm.getBackStackEntryCount() > 0 ) {
                  fm.popBackStack();//Pops one of the added fragments

              }
              FragmentTransaction ft = fm.beginTransaction();
              ft.replace(R.id.Framelay,fragg).addToBackStack(null).commit();
              break;
            case R.id.hanumanb :
                chalisaFragment fragment = new chalisaFragment();
                FragmentManager fc =getFragmentManager();
                FragmentTransaction fr = fc.beginTransaction();
                fr.replace(R.id.Framelay,fragment).addToBackStack("").commit();
                break;
            case
                 R.id.aaarti :
                aartifragment frng = new aartifragment();
            FragmentManager hh =getFragmentManager();
            FragmentTransaction kk =hh.beginTransaction();
            kk.replace(R.id.Framelay,frng).addToBackStack("").commit();
                break;
                case
                        R.id.about :
                    about frngg = new about();
                    FragmentManager hhh =getFragmentManager();
                    FragmentTransaction kkk =hhh.beginTransaction();
                    kkk.replace(R.id.Framelay,frngg).addToBackStack("").commit();

        }}

    }



//method///////////////////////


