package com.zmusic.app.data.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.zmusic.app.data.api.ApiConfig
import com.zmusic.app.data.api.ZMusicApiService
import com.zmusic.app.data.model.Song
import com.zmusic.app.data.model.SourceType
import com.zmusic.app.database.ZMusicDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

// ── Worker ──────────────────────────────────────────────────────────────────────
class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_VIDEO_ID  = "video_id"
        const val KEY_TITLE     = "title"
        const val KEY_ARTIST    = "artist"
        const val KEY_THUMBNAIL = "thumbnail"
        const val KEY_DURATION  = "duration"
        const val CHANNEL_ID    = "zmusic_download"
        const val NOTIF_ID      = 7777
    }

    override suspend fun doWork(): Result {
        val videoId   = inputData.getString(KEY_VIDEO_ID)   ?: return Result.failure()
        val title     = inputData.getString(KEY_TITLE)      ?: "Unknown"
        val artist    = inputData.getString(KEY_ARTIST)     ?: "Unknown"
        val thumbnail = inputData.getString(KEY_THUMBNAIL)  ?: ""
        val duration  = inputData.getLong(KEY_DURATION, 0L)

        setForeground(createForegroundInfo(title))

        return withContext(Dispatchers.IO) {
            try {
                val dir = File(applicationContext.getExternalFilesDir(null), "ZMusic/Downloads")
                dir.mkdirs()
                val file = File(dir, "${videoId}.mp3")

                if (!file.exists()) {
                    val streamUrl = "${ApiConfig.BASE_URL.trimEnd('/')}api/stream/$videoId"
                    val connection = URL(streamUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = 30_000
                    connection.readTimeout    = 120_000
                    connection.connect()

                    connection.inputStream.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    connection.disconnect()
                }

                // Save to Room DB as DOWNLOADED
                val db = ZMusicDatabase.getInstance(applicationContext)
                val stableId = (videoId.hashCode().toLong() and 0x7FFFFFFFFFFFFFFFL) + 800_000L
                val song = Song(
                    id         = stableId,
                    title      = title,
                    artist     = artist,
                    album      = "Downloaded",
                    duration   = duration,
                    contentUri = file.absolutePath,
                    streamUri  = file.absolutePath,
                    artworkUri = thumbnail,
                    genre      = "Downloaded",
                    sourceType = SourceType.DOWNLOADED,
                    videoId    = videoId,
                )
                db.songDao().insertSongs(listOf(song))

                Result.success()
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure()
            }
        }
    }

    private fun createForegroundInfo(title: String): ForegroundInfo {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Z-Music Downloads", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading: $title")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()
        return ForegroundInfo(NOTIF_ID, notification)
    }
}

// ── Helper to enqueue a download ────────────────────────────────────────────────
object SongDownloader {
    fun download(context: Context, song: Song) {
        val videoId = song.videoId ?: return
        val data: Data = workDataOf(
            DownloadWorker.KEY_VIDEO_ID  to videoId,
            DownloadWorker.KEY_TITLE     to song.title,
            DownloadWorker.KEY_ARTIST    to song.artist,
            DownloadWorker.KEY_THUMBNAIL to (song.artworkUri ?: ""),
            DownloadWorker.KEY_DURATION  to song.duration,
        )
        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(data)
            .addTag("download_$videoId")
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    fun isAlreadyDownloaded(context: Context, videoId: String): Boolean {
        val dir  = File(context.getExternalFilesDir(null), "ZMusic/Downloads")
        val file = File(dir, "${videoId}.mp3")
        return file.exists()
    }
}
