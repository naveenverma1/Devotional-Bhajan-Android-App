package com.nv.user.sunderkand.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nv.user.sunderkand.SunderkandApp
import kotlinx.coroutines.launch

/**
 * Persistent bottom mini-player.
 *
 *  - Auto-hides when no media is loaded.
 *  - Title-and-progress region is clickable -> opens the full
 *    NowPlayingSheet (scrubbable seek bar, skip back / forward 10s,
 *    replay, sleep-timer entry).
 *  - Bedtime icon -> SleepTimerSheet directly (so it's reachable in
 *    one tap from anywhere in the app, no need to expand first).
 *  - Play/Pause icon stays inline for quick toggle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(modifier: Modifier = Modifier) {
    val app = LocalContext.current.applicationContext as SunderkandApp
    val state by app.player.snapshot.collectAsStateWithLifecycle()
    var showSleepSheet by remember { mutableStateOf(false) }
    var showNowPlaying by remember { mutableStateOf(false) }
    val sleepSheetState = rememberModalBottomSheetState()
    val nowPlayingSheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = state.isAvailable,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                            .clickable { showNowPlaying = true },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = state.title.ifBlank { "Now playing" },
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowUp,
                                contentDescription = "Open player",
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(start = 4.dp),
                            )
                        }
                        state.sleepTimerRemainingMs?.let { remaining ->
                            Text(
                                text = "Sleep in ${formatMmSs(remaining)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            )
                        }
                    }
                    IconButton(
                        onClick = { showSleepSheet = true },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bedtime,
                            contentDescription = "Sleep timer",
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    IconButton(
                        onClick = { app.player.togglePlayPause() },
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            imageVector = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (state.isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.primary),
                    color = MaterialTheme.colorScheme.onPrimary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
                )
            }
        }
    }

    if (showNowPlaying) {
        ModalBottomSheet(
            onDismissRequest = { showNowPlaying = false },
            sheetState = nowPlayingSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            NowPlayingSheet(
                state = state,
                player = app.player,
                onOpenSleepTimer = {
                    coroutineScope.launch {
                        nowPlayingSheetState.hide()
                        showNowPlaying = false
                        showSleepSheet = true
                    }
                },
            )
        }
    }

    if (showSleepSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSleepSheet = false },
            sheetState = sleepSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            SleepTimerSheet(
                currentRemainingMs = state.sleepTimerRemainingMs,
                onPick = { minutes ->
                    app.player.setSleepTimer(minutes * 60_000L)
                    coroutineScope.launch {
                        sleepSheetState.hide()
                        showSleepSheet = false
                    }
                },
                onClear = {
                    app.player.cancelSleepTimer()
                    coroutineScope.launch {
                        sleepSheetState.hide()
                        showSleepSheet = false
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    currentRemainingMs: Long?,
    onPick: (minutes: Int) -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Sleep timer",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Audio will pause automatically when the timer ends.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
        )
        if (currentRemainingMs != null) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bedtime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Stopping in ${formatMmSs(currentRemainingMs)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp),
                    )
                    IconButton(onClick = onClear) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Cancel timer",
                        )
                    }
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            listOf(5, 10, 30).forEach { minutes ->
                AssistChip(
                    onClick = { onPick(minutes) },
                    label = { Text("${minutes} min") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    ),
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 24.dp),
        ) {
            listOf(45, 60, 90).forEach { minutes ->
                AssistChip(
                    onClick = { onPick(minutes) },
                    label = { Text("${minutes} min") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    ),
                )
            }
        }
        Box(modifier = Modifier.padding(bottom = 8.dp))
    }
}
