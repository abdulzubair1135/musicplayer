package com.zmusic.app.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.zmusic.app.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicPlayerController private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _currentQueue = MutableStateFlow<List<Song>>(emptyList())
    val currentQueue: StateFlow<List<Song>> = _currentQueue.asStateFlow()

    private val _isShuffleEnabled = MutableStateFlow(false)
    val isShuffleEnabled: StateFlow<Boolean> = _isShuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _sleepTimerMillis = MutableStateFlow<Long?>(null)
    val sleepTimerMillis: StateFlow<Long?> = _sleepTimerMillis.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _playbackPitch = MutableStateFlow(1.0f)
    val playbackPitch: StateFlow<Float> = _playbackPitch.asStateFlow()

    fun setPlaybackSpeedAndPitch(speed: Float, pitch: Float) {
        _playbackSpeed.value = speed
        _playbackPitch.value = pitch
        mediaController?.playbackParameters = androidx.media3.common.PlaybackParameters(speed, pitch)
    }

    fun setNormalMode() = setPlaybackSpeedAndPitch(1.0f, 1.0f)
    fun setSlowedReverbMode() = setPlaybackSpeedAndPitch(0.85f, 0.85f)
    fun setNightcoreMode() = setPlaybackSpeedAndPitch(1.15f, 1.15f)

    private var songMap = mutableMapOf<String, Song>()
    private var progressUpdateJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        initializeController()
    }

    private fun initializeController() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        val controller = mediaController ?: return
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.mediaId?.let { idStr ->
                    songMap[idStr]?.let { song ->
                        _currentSong.value = song
                        val index = _currentQueue.value.indexOfFirst { it.id.toString() == idStr }
                        if (index != -1) {
                            AudioCacheManager.preCacheNextSongs(context, _currentQueue.value, index)
                        }
                    }
                }
                _duration.value = controller.duration.coerceAtLeast(0L)
            }

            private var consecutiveErrors = 0

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    consecutiveErrors = 0
                    _duration.value = controller.duration.coerceAtLeast(0L)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                android.util.Log.e("MusicPlayerController", "Playback error: ${error.message}", error)
                consecutiveErrors++
                if (consecutiveErrors < 2 && _currentQueue.value.size > 1) {
                    skipToNext()
                } else {
                    _isPlaying.value = false
                    consecutiveErrors = 0
                }
            }
        })
    }

    private fun startProgressTracker() {
        progressUpdateJob?.cancel()
        progressUpdateJob = scope.launch {
            while (_isPlaying.value) {
                mediaController?.let { controller ->
                    _playbackPosition.value = controller.currentPosition.coerceAtLeast(0L)
                    _duration.value = controller.duration.coerceAtLeast(0L)
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressUpdateJob?.cancel()
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val controller = mediaController ?: return

        val targetSong = songs.getOrNull(startIndex) ?: songs.first()
        val isTargetOnline = targetSong.sourceType == com.zmusic.app.data.model.SourceType.ONLINE || !targetSong.videoId.isNullOrBlank()

        // If playing an online song, strictly ensure ONLY online songs are in queue!
        // Local tracks are never mixed into an online queue!
        val sanitizedSongs = if (isTargetOnline) {
            val onlineOnly = songs.filter { it.sourceType == com.zmusic.app.data.model.SourceType.ONLINE || !it.videoId.isNullOrBlank() }
            if (onlineOnly.isNotEmpty()) onlineOnly else listOf(targetSong)
        } else {
            songs
        }

        val actualIndex = sanitizedSongs.indexOfFirst { it.id == targetSong.id }.coerceAtLeast(0)

        _currentQueue.value = sanitizedSongs
        songMap.clear()
        sanitizedSongs.forEach { songMap[it.id.toString()] = it }

        val mediaItems = sanitizedSongs.map { song ->
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setArtworkUri(song.artworkUri?.let { Uri.parse(it) })
                .build()

            val playbackUri = if (song.sourceType == com.zmusic.app.data.model.SourceType.ONLINE || !song.videoId.isNullOrBlank()) {
                if (!song.videoId.isNullOrBlank()) {
                    Uri.parse(com.zmusic.app.data.api.ZMusicApiService.streamUrl(song.videoId!!))
                } else {
                    Uri.parse(song.streamUri ?: song.contentUri)
                }
            } else {
                Uri.parse(song.contentUri)
            }

            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(playbackUri)
                .setMediaMetadata(metadata)
                .build()
        }

        controller.setMediaItems(mediaItems, actualIndex, 0L)
        controller.prepare()
        controller.play()

        _currentSong.value = sanitizedSongs.getOrNull(actualIndex)
        AudioCacheManager.preCacheNextSongs(context, sanitizedSongs, actualIndex)
    }

    fun updateQueuePreservingCurrent(newSongs: List<Song>) {
        val controller = mediaController ?: return
        val current = _currentSong.value ?: return
        if (newSongs.isEmpty()) return

        val currentIndexInNew = newSongs.indexOfFirst { it.id == current.id || (!it.videoId.isNullOrBlank() && it.videoId == current.videoId) }
        val reordered = if (currentIndexInNew >= 0) {
            newSongs
        } else {
            listOf(current) + newSongs
        }

        _currentQueue.value = reordered
        reordered.forEach { songMap[it.id.toString()] = it }

        val currentPlayingIndex = reordered.indexOfFirst { it.id == current.id || (!it.videoId.isNullOrBlank() && it.videoId == current.videoId) }.coerceAtLeast(0)

        val mediaItems = reordered.map { song ->
            val metadata = MediaMetadata.Builder()
                .setTitle(song.title)
                .setArtist(song.artist)
                .setAlbumTitle(song.album)
                .setArtworkUri(song.artworkUri?.let { Uri.parse(it) })
                .build()

            val playbackUri = if (song.sourceType == com.zmusic.app.data.model.SourceType.ONLINE || !song.videoId.isNullOrBlank()) {
                if (!song.videoId.isNullOrBlank()) {
                    Uri.parse(com.zmusic.app.data.api.ZMusicApiService.streamUrl(song.videoId!!))
                } else {
                    Uri.parse(song.streamUri ?: song.contentUri)
                }
            } else {
                Uri.parse(song.contentUri)
            }

            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(playbackUri)
                .setMediaMetadata(metadata)
                .build()
        }

        val currentPosition = controller.currentPosition
        val isPlaying = controller.isPlaying
        controller.setMediaItems(mediaItems, currentPlayingIndex, currentPosition)
        if (isPlaying) {
            controller.play()
        }
        AudioCacheManager.preCacheNextSongs(context, reordered, currentPlayingIndex)
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) {
            controller.pause()
        } else {
            controller.play()
        }
    }

    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playbackPosition.value = positionMs
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        val newShuffle = !controller.shuffleModeEnabled
        controller.shuffleModeEnabled = newShuffle
        _isShuffleEnabled.value = newShuffle
    }

    fun toggleRepeat() {
        val controller = mediaController ?: return
        val nextMode = when (controller.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = nextMode
        _repeatMode.value = nextMode
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerMillis.value = null
            return
        }
        val durationMillis = minutes * 60 * 1000L
        val startTime = System.currentTimeMillis()

        sleepTimerJob = scope.launch {
            while (true) {
                val elapsed = System.currentTimeMillis() - startTime
                val remaining = durationMillis - elapsed
                if (remaining <= 0) {
                    _sleepTimerMillis.value = null
                    mediaController?.pause()
                    break
                } else {
                    _sleepTimerMillis.value = remaining
                    delay(1000)
                }
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: MusicPlayerController? = null

        fun getInstance(context: Context): MusicPlayerController {
            return INSTANCE ?: synchronized(this) {
                val instance = MusicPlayerController(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
