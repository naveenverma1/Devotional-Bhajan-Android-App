package com.nv.user.sunderkand.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Snapshot of the audio player's state that the Compose layer observes.
 */
data class PlayerSnapshot(
    val mediaId: String? = null,
    val title: String = "",
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    /** Remaining millis on the sleep timer, or null if no timer is set. */
    val sleepTimerRemainingMs: Long? = null,
) {
    val isAvailable: Boolean get() = mediaId != null
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

/**
 * Bridges the Compose UI and [ChalisaPlayerService].
 *
 *  - Builds a MediaController on first access (suspending) and caches
 *    it for the lifetime of the application process.
 *  - Exposes a hot [snapshot] StateFlow that the mini-player and the
 *    reader's play/pause button observe with collectAsStateWithLifecycle.
 *  - Provides high-level play / pause / toggle / seekTo APIs that
 *    push commands through the controller (which in turn forwards to
 *    the service's ExoPlayer).
 *  - Owns the sleep-timer coroutine; survives navigation because the
 *    holder is application-scoped.
 */
class PlayerControllerHolder(private val appContext: Context) {

    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _snapshot = MutableStateFlow(PlayerSnapshot())
    val snapshot: StateFlow<PlayerSnapshot> = _snapshot.asStateFlow()

    @Volatile
    private var controller: MediaController? = null
    private var listenerJob: Job? = null

    private var sleepTimerJob: Job? = null
    private var sleepTimerEndAt: Long = 0L

    /**
     * Suspend until we have a connected MediaController. Idempotent --
     * subsequent calls return the cached instance.
     */
    private suspend fun acquire(): MediaController {
        controller?.let { return it }
        val token = SessionToken(
            appContext,
            ComponentName(appContext, ChalisaPlayerService::class.java),
        )
        val future = MediaController.Builder(appContext, token).buildAsync()
        return suspendCancellableCoroutine { cont ->
            future.addListener({
                try {
                    val c = future.get()
                    controller = c
                    attachListener(c)
                    cont.resumeWith(Result.success(c))
                } catch (t: Throwable) {
                    cont.resumeWith(Result.failure(t))
                }
            }, MoreExecutors.directExecutor())
        }
    }

    private fun attachListener(c: MediaController) {
        listenerJob?.cancel()
        listenerJob = scope.launch {
            playerEvents(c).collect { /* state is pushed via the listener below */ }
        }
        c.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                pushSnapshot(player)
            }
        })
        pushSnapshot(c)
        // Tick position updates at 2 Hz while playing so the mini-player
        // progress bar moves without us hammering the controller.
        scope.launch {
            while (true) {
                val p = controller ?: return@launch
                if (p.isPlaying) pushSnapshot(p)
                delay(500)
            }
        }
    }

    private fun playerEvents(c: MediaController): Flow<Unit> = callbackFlow {
        val l = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                trySend(Unit)
            }
        }
        c.addListener(l)
        awaitClose { c.removeListener(l) }
    }

    private fun pushSnapshot(p: Player) {
        val meta = p.mediaMetadata
        val remaining = if (sleepTimerEndAt > 0L) {
            (sleepTimerEndAt - System.currentTimeMillis()).takeIf { it > 0L }
        } else {
            null
        }
        _snapshot.value = PlayerSnapshot(
            mediaId = p.currentMediaItem?.mediaId,
            title = meta.title?.toString().orEmpty(),
            isPlaying = p.isPlaying,
            positionMs = p.currentPosition.coerceAtLeast(0L),
            durationMs = p.duration.takeIf { it > 0L } ?: 0L,
            sleepTimerRemainingMs = remaining,
        )
    }

    /**
     * Play the bundled raw resource [audioResId]. If a different item
     * is currently loaded, swap it. If the same item is loaded but
     * paused, just resume.
     */
    fun playRaw(audioResId: Int, mediaId: String, title: String) {
        scope.launch {
            val c = runCatching { acquire() }.getOrNull() ?: return@launch
            val current = c.currentMediaItem?.mediaId
            if (current != mediaId) {
                val item = MediaItem.Builder()
                    .setMediaId(mediaId)
                    .setUri(Uri.parse("android.resource://${appContext.packageName}/$audioResId"))
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(title)
                            .setArtist("Sunderkand Path")
                            .setIsBrowsable(false)
                            .setIsPlayable(true)
                            .build(),
                    )
                    .build()
                c.setMediaItem(item)
                c.prepare()
            }
            c.play()
        }
    }

    fun pause() {
        scope.launch { controller?.pause() }
    }

    fun togglePlayPause() {
        scope.launch {
            val c = controller ?: return@launch
            if (c.isPlaying) c.pause() else c.play()
        }
    }

    fun seekToFraction(fraction: Float) {
        scope.launch {
            val c = controller ?: return@launch
            val d = c.duration
            if (d > 0L) c.seekTo((d * fraction.coerceIn(0f, 1f)).toLong())
        }
    }

    /** Restart the current track from the beginning (and start playing). */
    fun restart() {
        scope.launch {
            val c = controller ?: return@launch
            c.seekTo(0L)
            c.play()
        }
    }

    /** Skip by [deltaMs] (negative = back, positive = forward), clamped to [0..duration]. */
    fun skipBy(deltaMs: Long) {
        scope.launch {
            val c = controller ?: return@launch
            val d = c.duration
            val target = (c.currentPosition + deltaMs).coerceIn(0L, if (d > 0L) d else Long.MAX_VALUE)
            c.seekTo(target)
        }
    }

    /**
     * Schedule the player to pause after [durationMs]. Passing 0 (or
     * negative) cancels any pending timer.
     *
     * The timer ticks at 1 Hz so the mini-player chip can show a live
     * MM:SS countdown.
     */
    fun setSleepTimer(durationMs: Long) {
        sleepTimerJob?.cancel()
        if (durationMs <= 0L) {
            sleepTimerEndAt = 0L
            controller?.let { pushSnapshot(it) }
            return
        }
        sleepTimerEndAt = System.currentTimeMillis() + durationMs
        sleepTimerJob = scope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                if (now >= sleepTimerEndAt) {
                    controller?.pause()
                    sleepTimerEndAt = 0L
                    controller?.let { pushSnapshot(it) }
                    return@launch
                }
                controller?.let { pushSnapshot(it) }
                delay(1000)
            }
        }
    }

    fun cancelSleepTimer() = setSleepTimer(0L)

    fun release() {
        listenerJob?.cancel()
        sleepTimerJob?.cancel()
        controller?.release()
        controller = null
    }
}
