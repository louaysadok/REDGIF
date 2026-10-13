@file:OptIn(UnstableApi::class)

package com.louaysadok.redgif.data.media

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.louaysadok.redgif.data.model.RedMedia
import com.louaysadok.redgif.data.settings.AppSettings
import com.louaysadok.redgif.data.settings.AutoplayMode
import com.louaysadok.redgif.data.settings.VideoQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File

/**
 * Process-wide on-disk video cache. Clips played once (or prefetched) are served from disk
 * on replay and on scroll-back, which removes most re-buffering.
 */
object MediaCache {
    private const val MAX_BYTES = 384L * 1024L * 1024L

    @Volatile
    private var cache: SimpleCache? = null

    fun simpleCache(context: Context): SimpleCache =
        cache ?: synchronized(this) {
            cache ?: SimpleCache(
                File(context.cacheDir, "video"),
                LeastRecentlyUsedCacheEvictor(MAX_BYTES),
                StandaloneDatabaseProvider(context),
            ).also { cache = it }
        }

    fun sourceFactory(context: Context, http: OkHttpClient): CacheDataSource.Factory {
        val upstream = OkHttpDataSource.Factory(http)
            .setDefaultRequestProperties(mapOf("Referer" to "https://www.redgifs.com/"))
        return CacheDataSource.Factory()
            .setCache(simpleCache(context))
            .setUpstreamDataSourceFactory(upstream)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun clear(context: Context) {
        val c = simpleCache(context)
        c.keys.toList().forEach { c.removeResource(it) }
    }
}

object Players {
    /**
     * One ExoPlayer per screen. Tuned for fast start: a small minimum buffer so playback begins
     * quickly, and a larger ceiling so networks with bursty throughput do not stall.
     */
    fun create(context: Context, http: OkHttpClient): ExoPlayer {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 2_500,
                /* maxBufferMs = */ 25_000,
                /* bufferForPlaybackMs = */ 400,
                /* bufferForPlaybackAfterRebufferMs = */ 1_200,
            )
            .build()
        return ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(MediaCache.sourceFactory(context, http)))
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0f
            }
    }
}

object Prefetcher {
    /** Caches the first [bytes] of a clip so it starts instantly when it scrolls into view. */
    suspend fun prefetch(context: Context, http: OkHttpClient, url: String, bytes: Long = 2L * 1024L * 1024L) {
        withContext(Dispatchers.IO) {
            runCatching {
                val source = MediaCache.sourceFactory(context, http).createDataSource()
                CacheWriter(source, DataSpec(Uri.parse(url), 0L, bytes), null, null).cache()
            }
        }
    }
}

/** Hand-off between the feed and the full-screen viewer. */
object ViewerQueue {
    @Volatile
    private var items: List<RedMedia> = emptyList()

    fun set(list: List<RedMedia>) {
        items = list
    }

    fun get(): List<RedMedia> = items
}

data class PlaybackPolicy(
    val autoplay: Boolean,
    val preferHd: Boolean,
)

fun playbackPolicy(settings: AppSettings, unmetered: Boolean): PlaybackPolicy = PlaybackPolicy(
    autoplay = when (settings.autoplay) {
        AutoplayMode.ALWAYS -> true
        AutoplayMode.WIFI_ONLY -> unmetered
        AutoplayMode.OFF -> false
    },
    preferHd = when (settings.quality) {
        VideoQuality.HD -> true
        VideoQuality.SD -> false
        VideoQuality.AUTO -> unmetered
    },
)

/** True when the active network is not metered (Wi-Fi or Ethernet). */
fun Context.isUnmeteredNetwork(): Boolean {
    val cm = getSystemService(ConnectivityManager::class.java) ?: return false
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
}
