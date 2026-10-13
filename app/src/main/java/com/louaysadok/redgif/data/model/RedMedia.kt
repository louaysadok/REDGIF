package com.louaysadok.redgif.data.model

import kotlinx.serialization.Serializable

/**
 * App-facing media item. One type covers both GIF/video clips and still images.
 * Serializable so favorites can be stored locally without another round trip.
 */
@Serializable
data class RedMedia(
    val id: String,
    val creator: String,
    val createdAt: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val likes: Int = 0,
    val views: Int = 0,
    val hasAudio: Boolean = false,
    val tags: List<String> = emptyList(),
    val hdUrl: String? = null,
    val sdUrl: String? = null,
    val posterUrl: String? = null,
    val thumbUrl: String? = null,
) {
    /** True when the primary media file is an MP4/WebM clip rather than a still image. */
    val isVideo: Boolean
        get() {
            val url = (hdUrl ?: sdUrl ?: return false).substringBefore('?').lowercase()
            return url.endsWith(".mp4") || url.endsWith(".webm")
        }

    /** Video URL for the requested quality. Falls back to the other quality when one is missing. */
    fun playbackUrl(preferHd: Boolean): String? = if (preferHd) hdUrl ?: sdUrl else sdUrl ?: hdUrl

    /** Image shown while loading, and the still frame for images. */
    val displayUrl: String?
        get() = if (isVideo) posterUrl ?: thumbUrl else hdUrl ?: sdUrl ?: thumbUrl

    /** Card aspect ratio, clamped so very tall or very wide media stays readable. */
    val aspectRatio: Float
        get() = if (width > 0 && height > 0) (width.toFloat() / height.toFloat()).coerceIn(0.5f, 1.91f) else 16f / 9f

    val pageUrl: String
        get() = "https://www.redgifs.com/watch/$id"
}
