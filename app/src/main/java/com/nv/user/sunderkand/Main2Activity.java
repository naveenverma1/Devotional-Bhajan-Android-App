package com.nv.user.sunderkand;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Single-fragment host activity. The actual navigation is owned by
 * fraghome and the back stack of the FrameLayout it lives in.
 *
 * targetSdk 35 enforces edge-to-edge layout on Android 15+, so we
 * explicitly opt in via WindowCompat and pad the fragment container by
 * the system-bar insets so fragments aren't drawn under the status /
 * navigation bars. We also force dark status-bar icons because our
 * surface is the light cream brand colour.
 */
public class Main2Activity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(true);
        insetsController.setAppearanceLightNavigationBars(true);

        setContentView(R.layout.activity_main2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Framelay),
                (v, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                                    | WindowInsetsCompat.Type.displayCutout());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return WindowInsetsCompat.CONSUMED;
                });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.Framelay, new fraghome())
                    .commit();
        }
    }
}
