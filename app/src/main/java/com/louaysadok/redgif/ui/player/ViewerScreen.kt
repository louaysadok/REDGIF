@file:OptIn(ExperimentalFoundationApi::class, UnstableApi::class)

package com.louaysadok.redgif.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.louaysadok.redgif.data.local.FavoritesRepository
import com.louaysadok.redgif.data.media.Prefetcher
import com.louaysadok.redgif.data.media.Players
import com.louaysadok.redgif.data.media.ViewerQueue
import com.louaysadok.redgif.data.media.isUnmeteredNetwork
import com.louaysadok.redgif.data.media.playbackPolicy
import com.louaysadok.redgif.data.model.RedMedia
import com.louaysadok.redgif.data.settings.AppSettings
import com.louaysadok.redgif.ui.common.CenteredMessage
import com.louaysadok.redgif.ui.common.appContainer
import com.louaysadok.redgif.ui.common.formatCount
import com.louaysadok.redgif.ui.common.openInBrowser
import com.louaysadok.redgif.ui.common.shareLink
import kotlinx.coroutines.launch

/**
 * Full-screen, swipe-up/down viewer, like Infinity's video view.
 * The media list comes from [ViewerQueue], set by whichever list opened the viewer.
 */
@Composable
fun ViewerScreen(
    startIndex: Int,
    onBack: () -> Unit,
    onOpenCreator: (String) -> Unit,
) {
    val items = remember { ViewerQueue.get() }
    BackHandler(onBack = onBack)
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            CenteredMessage("Nothing to play. Open a clip from a list first.")
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.BottomCenter)) {
                Text("Back")
            }
        }
        return
    }
    ViewerContent(
        items = items,
        startIndex = startIndex.coerceIn(0, items.lastIndex),
        onBack = onBack,
        onOpenCreator = onOpenCreator,
    )
}

@Composable
private fun ViewerContent(
    items: List<RedMedia>,
    startIndex: Int,
    onBack: () -> Unit,
    onOpenCreator: (String) -> Unit,
) {
    val context = LocalContext.current
    val container = context.appContainer()
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val s: AppSettings = settings ?: AppSettings()
    val favoriteUids by remember { container.favorites.observeUids() }
        .collectAsStateWithLifecycle(initialValue = emptySet())

    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val unmetered = remember(s.quality) { context.isUnmeteredNetwork() }
    val policy = remember(s, unmetered) { playbackPolicy(s, unmetered) }

    val pagerState = rememberPagerState(initialPage = startIndex) { items.size }
    val currentPage = pagerState.currentPage
    val current = items[currentPage]

    val player: ExoPlayer = remember { Players.create(context, container.okHttp) }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    val isPlaying by rememberIsPlaying(player)

    // The viewer always plays the page the user is on, regardless of the feed's autoplay setting.
    LaunchedEffect(current.id, policy.preferHd) {
        if (current.isVideo) {
            current.playbackUrl(policy.preferHd)?.let { url ->
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
                player.playWhenReady = true
            }
        } else {
            player.stop()
            player.clearMediaItems()
        }
    }

    LaunchedEffect(s.muted) {
        player.volume = if (s.muted) 0f else 1f
    }

    // Prefetch the next clip, and warm thumbnails for the two after it.
    LaunchedEffect(currentPage) {
        items.getOrNull(currentPage + 1)
            ?.takeIf { it.isVideo }
            ?.playbackUrl(policy.preferHd)
            ?.let { Prefetcher.prefetch(context, container.okHttp, it) }
        items.drop(currentPage + 1).take(2).forEach { media ->
            media.displayUrl?.let { url ->
                container.imageLoader.enqueue(ImageRequest.Builder(context).data(url).build())
            }
        }
    }

    val favorited = FavoritesRepository.mediaUid(current.id) in favoriteUids

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { items[it].id },
        ) { page ->
            val media = items[page]
            val isActive = page == currentPage
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(page, isActive) {
                        detectTapGestures(onTap = {
                            if (isActive && media.isVideo) player.playWhenReady = !player.playWhenReady
                        })
                    },
            ) {
                if (isActive && media.isVideo) {
                    PlayerLayer(
                        player = player,
                        poster = media.posterUrl ?: media.thumbUrl,
                        mediaKey = media.id,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    AsyncImage(
                        model = media.displayUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (isActive && media.isVideo && !isPlaying) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                            .background(Color.Black.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape)
                            .padding(12.dp),
                    )
                }
            }
        }

        // Top bar: back, sound, save, share, open on the website.
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.65f), Color.Transparent)))
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = {
                scope.launch { container.settings.update { it.copy(muted = !it.muted) } }
            }) {
                Icon(
                    imageVector = if (s.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = if (s.muted) "Unmute" else "Mute",
                    tint = Color.White,
                )
            }
            IconButton(onClick = { scope.launch { container.favorites.toggleMedia(current) } }) {
                Icon(
                    imageVector = if (favorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = "Favorite",
                    tint = Color.White,
                )
            }
            IconButton(onClick = { context.shareLink(current.pageUrl) }) {
                Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
            }
            IconButton(onClick = { context.openInBrowser(current.pageUrl) }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open on RedGIFs", tint = Color.White)
            }
        }

        // Bottom info: creator and counters.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))))
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                text = "@${current.creator}",
                color = Color(0xFFFF8A80),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onOpenCreator(current.creator) },
            )
            Spacer(Modifier.size(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ThumbUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(formatCount(current.likes), color = Color.White)
                Spacer(Modifier.width(14.dp))
                Icon(Icons.Filled.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(formatCount(current.views), color = Color.White)
                Spacer(Modifier.weight(1f))
                Text("${currentPage + 1} / ${items.size}", color = Color.White)
            }
        }
    }
}
