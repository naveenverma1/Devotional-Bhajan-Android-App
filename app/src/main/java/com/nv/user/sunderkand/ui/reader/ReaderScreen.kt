package com.nv.user.sunderkand.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nv.user.sunderkand.SunderkandApp
import com.nv.user.sunderkand.data.model.Chalisa
import com.nv.user.sunderkand.data.model.Verse
import com.nv.user.sunderkand.data.prefs.ReaderPrefs
import com.nv.user.sunderkand.share.VerseSharer
import com.nv.user.sunderkand.ui.components.FontControls
import com.nv.user.sunderkand.ui.components.SectionHeading
import com.nv.user.sunderkand.ui.components.VerseRow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The reader screen for one chalisa.
 *
 *  - Top app bar:  back arrow, title, play/pause, A−/A+ font controls
 *  - Body:        LazyColumn of verses grouped by section. Long-press a
 *                 verse to open a Share / Copy bottom sheet.
 *  - Persists:    last scrolled-to verse index + font scale (DataStore)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    chalisaId: String,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SunderkandApp
    val prefs = app.readerPrefs

    val chalisa: Chalisa? = remember(chalisaId) { app.content.chalisaById(chalisaId) }
    if (chalisa == null) {
        // Unknown id — render a benign empty screen rather than crash.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Unknown chalisa: $chalisaId")
        }
        return
    }

    LoadedReaderScreen(chalisa = chalisa, prefs = prefs, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadedReaderScreen(
    chalisa: Chalisa,
    prefs: ReaderPrefs,
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SunderkandApp
    val playerState by app.player.snapshot.collectAsStateWithLifecycle()
    val fontScale by prefs.fontScale.collectAsStateWithLifecycle(
        initialValue = ReaderPrefs.DEFAULT_FONT_SCALE,
    )

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Flat ordered list of items to render: a heading is its own row,
    // a verse is its own row. Stable keys so item identity survives
    // recomposition.
    val items: List<ReaderItem> = remember(chalisa) { buildReaderItems(chalisa) }

    // Restore the user's last reading position on first composition.
    LaunchedEffect(chalisa.id) {
        prefs.setLastOpenedChalisa(chalisa.id)
        val idx = prefs.lastReadVerse(chalisa.id).first()
        if (idx in items.indices) {
            listState.scrollToItem(idx)
        }
    }

    // Persist the topmost visible item whenever the user scrolls.
    LaunchedEffect(listState, chalisa.id) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collectLatest { idx -> prefs.setLastReadVerse(chalisa.id, idx) }
    }

    fun bumpFont(delta: Float) {
        scope.launch {
            val current = prefs.fontScale.first()
            prefs.setFontScale(current + delta)
        }
    }

    val audioRaw: Int = remember(chalisa.audio) { app.rawIdForAudio(chalisa.audio) }
    val isThisChalisaPlaying = playerState.mediaId == chalisa.id && playerState.isPlaying

    // Long-pressed verse + the section heading it belongs to (for the share sheet).
    var shareTarget by remember { mutableStateOf<Pair<Verse, String?>?>(null) }
    val shareSheetState = rememberModalBottomSheetState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        chalisa.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    if (audioRaw != 0) {
                        IconButton(
                            onClick = {
                                if (isThisChalisaPlaying) {
                                    app.player.pause()
                                } else {
                                    app.player.playRaw(
                                        audioResId = audioRaw,
                                        mediaId = chalisa.id,
                                        title = chalisa.title,
                                    )
                                }
                            },
                        ) {
                            Icon(
                                imageVector = if (isThisChalisaPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isThisChalisaPlaying) "Pause" else "Play",
                            )
                        }
                    }
                    FontControls(
                        onDecrease = { bumpFont(-FONT_STEP) },
                        onIncrease = { bumpFont(+FONT_STEP) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            items(
                items = items,
                key = { it.stableKey },
            ) { item ->
                when (item) {
                    is ReaderItem.Heading -> SectionHeading(heading = item.text)
                    is ReaderItem.VerseItem -> VerseRow(
                        verse = item.verse,
                        fontScale = fontScale,
                        onLongPress = {
                            val heading = chalisa.sections.getOrNull(item.sectionIndex)?.heading
                            shareTarget = item.verse to heading
                        },
                    )
                }
            }
        }
    }

    val target = shareTarget
    if (target != null) {
        val (verse, heading) = target
        ModalBottomSheet(
            onDismissRequest = { shareTarget = null },
            sheetState = shareSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ShareVerseSheet(
                chalisa = chalisa,
                verse = verse,
                sectionHeading = heading,
                onShare = {
                    val text = VerseSharer.buildShareText(chalisa, heading, verse)
                    VerseSharer.shareViaSystem(ctx, text)
                    shareTarget = null
                },
                onCopy = {
                    val text = VerseSharer.buildShareText(chalisa, heading, verse)
                    VerseSharer.copyToClipboard(ctx, text)
                    shareTarget = null
                },
            )
        }
    }
}

@Composable
private fun ShareVerseSheet(
    chalisa: Chalisa,
    verse: Verse,
    sectionHeading: String?,
    onShare: () -> Unit,
    onCopy: () -> Unit,
) {
    val preview = remember(chalisa.id, verse) {
        VerseSharer.buildShareText(chalisa, sectionHeading, verse)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(
            text = "Share verse",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = preview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ShareActionButton(
                label = "Share",
                icon = Icons.Filled.Share,
                onClick = onShare,
                modifier = Modifier.weight(1f),
            )
            ShareActionButton(
                label = "Copy",
                icon = Icons.Filled.ContentCopy,
                onClick = onCopy,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ShareActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = modifier.height(56.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(Modifier.padding(start = 8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private const val FONT_STEP = 0.1f

/** Flattened item list for the LazyColumn. */
private sealed interface ReaderItem {
    val stableKey: String

    data class Heading(val text: String, val sectionIndex: Int) : ReaderItem {
        override val stableKey: String = "h_$sectionIndex"
    }

    data class VerseItem(
        val verse: Verse,
        val sectionIndex: Int,
        val verseIndex: Int,
    ) : ReaderItem {
        override val stableKey: String = "v_${sectionIndex}_${verseIndex}"
    }
}

private fun buildReaderItems(chalisa: Chalisa): List<ReaderItem> = buildList {
    chalisa.sections.forEachIndexed { sIdx, section ->
        if (section.heading.isNotBlank()) {
            add(ReaderItem.Heading(section.heading, sIdx))
        }
        section.verses.forEachIndexed { vIdx, verse ->
            add(ReaderItem.VerseItem(verse, sIdx, vIdx))
        }
    }
}
