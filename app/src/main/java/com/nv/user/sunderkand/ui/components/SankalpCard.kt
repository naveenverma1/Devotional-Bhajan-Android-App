package com.nv.user.sunderkand.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nv.user.sunderkand.data.prefs.SankalpState

/**
 * Card shown on the home screen when the user has an active sankalp.
 * Quiet: no confetti, no animation. The progress bar fills as paths
 * complete; on completion (done >= goal) the subtitle changes to a
 * "सङ्कल्प पूर्ण" line in gold and the tertiary color leads.
 */
@Composable
fun SankalpCard(
    state: SankalpState,
    chalisaTitle: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val complete = state.isComplete
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (complete) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = if (complete) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
                Spacer(Modifier.padding(start = 8.dp))
                Text(
                    text = "सङ्कल्प",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (chalisaTitle != null) {
                Text(
                    text = chalisaTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (complete) {
                    "सङ्कल्प पूर्ण  · ${state.done} / ${state.goal}"
                } else {
                    "${state.done} / ${state.goal} पाठ पूर्ण"
                },
                style = MaterialTheme.typography.titleMedium,
                color = if (complete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (complete) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            )
        }
    }
}
