package com.nv.user.sunderkand.ui.sankalp

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nv.user.sunderkand.SunderkandApp
import com.nv.user.sunderkand.data.prefs.SankalpState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val GOAL_OPTIONS = listOf(11, 21, 41, 108)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SankalpScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SunderkandApp
    val state by app.sankalp.state.collectAsStateWithLifecycle(initialValue = SankalpState())
    val scope = rememberCoroutineScope()

    var showStartDialog by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val chalisaTitle: String? = remember(state.chalisaId) {
        state.chalisaId?.let { app.content.chalisaById(it)?.title }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("सङ्कल्प", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isActive) {
                ActiveSankalpCard(
                    state = state,
                    chalisaTitle = chalisaTitle,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { scope.launch { app.sankalp.incrementDone() } },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isComplete,
                    ) {
                        Text("एक पाठ अंकित करें")
                    }
                }
                OutlinedButton(
                    onClick = { showResetConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("नया सङ्कल्प आरंभ करें")
                }
                Text(
                    text = "Sankalp is a quiet personal commitment — to read one of the chalisas N times. Tap the button above each time you complete a path; the count syncs to your device only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                EmptyStateCopy(
                    onStart = { showStartDialog = true },
                )
            }
        }
    }

    if (showStartDialog) {
        StartSankalpDialog(
            onDismiss = { showStartDialog = false },
            onConfirm = { chalisaId, goal ->
                scope.launch {
                    app.sankalp.startSankalp(chalisaId, goal)
                }
                showStartDialog = false
            },
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset sankalp?") },
            text = { Text("Your current count will be lost. A new sankalp can be started right after.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        app.sankalp.reset()
                    }
                    showResetConfirm = false
                    showStartDialog = true
                }) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ActiveSankalpCard(
    state: SankalpState,
    chalisaTitle: String?,
) {
    val complete = state.isComplete
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (complete) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "सङ्कल्प",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (chalisaTitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = chalisaTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "${state.done} / ${state.goal}",
                style = MaterialTheme.typography.displaySmall,
                color = if (complete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (complete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            )
            if (complete) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "सङ्कल्प पूर्ण ·  जय जय श्री राम",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCopy(onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "एक नया सङ्कल्प लें",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Commit to read a chalisa or path a chosen number of times (11, 21, 41, or 108). Mark each completion when you finish a reading; the progress is saved on this device only.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text("सङ्कल्प आरंभ करें", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartSankalpDialog(
    onDismiss: () -> Unit,
    onConfirm: (chalisaId: String, goal: Int) -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SunderkandApp
    val chalisas = remember { app.content.allChalisas() }
    var selectedChalisaId by remember { mutableStateOf(chalisas.firstOrNull()?.id.orEmpty()) }
    var selectedGoal by remember { mutableStateOf(11) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("नया सङ्कल्प") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose a chalisa:", style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    chalisas.forEach { c ->
                        FilterChip(
                            selected = c.id == selectedChalisaId,
                            onClick = { selectedChalisaId = c.id },
                            label = { Text(c.title, maxLines = 1) },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Choose a count:", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GOAL_OPTIONS.forEach { goal ->
                        FilterChip(
                            selected = goal == selectedGoal,
                            onClick = { selectedGoal = goal },
                            label = { Text("$goal") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedChalisaId, selectedGoal) },
                enabled = selectedChalisaId.isNotBlank(),
            ) { Text("Start") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
