package com.nv.user.sunderkand.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nv.user.sunderkand.data.model.Verse
import com.nv.user.sunderkand.data.model.VerseType

/**
 * Renders a single Verse (doha / chaupai / shlok / mantra / verse).
 * The visual treatment differs slightly per kind:
 *
 *  - doha     : centered, italic, slight extra side padding (these are
 *               always the opening / closing couplets, so they should
 *               feel like a frame around the chaupais).
 *  - chaupai  : left-aligned with a small numeric label.
 *  - shlok    : centered, larger leading for breath.
 *  - mantra   : same as verse, but italicized.
 *  - verse    : the default (used for Aarti refrains, etc.).
 *
 * Long-pressing a verse triggers [onLongPress] (used by the reader to
 * surface a Share / Copy sheet).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VerseRow(
    verse: Verse,
    fontScale: Float,
    onLongPress: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val baseStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontScale,
        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * fontScale,
    )

    val align: TextAlign = when (verse.type) {
        VerseType.DOHA, VerseType.SHLOK -> TextAlign.Center
        else -> TextAlign.Start
    }
    val fontStyle = when (verse.type) {
        VerseType.DOHA, VerseType.MANTRA -> FontStyle.Italic
        else -> FontStyle.Normal
    }
    val color = when (verse.type) {
        VerseType.SHLOK -> MaterialTheme.colorScheme.secondary
        VerseType.DOHA -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val sidePadding = when (verse.type) {
        VerseType.DOHA, VerseType.SHLOK -> 24.dp
        else -> 16.dp
    }

    val interaction = remember { MutableInteractionSource() }
    val clickable = if (onLongPress != null) {
        Modifier.combinedClickable(
            interactionSource = interaction,
            indication = ripple(bounded = true, color = MaterialTheme.colorScheme.primary),
            onClick = { /* no-op for now; long-press is the gesture */ },
            onLongClick = onLongPress,
        )
    } else Modifier

    Column(
        modifier = modifier
            .then(clickable)
            .padding(PaddingValues(horizontal = sidePadding, vertical = 8.dp)),
        horizontalAlignment = if (align == TextAlign.Center) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        if (verse.number != null && verse.type == VerseType.CHAUPAI) {
            Text(
                text = "॥${verse.number}॥",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        verse.lines.forEach { line ->
            Text(
                text = line,
                style = baseStyle.copy(fontStyle = fontStyle),
                color = color,
                textAlign = align,
                modifier = Modifier.padding(vertical = 1.dp),
            )
        }
    }
}
