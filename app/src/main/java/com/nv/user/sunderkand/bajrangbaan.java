package com.nv.user.sunderkand;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class bajrangbaan extends Fragment {

    private static final String URL = "file:///android_asset/baaan.html";

    private WebView webView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bajrangbaan, container, false);
        webView = view.findViewById(R.id.bajrangbaan);

        ReaderUi.wireReader(view, webView, URL);
        webView.loadUrl(URL);
        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        ReaderUi.saveScroll(getContext(), webView, URL);
    }
}
