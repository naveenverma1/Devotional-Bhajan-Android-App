package com.nv.user.sunderkand.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nv.user.sunderkand.data.prefs.ReaderPrefs
import com.nv.user.sunderkand.ui.components.FontControls
import kotlin.math.roundToInt

/**
 * "Aa" sheet: everything that changes how the text looks or how the
 * screen behaves while reading. Lives in a ModalBottomSheet opened from
 * the reader's top bar so the bar itself stays uncluttered.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    lineSpacing: Float,
    onLineSpacingChange: (Float) -> Unit,
    themePref: String,
    onThemeChange: (String) -> Unit,
    keepScreenOn: Boolean,
    onKeepScreenOnChange: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Reading settings",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(20.dp))

        SettingRow(label = "Text size", value = "${(fontScale * 100).roundToInt()}%") {
            FontControls(
                onDecrease = { onFontScaleChange(fontScale - FONT_STEP) },
                onIncrease = { onFontScaleChange(fontScale + FONT_STEP) },
            )
        }
        Spacer(Modifier.height(16.dp))

        SettingLabel("Line spacing")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            LINE_SPACING_OPTIONS.forEachIndexed { index, (label, value) ->
                SegmentedButton(
                    selected = nearest(lineSpacing, LINE_SPACING_OPTIONS.map { it.second }) == value,
                    onClick = { onLineSpacingChange(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, LINE_SPACING_OPTIONS.size),
                ) { Text(label) }
            }
        }
        Spacer(Modifier.height(16.dp))

        SettingLabel("Theme")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            THEME_OPTIONS.forEachIndexed { index, (label, value) ->
                SegmentedButton(
                    selected = themePref == value,
                    onClick = { onThemeChange(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, THEME_OPTIONS.size),
                ) { Text(label) }
            }
        }
        Spacer(Modifier.height(16.dp))

        SettingRow(
            label = "Keep screen on",
            value = "Screen stays awake while reading",
        ) {
            Switch(checked = keepScreenOn, onCheckedChange = onKeepScreenOnChange)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    control: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        control()
    }
}

private fun nearest(value: Float, options: List<Float>): Float =
    options.minByOrNull { kotlin.math.abs(it - value) } ?: value

internal const val FONT_STEP = 0.1f

// "Normal" is the v4.0 look (1.0); the others nudge line-height either way.
private val LINE_SPACING_OPTIONS = listOf(
    "Compact" to 0.9f,
    "Normal" to 1.0f,
    "Relaxed" to 1.2f,
)

private val THEME_OPTIONS = listOf(
    "System" to ReaderPrefs.THEME_SYSTEM,
    "Light" to ReaderPrefs.THEME_LIGHT,
    "Dark" to ReaderPrefs.THEME_DARK,
)
