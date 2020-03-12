package com.nv.user.sunderkand;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class chalisaFragment extends Fragment {

    chalisaadapter mchalisaadapter;
    ListView listView;
    boolean mHasMore =true;
    //private ProgressBar progressBar;
    View view;
   /* CountDownTimer mCountDownTimer;
    int twoMin = 2 * 60 * 1000;
    int dTotal;*/

    String url="https://sunderkand-5b024.firebaseio.com/chalisa.json";
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view =inflater.inflate (R.layout.fragment_chalisa, container, false);
     //   progressBar = (ProgressBar)view.findViewById(R.id.simpleProgressBar);
     //   mCountDownTimer = new CountDownTimer(twoMin, 1000) {
         /*   public void onTick(long millisUntilFinished) {


                int total = (int) ((dTotal / 120) * 100);
                progressBar.setProgress(total);
            }

            public void onFinish() {
                // DO something when 2 minutes is up
            }
        }.start();*/
            /*@Override
            public void onTick(long millisUntilFinished) {
                Log.v("Log_tag", "Tick of Progress" + i + millisUntilFinished);

                progressBar.setProgress((int) i * 100 / (5000 / 1000));

            }

            @Override
            public void onFinish() {
                i++;
                progressBar.setProgress(100);
            }
        };mCountDownTimer.start();*/
           /* progressBar.setProgress(0);


        final ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", 0, 100);
        animation.setDuration(5000);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) { }

            @Override
            public void onAnimationEnd(Animator animator) {
                //do something when the countdown is complete
            }

            @Override
            public void onAnimationCancel(Animator animator) { }

            @Override
            public void onAnimationRepeat(Animator animator) { }
        });
        animation.start();
        class Task implements Runnable {
            @Override
            public void run() {
                for (int i = 0; i <= 10; i++) {
                    final int value = i;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    progressBar.setProgress(value);

                }
            }

        }*/
        listView= view.findViewById(R.id.listschalisa);
        mchalisaadapter =new chalisaadapter(getActivity(),new ArrayList<JSONObject>());
        getAmazonProducts();
        listView.setAdapter(mchalisaadapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                JSONObject object =mchalisaadapter.mlist.get(position);
                String string =object.optString("name");

                if (string.equals("hanuman chalisa")){
                    chalisadata fragment = new chalisadata();
                    FragmentManager fc =getFragmentManager();
                    FragmentTransaction fr = fc.beginTransaction();
                    fr.replace(R.id.Framelay,fragment).addToBackStack("").commit();
                }

                String stringg =object.optString("name");
                if (string.equals("Khatu Shayam ji chalisa")){
                    khatushaamfrag fragment = new khatushaamfrag();
                    FragmentManager ll =getFragmentManager();
                    FragmentTransaction rr = ll.beginTransaction();
                    rr.replace(R.id.Framelay,fragment).addToBackStack("").commit();
                }
                String stringgg =object.optString("name");
                if (string.equals("Shri Bajrang Baan")){
                    bajrangbaan fragment = new bajrangbaan();
                    FragmentManager mm =getFragmentManager();
                    FragmentTransaction nn = mm.beginTransaction();
                    nn.replace(R.id.Framelay,fragment).addToBackStack("").commit();
                }
            }
        });
        return view;



    }
    private void getAmazonProducts() {

        StringRequest stringRequest = new StringRequest(Request.Method.GET,url , new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {

                List<JSONObject> objectList = new ArrayList<>();
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    if(jsonObject != null){
                        Iterator<String> keys = jsonObject.keys();
                        while(keys.hasNext()){
                            String key = String.valueOf(keys.next()); // this will be your JsonObject key
                            JSONObject childObj = jsonObject.getJSONObject(key);
                            if(childObj != null){
                                objectList.add(childObj);
                                //  textView.setText(childObj.optString("sundar"));


                            }
                            mHasMore = mchalisaadapter.mlist.size() < objectList.size();
                            mchalisaadapter.updateView(objectList);

                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                //   System.out.println(objectList.size());

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

    }

