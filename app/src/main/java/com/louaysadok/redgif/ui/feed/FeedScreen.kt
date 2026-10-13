@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.louaysadok.redgif.ui.feed

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import coil.request.ImageRequest
import com.louaysadok.redgif.data.local.FavoritesRepository
import com.louaysadok.redgif.data.media.Prefetcher
import com.louaysadok.redgif.data.media.Players
import com.louaysadok.redgif.data.media.isUnmeteredNetwork
import com.louaysadok.redgif.data.media.playbackPolicy
import com.louaysadok.redgif.data.model.RedMedia
import com.louaysadok.redgif.data.net.MediaKind
import com.louaysadok.redgif.data.net.Order
import com.louaysadok.redgif.data.settings.AppSettings
import com.louaysadok.redgif.ui.common.CenteredMessage
import com.louaysadok.redgif.ui.common.ErrorMessage
import com.louaysadok.redgif.ui.common.appContainer
import com.louaysadok.redgif.ui.common.shareLink
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

private val BROWSE_ORDERS = listOf(Order.TRENDING, Order.LATEST, Order.TOP7, Order.TOP28, Order.TOP)

/**
 * A scrolling media feed. Used for browse, search results and a creator's profile.
 *
 * Autoplay uses ONE shared ExoPlayer. Only the card closest to the centre of the screen plays,
 * so scrolling never spins up a decoder per card. The next clip is prefetched into the disk cache.
 */
@Composable
fun FeedScreen(
    base: FeedSource,
    title: String,
    onMenu: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    onSearch: (() -> Unit)? = null,
    onOpenViewer: (List<RedMedia>, Int) -> Unit,
    onOpenCreator: (String) -> Unit,
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val scope = rememberCoroutineScope()

    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val s: AppSettings = settings ?: AppSettings()

    val browse = base.kind == FeedKind.BROWSE
    var order by remember(base.key) { mutableStateOf(base.order) }
    var mediaKind by remember(base.key) { mutableStateOf(base.mediaKind) }
    val source = if (browse) base.copy(order = order, mediaKind = mediaKind) else base.copy(mediaKind = mediaKind)

    val vm: FeedViewModel = viewModel(
        key = source.key,
        factory = viewModelFactory {
            initializer { FeedViewModel(container.repository, container.favorites, source) }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val favoriteUids by vm.favoriteUids.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()

    val player: ExoPlayer = remember { Players.create(context, container.okHttp) }
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    val unmetered = remember(s.autoplay, s.quality) { context.isUnmeteredNetwork() }
    val policy = remember(s, unmetered) { playbackPolicy(s, unmetered) }

    val itemCount = state.items.size
    val activeIndex by remember(itemCount) {
        derivedStateOf { activeIndexOf(listState, itemCount) }
    }
    val active: RedMedia? = state.items.getOrNull(activeIndex)

    // Point the shared player at the active clip. Stop when the active card is an image.
    LaunchedEffect(active?.id, policy.autoplay, policy.preferHd) {
        val media = active
        if (media == null || !media.isVideo || !policy.autoplay) {
            player.stop()
            player.clearMediaItems()
            return@LaunchedEffect
        }
        val url = media.playbackUrl(policy.preferHd) ?: return@LaunchedEffect
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    LaunchedEffect(s.muted) {
        player.volume = if (s.muted) 0f else 1f
    }

    // Warm up the cache for the next few cards: thumbnails always, the next clip only on allowed networks.
    LaunchedEffect(activeIndex, policy.autoplay) {
        if (activeIndex < 0) return@LaunchedEffect
        val upcoming = vm.state.value.items.drop(activeIndex + 1).take(3)
        upcoming.forEachIndexed { i, media ->
            media.displayUrl?.let { url ->
                container.imageLoader.enqueue(ImageRequest.Builder(context).data(url).build())
            }
            if (i == 0 && media.isVideo && policy.autoplay) {
                media.playbackUrl(policy.preferHd)?.let { Prefetcher.prefetch(context, container.okHttp, it) }
            }
        }
    }

    // Infinite scroll: request the next page when the user nears the end of what is loaded.
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= vm.state.value.items.size - 12) vm.loadNext()
            }
    }

    val isCreatorFavorite = source.kind == FeedKind.CREATOR &&
        FavoritesRepository.creatorUid(source.creator) in favoriteUids

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    } else if (onMenu != null) {
                        IconButton(onClick = onMenu) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                },
                actions = {
                    if (onSearch != null) {
                        IconButton(onClick = onSearch) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    }
                    if (source.kind == FeedKind.CREATOR) {
                        IconButton(onClick = {
                            scope.launch { container.favorites.toggleCreator(source.creator) }
                        }) {
                            Icon(
                                imageVector = if (isCreatorFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Favorite creator",
                            )
                        }
                    }
                    IconButton(onClick = {
                        scope.launch { container.settings.update { it.copy(muted = !it.muted) } }
                    }) {
                        Icon(
                            imageVector = if (s.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                            contentDescription = if (s.muted) "Unmute previews" else "Mute previews",
                        )
                    }
                    FeedMenu(
                        mediaKind = mediaKind,
                        onMediaKind = { mediaKind = it },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner),
        ) {
            if (browse) {
                val selected = BROWSE_ORDERS.indexOf(order).coerceAtLeast(0)
                PrimaryScrollableTabRow(
                    selectedTabIndex = selected,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    edgePadding = 0.dp,
                ) {
                    BROWSE_ORDERS.forEachIndexed { index, option ->
                        Tab(
                            selected = index == selected,
                            onClick = { order = option },
                            selectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            text = { Text(option.label) },
                        )
                    }
                }
            }

            PullToRefreshBox(
                isRefreshing = state.loading && state.items.isEmpty(),
                onRefresh = { vm.refresh() },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    state.items.isEmpty() && state.loading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }

                    state.items.isEmpty() && state.error != null -> ErrorMessage(
                        message = state.error.orEmpty(),
                        onRetry = { vm.refresh() },
                    )

                    state.items.isEmpty() -> CenteredMessage(
                        if (source.kind == FeedKind.SEARCH) "No results for \"${source.query}\"" else "Nothing to show here yet",
                    )

                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 6.dp),
                    ) {
                        itemsIndexed(
                            items = state.items,
                            key = { _, media -> media.id },
                            contentType = { _, _ -> "media" },
                        ) { index, media ->
                            MediaCard(
                                media = media,
                                activePlayer = if (index == activeIndex && policy.autoplay && media.isVideo) player else null,
                                isFavorite = FavoritesRepository.mediaUid(media.id) in favoriteUids,
                                onOpen = { onOpenViewer(state.items, index) },
                                onCreatorClick = { onOpenCreator(media.creator) },
                                onFavoriteClick = { vm.toggleMediaFavorite(media) },
                                onShare = { context.shareLink(media.pageUrl) },
                            )
                        }
                        item(key = "feed-footer") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    state.loading -> CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 3.dp,
                                    )
                                    state.error != null -> TextButton(onClick = { vm.loadNext() }) {
                                        Text("Couldn't load more. Tap to retry", maxLines = 1)
                                    }
                                    state.exhausted -> Text(
                                        "You've reached the end",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    else -> Spacer(Modifier.size(1.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedMenu(
    mediaKind: MediaKind,
    onMediaKind: (MediaKind) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "More")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MediaKind.entries.forEach { kind ->
                DropdownMenuItem(
                    text = { Text(kind.label) },
                    leadingIcon = {
                        if (kind == mediaKind) Icon(Icons.Filled.Check, contentDescription = null)
                    },
                    onClick = {
                        onMediaKind(kind)
                        open = false
                    },
                )
            }
        }
    }
}

/**
 * Index of the card whose centre is closest to the viewport centre. Only items that are
 * real media (index below [count]) are considered; the footer row is ignored.
 */
private fun activeIndexOf(listState: LazyListState, count: Int): Int {
    val info: LazyListLayoutInfo = listState.layoutInfo
    val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
    var best = -1
    var bestDistance = Int.MAX_VALUE
    for (item: LazyListItemInfo in info.visibleItemsInfo) {
        if (item.index >= count) continue
        val center = item.offset + item.size / 2
        val distance = abs(center - viewportCenter)
        if (distance < bestDistance) {
            bestDistance = distance
            best = item.index
        }
    }
    return best
}
