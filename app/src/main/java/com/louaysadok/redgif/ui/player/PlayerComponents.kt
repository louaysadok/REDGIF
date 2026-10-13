@file:OptIn(UnstableApi::class)

package com.louaysadok.redgif.ui.player

import android.view.TextureView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage

/**
 * Renders an ExoPlayer into a TextureView. TextureView is used instead of SurfaceView because
 * SurfaceView punches a hole through the window and flickers while a list scrolls.
 */
@Composable
fun VideoSurface(player: ExoPlayer, modifier: Modifier = Modifier) {
    key(player) {
        AndroidView(
            modifier = modifier,
            factory = { context -> TextureView(context).also { player.setVideoTextureView(it) } },
            onRelease = { view -> player.clearVideoTextureView(view) },
        )
    }
}

/**
 * Video layer with the poster held on top until the first frame of the current clip is drawn.
 * This prevents the previous clip's last frame from flashing when the active card changes.
 */
@Composable
fun PlayerLayer(
    player: ExoPlayer,
    poster: String?,
    mediaKey: Any?,
    modifier: Modifier = Modifier,
) {
    val firstFrame by rememberFirstFrame(player, mediaKey)
    Box(modifier) {
        VideoSurface(player, Modifier.fillMaxSize())
        if (!firstFrame) {
            AsyncImage(
                model = poster,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun rememberFirstFrame(player: Player, mediaKey: Any?): State<Boolean> {
    val state = remember(player, mediaKey) { mutableStateOf(false) }
    DisposableEffect(player, mediaKey) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                state.value = true
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    return state
}

@Composable
fun rememberIsPlaying(player: Player): State<Boolean> {
    val state = remember(player) { mutableStateOf(player.isPlaying) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                state.value = isPlaying
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    return state
}
