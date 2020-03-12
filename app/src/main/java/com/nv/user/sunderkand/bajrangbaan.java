package com.nv.user.sunderkand;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;




public class bajrangbaan extends Fragment {
    WebView webView;
   /* InterstitialAd mInterstitialAd;
    private InterstitialAd interstitial;
    private AdView mAdView;*/
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bajrangbaan, container, false);

  /*      interstitial = new InterstitialAd(getContext());

        interstitial.setAdUnitId(getString(R.string.admob_interstitial_id));
        AdRequest adRequest = new AdRequest.Builder().build();
      interstitial.loadAd(adRequest);
        interstitial.setAdListener(new AdListener() {

            public void onAdLoaded() {
                displayInterstitial();
            }
        });*/

        webView = view.findViewById(R.id.bajrangbaan);
       /* mAdView = view.findViewById(R.id.adView);
        adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);*/
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/baaan.html");
        return view;
    }
  /*  public void displayInterstitial() {

        if (interstitial.isLoaded()) {
            interstitial.show();
        }
    }*/

}
