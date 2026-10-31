package com.zmusic.app.data.organizer

import com.zmusic.app.data.model.Song

enum class MusicCategory(val title: String, val iconEmoji: String) {
    PHONK("Phonk & Bass", "⚡"),
    BOLLYWOOD("Bollywood/Hindi", "🇮🇳"),
    ENGLISH("English & Pop", "🌍"),
    CALL_RECORDINGS("Call Records", "📞"),
    DOWNLOADED("Downloads", "📥"),
    ALL_MUSIC("All Songs", "🎵")
}

data class OrganizedLibrary(
    val totalTracks: Int,
    val musicTracks: Int,
    val quarantinedCount: Int,
    val phonkTracks: List<Song>,
    val bollywoodTracks: List<Song>,
    val englishTracks: List<Song>,
    val callRecordings: List<Song>,
    val downloadedTracks: List<Song>,
    val allCleanMusic: List<Song>
)

object LocalMusicOrganizer {

    private val PHONK_KEYWORDS = listOf(
        "phonk", "montagem", "funk", "brazilian", "slowed", "reverb", "bass",
        "drift", "sigma", "gym phonk", "sombra", "estelar", "dama"
    )

    private val CALL_RECORD_KEYWORDS = listOf(
        "call recording", "call_", "emergency_", "sound recordings",
        "voice", "recordings/emergencyrecorder", "audiorecord", "call recording"
    )

    private val BOLLYWOOD_KEYWORDS = listOf(
        "arijit", "singh", "kabir", "bekhayali", "rahman", "mishra", "shreya",
        "pritam", "sonu nigam", "terebina", "pyaar", "humsafar", "saiyaara",
        "tum ho toh", "tu hi hai", "aashiqui", "abhi mujh", "agneepath",
        "bollywood", "hindi", "remix", "neha", "badshah", "jubin", "armaan",
        "atif", "amaran", "anirudh", "dhanush"
    )

    fun isCallOrVoiceRecording(song: Song): Boolean {
        val pathLower = song.contentUri.lowercase()
        val titleLower = song.title.lowercase()
        return CALL_RECORD_KEYWORDS.someMatch(pathLower, titleLower) ||
                pathLower.contains("call%20recording") ||
                pathLower.contains("call") ||
                pathLower.contains("sound%20recordings") ||
                pathLower.contains("emergencyrecorder") ||
                titleLower.startsWith("call_") ||
                titleLower.startsWith("emergency_") ||
                titleLower.matches(Regex("^[0-9_.-]{10,}$")) // raw timestamp audio files
    }

    fun isPhonk(song: Song): Boolean {
        val text = "${song.title} ${song.artist} ${song.album} ${song.contentUri}".lowercase()
        return PHONK_KEYWORDS.someMatch(text)
    }

    fun isBollywood(song: Song): Boolean {
        val text = "${song.title} ${song.artist} ${song.album}".lowercase()
        return BOLLYWOOD_KEYWORDS.someMatch(text)
    }

    fun organize(songs: List<Song>): OrganizedLibrary {
        val calls = mutableListOf<Song>()
        val phonk = mutableListOf<Song>()
        val bollywood = mutableListOf<Song>()
        val english = mutableListOf<Song>()
        val downloads = mutableListOf<Song>()
        val cleanMusic = mutableListOf<Song>()

        for (song in songs) {
            if (isCallOrVoiceRecording(song)) {
                calls.add(song)
                continue // Quarantine from normal music
            }

            cleanMusic.add(song)

            if (song.sourceType == com.zmusic.app.data.model.SourceType.DOWNLOADED) {
                downloads.add(song)
            }

            if (isPhonk(song)) {
                phonk.add(song)
            } else if (isBollywood(song)) {
                bollywood.add(song)
            } else {
                english.add(song)
            }
        }

        return OrganizedLibrary(
            totalTracks = songs.size,
            musicTracks = cleanMusic.size,
            quarantinedCount = calls.size,
            phonkTracks = phonk,
            bollywoodTracks = bollywood,
            englishTracks = english,
            callRecordings = calls,
            downloadedTracks = downloads,
            allCleanMusic = cleanMusic
        )
    }

    private fun List<String>.someMatch(vararg texts: String): Boolean {
        return any { kw -> texts.any { it.contains(kw) } }
    }
}
