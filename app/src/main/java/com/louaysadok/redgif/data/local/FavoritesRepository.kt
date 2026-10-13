package com.louaysadok.redgif.data.local

import com.louaysadok.redgif.data.model.RedMedia
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

enum class FavoriteType { MEDIA, CREATOR }

data class Favorite(
    val uid: String,
    val type: FavoriteType,
    /** Creator name for both kinds. */
    val title: String,
    /** Full media for MEDIA favorites so they can open in the viewer offline. */
    val media: RedMedia?,
)

/** Local favorites (creators and media), kept on the device in Room. */
class FavoritesRepository(
    private val db: AppDatabase,
    private val json: Json,
) {
    fun observe(type: FavoriteType): Flow<List<Favorite>> =
        db.favorites().observeByType(type.name).map { rows -> rows.map { it.toFavorite() } }

    fun observeUids(): Flow<Set<String>> =
        db.favorites().observeUids().map { it.toSet() }

    suspend fun toggleMedia(media: RedMedia) {
        val dao = db.favorites()
        val uid = mediaUid(media.id)
        if (dao.count(uid) > 0) {
            dao.delete(uid)
        } else {
            dao.upsert(
                FavoriteEntity(
                    uid = uid,
                    type = FavoriteType.MEDIA.name,
                    title = media.creator,
                    payload = json.encodeToString(RedMedia.serializer(), media),
                    addedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun toggleCreator(name: String) {
        val dao = db.favorites()
        val uid = creatorUid(name)
        if (dao.count(uid) > 0) {
            dao.delete(uid)
        } else {
            dao.upsert(
                FavoriteEntity(
                    uid = uid,
                    type = FavoriteType.CREATOR.name,
                    title = name,
                    payload = null,
                    addedAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    suspend fun remove(uid: String) = db.favorites().delete(uid)

    private fun FavoriteEntity.toFavorite(): Favorite = Favorite(
        uid = uid,
        type = FavoriteType.valueOf(type),
        title = title,
        media = payload?.let { runCatching { json.decodeFromString(RedMedia.serializer(), it) }.getOrNull() },
    )

    companion object {
        fun mediaUid(id: String) = "media:$id"
        fun creatorUid(name: String) = "creator:${name.trim().lowercase()}"
    }
}
