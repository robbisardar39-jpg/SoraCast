package com.robbi.soracast.home

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.mvvm.Resource

private val FrostedGlassFill = Color(0xB8181B24)
private val FrostedGlassBorder = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))

@Composable
fun FloatingDockSoraCastScreen(
    controller: UnifiedHomeController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shelves by controller.visibleShelves.collectAsState()
    val loadState by controller.loadState.collectAsState()
    val providerCounts by controller.providerCounts.collectAsState()
    val statusMessage by controller.statusMessage.collectAsState()
    val selectedFilter by controller.selectedCategory.collectAsState()

    LaunchedEffect(Unit) {
        controller.bootstrapAndLoad(context)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 64.dp)
    ) {
        if (shelves.isNotEmpty()) {
            val preferredHeroShelf = if (selectedFilter.equals("All", ignoreCase = true)) {
                shelves.firstOrNull { it.bucket == ShelfBucket.MOVIES } ?: shelves.firstOrNull()
            } else {
                shelves.firstOrNull()
            }
            val heroItem = preferredHeroShelf?.items?.firstOrNull()
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 0.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (heroItem != null) {
                        item(key = "hero:" + heroItem.url) {
                            FeaturedHeroBanner(
                                item = heroItem,
                                sourceLabel = preferredHeroShelf?.providerNames?.firstOrNull().orEmpty(),
                                onOpenItem = { openSearchResponse(context, heroItem) }
                            )
                        }
                    } else {
                        item(key = "top_spacer") {
                            Spacer(modifier = Modifier.height(76.dp))
                        }
                    }

                    items(shelves, key = { it.bucket.name + ":" + it.title }) { shelf ->
                        Box(modifier = Modifier.padding(horizontal = 12.dp)) {
                            ShelfRow(
                                shelf = shelf,
                                onItemClick = { item -> openSearchResponse(context, item) }
                            )
                        }
                    }
                }
            }
        } else if (loadState is Resource.Failure) {
            val failure = loadState as Resource.Failure
            Surface(
                color = FrostedGlassFill,
                border = FrostedGlassBorder,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = failure.errorString,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = { controller.bootstrapAndLoad(context, forceReinstall = false) }
                    ) {
                        Text("Retry Sync")
                    }
                }
            }
        }

        TopCategoryBar(
            selectedFilter = selectedFilter,
            onSelectFilter = { filter -> controller.setCategoryFilter(filter) },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(3f)
                .statusBarsPadding()
                .padding(top = 2.dp, start = 12.dp, end = 12.dp)
                .wrapContentSize()
        )
    }
}

@Composable
private fun FeaturedHeroBanner(
    item: SearchResponse,
    sourceLabel: String,
    onOpenItem: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(380.dp)
    ) {
        AsyncImage(
            model = item.posterUrl,
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.62f),
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(top = 68.dp, start = 16.dp, end = 16.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (sourceLabel.isNotBlank()) {
                Text(
                    text = sourceLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onOpenItem) {
                    Text("▶ Play")
                }
                OutlinedButton(onClick = onOpenItem) {
                    Text("ⓘ Info")
                }
            }
        }
    }
}

@Composable
private fun TopCategoryBar(
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("All", "Movies", "Anime", "Live")
    Surface(
        modifier = modifier.wrapContentSize(),
        shape = CircleShape,
        color = FrostedGlassFill,
        border = FrostedGlassBorder,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = selectedFilter.equals(option, ignoreCase = true)
                Surface(
                    onClick = { onSelectFilter(option) },
                    shape = CircleShape,
                    color = if (isSelected) Color.White.copy(alpha = 0.22f) else Color.Transparent,
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                        Text(text = option, maxLines = 1, style = MaterialTheme.typography.labelMedium, color = if (isSelected) Color.White else Color.White.copy(alpha = 0.82f))
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingBottomDock(
    isLoading: Boolean,
    selectedCategory: String,
    statusText: String,
    onSyncAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.wrapContentSize(),
        shape = CircleShape,
        color = FrostedGlassFill,
        border = FrostedGlassBorder,
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            }
            Text(
                text = "$selectedCategory • $statusText",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
            IconButton(
                onClick = onSyncAll,
                modifier = Modifier.size(34.dp)
            ) {
                Text(
                    "↻",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun ShelfRow(
    shelf: AggregatedShelf,
    onItemClick: (SearchResponse) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = shelf.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (shelf.providerNames.isNotEmpty()) {
                Text(
                    text = shelf.providerNames.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(shelf.items, key = { item -> item.url.ifBlank { "${item.apiName}:${item.id ?: item.name}" } }) { item ->
                Card(
                    onClick = { onItemClick(item) },
                    modifier = Modifier.width(124.dp)
                ) {
                    Column {
                        AsyncImage(
                            model = item.posterUrl,
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .size(width = 124.dp, height = 180.dp)
                        )
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberNb01CompactDockState(
    firstVisibleIndex: Int,
    firstVisibleOffset: Int
): Pair<Boolean, () -> Unit> {
    var isCompact by remember { mutableStateOf(false) }
    var lastY by remember { mutableIntStateOf(0) }
    val thresholdPx = 28

    LaunchedEffect(firstVisibleIndex, firstVisibleOffset) {
        val currentY = firstVisibleIndex * 420 + firstVisibleOffset
        if (currentY > lastY + thresholdPx && currentY > 90) {
            isCompact = true
        } else if (currentY < lastY - thresholdPx) {
            isCompact = false
        }
        lastY = currentY

        delay(600L)
        isCompact = false
    }

    val expand = { isCompact = false }
    return isCompact to expand
}
