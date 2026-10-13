package com.louaysadok.redgif.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "favorites")
data class FavoriteEntity(
    /** "media:<id>" or "creator:<name>". */
    @PrimaryKey val uid: String,
    /** FavoriteType name. */
    val type: String,
    /** Creator name (for both kinds). */
    val title: String,
    /** JSON-encoded RedMedia for MEDIA favorites, null for CREATOR favorites. */
    val payload: String?,
    val addedAt: Long,
)

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE type = :type ORDER BY addedAt DESC")
    fun observeByType(type: String): Flow<List<FavoriteEntity>>

    @Query("SELECT uid FROM favorites")
    fun observeUids(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM favorites WHERE uid = :uid")
    suspend fun count(uid: String): Int

    @Upsert
    suspend fun upsert(entity: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE uid = :uid")
    suspend fun delete(uid: String)
}

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val text: String,
    val usedAt: Long,
)

@Dao
interface RecentSearchDao {
    @Query("SELECT * FROM recent_searches ORDER BY usedAt DESC LIMIT 15")
    fun observe(): Flow<List<RecentSearchEntity>>

    @Upsert
    suspend fun upsert(entity: RecentSearchEntity)

    @Query("DELETE FROM recent_searches WHERE text = :text")
    suspend fun delete(text: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clear()
}

@Database(
    entities = [FavoriteEntity::class, RecentSearchEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favorites(): FavoriteDao
    abstract fun recentSearches(): RecentSearchDao
}
