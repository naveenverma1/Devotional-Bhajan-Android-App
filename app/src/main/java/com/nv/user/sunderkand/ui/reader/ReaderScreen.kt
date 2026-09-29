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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nv.user.sunderkand.SunderkandApp
import com.nv.user.sunderkand.data.model.Chalisa
import com.nv.user.sunderkand.data.model.Verse
import com.nv.user.sunderkand.data.prefs.ReaderPrefs
import com.nv.user.sunderkand.share.VerseSharer
import com.nv.user.sunderkand.ui.components.LocalBottomOverlayHeight
import com.nv.user.sunderkand.ui.components.SectionHeading
import com.nv.user.sunderkand.ui.components.VerseRow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The reader screen for one chalisa.
 *
 *  - Top app bar:  back arrow, title, play/pause, auto-scroll toggle,
 *                 "Aa" reading-settings sheet (text size, line spacing,
 *                 theme, keep-screen-on).
 *  - Body:        LazyColumn of verses grouped by section. Long-press a
 *                 verse to open a Share / Copy bottom sheet.
 *  - Auto-scroll: floating control pill at the bottom (see AutoScroll.kt).
 *  - Persists:    last scrolled-to verse index, font scale, line spacing,
 *                 auto-scroll speed (DataStore).
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
    val fontScale by prefs.fontScale.collectAsStateWithLifecycle(initialValue = ReaderPrefs.DEFAULT_FONT_SCALE)
    val lineSpacing by prefs.lineSpacing.collectAsStateWithLifecycle(initialValue = ReaderPrefs.DEFAULT_LINE_SPACING)
    val themePref by prefs.themePref.collectAsStateWithLifecycle(initialValue = ReaderPrefs.DEFAULT_THEME)
    val keepScreenOnPref by prefs.keepScreenOn.collectAsStateWithLifecycle(initialValue = ReaderPrefs.DEFAULT_KEEP_SCREEN_ON)
    val autoScrollLevel by prefs.autoScrollLevel.collectAsStateWithLifecycle(initialValue = ReaderPrefs.DEFAULT_AUTO_SCROLL_LEVEL)

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val bottomOverlay = LocalBottomOverlayHeight.current

    // Flat ordered list of items to render: a heading is its own row,
    // a verse is its own row. Stable keys so item identity survives
    // recomposition.
    val items: List<ReaderItem> = remember(chalisa) { buildReaderItems(chalisa) }

    // Restore the user's last reading position on first composition.
    // `restored` gates the persist effect below: without it the very
    // first snapshotFlow emission (index 0, before the restore has run)
    // would overwrite the saved position and every open would start at
    // the top.
    var restored by remember(chalisa.id) { mutableStateOf(false) }
    LaunchedEffect(chalisa.id) {
        val idx = prefs.lastReadVerse(chalisa.id).first()
        if (idx in items.indices) {
            listState.scrollToItem(idx)
        }
        restored = true
        prefs.setLastOpenedChalisa(chalisa.id)
    }

    // Persist the topmost visible item whenever the user scrolls.
    // Debounced so auto-scroll doesn't hammer DataStore every frame.
    LaunchedEffect(restored, chalisa.id) {
        if (!restored) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .drop(1)
            .collectLatest { idx ->
                delay(400)
                prefs.setLastReadVerse(chalisa.id, idx)
            }
    }

    val autoScroll = rememberAutoScrollState(
        listState = listState,
        level = autoScrollLevel,
        onReachedEnd = {
            scope.launch { snackbarHostState.showSnackbar("पाठ पूर्ण हुआ · Reached the end") }
        },
    )

    // ---- Audio sync ---------------------------------------------------
    // (startMs, flat item index) for every timed verse, ascending.
    val timedItems: List<Pair<Long, Int>> = remember(items) {
        items.mapIndexedNotNull { i, item ->
            (item as? ReaderItem.VerseItem)?.verse?.startMs?.let { it to i }
        }.sortedBy { it.first }
    }
    val audioLoadedHere = playerState.mediaId == chalisa.id
    val syncAvailable = audioLoadedHere && timedItems.isNotEmpty()
    // Index of the verse currently being sung, or -1.
    val currentItemIndex: Int = remember(syncAvailable, playerState.positionMs) {
        if (!syncAvailable) -1
        else timedItems.lastOrNull { it.first <= playerState.positionMs }?.second ?: -1
    }
    // While this chalisa's timed audio is loaded, auto-scroll follows the
    // singer instead of running at a fixed pace.
    SideEffect { autoScroll.followAudio = syncAvailable }
    val following = autoScroll.active && !autoScroll.paused && autoScroll.followAudio
    LaunchedEffect(following, currentItemIndex) {
        if (!following || currentItemIndex < 0) return@LaunchedEffect
        try {
            // Park the sung verse about a third of the way down the viewport
            // so the eye has the previous line above and the next below.
            val offset = -(listState.layoutInfo.viewportSize.height * 0.3f).toInt()
            listState.animateScrollToItem(currentItemIndex, offset)
        } catch (e: CancellationException) {
            // User grabbed the list mid-animation -> same touch-pause /
            // auto-resume behaviour as the constant-speed mode.
            if (!currentCoroutineContext().isActive) throw e
            autoScroll.touchInterrupt()
        }
    }

    // Keep the screen awake while reading if the user wants it, and
    // always while auto-scroll is running (a dimming screen mid-path is
    // the #1 complaint for this kind of app).
    val view = LocalView.current
    val wantScreenOn = keepScreenOnPref || autoScroll.active
    DisposableEffect(view, wantScreenOn) {
        view.keepScreenOn = wantScreenOn
        onDispose { view.keepScreenOn = false }
    }

    val audioRaw: Int = remember(chalisa.audio) { app.rawIdForAudio(chalisa.audio) }
    val isThisChalisaPlaying = playerState.mediaId == chalisa.id && playerState.isPlaying

    // Long-pressed verse + the section heading it belongs to (for the share sheet).
    var shareTarget by remember { mutableStateOf<Pair<Verse, String?>?>(null) }
    val shareSheetState = rememberModalBottomSheetState()
    var showSettings by remember { mutableStateOf(false) }
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Any modal sheet on top of the text pauses auto-scroll; it picks
    // back up when the sheet goes away (if it was running before).
    val sheetOpen = shareTarget != null || showSettings
    var resumeAfterSheet by remember { mutableStateOf(false) }
    LaunchedEffect(sheetOpen) {
        if (sheetOpen) {
            resumeAfterSheet = autoScroll.active && (!autoScroll.paused || autoScroll.isTouchPaused)
            if (resumeAfterSheet) autoScroll.pause()
        } else if (resumeAfterSheet) {
            resumeAfterSheet = false
            autoScroll.resume()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = bottomOverlay),
            )
        },
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
                    FilledIconButton(
                        onClick = { autoScroll.toggle() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (autoScroll.active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (autoScroll.active) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardDoubleArrowDown,
                            contentDescription = if (autoScroll.active) "Stop auto-scroll" else "Start auto-scroll",
                        )
                    }
                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FormatSize,
                            contentDescription = "Reading settings",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                // Leave room for the mini-player and the auto-scroll pill
                // so the closing doha is never hidden behind them.
                contentPadding = PaddingValues(
                    bottom = 32.dp + bottomOverlay + if (autoScroll.active) AUTO_SCROLL_BAR_SPACE else 0.dp,
                ),
                verticalArrangement = Arrangement.Top,
            ) {
                itemsIndexed(
                    items = items,
                    key = { _, item -> item.stableKey },
                ) { index, item ->
                    when (item) {
                        is ReaderItem.Heading -> SectionHeading(heading = item.text)
                        is ReaderItem.VerseItem -> VerseRow(
                            verse = item.verse,
                            fontScale = fontScale,
                            lineSpacing = lineSpacing,
                            highlighted = index == currentItemIndex,
                            // Tap a verse to jump the audio to it (only when
                            // this chalisa's audio is already loaded, so a
                            // stray tap never starts playback unasked).
                            onClick = item.verse.startMs
                                ?.takeIf { audioLoadedHere }
                                ?.let { ms -> { app.player.seekToMs(ms) } },
                            onLongPress = {
                                val heading = chalisa.sections.getOrNull(item.sectionIndex)?.heading
                                shareTarget = item.verse to heading
                            },
                        )
                    }
                }
            }
            AutoScrollBar(
                state = autoScroll,
                level = autoScrollLevel,
                onLevelChange = { level -> scope.launch { prefs.setAutoScrollLevel(level) } },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp + bottomOverlay),
            )
        }
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = settingsSheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            ReaderSettingsSheet(
                fontScale = fontScale,
                onFontScaleChange = { scope.launch { prefs.setFontScale(it) } },
                lineSpacing = lineSpacing,
                onLineSpacingChange = { scope.launch { prefs.setLineSpacing(it) } },
                themePref = themePref,
                onThemeChange = { scope.launch { prefs.setThemePref(it) } },
                keepScreenOn = keepScreenOnPref,
                onKeepScreenOnChange = { scope.launch { prefs.setKeepScreenOn(it) } },
            )
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

/** Vertical room reserved under the list while the auto-scroll pill is showing. */
private val AUTO_SCROLL_BAR_SPACE = 80.dp

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
