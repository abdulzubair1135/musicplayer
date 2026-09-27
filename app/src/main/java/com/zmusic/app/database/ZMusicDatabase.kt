package com.zmusic.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zmusic.app.data.model.Favorite
import com.zmusic.app.data.model.HistoryEntry
import com.zmusic.app.data.model.Playlist
import com.zmusic.app.data.model.PlaylistSongCrossRef
import com.zmusic.app.data.model.Song
import com.zmusic.app.database.dao.FavoriteDao
import com.zmusic.app.database.dao.HistoryDao
import com.zmusic.app.database.dao.PlaylistDao
import com.zmusic.app.database.dao.SongDao

@Database(
    entities = [
        Song::class,
        Playlist::class,
        PlaylistSongCrossRef::class,
        Favorite::class,
        HistoryEntry::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ZMusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: ZMusicDatabase? = null

        fun getInstance(context: Context): ZMusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZMusicDatabase::class.java,
                    "zmusic_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
