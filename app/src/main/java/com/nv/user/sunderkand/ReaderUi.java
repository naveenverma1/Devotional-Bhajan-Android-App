package com.nv.user.sunderkand;

import android.content.Context;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Glue between a reader Fragment's view tree (WebView + font_controls
 * include) and {@link ReaderPrefs}. Handles three things:
 *
 *  1. Applies the saved text-zoom level to the WebView.
 *  2. Restores the saved scroll position once the page has finished
 *     loading (posted to the view's queue so the WebView has measured).
 *  3. Wires the A-/A+ buttons (if present) to cycle the zoom level.
 *
 * Fragments should also call {@link #saveScroll} in onPause so the user
 * can resume reading next time.
 */
final class ReaderUi {

    private ReaderUi() { }

    static void wireReader(View root, final WebView webView, final String url) {
        if (webView == null || url == null) return;

        ReaderPrefs.applyTextZoom(webView);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String loadedUrl) {
                final int savedY = ReaderPrefs.getScrollPosition(view.getContext(), url);
                if (savedY > 0) {
                    view.post(() -> view.scrollTo(0, savedY));
                }
            }
        });

        View smaller = root.findViewById(R.id.btn_font_smaller);
        View larger = root.findViewById(R.id.btn_font_larger);
        if (smaller != null) {
            smaller.setOnClickListener(v -> {
                ReaderPrefs.cycleZoomDown(v.getContext());
                ReaderPrefs.applyTextZoom(webView);
            });
        }
        if (larger != null) {
            larger.setOnClickListener(v -> {
                ReaderPrefs.cycleZoomUp(v.getContext());
                ReaderPrefs.applyTextZoom(webView);
            });
        }
    }

    static void saveScroll(Context ctx, WebView webView, String url) {
        if (ctx == null || webView == null || url == null) return;
        ReaderPrefs.saveScrollPosition(ctx, url, webView.getScrollY());
    }
}
