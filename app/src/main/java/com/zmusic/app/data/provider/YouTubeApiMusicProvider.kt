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
            val backendHits = runCatching { api.search(query).body()?.results?.map { it.toSong() } ?: emptyList() }
                .getOrDefault(emptyList())
            if (backendHits.isNotEmpty()) {
                emit(backendHits)
            } else {
                emit(YouTubeDirectSearch.searchDirect(query))
            }
        }
    }

    override suspend fun getDiscoverSection(): List<Song> = getDiscoverSection(false)

    suspend fun getDiscoverSection(allowLong: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        val backendHits = runCatching { api.discover(allowLong = allowLong).body()?.results?.map { it.toSong() } ?: emptyList() }
            .getOrDefault(emptyList())
        if (backendHits.isNotEmpty()) {
            backendHits
        } else {
            YouTubeDirectSearch.searchDirect("Trending Hindi Songs Official", allowLong)
        }
    }

    override suspend fun getTrending(): List<Song> = getTrending(false)

    suspend fun getTrending(allowLong: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        val backendHits = runCatching { api.trending(allowLong = allowLong).body()?.results?.map { it.toSong() } ?: emptyList() }
            .getOrDefault(emptyList())
        if (backendHits.isNotEmpty()) {
            backendHits
        } else {
            YouTubeDirectSearch.searchDirect("Top Hit Songs Official Audio", allowLong)
        }
    }

    override suspend fun getRadioForSong(seedSong: Song): List<Song> = getRadioForSong(seedSong, false)

    suspend fun getRadioForSong(seedSong: Song, allowLong: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        val radioHits = runCatching {
            api.radio(
                videoId = seedSong.videoId,
                title   = seedSong.title,
                artist  = seedSong.artist,
                allowLong = allowLong
            ).body()?.results?.map { it.toSong() } ?: emptyList()
        }.getOrDefault(emptyList())

        if (radioHits.isNotEmpty()) {
            radioHits
        } else {
            val q = if (seedSong.artist.isNotBlank() && seedSong.artist != "YouTube") {
                "${seedSong.artist} official songs"
            } else {
                "${seedSong.title} official song"
            }
            YouTubeDirectSearch.searchDirect(q, allowLong)
        }
    }

    /** Direct search returning raw list (used by SearchScreen) with pagination & resilient fallback */
    suspend fun searchDirect(query: String, page: Int = 1, allowLong: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val backendResults = runCatching { 
            api.search(query = query, page = page, allowLong = allowLong).body()?.results?.map { it.toSong() } ?: emptyList() 
        }.getOrDefault(emptyList())

        if (backendResults.isNotEmpty()) {
            backendResults
        } else {
            // Direct YouTube fallback ensures Search NEVER fails!
            val fallbackQuery = if (page > 1) "$query track $page" else query
            YouTubeDirectSearch.searchDirect(query = fallbackQuery, allowLong = allowLong)
        }
    }
}
