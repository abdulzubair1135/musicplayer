package com.zmusic.app.data.provider

import android.util.Log
import com.zmusic.app.data.api.ZMusicApiService
import com.zmusic.app.data.model.Song
import com.zmusic.app.data.model.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Robust Client-Side YouTube Search Fallback.
 * Scrapes YouTube web search directly on the Android device so search works
 * even when backend server is asleep, sleeping on Render, or disconnected!
 */
object YouTubeDirectSearch {
    private const val TAG = "YouTubeDirectSearch"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun searchDirect(query: String, allowLong: Boolean = false): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://www.youtube.com/results?search_query=$encoded"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Direct search HTTP ${response.code}")
                return@withContext emptyList()
            }

            val html = response.body?.string() ?: return@withContext emptyList()

            // Match ytInitialData JSON inside script tag
            val regex = Regex("""var\s+ytInitialData\s*=\s*(\{.+?\});\s*</script>""")
            val match = regex.find(html) ?: run {
                // Secondary regex variant
                Regex("""ytInitialData\s*=\s*(\{.+?\});""").find(html)
            }

            if (match == null) {
                Log.w(TAG, "ytInitialData regex not matched")
                return@withContext emptyList()
            }

            val jsonStr = match.groupValues[1]
            val root = JSONObject(jsonStr)

            val sectionList = root.optJSONObject("contents")
                ?.optJSONObject("twoColumnSearchResultsRenderer")
                ?.optJSONObject("primaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents")

            val songs = mutableListOf<Song>()
            val seenIds = mutableSetOf<String>()

            if (sectionList != null) {
                for (s in 0 until sectionList.length()) {
                    val itemSection = sectionList.optJSONObject(s)?.optJSONObject("itemSectionRenderer") ?: continue
                    val items = itemSection.optJSONArray("contents") ?: continue

                    for (i in 0 until items.length()) {
                        val video = items.optJSONObject(i)?.optJSONObject("videoRenderer") ?: continue
                        val videoId = video.optString("videoId").takeIf { it.isNotBlank() } ?: continue

                        if (seenIds.contains(videoId)) continue
                        seenIds.add(videoId)

                        val title = video.optJSONObject("title")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                            ?: video.optJSONObject("title")?.optString("simpleText") ?: "Song"

                        val artist = video.optJSONObject("ownerText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                            ?: video.optJSONObject("shortBylineText")?.optJSONArray("runs")?.optJSONObject(0)?.optString("text")
                            ?: "YouTube"

                        val lengthText = video.optJSONObject("lengthText")?.optString("simpleText") ?: "3:30"
                        val durSec = parseDurationToSeconds(lengthText)

                        // If not allowLong, skip tracks longer than 5 minutes (300 seconds)
                        if (!allowLong && durSec > 300) continue
                        // Skip very short clips under 30s (shorts/trailers)
                        if (durSec < 30) continue

                        val thumbs = video.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                        val thumbUrl = if (thumbs != null && thumbs.length() > 0) {
                            thumbs.optJSONObject(thumbs.length() - 1)?.optString("url") ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        } else {
                            "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
                        }

                        val streamUrl = ZMusicApiService.streamUrl(videoId)
                        val stableId = (videoId.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL) + 800_000L

                        songs.add(
                            Song(
                                id = stableId,
                                title = title,
                                artist = artist,
                                album = "YouTube",
                                duration = durSec * 1000L,
                                contentUri = streamUrl,
                                streamUri = streamUrl,
                                artworkUri = thumbUrl,
                                genre = "Online",
                                sourceType = SourceType.ONLINE,
                                videoId = videoId
                            )
                        )
                    }
                }
            }

            Log.i(TAG, "Direct YouTube search for '$query' successfully parsed ${songs.size} songs")
            songs
        } catch (e: Exception) {
            Log.e(TAG, "Direct YouTube search failed: ${e.message}", e)
            emptyList()
        }
    }

    private fun parseDurationToSeconds(lengthText: String): Long {
        return try {
            val parts = lengthText.trim().split(":")
            when (parts.size) {
                3 -> (parts[0].toLongOrNull() ?: 0L) * 3600 + (parts[1].toLongOrNull() ?: 0L) * 60 + (parts[2].toLongOrNull() ?: 0L)
                2 -> (parts[0].toLongOrNull() ?: 0L) * 60 + (parts[1].toLongOrNull() ?: 0L)
                1 -> parts[0].toLongOrNull() ?: 210L
                else -> 210L
            }
        } catch (_: Exception) {
            210L
        }
    }
}
