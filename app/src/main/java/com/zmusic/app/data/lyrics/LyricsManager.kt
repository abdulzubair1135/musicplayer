package com.zmusic.app.data.lyrics

import com.zmusic.app.data.api.ZMusicApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

object LyricsManager {
    private val api = ZMusicApiService.getInstance()
    private val cache = mutableMapOf<String, List<LyricLine>>()

    suspend fun getLyrics(title: String, artist: String?, duration: Long): List<LyricLine> = withContext(Dispatchers.IO) {
        val cacheKey = "$title - $artist"
        cache[cacheKey]?.let { return@withContext it }

        val lines = try {
            val resp = api.getLyrics(title = title, artist = artist, duration = duration)
            if (resp.isSuccessful && resp.body()?.syncedLyrics != null) {
                parseLrc(resp.body()!!.syncedLyrics!!)
            } else if (resp.isSuccessful && resp.body()?.plainLyrics != null) {
                resp.body()!!.plainLyrics!!.lines().mapIndexed { index, line ->
                    LyricLine(index * 3500L, line)
                }
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }

        if (lines.isNotEmpty()) {
            cache[cacheKey] = lines
        }
        lines
    }

    private fun parseLrc(lrcText: String): List<LyricLine> {
        val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})\](.*)""")
        val result = mutableListOf<LyricLine>()

        for (line in lrcText.lines()) {
            val match = regex.find(line.trim())
            if (match != null) {
                val min = match.groupValues[1].toLongOrNull() ?: 0L
                val sec = match.groupValues[2].toLongOrNull() ?: 0L
                val msStr = match.groupValues[3].padEnd(3, '0').take(3)
                val ms = msStr.toLongOrNull() ?: 0L
                val text = match.groupValues[4].trim()

                val timestamp = (min * 60 + sec) * 1000L + ms
                if (text.isNotBlank()) {
                    result.add(LyricLine(timestamp, text))
                }
            }
        }
        return result.sortedBy { it.timestampMs }
    }
}
