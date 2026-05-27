package com.nv.user.sunderkand

import android.app.Application
import com.nv.user.sunderkand.audio.PlayerControllerHolder
import com.nv.user.sunderkand.data.ContentRepository
import com.nv.user.sunderkand.data.prefs.ReaderPrefs
import com.nv.user.sunderkand.data.prefs.SankalpStore

/**
 * Application singleton — owns the long-lived data + audio layer so
 * screens can pull it via
 *   (LocalContext.current.applicationContext as SunderkandApp)
 * without dragging in a DI framework.
 */
class SunderkandApp : Application() {

    /** Bundled-content repository (loads assets/content.json once). */
    val content: ContentRepository by lazy { ContentRepository(this) }

    /** User-controllable reader prefs + per-chalisa reading state. */
    val readerPrefs: ReaderPrefs by lazy { ReaderPrefs(this) }

    /** Optional sankalp / path-count commitment tracker. */
    val sankalp: SankalpStore by lazy { SankalpStore(this) }

    /**
     * Bridge to the Media3 MediaSessionService. The actual ExoPlayer
     * lives in [com.nv.user.sunderkand.audio.ChalisaPlayerService];
     * this holder lazily acquires a MediaController and exposes a
     * StateFlow<PlayerSnapshot> for the UI.
     */
    val player: PlayerControllerHolder by lazy { PlayerControllerHolder(this) }

    /**
     * Translate the chalisa's `audio` field (e.g. "hanumanchalisa")
     * to the matching R.raw resource id. Returns 0 if not found.
     */
    fun rawIdForAudio(name: String?): Int {
        if (name.isNullOrBlank()) return 0
        return resources.getIdentifier(name, "raw", packageName)
    }
}
