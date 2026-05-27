package com.nv.user.sunderkand

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.nv.user.sunderkand.nav.SunderkandNavHost
import com.nv.user.sunderkand.ui.components.MiniPlayer
import com.nv.user.sunderkand.ui.theme.SunderkandTheme

/**
 * v4.0 single-Activity entry point.
 *
 *  - installSplashScreen() runs FIRST so the saffron Hanuman splash
 *    (Theme.Sunderkand.Splash) is the very first frame on cold start.
 *  - The splash auto-dismisses when the Compose tree's first frame is
 *    ready, which is what you want -- no fixed delay, no flash.
 *
 * Hosts:
 *  - SunderkandNavHost  (home / reader / about / sankalp)
 *  - MiniPlayer overlay (auto-hides when no audio is loaded; persists
 *                        across screens via the Activity's compose
 *                        tree, not inside the NavHost)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SunderkandTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    SunderkandNavHost()
                    MiniPlayer(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}
