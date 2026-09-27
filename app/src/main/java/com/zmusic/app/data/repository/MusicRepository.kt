package com.zmusic.app.data.repository

import android.content.Context
import com.zmusic.app.data.model.Favorite
import com.zmusic.app.data.model.HistoryEntry
import com.zmusic.app.data.model.Playlist
import com.zmusic.app.data.model.PlaylistSongCrossRef
import com.zmusic.app.data.model.Song
import com.zmusic.app.data.scanner.LocalMusicScanner
import com.zmusic.app.database.ZMusicDatabase
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

    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()
    fun getSongCount(): Flow<Int> = songDao.getSongCount()
    fun searchSongs(query: String): Flow<List<Song>> = songDao.searchSongs(query)

    suspend fun scanMusic(): Int = scanner.scanLocalMusic()

    fun getAllArtists(): Flow<List<String>> = songDao.getAllArtists()
    fun getSongsByArtist(artist: String): Flow<List<Song>> = songDao.getSongsByArtist(artist)

    fun getAllAlbums(): Flow<List<String>> = songDao.getAllAlbums()
    fun getSongsByAlbum(album: String): Flow<List<Song>> = songDao.getSongsByAlbum(album)

    fun getAllGenres(): Flow<List<String>> = songDao.getAllGenres()
    fun getSongsByGenre(genre: String): Flow<List<Song>> = songDao.getSongsByGenre(genre)

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

    suspend fun toggleFavorite(songId: Long) {
        val isFav = favoriteDao.isFavoriteDirect(songId)
        if (isFav) {
            favoriteDao.removeFavorite(songId)
        } else {
            favoriteDao.addFavorite(Favorite(songId = songId))
        }
    }

    // History
    fun getRecentlyPlayed(): Flow<List<Song>> = historyDao.getRecentlyPlayedSongs()

    suspend fun recordPlayHistory(songId: Long) {
        historyDao.insertHistory(HistoryEntry(songId = songId))
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    // Song Radio Recommendation
    suspend fun getSongRadioQueue(seedSong: Song): List<Song> {
        val allSongs = songDao.getAllSongs().first()
        val recentIds = historyDao.getRecentSongIds()
        return recommendationEngine.generateSongRadio(seedSong, allSongs, recentIds)
    }
}
