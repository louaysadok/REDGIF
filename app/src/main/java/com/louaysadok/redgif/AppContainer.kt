package com.louaysadok.redgif

import android.app.Application
import androidx.room.Room
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.louaysadok.redgif.data.local.AppDatabase
import com.louaysadok.redgif.data.local.FavoritesRepository
import com.louaysadok.redgif.data.net.RedGifsClient
import com.louaysadok.redgif.data.net.RedGifsRepository
import com.louaysadok.redgif.data.settings.SettingsStore
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Hand-rolled dependency container. Everything is created once per process. */
class AppContainer(app: Application) {

    val okHttp: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(8, 5, TimeUnit.MINUTES))
        .build()

    val client = RedGifsClient(okHttp)

    val repository = RedGifsRepository(client)

    val database: AppDatabase = Room.databaseBuilder(app, AppDatabase::class.java, "redgif.db")
        .fallbackToDestructiveMigration()
        .build()

    val favorites = FavoritesRepository(database, client.json)

    val settings = SettingsStore(app)

    /** Thumbnails and posters: large memory cache plus a 256 MB disk cache. */
    val imageLoader: ImageLoader = ImageLoader.Builder(app)
        .memoryCache { MemoryCache.Builder(app).maxSizePercent(0.25).build() }
        .diskCache {
            DiskCache.Builder()
                .directory(app.cacheDir.resolve("images"))
                .maxSizeBytes(256L * 1024L * 1024L)
                .build()
        }
        .crossfade(true)
        .build()
}
