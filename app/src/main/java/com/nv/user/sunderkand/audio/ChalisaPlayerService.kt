package com.nv.user.sunderkand.audio

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.nv.user.sunderkand.MainActivity

/**
 * Foreground Media3 service that owns the single ExoPlayer for the whole
 * app. By living in a separate service:
 *
 *  - Audio survives the Activity being backgrounded or destroyed.
 *  - Android draws a media-style notification with play/pause/seek
 *    controls (handled entirely by MediaSessionService).
 *  - Lock-screen + Bluetooth + Android Auto controls work for free
 *    via MediaSession's PlaybackState wiring.
 *
 * The Compose UI talks to the service via a MediaController acquired
 * through [PlayerControllerHolder]; the service itself never holds a
 * reference back into the UI layer.
 */
@OptIn(UnstableApi::class)
class ChalisaPlayerService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        // Tap-the-notification deep-link back to the host activity.
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    /** MediaSessionService contract: hand out the session to controllers. */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // If the user swipes the app away while audio is paused, allow
        // the service to be cleaned up. (When playing, Media3 keeps the
        // service alive automatically.)
        mediaSession?.let { session ->
            if (!session.player.isPlaying) {
                stopSelf()
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
