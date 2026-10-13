package com.louaysadok.redgif.data.net

import com.louaysadok.redgif.data.model.RedMedia
import kotlinx.serialization.Serializable

/*
 * Wire-format DTOs for the RedGIFs JSON API (https://api.redgifs.com).
 * Shapes follow the public endpoints used by the RedGIFs web app. Every field has a default
 * and the client sets ignoreUnknownKeys + coerceInputValues, so a missing or null field
 * degrades gracefully instead of crashing the feed.
 */

@Serializable
data class TempTokenDto(
    val token: String = "",
    /** User-Agent the token is bound to. Requests must send the same value. */
    val agent: String? = null,
)

@Serializable
data class UrlsDto(
    val hd: String? = null,
    val sd: String? = null,
    val poster: String? = null,
    val thumbnail: String? = null,
    val vthumbnail: String? = null,
)

@Serializable
data class GifDto(
    val id: String = "",
    val userName: String = "",
    val createDate: Long = 0,
    val width: Int = 0,
    val height: Int = 0,
    val likes: Int = 0,
    val views: Int = 0,
    val hasAudio: Boolean = false,
    val tags: List<String> = emptyList(),
    val urls: UrlsDto = UrlsDto(),
)

@Serializable
data class SearchResponseDto(
    val page: Int = 1,
    val pages: Int = 1,
    val total: Int = 0,
    val gifs: List<GifDto> = emptyList(),
)

fun GifDto.toDomain(): RedMedia = RedMedia(
    id = id,
    creator = userName,
    createdAt = createDate,
    width = width,
    height = height,
    likes = likes,
    views = views,
    hasAudio = hasAudio,
    tags = tags,
    hdUrl = urls.hd,
    sdUrl = urls.sd,
    posterUrl = urls.poster,
    thumbUrl = urls.thumbnail ?: urls.vthumbnail,
)
