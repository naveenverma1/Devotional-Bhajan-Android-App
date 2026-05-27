package com.nv.user.sunderkand.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nv.user.sunderkand.R
import com.nv.user.sunderkand.SunderkandApp
import com.nv.user.sunderkand.data.model.Chalisa
import com.nv.user.sunderkand.data.prefs.SankalpState
import com.nv.user.sunderkand.ui.components.ContinueReadingCard
import com.nv.user.sunderkand.ui.components.SankalpCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenChalisa: (chalisaId: String) -> Unit,
    onOpenAbout: () -> Unit,
    onOpenSankalp: () -> Unit,
) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as SunderkandApp
    val chalisas: List<Chalisa> = remember { app.content.allChalisas() }
    val sankalpState by app.sankalp.state.collectAsStateWithLifecycle(initialValue = SankalpState())
    val lastOpenedId by app.readerPrefs.lastOpenedChalisa.collectAsStateWithLifecycle(initialValue = null)
    val lastOpenedChalisa: Chalisa? = remember(lastOpenedId, chalisas) {
        lastOpenedId?.let { id -> chalisas.firstOrNull { it.id == id } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "सुन्दर पाठ",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSankalp) {
                        Icon(
                            imageVector = Icons.Filled.Bookmark,
                            contentDescription = "Sankalp",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    IconButton(onClick = onOpenAbout) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = "About",
                            tint = MaterialTheme.colorScheme.onSurface,
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
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                HomeHero()
            }
            lastOpenedChalisa?.let { chalisa ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ContinueReadingCard(
                        chalisaTitle = chalisa.title,
                        verseLabel = "${chalisa.verseCount} छंद",
                        onClick = { onOpenChalisa(chalisa.id) },
                    )
                }
            }
            if (sankalpState.isActive) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SankalpCard(
                        state = sankalpState,
                        chalisaTitle = sankalpState.chalisaId?.let { app.content.chalisaById(it)?.title },
                        onClick = onOpenSankalp,
                    )
                }
            }
            items(chalisas, key = { it.id }) { chalisa ->
                ChalisaCard(
                    chalisa = chalisa,
                    onClick = { onOpenChalisa(chalisa.id) },
                )
            }
        }
    }
}

// LazyVerticalGrid item-scoped helpers
@Composable
private fun HomeHero(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.hero_hanuman),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun ChalisaCard(
    chalisa: Chalisa,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = chalisa.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${chalisa.verseCount} छंद",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (chalisa.audio != null) {
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = "Has audio",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .height(28.dp),
                )
            }
        }
    }
}
