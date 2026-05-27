package com.nv.user.sunderkand.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nv.user.sunderkand.audio.PlayerControllerHolder
import com.nv.user.sunderkand.audio.PlayerSnapshot

/**
 * The expanded "Now Playing" view shown in a ModalBottomSheet when
 * the user taps the mini-player. Music-player ergonomics:
 *
 *  - Large title at the top.
 *  - Scrubbable Slider with current / total mm:ss labels.
 *  - Skip back 10s · play/pause · skip forward 10s row.
 *  - Replay (seek to 0) + sleep-timer entry point row.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    state: PlayerSnapshot,
    player: PlayerControllerHolder,
    onOpenSleepTimer: () -> Unit,
) {
    // While the user is dragging the slider, ignore live position
    // updates so the thumb doesn't jump backwards under their finger.
    var scrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    val effectiveFraction = if (scrubbing) scrubFraction else state.progress

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(
            text = state.title.ifBlank { "Now playing" },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(24.dp))

        Slider(
            value = effectiveFraction.coerceIn(0f, 1f),
            onValueChange = {
                scrubbing = true
                scrubFraction = it
            },
            onValueChangeFinished = {
                player.seekToFraction(scrubFraction)
                scrubbing = false
            },
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
            ),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val pos = if (scrubbing) (state.durationMs * scrubFraction).toLong() else state.positionMs
            Text(
                text = formatMmSs(pos),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatMmSs(state.durationMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleIconButton(
                icon = Icons.Filled.Replay10,
                contentDescription = "Skip back 10 seconds",
                onClick = { player.skipBy(-10_000L) },
                size = 56.dp,
            )
            CircleIconButton(
                icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (state.isPlaying) "Pause" else "Play",
                onClick = { player.togglePlayPause() },
                size = 72.dp,
                primary = true,
            )
            CircleIconButton(
                icon = Icons.Filled.Forward10,
                contentDescription = "Skip forward 10 seconds",
                onClick = { player.skipBy(10_000L) },
                size = 56.dp,
            )
        }

        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            LabelledIconButton(
                icon = Icons.Filled.Replay,
                label = "Replay",
                onClick = { player.restart() },
            )
            LabelledIconButton(
                icon = Icons.Filled.Bedtime,
                label = if (state.sleepTimerRemainingMs != null) {
                    "Sleep ${formatMmSs(state.sleepTimerRemainingMs)}"
                } else "Sleep timer",
                onClick = onOpenSleepTimer,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun CircleIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    primary: Boolean = false,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(size / 2),
            )
        }
    }
}

@Composable
private fun LabelledIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(120.dp),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal fun formatMmSs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0)
    val mm = totalSec / 60
    val ss = totalSec % 60
    return String.format("%d:%02d", mm, ss)
}
