package com.zmusic.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
                MainAppScreen()
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

        LaunchedEffect(isNetworkAvailable) {
            if (isNetworkAvailable) {
                try {
                    onlineDiscover = repository.getOnlineDiscover()
                    onlineTrending = repository.getOnlineTrending()
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

        val isCurrentFavorite by remember(currentSong, favorites) {
            mutableStateOf(favorites.any { it.id == currentSong?.id })
        }

        val navController = rememberNavController()
        var isFullPlayerExpanded by remember { mutableStateOf(false) }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val bottomBarScreens = listOf(
            Screen.Home,
            Screen.Search,
            Screen.Library,
            Screen.Settings
        )

        val playSongAction: (Song, List<Song>) -> Unit = { song, list ->
            playerController.playQueue(list, list.indexOf(song).coerceAtLeast(0))
            scope.launch {
                repository.recordPlayHistory(song)
            }
        }

        val startRadioAction: (Song) -> Unit = { seedSong ->
            scope.launch {
                val radioList = repository.getSongRadioQueue(seedSong, isNetworkAvailable)
                if (radioList.isNotEmpty()) {
                    playerController.playQueue(radioList, 0)
                    Toast.makeText(this@MainActivity, "Song Radio started for ${seedSong.title}", Toast.LENGTH_SHORT).show()
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
                        HomeScreen(
                            songs = allSongs,
                            recentlyPlayed = recentlyPlayed,
                            favorites = favorites,
                            artists = artists,
                            albums = albums,
                            onlineDiscover = onlineDiscover,
                            onlineTrending = onlineTrending,
                            isNetworkAvailable = isNetworkAvailable,
                            currentSong = currentSong,
                            onPlaySong = playSongAction,
                            onStartRadio = startRadioAction,
                            onToggleFavorite = { song -> scope.launch { repository.toggleFavorite(song) } },
                            onScanMusic = { triggerMusicScan() },
                            onNavigateToArtist = { },
                            onNavigateToAlbum = { }
                        )
                    }

                    composable(Screen.Search.route) {
                        SearchScreen(
                            allLocalSongs = allSongs,
                            onlineSongs = onlineDiscover + onlineTrending,
                            isNetworkAvailable = isNetworkAvailable,
                            currentSong = currentSong,
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
