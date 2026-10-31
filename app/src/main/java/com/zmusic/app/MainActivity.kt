package com.zmusic.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.zmusic.app.data.model.Playlist
import com.zmusic.app.data.model.Song
import com.zmusic.app.ui.components.MiniPlayer
import com.zmusic.app.ui.navigation.Screen
import com.zmusic.app.ui.screens.home.HomeScreen
import com.zmusic.app.ui.screens.library.LibraryScreen
import com.zmusic.app.ui.screens.library.PlaylistDetailScreen
import com.zmusic.app.ui.screens.player.FullPlayerScreen
import com.zmusic.app.ui.screens.search.SearchScreen
import com.zmusic.app.ui.screens.settings.SettingsScreen
import com.zmusic.app.ui.screens.splash.AppSplashScreen
import com.zmusic.app.ui.theme.BackgroundDark
import com.zmusic.app.ui.theme.PrimaryPurple
import com.zmusic.app.ui.theme.SurfaceDark
import com.zmusic.app.ui.theme.TextPrimary
import com.zmusic.app.ui.theme.TextSecondary
import com.zmusic.app.ui.theme.ZMusicTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] == true ||
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        if (audioGranted) {
            triggerMusicScan()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()

        setContent {
            ZMusicTheme {
                var showSplash by rememberSaveable { mutableStateOf(true) }
                AnimatedContent(
                    targetState = showSplash,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(350)) togetherWith fadeOut(animationSpec = tween(350))
                    },
                    label = "SplashTransition"
                ) { isSplash ->
                    if (isSplash) {
                        AppSplashScreen(onFinished = { showSplash = false })
                    } else {
                        MainAppScreen()
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            triggerMusicScan()
        }
    }

    private fun triggerMusicScan() {
        val app = ZMusicApp.instance
        val scope = (app.applicationContext as? ZMusicApp)?.let {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
        }
        scope?.launch {
            app.repository.scanMusic()
        }
    }

    @Composable
    private fun MainAppScreen() {
        val app = ZMusicApp.instance
        val repository = app.repository
        val playerController = app.playerController
        val scope = rememberCoroutineScope()

        val isNetworkAvailable by repository.isNetworkAvailable.collectAsState(initial = true)
        val allSongs by repository.getAllSongs().collectAsState(initial = emptyList())
        val recentlyPlayed by repository.getRecentlyPlayed().collectAsState(initial = emptyList())
        val favorites by repository.getFavoriteSongs().collectAsState(initial = emptyList())
        val playlists by repository.getAllPlaylists().collectAsState(initial = emptyList())
        val artists by repository.getAllArtists().collectAsState(initial = emptyList())
        val albums by repository.getAllAlbums().collectAsState(initial = emptyList())

        var onlineDiscover by remember { mutableStateOf<List<Song>>(emptyList()) }
        var onlineTrending by remember { mutableStateOf<List<Song>>(emptyList()) }

        val appSettings = remember { com.zmusic.app.data.settings.AppSettings.getInstance(this@MainActivity) }
        val allowLongTracks by appSettings.allowLongTracks.collectAsState(initial = false)

        LaunchedEffect(isNetworkAvailable, allowLongTracks) {
            if (isNetworkAvailable) {
                try {
                    onlineDiscover = repository.getOnlineDiscover(allowLong = allowLongTracks)
                    onlineTrending = repository.getOnlineTrending(allowLong = allowLongTracks)
                } catch (e: Exception) {
                    onlineDiscover = emptyList()
                    onlineTrending = emptyList()
                }
            }
        }

        val currentSong by playerController.currentSong.collectAsState()
        val isPlaying by playerController.isPlaying.collectAsState()
        val playbackPosition by playerController.playbackPosition.collectAsState()
        val duration by playerController.duration.collectAsState()
        val isShuffleEnabled by playerController.isShuffleEnabled.collectAsState()
        val repeatMode by playerController.repeatMode.collectAsState()
        val sleepTimerMillis by playerController.sleepTimerMillis.collectAsState()
        val currentQueue by playerController.currentQueue.collectAsState()
        val playbackSpeed by playerController.playbackSpeed.collectAsState()

        val isCurrentFavorite by remember(currentSong, favorites) {
            mutableStateOf(favorites.any { it.id == currentSong?.id })
        }

        val navController = rememberNavController()
        var isFullPlayerExpanded by remember { mutableStateOf(false) }
        var searchInitialQuery by remember { mutableStateOf("") }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // ── Smart Back Button Handling ──────────────────────────────────────────
        // 1. If full player is expanded, back collapses it
        BackHandler(enabled = isFullPlayerExpanded) {
            isFullPlayerExpanded = false
        }

        // 2. If on another screen or sub-screen, back returns to Home
        val isNotOnHome = currentRoute != null && currentRoute != Screen.Home.route
        BackHandler(enabled = !isFullPlayerExpanded && isNotOnHome) {
            if (currentRoute == Screen.PlaylistDetail.route) {
                navController.popBackStack()
            } else {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
        }

        // 3. If on Home, double-tap back to exit (prevents sudden app closing!)
        var backPressedOnce by remember { mutableStateOf(false) }
        BackHandler(enabled = !isFullPlayerExpanded && !isNotOnHome) {
            if (backPressedOnce) {
                finish()
            } else {
                backPressedOnce = true
                Toast.makeText(this@MainActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
                scope.launch {
                    kotlinx.coroutines.delay(2000)
                    backPressedOnce = false
                }
            }
        }

        val bottomBarScreens = listOf(
            Screen.Home,
            Screen.Search,
            Screen.Library,
            Screen.Settings
        )

        val playSongAction: (Song, List<Song>) -> Unit = { song, list ->
            val isOnline = song.sourceType == com.zmusic.app.data.model.SourceType.ONLINE || !song.videoId.isNullOrBlank()

            // Filter out duplicate versions (e.g. karaoke, remixes, bonus tracks) of this seed song from the list
            val cleanTitlePrefix = song.title.lowercase().replace(Regex("""\(.*?\)|\[.*?\]"""), "").trim().take(7)
            val filteredList = if (isOnline && cleanTitlePrefix.length >= 3) {
                list.filter { item ->
                    item.id == song.id || !item.title.lowercase().replace(Regex("""\(.*?\)|\[.*?\]"""), "").trim().startsWith(cleanTitlePrefix)
                }
            } else {
                list
            }

            val targetQueue = if (filteredList.any { it.id == song.id }) filteredList else listOf(song) + filteredList
            val playIndex = targetQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
            playerController.playQueue(targetQueue, playIndex)

            scope.launch {
                repository.recordPlayHistory(song)
            }

            // AUTO-RECOMMENDATION ALGORITHM:
            // When playing an online song, if queue has fewer than 6 different tracks (e.g. from search),
            // automatically fetch 20+ similar mood/genre songs and append them so next songs always match the vibe!
            if (isOnline && isNetworkAvailable && targetQueue.size < 6) {
                scope.launch {
                    try {
                        val radioSongs = repository.getSongRadioQueue(song, isOnlineAvailable = isNetworkAvailable, allowLong = allowLongTracks)
                        if (radioSongs.isNotEmpty()) {
                            val existingIds = targetQueue.map { it.videoId ?: it.id.toString() }.toSet()
                            val uniqueRadio = radioSongs.filter { (it.videoId ?: it.id.toString()) !in existingIds }
                            val fullQueue = targetQueue + uniqueRadio
                            playerController.updateQueuePreservingCurrent(fullQueue)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Auto radio queue enhancement error", e)
                    }
                }
            }
        }

        val startRadioAction: (Song) -> Unit = { seedSong ->
            // Start seed song immediately with zero wait!
            playerController.playQueue(listOf(seedSong), 0)
            scope.launch {
                repository.recordPlayHistory(seedSong)
                val radioList = repository.getSongRadioQueue(seedSong, isOnlineAvailable = isNetworkAvailable, allowLong = allowLongTracks)
                if (radioList.isNotEmpty()) {
                    val fresh = radioList.filter { it.videoId != seedSong.videoId && it.id != seedSong.id }
                    playerController.updateQueuePreservingCurrent(listOf(seedSong) + fresh)
                    Toast.makeText(this@MainActivity, "Auto-queue loaded with similar songs!", Toast.LENGTH_SHORT).show()
                }
            }
        }

        Scaffold(
            bottomBar = {
                Column {
                    AnimatedVisibility(
                        visible = currentSong != null && !isFullPlayerExpanded,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        currentSong?.let { song ->
                            val progress = if (duration > 0) playbackPosition.toFloat() / duration.toFloat() else 0f
                            MiniPlayer(
                                song = song,
                                isPlaying = isPlaying,
                                progress = progress,
                                onExpand = { isFullPlayerExpanded = true },
                                onPlayPauseToggle = { playerController.togglePlayPause() },
                                onSkipNext = { playerController.skipToNext() }
                            )
                        }
                    }

                    NavigationBar(
                        containerColor = SurfaceDark,
                        contentColor = TextPrimary
                    ) {
                        bottomBarScreens.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                icon = {
                                    screen.icon?.let {
                                        Icon(
                                            imageVector = it,
                                            contentDescription = screen.title
                                        )
                                    }
                                },
                                label = { Text(text = screen.title) },
                                selected = selected,
                                onClick = {
                                    if (screen == Screen.Search && currentRoute != Screen.Search.route) {
                                        searchInitialQuery = ""
                                    }
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrimaryPurple,
                                    selectedTextColor = PrimaryPurple,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }
                }
            },
            containerColor = BackgroundDark
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) {
                        val onlineRecentlyPlayed = remember(recentlyPlayed, allowLongTracks) {
                            recentlyPlayed.filter {
                                val isOnline = (it.sourceType == com.zmusic.app.data.model.SourceType.ONLINE || !it.videoId.isNullOrBlank())
                                val isNotMix = !it.title.contains("100 songs", ignoreCase = true)
                                        && !it.title.contains("top 50", ignoreCase = true)
                                        && !it.title.contains("top 100", ignoreCase = true)
                                        && !it.title.contains("mashup", ignoreCase = true)
                                        && !it.title.contains("jukebox", ignoreCase = true)
                                        && !it.title.contains("compilation", ignoreCase = true)
                                        && !it.title.contains("non stop", ignoreCase = true)
                                        && !it.title.contains("nonstop", ignoreCase = true)
                                        && !it.title.contains("megamix", ignoreCase = true)
                                        && !it.title.contains("best of", ignoreCase = true)
                                val durationOk = allowLongTracks || (it.duration in 1..300_000)
                                isOnline && isNotMix && durationOk
                            }
                        }
                        HomeScreen(
                            recentlyPlayed = onlineRecentlyPlayed,
                            onlineDiscover = onlineDiscover,
                            onlineTrending = onlineTrending,
                            artists = artists,
                            isNetworkAvailable = isNetworkAvailable,
                            onPlaySong = playSongAction,
                            onNavigateToArtist = { artistQuery ->
                                searchInitialQuery = artistQuery
                                navController.navigate(Screen.Search.route) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(Screen.Search.route) {
                        SearchScreen(
                            allLocalSongs = allSongs,
                            onlineSongs = emptyList(),
                            isNetworkAvailable = isNetworkAvailable,
                            currentSong = currentSong,
                            initialQuery = searchInitialQuery,
                            onPlaySong = playSongAction,
                            onStartRadio = startRadioAction,
                            onToggleFavorite = { song -> scope.launch { repository.toggleFavorite(song) } }
                        )
                    }

                    composable(Screen.Library.route) {
                        LibraryScreen(
                            playlists = playlists,
                            favoriteSongs = favorites,
                            allSongs = allSongs,
                            currentSong = currentSong,
                            onCreatePlaylist = { name -> scope.launch { repository.createPlaylist(name) } },
                            onDeletePlaylist = { id -> scope.launch { repository.deletePlaylist(id) } },
                            onPlaySong = playSongAction,
                            onStartRadio = startRadioAction,
                            onToggleFavorite = { song -> scope.launch { repository.toggleFavorite(song) } },
                            onSelectPlaylist = { playlist ->
                                navController.navigate(Screen.PlaylistDetail.createRoute(playlist.id))
                            }
                        )
                    }

                    composable(
                        route = Screen.PlaylistDetail.route,
                        arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                        val playlist = playlists.find { it.id == playlistId } ?: Playlist(id = playlistId, name = "Playlist")
                        val playlistSongs by repository.getSongsForPlaylist(playlistId).collectAsState(initial = emptyList())

                        PlaylistDetailScreen(
                            playlist = playlist,
                            playlistSongs = playlistSongs,
                            allSongs = allSongs,
                            currentSong = currentSong,
                            onBack = { navController.popBackStack() },
                            onPlaySong = playSongAction,
                            onShufflePlay = { list -> playerController.playQueue(list.shuffled(), 0) },
                            onAddSongToPlaylist = { songId -> scope.launch { repository.addSongToPlaylist(playlistId, songId) } },
                            onRemoveSongFromPlaylist = { songId -> scope.launch { repository.removeSongFromPlaylist(playlistId, songId) } }
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            onScanMusic = { triggerMusicScan() },
                            onClearHistory = { scope.launch { repository.clearHistory() } }
                        )
                    }
                }

                // Fullscreen Player Overlay
                if (isFullPlayerExpanded && currentSong != null) {
                    FullPlayerScreen(
                        song = currentSong!!,
                        isPlaying = isPlaying,
                        playbackPosition = playbackPosition,
                        duration = duration,
                        isShuffleEnabled = isShuffleEnabled,
                        repeatMode = repeatMode,
                        isFavorite = isCurrentFavorite,
                        currentQueue = currentQueue,
                        sleepTimerMillis = sleepTimerMillis,
                        playbackSpeed = playbackSpeed,
                        onSetSpeedAndPitch = { speed, pitch ->
                            playerController.setPlaybackSpeedAndPitch(speed, pitch)
                        },
                        onDismiss = { isFullPlayerExpanded = false },
                        onPlayPauseToggle = { playerController.togglePlayPause() },
                        onSkipNext = { playerController.skipToNext() },
                        onSkipPrevious = { playerController.skipToPrevious() },
                        onSeekTo = { pos -> playerController.seekTo(pos) },
                        onToggleShuffle = { playerController.toggleShuffle() },
                        onToggleRepeat = { playerController.toggleRepeat() },
                        onToggleFavorite = { currentSong?.let { song -> scope.launch { repository.toggleFavorite(song) } } },
                        onSetSleepTimer = { mins -> playerController.setSleepTimer(mins) },
                        onSelectQueueSong = { song -> playSongAction(song, currentQueue) }
                    )
                }
            }
        }
    }
}
