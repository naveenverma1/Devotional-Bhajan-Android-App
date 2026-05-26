package com.nv.user.sunderkand;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.WebView;

/**
 * Tiny SharedPreferences-backed helper for two reader features:
 *
 *  1. Text-zoom level (font size) shared across all readers.
 *     Levels cycle: 85% → 100% → 120% → 140% → 170% → 200% → back to 85%.
 *
 *  2. Per-URL scroll position. Each WebView page (Sunderkand, Chalisa,
 *     Aarti, etc.) remembers where the user last left off.
 *
 * Kept deliberately tiny — no DI, no Kotlin, no singletons — to fit the
 * style of the rest of this codebase.
 */
final class ReaderPrefs {

    private static final String PREFS_NAME = "reader_prefs";
    private static final String KEY_TEXT_ZOOM = "text_zoom";
    private static final String KEY_SCROLL_PREFIX = "scroll_";

    /** Allowed text zoom percentages, in cycle order. */
    static final int[] ZOOM_LEVELS = { 85, 100, 120, 140, 170, 200 };
    private static final int DEFAULT_ZOOM = 100;

    private ReaderPrefs() { }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    static int getTextZoom(Context ctx) {
        return prefs(ctx).getInt(KEY_TEXT_ZOOM, DEFAULT_ZOOM);
    }

    /** Apply the saved text zoom to a WebView. Safe to call any time. */
    static void applyTextZoom(WebView webView) {
        if (webView == null) return;
        webView.getSettings().setTextZoom(getTextZoom(webView.getContext()));
    }

    /**
     * Move to the next larger zoom level (wraps back to smallest after
     * the max). Returns the new zoom percentage so callers can apply it.
     */
    static int cycleZoomUp(Context ctx) {
        int current = getTextZoom(ctx);
        int next = ZOOM_LEVELS[0];
        for (int i = 0; i < ZOOM_LEVELS.length; i++) {
            if (ZOOM_LEVELS[i] == current) {
                next = ZOOM_LEVELS[(i + 1) % ZOOM_LEVELS.length];
                break;
            }
        }
        prefs(ctx).edit().putInt(KEY_TEXT_ZOOM, next).apply();
        return next;
    }

    /** Move to the next smaller zoom level (wraps to largest). */
    static int cycleZoomDown(Context ctx) {
        int current = getTextZoom(ctx);
        int next = ZOOM_LEVELS[ZOOM_LEVELS.length - 1];
        for (int i = 0; i < ZOOM_LEVELS.length; i++) {
            if (ZOOM_LEVELS[i] == current) {
                next = ZOOM_LEVELS[(i - 1 + ZOOM_LEVELS.length) % ZOOM_LEVELS.length];
                break;
            }
        }
        prefs(ctx).edit().putInt(KEY_TEXT_ZOOM, next).apply();
        return next;
    }

    static void saveScrollPosition(Context ctx, String url, int scrollY) {
        if (ctx == null || url == null) return;
        prefs(ctx).edit().putInt(KEY_SCROLL_PREFIX + url, scrollY).apply();
    }

    static int getScrollPosition(Context ctx, String url) {
        if (ctx == null || url == null) return 0;
        return prefs(ctx).getInt(KEY_SCROLL_PREFIX + url, 0);
    }
}
