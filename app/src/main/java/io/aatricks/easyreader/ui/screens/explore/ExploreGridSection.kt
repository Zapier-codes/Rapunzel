package io.aatricks.easyreader.ui.screens.explore

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.aatricks.easyreader.data.model.ExploreItem
import io.aatricks.easyreader.ui.components.ErrorTile
import io.aatricks.easyreader.ui.theme.RapunzelSpacing
import io.aatricks.easyreader.ui.viewmodel.ExploreViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@Composable
internal fun ExploreGrid(
    gridState: LazyGridState,
    modifier: Modifier = Modifier,
    uiState: ExploreViewModel.ExploreUiState,
    hasActiveFilters: Boolean,
    onItemSelect: (ExploreItem) -> Unit,
    onLoadMore: () -> Unit,
    onClearFilters: () -> Unit,
    onRetryFailedSource: (String) -> Unit
): Unit {
    Box(modifier = modifier.fillMaxWidth()) {
        LazyVerticalGrid(
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Adaptive(minSize = 156.dp),
            contentPadding = PaddingValues(bottom = RapunzelSpacing.sm),
            horizontalArrangement = Arrangement.spacedBy(RapunzelSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(RapunzelSpacing.md)
        ) {
            if (uiState.searchFailures.isNotEmpty()) {
                items(uiState.searchFailures, span = { GridItemSpan(maxLineSpan) }) { failure ->
                    ErrorTile(
                        message = "${failure.sourceName} is unavailable" +
                            (failure.reason?.let { ": $it" } ?: ""),
                        onRetry = { onRetryFailedSource(failure.sourceName) }
                    )
                }
            }
            when {
                uiState.isLoading && uiState.items.isEmpty() -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SkeletonFeaturedExploreCard()
                    }
                    items(8) { SkeletonExploreCard() }
                }

                uiState.items.isEmpty() -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        if (uiState.hasError || uiState.searchFailures.isNotEmpty()) {
                            ErrorTile(
                                message = "Offline or failed to fetch results. Check your connection.",
                                onRetry = { onRetryFailedSource("") },
                                modifier = Modifier.padding(top = RapunzelSpacing.xxl)
                            )
                        } else {
                            EmptyExploreState(
                                query = uiState.searchQuery,
                                hasActiveFilters = hasActiveFilters,
                                onClearFilters = onClearFilters
                            )
                        }
                    }
                }

                else -> {
                    val featuredItem = uiState.items.first()
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        FeaturedExploreCard(
                            item = featuredItem,
                            onClick = { onItemSelect(featuredItem) }
                        )
                    }

                    items(uiState.items.drop(1), key = { it.url }) { item ->
                        ExploreItemCard(
                            item = item,
                            onClick = { onItemSelect(item) }
                        )
                    }

                    if (uiState.isLoading) {
                        items(4) { SkeletonExploreCard() }
                    } else if (!uiState.canLoadMore && uiState.items.size > 1) {
                        item(span = { GridItemSpan(maxLineSpan) }) { EndOfResultsMarker() }
                    } else {
                        item(span = { GridItemSpan(maxLineSpan) }) { Spacer(modifier = Modifier.height(1.dp)) }
                    }
                }
            }
        }

        LaunchedEffect(gridState, uiState.items.size, uiState.isLoading, uiState.canLoadMore) {
            snapshotFlow {
                val layoutInfo = gridState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                if (totalItems == 0) {
                    false
                } else {
                    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    val shouldLoadMore = uiState.canLoadMore && !uiState.isLoading && uiState.items.isNotEmpty() && lastVisible >= totalItems - 4
                    shouldLoadMore
                }
            }
                .distinctUntilChanged()
                .filter { it }
                .collectLatest { onLoadMore() }
        }
    }
}

@Composable
private fun EmptyExploreState(
    query: String,
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit
): Unit {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = RapunzelSpacing.xxl),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = RapunzelSpacing.lg, vertical = RapunzelSpacing.xl),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(RapunzelSpacing.xs)
        ) {
            Text(
                text = if (query.isNotBlank()) "No matches for \"$query\"" else "Nothing to show yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (hasActiveFilters) {
                    "Try another source or clear your filters to broaden the results."
                } else {
                    "Pull results from another source or try a different search."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (hasActiveFilters) {
                Spacer(modifier = Modifier.height(RapunzelSpacing.xxs))
                androidx.compose.material3.OutlinedButton(onClick = onClearFilters) {
                    Text("Clear filters")
                }
            }
        }
    }
}

@Composable
private fun FeaturedExploreCard(
    item: ExploreItem,
    onClick: () -> Unit
): Unit {
    val imageRequest = rememberExploreImageRequest(item)

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(RapunzelSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(RapunzelSpacing.lg)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(RapunzelSpacing.xs)) {
                    Text(
                        text = "Popular on ${item.source}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = supportingLine(item),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(RapunzelSpacing.xs)) {
                    MetaPill(text = item.source)
                    if (item.chapterCount > 0) {
                        MetaPill(text = "${item.chapterCount} ch")
                    }
                }
            }

            AsyncImage(
                model = imageRequest,
                contentDescription = item.title,
                modifier = Modifier
                    .width(128.dp)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun ExploreItemCard(
    item: ExploreItem,
    onClick: () -> Unit
): Unit {
    val imageRequest = rememberExploreImageRequest(item)

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.78f)
            ) {
                AsyncImage(
                    model = imageRequest,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = RapunzelSpacing.lg, topEnd = RapunzelSpacing.lg)),
                    contentScale = ContentScale.Crop
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(RapunzelSpacing.xs),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = item.source,
                        modifier = Modifier.padding(horizontal = RapunzelSpacing.xs, vertical = RapunzelSpacing.xxs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RapunzelSpacing.sm, vertical = RapunzelSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(RapunzelSpacing.xxs)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = supportingLine(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EndOfResultsMarker(): Unit {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = RapunzelSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(RapunzelSpacing.sm)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
        Text(
            text = "End of results",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
internal fun MetaPill(text: String): Unit {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = RapunzelSpacing.xs, vertical = RapunzelSpacing.xxs),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SkeletonExploreCard(): Unit {
    val infiniteTransition = rememberInfiniteTransition(label = "skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(950),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.78f)
            )
            Column(
                modifier = Modifier.padding(horizontal = RapunzelSpacing.sm, vertical = RapunzelSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(RapunzelSpacing.xs)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
                )
            }
        }
    }
}

@Composable
private fun SkeletonFeaturedExploreCard(): Unit {
    val infiniteTransition = rememberInfiniteTransition(label = "featured_skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(950),
            repeatMode = RepeatMode.Reverse
        ),
        label = "featured_alpha"
    )

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(RapunzelSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(RapunzelSpacing.lg)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(RapunzelSpacing.xs)) {
                    Box(
                        modifier = Modifier
                            .width(92.dp)
                            .height(14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(18.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f))
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(RapunzelSpacing.xs)) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .width(72.dp)
                                .height(28.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(128.dp)
                    .fillMaxHeight()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
internal fun rememberExploreImageRequest(item: ExploreItem): ImageRequest {
    val context = LocalContext.current

    return remember(item.coverUrl, item.url, item.source) {
        val uri = try {
            java.net.URI(item.url)
        } catch (_: Exception) {
            null
        }

        var referer = if (uri != null) "${uri.scheme}://${uri.host}/" else item.url
        if (item.source == "MangaBat" || referer.contains("mangabat")) {
            referer = "https://www.mangabats.com/"
        } else if (referer.contains("manganato")) {
            referer = "https://manganato.com/"
        }

        ImageRequest.Builder(context)
            .data(item.coverUrl)
            .httpHeaders(NetworkHeaders.Builder().set("Referer", referer).build())
            .crossfade(true)
            .build()
    }
}

private fun supportingLine(item: ExploreItem): String {
    return when {
        item.chapterCount > 0 -> "${item.chapterCount} chapters"
        !item.author.isNullOrBlank() -> item.author
        !item.rating.isNullOrBlank() -> "Rating ${item.rating}"
        else -> "Open details"
    }
}
