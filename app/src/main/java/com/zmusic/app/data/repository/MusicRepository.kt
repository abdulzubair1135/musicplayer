package com.zmusic.app.data.repository

import android.content.Context
import com.zmusic.app.data.model.Favorite
import com.zmusic.app.data.model.HistoryEntry
import com.zmusic.app.data.model.Playlist
import com.zmusic.app.data.model.PlaylistSongCrossRef
import com.zmusic.app.data.model.Song
import com.zmusic.app.data.model.SourceType
import com.zmusic.app.data.provider.LocalMusicProvider
import com.zmusic.app.data.provider.OnlineMusicProvider
import com.zmusic.app.data.scanner.LocalMusicScanner
import com.zmusic.app.database.ZMusicDatabase
import com.zmusic.app.util.NetworkObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class MusicRepository(context: Context) {
    private val db = ZMusicDatabase.getInstance(context)
    private val songDao = db.songDao()
    private val playlistDao = db.playlistDao()
    private val favoriteDao = db.favoriteDao()
    private val historyDao = db.historyDao()
    private val scanner = LocalMusicScanner(context)
    private val recommendationEngine = RecommendationEngine()
    private val networkObserver = NetworkObserver(context)

    val localProvider = LocalMusicProvider(songDao)
    val onlineProvider = OnlineMusicProvider()

    val isNetworkAvailable: Flow<Boolean> = networkObserver.isConnected

    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()
    fun getSongCount(): Flow<Int> = songDao.getSongCount()

    fun searchLocalSongs(query: String): Flow<List<Song>> = songDao.searchSongs(query)
    fun searchOnlineSongs(query: String): Flow<List<Song>> = onlineProvider.searchSongs(query)

    suspend fun scanMusic(): Int = scanner.scanLocalMusic()

    fun getAllArtists(): Flow<List<String>> = songDao.getAllArtists()
    fun getSongsByArtist(artist: String): Flow<List<Song>> = songDao.getSongsByArtist(artist)

    fun getAllAlbums(): Flow<List<String>> = songDao.getAllAlbums()
    fun getSongsByAlbum(album: String): Flow<List<Song>> = songDao.getSongsByAlbum(album)

    fun getAllGenres(): Flow<List<String>> = songDao.getAllGenres()
    fun getSongsByGenre(genre: String): Flow<List<Song>> = songDao.getSongsByGenre(genre)

    suspend fun getOnlineDiscover(): List<Song> = onlineProvider.getDiscoverSection()
    suspend fun getOnlineTrending(): List<Song> = onlineProvider.getTrending()

    // Playlists
    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> = playlistDao.getSongsForPlaylist(playlistId)

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(Playlist(name = name))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        playlistDao.renamePlaylist(playlistId, newName)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.clearPlaylistSongs(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val currentCount = playlistDao.getPlaylistSongCount(playlistId)
        playlistDao.addSongToPlaylist(
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId, orderIndex = currentCount)
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    // Favorites
    fun getFavoriteSongs(): Flow<List<Song>> = favoriteDao.getFavoriteSongs()
    fun isFavorite(songId: Long): Flow<Boolean> = favoriteDao.isFavorite(songId)

    suspend fun toggleFavorite(song: Song) {
        val isFav = favoriteDao.isFavoriteDirect(song.id)
        if (isFav) {
            favoriteDao.removeFavorite(song.id)
        } else {
            // Ensure song exists in local DB so foreign keys resolve if favoriting
            if (song.sourceType == SourceType.ONLINE) {
                songDao.insertSongs(listOf(song))
            }
            favoriteDao.addFavorite(Favorite(songId = song.id))
        }
    }

    // History
    fun getRecentlyPlayed(): Flow<List<Song>> = historyDao.getRecentlyPlayedSongs()

    suspend fun recordPlayHistory(song: Song) {
        if (song.sourceType == SourceType.ONLINE) {
            songDao.insertSongs(listOf(song))
        }
        historyDao.insertHistory(HistoryEntry(songId = song.id))
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    // Radio
    suspend fun getSongRadioQueue(seedSong: Song, isOnlineAvailable: Boolean): List<Song> {
        if (seedSong.sourceType == SourceType.ONLINE && isOnlineAvailable) {
            return onlineProvider.getRadioForSong(seedSong)
        }
        val allLocal = songDao.getAllSongs().first()
        val recentIds = historyDao.getRecentSongIds()
        return recommendationEngine.generateSongRadio(seedSong, allLocal, recentIds)
    }
}
