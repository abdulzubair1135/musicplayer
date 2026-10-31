package com.zmusic.app.data.provider

import com.zmusic.app.data.api.ApiSong
import com.zmusic.app.data.api.ZMusicApiService
import com.zmusic.app.data.model.Song
import com.zmusic.app.data.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext

/**
 * Converts a backend [ApiSong] to the app's [Song] model.
 * The stream URL is built from the videoId so Media3 can play it directly.
 */
fun ApiSong.toSong(): Song {
    val streamUrl = ZMusicApiService.streamUrl(videoId)
    // Use a stable numeric ID derived from videoId hash so Room FK works
    val stableId = (videoId.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL) + 800_000L
    return Song(
        id         = stableId,
        title      = title,
        artist     = artist,
        album      = album,
        duration   = duration,
        contentUri = streamUrl,
        streamUri  = streamUrl,
        artworkUri = thumbnail,
        genre      = genre,
        sourceType = SourceType.ONLINE,
        videoId    = videoId,
    )
}

/**
 * Live implementation of [MusicProvider] backed by the Z-Music backend
 * (Node.js / Express on Render).
 */
class YouTubeApiMusicProvider : MusicProvider {
    override val providerName: String = "Z-Music YouTube Provider"
    override val isOnline: Boolean    = true

    private val api = ZMusicApiService.getInstance()

    override fun getSongs(): Flow<List<Song>> = flow {
        runCatching { api.discover().body()?.results?.map { it.toSong() } ?: emptyList() }
            .onSuccess { emit(it) }
            .onFailure { emit(emptyList()) }
    }

    override suspend fun getSongById(id: Long): Song? = null // not used for online

    override fun searchSongs(query: String): Flow<List<Song>> {
        if (query.isBlank()) return flowOf(emptyList())
        return flow {
            runCatching { api.search(query).body()?.results?.map { it.toSong() } ?: emptyList() }
                .onSuccess { emit(it) }
                .onFailure { emit(emptyList()) }
        }
    }

    override suspend fun getDiscoverSection(): List<Song> = withContext(Dispatchers.IO) {
        runCatching { api.discover().body()?.results?.map { it.toSong() } ?: emptyList() }
            .getOrDefault(emptyList())
    }

    override suspend fun getTrending(): List<Song> = withContext(Dispatchers.IO) {
        runCatching { api.trending().body()?.results?.map { it.toSong() } ?: emptyList() }
            .getOrDefault(emptyList())
    }

    override suspend fun getRadioForSong(seedSong: Song): List<Song> = withContext(Dispatchers.IO) {
        runCatching {
            api.radio(
                videoId = seedSong.videoId,
                title   = seedSong.title,
                artist  = seedSong.artist,
            ).body()?.results?.map { it.toSong() } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    /** Direct search returning raw list (used by SearchScreen) */
    suspend fun searchDirect(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching { api.search(query).body()?.results?.map { it.toSong() } ?: emptyList() }
            .getOrDefault(emptyList())
    }
}
