package com.nv.user.sunderkand.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nv.user.sunderkand.data.prefs.ReaderPrefs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive

/**
 * Teleprompter-style auto-scroll for the reader's LazyColumn.
 *
 * Behaviour (modelled on Moon+ Reader / teleprompter apps, tuned for
 * older devotees reading along while reciting):
 *
 *  - [active]  : the feature is switched on (control bar visible).
 *  - [paused]  : movement is suspended. Either the user pressed Pause
 *                (stays paused until they press Play) or they touched
 *                the text (auto-resumes after [TOUCH_RESUME_DELAY_MS]
 *                once their finger is off the screen).
 *  - Reaching the end of the list switches the feature off and fires
 *                [onReachedEnd] so the screen can say so.
 *
 * The scroll itself is driven by `withFrameNanos` + `scrollBy`, i.e. a
 * constant pixel velocity independent of frame rate. Each `scrollBy` is
 * a Default-priority mutation, so the moment the user starts dragging
 * (UserInput priority) our call is rejected with a CancellationException
 * -- that is how we detect the touch without any extra pointer plumbing.
 */
@Stable
class AutoScrollState internal constructor(
    private val listState: LazyListState,
) {
    var active by mutableStateOf(false)
        private set

    /** True while movement is suspended (user pause or touch pause). */
    var paused by mutableStateOf(false)
        private set

    /** Distinguishes an explicit Pause tap from a touch-interrupt. */
    private var pausedByUser by mutableStateOf(false)

    /** Paused because the user touched the list (will auto-resume), not via the Pause button. */
    val isTouchPaused: Boolean get() = paused && !pausedByUser

    /** Bumped every time the user touch-interrupts; drives the auto-resume timer. */
    private var interruptTick by mutableStateOf(0)

    /**
     * When true the reader is driving the list from the audio position
     * (verse timings) instead of the constant-velocity loop. Set by the
     * screen; the bar swaps its speed controls for a "with audio" label.
     */
    var followAudio by mutableStateOf(false)

    fun start() {
        active = true
        paused = false
        pausedByUser = false
    }

    fun stop() {
        active = false
        paused = false
        pausedByUser = false
    }

    fun toggle() = if (active) stop() else start()

    /** Explicit pause: stays paused until [resume]. */
    fun pause() {
        paused = true
        pausedByUser = true
    }

    fun resume() {
        paused = false
        pausedByUser = false
    }

    /** The user grabbed the list: pause now, auto-resume once they let go. */
    fun touchInterrupt() {
        paused = true
        pausedByUser = false
        interruptTick++
    }

    /**
     * Runs the scroll loop. Suspends for as long as the state is
     * [active] and not [paused]; returns when the end of the list is
     * reached (after calling [stop]) or when interrupted by user touch
     * (after flipping to the touch-paused state).
     */
    internal suspend fun run(pxPerSecond: Float, onReachedEnd: () -> Unit) {
        var lastFrame = 0L
        var carry = 0f
        try {
            while (currentCoroutineContext().isActive && active && !paused) {
                val dtNanos = withFrameNanos { now ->
                    val dt = if (lastFrame == 0L) 0L else now - lastFrame
                    lastFrame = now
                    dt
                }
                // Accumulate sub-pixel movement so slow speeds still move
                // smoothly instead of stalling on rounding.
                carry += pxPerSecond * dtNanos / 1_000_000_000f
                if (carry < 1f) continue
                val requested = carry
                val consumed = listState.scrollBy(requested)
                carry -= consumed
                if (consumed < requested * 0.5f && !listState.canScrollForward) {
                    stop()
                    onReachedEnd()
                    return
                }
            }
        } catch (e: CancellationException) {
            // Our own job being cancelled (screen left, state changed) must
            // propagate. A still-active job means the mutation was rejected
            // because the user grabbed the list -> treat as touch-pause.
            if (!currentCoroutineContext().isActive) throw e
            touchInterrupt()
        }
    }

    /** Auto-resume after the user's finger has been off the list for a moment. */
    internal suspend fun awaitTouchResume() {
        val tick = interruptTick
        // Wait until the user's drag / fling has fully settled.
        snapshotFlow { listState.isScrollInProgress }.filter { !it }.first()
        delay(TOUCH_RESUME_DELAY_MS)
        // Still the same interruption, still not an explicit pause -> go.
        if (active && paused && !pausedByUser && tick == interruptTick) resume()
    }

    companion object {
        const val TOUCH_RESUME_DELAY_MS = 2_000L
    }
}

/**
 * Creates the state and wires the side-effects (scroll loop and the
 * touch auto-resume timer) into the composition.
 *
 * @param level speed level (1..10); owned and persisted by the caller
 *              (ReaderPrefs) so it survives across chalisas and launches.
 */
@Composable
fun rememberAutoScrollState(
    listState: LazyListState,
    level: Int,
    onReachedEnd: () -> Unit,
): AutoScrollState {
    val state = remember(listState) { AutoScrollState(listState) }
    val density = LocalDensity.current
    val latestOnReachedEnd by rememberUpdatedState(onReachedEnd)

    // Constant-velocity loop: restarts whenever active/paused/speed flips.
    // Not used while following audio -- the screen drives the list then.
    val running = state.active && !state.paused && !state.followAudio
    LaunchedEffect(running, level) {
        if (!running) return@LaunchedEffect
        val pxPerSecond = with(density) { ReaderPrefs.autoScrollDpPerSecond(level).dp.toPx() }
        state.run(pxPerSecond) { latestOnReachedEnd() }
    }

    // Auto-resume after a touch interruption.
    val touchPaused = state.active && state.paused
    LaunchedEffect(touchPaused) {
        if (touchPaused) state.awaitTouchResume()
    }

    return state
}

/**
 * Floating pill with the auto-scroll controls:
 *   [Pause/Play]  [−]  speed  [+]  [✕]
 *
 * Large 48dp targets throughout -- the audience skews older.
 */
@Composable
fun AutoScrollBar(
    state: AutoScrollState,
    level: Int,
    onLevelChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = state.active,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 6.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                IconButton(
                    onClick = { if (state.paused) state.resume() else state.pause() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = if (state.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (state.paused) "Resume auto-scroll" else "Pause auto-scroll",
                        modifier = Modifier.size(28.dp),
                    )
                }
                if (state.followAudio) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Column {
                            Text(
                                text = "गायन के साथ",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (state.paused) "रुका · Paused" else "Following audio",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = { onLevelChange(level - 1) },
                        enabled = level > 1,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Slower")
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(44.dp),
                    ) {
                        Text(
                            text = "$level",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = if (state.paused) "रुका" else "गति",
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                    IconButton(
                        onClick = { onLevelChange(level + 1) },
                        enabled = level < ReaderPrefs.MAX_AUTO_SCROLL_LEVEL,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Faster")
                    }
                }
                IconButton(
                    onClick = { state.stop() },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Stop auto-scroll")
                }
            }
        }
    }
}
