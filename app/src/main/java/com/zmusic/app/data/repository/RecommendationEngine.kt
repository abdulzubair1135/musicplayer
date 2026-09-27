package com.zmusic.app.data.repository

import com.zmusic.app.data.model.Song
import com.zmusic.app.data.model.SourceType

class RecommendationEngine {

    /**
     * Generates a list of recommended songs (Song Radio queue) based on a seed song.
     * Evaluates similarity scores:
     * - Same artist: +50 pts
     * - Same album: +30 pts
     * - Same genre: +20 pts
     * - Same language: +15 pts
     * - History boost: +10 pts
     */
    fun generateSongRadio(
        seedSong: Song,
        allSongs: List<Song>,
        recentHistoryIds: List<Long> = emptyList()
    ): List<Song> {
        val candidateSongs = allSongs.filter { it.id != seedSong.id }

        val scoredSongs = candidateSongs.map { song ->
            var score = 0
            if (song.artist.equals(seedSong.artist, ignoreCase = true)) {
                score += 50
            }
            if (song.album.equals(seedSong.album, ignoreCase = true)) {
                score += 30
            }
            if (song.genre.equals(seedSong.genre, ignoreCase = true)) {
                score += 20
            }
            if (song.language.equals(seedSong.language, ignoreCase = true)) {
                score += 15
            }
            if (recentHistoryIds.contains(song.id)) {
                score += 10
            }
            val hashBoost = (song.id xor seedSong.id).hashCode() % 5
            score += kotlin.math.abs(hashBoost)

            song to score
        }

        val radioList = mutableListOf<Song>()
        radioList.add(seedSong)
        radioList.addAll(scoredSongs.sortedByDescending { it.second }.map { it.first })
        return radioList
    }

    /**
     * Fallback strategy when internet is lost during Online Radio playback.
     */
    fun fallbackOnlineRadioToLocal(
        onlineSeed: Song,
        localSongs: List<Song>
    ): List<Song> {
        if (localSongs.isEmpty()) return emptyList()
        return generateSongRadio(onlineSeed, localSongs)
    }
}
