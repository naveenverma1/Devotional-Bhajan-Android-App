package com.nv.user.sunderkand.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * The label that introduces a group of verses — दोहा / चौपाई / श्लोक.
 * Uses the display font so it visually outranks the body text without
 * dominating it.
 */
@Composable
fun SectionHeading(
    heading: String,
    modifier: Modifier = Modifier,
) {
    if (heading.isBlank()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = heading,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        HorizontalDivider(
            modifier = Modifier
                .padding(top = 6.dp, start = 96.dp, end = 96.dp),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
        )
    }
}
