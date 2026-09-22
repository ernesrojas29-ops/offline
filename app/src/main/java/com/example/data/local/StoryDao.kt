package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object para la entidad StoryEntity.
 *
 * Todas las operaciones de modificación son suspend functions.
 * Las consultas reactivas devuelven Flow<T>.
 */
@Dao
interface StoryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStory(story: StoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStories(stories: List<StoryEntity>): List<Long>

    @Query("SELECT * FROM stories ORDER BY savedAt DESC")
    fun getAllStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE category = :category ORDER BY savedAt DESC")
    fun getStoriesByCategory(category: String): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE isFavorite = 1 ORDER BY savedAt DESC")
    fun getFavoriteStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE isRead = 0 ORDER BY savedAt DESC")
    fun getUnreadStories(): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%') ORDER BY savedAt DESC")
    fun searchStories(query: String): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE category = :category AND (title LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%') ORDER BY savedAt DESC")
    fun searchStoriesByCategory(category: String, query: String): Flow<List<StoryEntity>>

    @Query("SELECT * FROM stories WHERE id = :id LIMIT 1")
    fun getStoryById(id: String): Flow<StoryEntity?>

    @Query("SELECT DISTINCT category FROM stories ORDER BY category ASC")
    fun getSavedCategories(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM stories")
    fun getTotalStoriesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM stories")
    suspend fun getCountDirect(): Int

    @Query("SELECT COUNT(*) FROM stories WHERE category = :category")
    fun getCountByCategory(category: String): Flow<Int>

    @Query("UPDATE stories SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean)

    @Query("UPDATE stories SET isRead = :isRead WHERE id = :id")
    suspend fun setRead(id: String, isRead: Boolean)

    @Query("DELETE FROM stories WHERE id = :id")
    suspend fun deleteStory(id: String)

    @Query("DELETE FROM stories")
    suspend fun deleteAllStories()

    @Transaction
    suspend fun replaceAllStories(stories: List<StoryEntity>) {
        deleteAllStories()
        insertStories(stories)
    }
}
