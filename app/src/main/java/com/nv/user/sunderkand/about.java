package com.nv.user.sunderkand;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

/**
 * About screen — WebView with credits + three action buttons:
 * Rate (Play listing), Share, Privacy policy (loads privacy_policy.html
 * in the same WebView).
 */
public class about extends Fragment {

    private static final String PLAY_URL =
            "https://play.google.com/store/apps/details?id=com.nv.user.sunderkand";
    private static final String MARKET_URL =
            "market://details?id=com.nv.user.sunderkand";
    private static final String ABOUT_HTML =
            "file:///android_asset/about.html";
    private static final String PRIVACY_HTML =
            "file:///android_asset/privacy_policy.html";

    private WebView webView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        webView = view.findViewById(R.id.aboutweb);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.getSettings().setUseWideViewPort(true);
        webView.setInitialScale(1);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(ABOUT_HTML);

        Button rate = view.findViewById(R.id.about_rate);
        Button share = view.findViewById(R.id.about_share);
        Button privacy = view.findViewById(R.id.about_privacy);

        rate.setOnClickListener(v -> openPlayListing());
        share.setOnClickListener(v -> shareApp());
        privacy.setOnClickListener(v -> webView.loadUrl(PRIVACY_HTML));

        return view;
    }

    private void openPlayListing() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URL)));
        } catch (ActivityNotFoundException notInstalled) {
            // No Play Store app — fall back to the web URL.
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_URL)));
        }
    }

    private void shareApp() {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
        i.putExtra(Intent.EXTRA_TEXT,
                getString(R.string.share_body, PLAY_URL));
        startActivity(Intent.createChooser(i, getString(R.string.share_chooser_title)));
    }
}
