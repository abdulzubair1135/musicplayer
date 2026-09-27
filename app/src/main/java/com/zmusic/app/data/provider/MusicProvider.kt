package com.zmusic.app.data.provider

import com.zmusic.app.data.model.Song
import com.zmusic.app.data.model.SourceType
import com.zmusic.app.database.dao.SongDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

interface MusicProvider {
    val providerName: String
    val isOnline: Boolean

    fun getSongs(): Flow<List<Song>>
    suspend fun getSongById(id: Long): Song?
    fun searchSongs(query: String): Flow<List<Song>>
    suspend fun getDiscoverSection(): List<Song>
    suspend fun getTrending(): List<Song>
    suspend fun getRadioForSong(seedSong: Song): List<Song>
}

class LocalMusicProvider(
    private val songDao: SongDao
) : MusicProvider {
    override val providerName: String = "Local Storage"
    override val isOnline: Boolean = false

    override fun getSongs(): Flow<List<Song>> = songDao.getAllSongs()

    override suspend fun getSongById(id: Long): Song? = songDao.getSongById(id)

    override fun searchSongs(query: String): Flow<List<Song>> = songDao.searchSongs(query)

    override suspend fun getDiscoverSection(): List<Song> = emptyList()

    override suspend fun getTrending(): List<Song> = emptyList()

    override suspend fun getRadioForSong(seedSong: Song): List<Song> = emptyList()
}

class OnlineMusicProvider : MusicProvider {
    override val providerName: String = "Z-Music Online Provider"
    override val isOnline: Boolean = true

    // Open licensed / public catalog online tracks for real streaming discovery
    private val onlineCatalog = listOf(
        Song(
            id = 900001L,
            title = "Midnight Horizon",
            artist = "Aether",
            album = "Chillhop Essentials",
            duration = 184000L,
            contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            streamUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            artworkUri = "https://picsum.photos/seed/aether/400/400",
            genre = "Lo-Fi / Chill",
            sourceType = SourceType.ONLINE
        ),
        Song(
            id = 900002L,
            title = "Neon Dreams",
            artist = "Synthwave Boy",
            album = "Retro City",
            duration = 212000L,
            contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            streamUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            artworkUri = "https://picsum.photos/seed/synthwave/400/400",
            genre = "Electronic",
            sourceType = SourceType.ONLINE
        ),
        Song(
            id = 900003L,
            title = "Acoustic Sunset",
            artist = "Elena Rostova",
            album = "Unplugged Sessions",
            duration = 195000L,
            contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            streamUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            artworkUri = "https://picsum.photos/seed/elena/400/400",
            genre = "Acoustic / Folk",
            sourceType = SourceType.ONLINE
        ),
        Song(
            id = 900004L,
            title = "Starlight Symphony",
            artist = "Orchestral Groove",
            album = "Cosmic Journey",
            duration = 245000L,
            contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            streamUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            artworkUri = "https://picsum.photos/seed/starlight/400/400",
            genre = "Ambient",
            sourceType = SourceType.ONLINE
        ),
        Song(
            id = 900005L,
            title = "Urban Rhythm",
            artist = "Beatmaster",
            album = "Street Tape Vol 1",
            duration = 178000L,
            contentUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            streamUri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            artworkUri = "https://picsum.photos/seed/urban/400/400",
            genre = "Hip-Hop",
            sourceType = SourceType.ONLINE
        )
    )

    override fun getSongs(): Flow<List<Song>> = flowOf(onlineCatalog)

    override suspend fun getSongById(id: Long): Song? = withContext(Dispatchers.IO) {
        onlineCatalog.find { it.id == id }
    }

    override fun searchSongs(query: String): Flow<List<Song>> {
        if (query.isBlank()) return flowOf(emptyList())
        val q = query.trim().lowercase()
        val filtered = onlineCatalog.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.album.lowercase().contains(q) ||
            it.genre.lowercase().contains(q)
        }
        return flowOf(filtered)
    }

    override suspend fun getDiscoverSection(): List<Song> = withContext(Dispatchers.IO) {
        onlineCatalog.take(4)
    }

    override suspend fun getTrending(): List<Song> = withContext(Dispatchers.IO) {
        onlineCatalog.shuffled()
    }

    override suspend fun getRadioForSong(seedSong: Song): List<Song> = withContext(Dispatchers.IO) {
        val radioQueue = mutableListOf<Song>()
        radioQueue.add(seedSong)
        radioQueue.addAll(onlineCatalog.filter { it.id != seedSong.id })
        radioQueue
    }
}
