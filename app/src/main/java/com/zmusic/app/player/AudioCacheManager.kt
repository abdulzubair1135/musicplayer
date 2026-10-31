package com.zmusic.app.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.zmusic.app.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

@OptIn(UnstableApi::class)
object AudioCacheManager {
    private const val CACHE_SIZE = 500L * 1024 * 1024 // 500 MB local audio cache
    private const val PRECACHE_BYTES = 2L * 1024 * 1024 // 2 MB per upcoming track for instant playback

    private var simpleCache: SimpleCache? = null
    private var databaseProvider: StandaloneDatabaseProvider? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @Synchronized
    fun getCache(context: Context): SimpleCache {
        if (simpleCache == null) {
            val cacheDir = File(context.cacheDir, "zmusic_audio_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
            databaseProvider = StandaloneDatabaseProvider(context)
            simpleCache = SimpleCache(cacheDir, evictor, databaseProvider!!)
        }
        return simpleCache!!
    }

    fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
        val cache = getCache(context)
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(30000)
            .setUserAgent("Z-Music/1.0")

        val upstreamFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

        val cacheSink = CacheDataSink.Factory()
            .setCache(cache)

        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory)
            .setCacheWriteDataSinkFactory(cacheSink)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /**
     * Pre-buffers the next 5 songs in the queue into local disk cache.
     * When current song finishes, next song starts with zero network latency.
     */
    fun preCacheNextSongs(context: Context, queue: List<Song>, currentIndex: Int) {
        if (queue.isEmpty()) return
        scope.launch {
            val nextSongs = queue.drop(currentIndex + 1).take(5)
            val cache = getCache(context)
            val httpDataSource = DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setUserAgent("Z-Music/1.0")
                .createDataSource()

            for (song in nextSongs) {
                val uriString = song.streamUri ?: song.contentUri
                if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
                    try {
                        val uri = Uri.parse(uriString)
                        val dataSpec = DataSpec.Builder()
                            .setUri(uri)
                            .setLength(PRECACHE_BYTES)
                            .build()

                        val cacheWriter = CacheWriter(
                            CacheDataSource.Factory()
                                .setCache(cache)
                                .setUpstreamDataSourceFactory { httpDataSource }
                                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
                                .createDataSource(),
                            dataSpec,
                            null,
                            null
                        )
                        cacheWriter.cache()
                    } catch (_: Exception) {
                        // Continue to next song if one fails
                    }
                }
            }
        }
    }
}
