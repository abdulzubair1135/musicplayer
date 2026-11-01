package com.zmusic.app.data.provider

import android.util.Log
import com.zmusic.app.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Smart Vibe Autoplay Engine.
 * Ensures that when a user plays ANY song, the player automatically
 * creates an infinite radio queue of the exact same vibe and genre
 * (Phonk, Sad Vibe, Romantic Bollywood, Punjabi Hits, Lo-Fi, etc.),
 * and continuously appends more matching songs so the music NEVER stops.
 */
object VibeAutoplayHelper {
    private const val TAG = "VibeAutoplayHelper"

    fun getVibeQueries(song: Song): List<String> {
        val titleLower = song.title.lowercase()
        val artistLower = song.artist.lowercase()
        val text = "$titleLower $artistLower ${song.genre.lowercase()}"

        return when {
            // ── Phonk & Drift ──────────────────────────────────────────────────
            text.contains("phonk") || text.contains("drift") || text.contains("sigma") || text.contains("brazilian") -> {
                listOf(
                    "brazilian phonk track audio",
                    "drift phonk audio track",
                    "phonk music official audio",
                    "phonk slowed reverb official"
                )
            }

            // ── Sad & Heartbreak Vibes ─────────────────────────────────────────
            text.contains("sad") || text.contains("dard") || text.contains("broken") ||
            text.contains("alone") || text.contains("heartbreak") || text.contains("khairiyat") ||
            text.contains("channa mereya") || text.contains("judai") || text.contains("agar tum saath") -> {
                listOf(
                    "arijit singh sad hit songs",
                    "heartbreak sad hindi songs",
                    "pritam arijit emotional songs",
                    "sad lyrical bollywood songs"
                )
            }

            // ── Romantic Bollywood ─────────────────────────────────────────────
            text.contains("romantic") || text.contains("love") || text.contains("ishq") ||
            text.contains("pyaar") || text.contains("mohabbat") || text.contains("tum hi ho") ||
            text.contains("kesariya") || text.contains("raataan lambiyan") || text.contains("humsafar") -> {
                listOf(
                    "romantic bollywood hit songs",
                    "arijit singh romantic hits",
                    "shreya ghoshal romantic songs",
                    "latest love hindi songs"
                )
            }

            // ── Punjabi Hits ───────────────────────────────────────────────────
            text.contains("punjabi") || text.contains("sidhu") || text.contains("moose") ||
            text.contains("dhillon") || text.contains("diljit") || text.contains("karan aujla") ||
            text.contains("shubh") || text.contains("honey singh") || text.contains("amrit maan") -> {
                listOf(
                    "punjabi hit songs official audio",
                    "diljit dosanjh hit songs",
                    "karan aujla latest songs",
                    "shubh official music audio"
                )
            }

            // ── Lo-Fi & Chill ──────────────────────────────────────────────────
            text.contains("lofi") || text.contains("lo-fi") || text.contains("chill") ||
            text.contains("slowed") || text.contains("reverb") -> {
                listOf(
                    "lofi chill hindi songs",
                    "midnight lofi hindi beats",
                    "slowed reverb bollywood songs"
                )
            }

            // ── Hip-Hop & Rap ──────────────────────────────────────────────────
            text.contains("hip hop") || text.contains("hip-hop") || text.contains("rap") ||
            text.contains("emiway") || text.contains("divine") || text.contains("raftaar") ||
            text.contains("kr\$na") || text.contains("mc stan") -> {
                listOf(
                    "hindi hip hop rap songs",
                    "divine emiway krsna rap",
                    "desi hip hop hits"
                )
            }

            // ── Devotional / Bhakti ────────────────────────────────────────────
            text.contains("bhajan") || text.contains("aarti") || text.contains("krishna") ||
            text.contains("ram") || text.contains("shiva") || text.contains("hanuman") -> {
                listOf(
                    "popular devotional bhakti songs",
                    "shree ram krishna aarti bhajans",
                    "gulshan kumar bhakti bhajans"
                )
            }

            // ── Bollywood Hit Songs ────────────────────────────────────────────
            text.contains("arijit") || text.contains("t-series") || text.contains("bollywood") ||
            text.contains("pritam") || text.contains("shreya") || text.contains("jubin") ||
            text.contains("zeemusic") || text.contains("yrf") -> {
                listOf(
                    "bollywood hit songs official",
                    "trending bollywood songs audio",
                    "popular hindi movie songs"
                )
            }

            // ── Known Artist Specific ──────────────────────────────────────────
            song.artist.isNotBlank() && song.artist != "YouTube" && song.artist != "Unknown" -> {
                listOf(
                    "${song.artist} official songs",
                    "${song.artist} hit songs audio",
                    "${song.artist} best music tracks"
                )
            }

            // ── Fallback based on Song Title ───────────────────────────────────
            else -> {
                val cleanTitle = song.title.replace(Regex("""\(.*?\)|\[.*?\]"""), "").trim()
                listOf(
                    "$cleanTitle official song",
                    "$cleanTitle similar songs audio",
                    "trending hit songs official"
                )
            }
        }
    }

    /**
     * Fetches a batch of 15-20 songs matching the exact vibe of the seed song,
     * skipping any songs already in the queue.
     */
    suspend fun fetchVibeRecommendations(
        seedSong: Song,
        existingVideoIds: Set<String>,
        batchIndex: Int = 0
    ): List<Song> = withContext(Dispatchers.IO) {
        val queries = getVibeQueries(seedSong)
        val targetQuery = queries.getOrElse(batchIndex % queries.size) { queries.first() }
        Log.i(TAG, "Fetching vibe batch for '${seedSong.title}' using query: '$targetQuery'")

        try {
            val results = YouTubeDirectSearch.searchDirect(targetQuery, allowLong = false)
            val seedVid = seedSong.videoId ?: ""
            val cleanSeedTitle = seedSong.title.lowercase().take(6)

            val fresh = results.filter { item ->
                val vId = item.videoId ?: item.id.toString()
                vId != seedVid &&
                vId !in existingVideoIds &&
                !item.title.lowercase().startsWith(cleanSeedTitle) // avoid duplicate remixes of same song
            }

            Log.i(TAG, "Vibe recommendation found ${fresh.size} unique songs matching vibe")
            fresh
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch vibe batch: ${e.message}", e)
            emptyList()
        }
    }
}
