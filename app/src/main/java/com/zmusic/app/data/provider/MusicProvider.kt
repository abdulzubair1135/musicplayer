package com.zmusic.app.data.provider

import com.zmusic.app.data.model.Song
import kotlinx.coroutines.flow.Flow

interface MusicProvider {
    val providerName: String
    val isOnline: Boolean

    fun getSongs(): Flow<List<Song>>
    suspend fun getSongById(id: Long): Song?
    fun searchSongs(query: String): Flow<List<Song>>
}

class LocalMusicProvider(
    private val songDao: com.zmusic.app.database.dao.SongDao
) : MusicProvider {
    override val providerName: String = "Local Storage"
    override val isOnline: Boolean = false

    override fun getSongs(): Flow<List<Song>> = songDao.getAllSongs()

    override suspend fun getSongById(id: Long): Song? = songDao.getSongById(id)

    override fun searchSongs(query: String): Flow<List<Song>> = songDao.searchSongs(query)
}

class OnlineMusicProvider : MusicProvider {
    override val providerName: String = "Online Provider (Disabled in V1)"
    override val isOnline: Boolean = true

    override fun getSongs(): Flow<List<Song>> = kotlinx.coroutines.flow.flowOf(emptyList())

    override suspend fun getSongById(id: Long): Song? = null

    override fun searchSongs(query: String): Flow<List<Song>> = kotlinx.coroutines.flow.flowOf(emptyList())
}
