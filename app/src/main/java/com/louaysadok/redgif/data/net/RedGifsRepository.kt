package com.louaysadok.redgif.data.net

import com.louaysadok.redgif.data.model.RedMedia
import java.net.URLEncoder

/** Sort orders offered by the RedGIFs search endpoints. */
enum class Order(val api: String, val label: String) {
    TRENDING("trending", "Trending"),
    LATEST("latest", "Latest"),
    TOP7("top7", "Top 7 days"),
    TOP28("top28", "Top 28 days"),
    TOP("top", "Top all time"),
}

/** Media type filter. "g" = GIF/video clips, "i" = still images. */
enum class MediaKind(val api: String, val label: String) {
    GIF("g", "GIFs & videos"),
    IMAGE("i", "Images"),
}

data class Page(
    val items: List<RedMedia>,
    val page: Int,
    val pages: Int,
)

class RedGifsRepository(private val client: RedGifsClient) {

    /** Trending, latest, top, etc. across all creators. */
    suspend fun browse(order: Order, kind: MediaKind, page: Int): Page =
        search(query = null, order = order, kind = kind, page = page)

    /** Text search across tags and titles. */
    suspend fun search(
        query: String?,
        order: Order,
        kind: MediaKind,
        page: Int,
        count: Int = PAGE_SIZE,
    ): Page {
        val params = linkedMapOf<String, String>()
        if (!query.isNullOrBlank()) params["search_text"] = query.trim()
        params["order"] = order.api
        params["type"] = kind.api
        params["count"] = count.toString()
        params["page"] = page.toString()
        return parsePage(client.get("/v2/gifs/search", params))
    }

    /** Every item published by one creator. */
    suspend fun creator(
        username: String,
        order: Order,
        kind: MediaKind,
        page: Int,
        count: Int = PAGE_SIZE,
    ): Page {
        val name = URLEncoder.encode(username.trim().lowercase(), "UTF-8").replace("+", "%20")
        val params = mapOf(
            "order" to order.api,
            "type" to kind.api,
            "count" to count.toString(),
            "page" to page.toString(),
        )
        return parsePage(client.get("/v2/users/$name/search", params))
    }

    private fun parsePage(body: String): Page {
        val dto = client.json.decodeFromString(SearchResponseDto.serializer(), body)
        val items = dto.gifs
            .filter { it.id.isNotBlank() }
            .map { it.toDomain() }
        return Page(items = items, page = dto.page, pages = dto.pages)
    }

    companion object {
        const val PAGE_SIZE = 40
    }
}
