package com.nv.user.sunderkand;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Lists the available chalisas fetched from the public Firebase Realtime
 * DB endpoint. Has three UI states:
 *
 *  - LOADING: ProgressBar centered, list hidden.
 *  - SUCCESS: ListView populated, others hidden.
 *  - ERROR:   inline error message + Retry button.
 *
 * The retry button re-runs the fetch. The Volley request is tagged with
 * REQUEST_TAG and cancelled in onDestroyView so we don't deliver
 * callbacks into a destroyed fragment.
 */
public class chalisaFragment extends Fragment {

    private static final String URL = "https://sunderkand-5b024.firebaseio.com/chalisa.json";
    private static final String REQUEST_TAG = "chalisaListRequest";

    private chalisaadapter mchalisaadapter;
    private ListView listView;
    private ProgressBar progressBar;
    private LinearLayout errorBlock;
    private TextView errorText;
    private RequestQueue requestQueue;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chalisa, container, false);

        listView = view.findViewById(R.id.listschalisa);
        progressBar = view.findViewById(R.id.chalisa_progress);
        errorBlock = view.findViewById(R.id.chalisa_error);
        errorText = view.findViewById(R.id.chalisa_error_text);
        Button retry = view.findViewById(R.id.chalisa_retry);

        mchalisaadapter = new chalisaadapter(requireActivity(), new ArrayList<>());
        listView.setAdapter(mchalisaadapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                JSONObject object = mchalisaadapter.mlist.get(position);
                String name = object.optString("name");
                Fragment next = null;
                if ("hanuman chalisa".equalsIgnoreCase(name)) {
                    next = new chalisadata();
                } else if ("Khatu Shayam ji chalisa".equalsIgnoreCase(name)) {
                    next = new khatushaamfrag();
                } else if ("Shri Bajrang Baan".equalsIgnoreCase(name)) {
                    next = new bajrangbaan();
                }
                if (next != null) {
                    FragmentManager fm = getParentFragmentManager();
                    FragmentTransaction ft = fm.beginTransaction();
                    ft.replace(R.id.Framelay, next).addToBackStack(null).commit();
                }
            }
        });

        retry.setOnClickListener(v -> loadChalisas());

        loadChalisas();

        return view;
    }

    private void showLoading() {
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (errorBlock != null) errorBlock.setVisibility(View.GONE);
        if (listView != null) listView.setVisibility(View.GONE);
    }

    private void showList() {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (errorBlock != null) errorBlock.setVisibility(View.GONE);
        if (listView != null) listView.setVisibility(View.VISIBLE);
    }

    private void showError(int messageRes) {
        if (progressBar != null) progressBar.setVisibility(View.GONE);
        if (errorBlock != null) errorBlock.setVisibility(View.VISIBLE);
        if (errorText != null) errorText.setText(messageRes);
        if (listView != null) listView.setVisibility(View.GONE);
    }

    private void loadChalisas() {
        if (!isAdded()) return;
        showLoading();

        StringRequest request = new StringRequest(Request.Method.GET, URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (!isAdded()) return;
                        List<JSONObject> items = parseResponse(response);
                        if (items.isEmpty()) {
                            showError(R.string.error_loading);
                            return;
                        }
                        mchalisaadapter.updateView(items);
                        showList();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (!isAdded()) return;
                        if (error instanceof NoConnectionError
                                || error instanceof TimeoutError) {
                            showError(R.string.error_no_internet);
                        } else {
                            showError(R.string.error_loading);
                        }
                    }
                });
        // Reasonable timeouts (30s, no exponential backoff retries).
        request.setRetryPolicy(new DefaultRetryPolicy(
                30_000,
                1,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        request.setTag(REQUEST_TAG);

        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(requireContext().getApplicationContext());
        }
        requestQueue.add(request);
    }

    private static List<JSONObject> parseResponse(String response) {
        List<JSONObject> out = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(response);
            Iterator<String> keys = jsonObject.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject child = jsonObject.optJSONObject(key);
                if (child != null) out.add(child);
            }
        } catch (JSONException e) {
            // Returning the partial list is fine; caller treats empty as error.
        }
        return out;
    }

    @Override
    public void onDestroyView() {
        if (requestQueue != null) {
            requestQueue.cancelAll(REQUEST_TAG);
        }
        super.onDestroyView();
    }
}
