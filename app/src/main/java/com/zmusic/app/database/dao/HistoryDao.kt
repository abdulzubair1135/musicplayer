package com.zmusic.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.zmusic.app.data.model.HistoryEntry
import com.zmusic.app.data.model.Song
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(entry: HistoryEntry)

    @Query("""
        SELECT DISTINCT s.* FROM songs s
        INNER JOIN history h ON s.id = h.songId
        ORDER BY h.playedAt DESC
        LIMIT :limit
    """)
    fun getRecentlyPlayedSongs(limit: Int = 30): Flow<List<Song>>

    @Query("SELECT songId FROM history ORDER BY playedAt DESC LIMIT 100")
    suspend fun getRecentSongIds(): List<Long>

    @Query("DELETE FROM history")
    suspend fun clearHistory()
}
