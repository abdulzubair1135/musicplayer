package com.zmusic.app.ui.screens.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zmusic.app.ZMusicApp
import com.zmusic.app.data.downloader.SongDownloader
import com.zmusic.app.data.model.Song
import com.zmusic.app.data.model.SourceType
import com.zmusic.app.ui.theme.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@Composable
fun SearchScreen(
    allLocalSongs: List<Song>,
    onlineSongs: List<Song>,
    isNetworkAvailable: Boolean,
    currentSong: Song?,
    initialQuery: String = "",
    onPlaySong: (Song, List<Song>) -> Unit,
    onStartRadio: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
) {
    val context        = LocalContext.current
    val repository     = ZMusicApp.instance.repository
    val scope          = rememberCoroutineScope()
    val keyboard       = LocalSoftwareKeyboardController.current
    val focusManager   = androidx.compose.ui.platform.LocalFocusManager.current
    val allowLongTracks by com.zmusic.app.data.settings.AppSettings.getInstance(context).allowLongTracks.collectAsState()

    var query          by remember(initialQuery) { mutableStateOf(initialQuery) }
    var searchResults  by remember { mutableStateOf<List<Song>>(emptyList()) }
    var isSearching    by remember { mutableStateOf(false) }
    var searchMode     by remember { mutableStateOf<SearchMode>(SearchMode.Idle) }
    var downloadingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var downloadedIds  by remember { mutableStateOf<Set<String>>(emptySet()) }
    var debounceJob    by remember { mutableStateOf<Job?>(null) }

    var currentPage    by remember { mutableStateOf(1) }
    var isLoadingMore  by remember { mutableStateOf(false) }
    var hasMoreResults by remember { mutableStateOf(true) }
    val listState      = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            query = initialQuery
        }
    }

    // ── Debounced live search ─────────────────────────────────────────────────
    LaunchedEffect(query) {
        if (query.length < 2) {
            searchResults = emptyList()
            searchMode    = SearchMode.Idle
            currentPage   = 1
            hasMoreResults = true
            return@LaunchedEffect
        }
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(400)   // wait 400ms after user stops typing
            isSearching   = true
            currentPage   = 1
            hasMoreResults = true
            searchMode    = if (isNetworkAvailable) SearchMode.YouTube else SearchMode.Local

            val results = if (isNetworkAvailable) {
                repository.searchOnlineSongs(query, page = 1, allowLong = allowLongTracks)
            } else {
                repository.searchLocalSongs(query).let { flow ->
                    allLocalSongs.filter {
                        it.title.contains(query, true) ||
                        it.artist.contains(query, true) ||
                        it.album.contains(query, true)
                    }
                }
            }

            val finalResults = if (allowLongTracks) results else results.filter { it.duration <= 300_000 }
            searchResults = finalResults
            isSearching   = false

            // Check which songs already downloaded
            downloadedIds = finalResults
                .filter { it.videoId != null && SongDownloader.isAlreadyDownloaded(context, it.videoId!!) }
                .map { it.videoId!! }
                .toSet()
        }
    }

    // ── Infinite Scroll Pagination (Safe non-cancelling snapshotFlow) ───────
    LaunchedEffect(listState, query, allowLongTracks) {
        snapshotFlow {
            val totalItems = searchResults.size
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems >= 8 && lastVisibleIndex >= (totalItems - 2)
        }
        .filter { it }
        .collect {
            if (!isLoadingMore && !isSearching && hasMoreResults && isNetworkAvailable && query.length >= 2) {
                isLoadingMore = true
                try {
                    val nextPage = currentPage + 1
                    val moreSongs = repository.searchOnlineSongs(query, page = nextPage, allowLong = allowLongTracks)
                    val cleanMore = if (allowLongTracks) moreSongs else moreSongs.filter { it.duration <= 300_000 }
                    if (cleanMore.isEmpty()) {
                        hasMoreResults = false
                    } else {
                        val currentIds = searchResults.map { it.id }.toSet()
                        val newUnique = cleanMore.filterNot { currentIds.contains(it.id) }
                        if (newUnique.isEmpty()) {
                            hasMoreResults = false
                        } else {
                            searchResults = searchResults + newUnique
                            currentPage = nextPage
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SearchScreen", "Pagination error", e)
                    hasMoreResults = false
                } finally {
                    isLoadingMore = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // ── Header ─────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(PrimaryPurple.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                )
                Text(
                    text = if (isNetworkAvailable) "Search any song on YouTube" else "Offline — searching local library",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                )
            }
        }

        // ── Search Bar ─────────────────────────────────────────────────────────
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            placeholder = {
                Text(
                    text = if (isNetworkAvailable) "Search songs, artists, albums…" else "Search offline library…",
                    color = TextSecondary,
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryPurple)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        query = ""
                        searchResults = emptyList()
                        focusManager.clearFocus()
                        keyboard?.hide()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = PrimaryPurple,
                unfocusedBorderColor = SurfaceDark,
                focusedContainerColor   = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                cursorColor          = PrimaryPurple,
                focusedTextColor     = TextPrimary,
                unfocusedTextColor   = TextPrimary,
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                keyboard?.hide()
                focusManager.clearFocus()
            }),
        )

        // ── Quick Filter Pills ───────────────────────────────────────────────
        val quickCategories = listOf("Bollywood", "Phonk", "Pop", "Punjabi", "English", "Lo-Fi", "EDM", "Romantic", "Rock", "Hip Hop")
        androidx.compose.foundation.lazy.LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickCategories) { cat ->
                val isSelected = query.contains(cat, ignoreCase = true)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        focusManager.clearFocus()
                        keyboard?.hide()
                        query = when (cat) {
                            "Pop" -> "Pop hit songs"
                            "Bollywood" -> "Bollywood hit songs"
                            "Phonk" -> "Phonk music"
                            "English" -> "Trending English songs"
                            "Lo-Fi" -> "Lo-Fi chill beats"
                            "EDM" -> "EDM dance hits"
                            "Punjabi" -> "Punjabi hit songs"
                            "Romantic" -> "Romantic Hindi songs"
                            "Rock" -> "Classic rock songs"
                            "Hip Hop" -> "Hip Hop rap songs"
                            else -> "$cat songs"
                        }
                        keyboard?.hide()
                    },
                    label = { Text(cat, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryPurple,
                        selectedLabelColor = Color.White,
                        containerColor = SurfaceDark,
                        labelColor = TextSecondary
                    )
                )
            }
        }

        // ── Body ───────────────────────────────────────────────────────────────
        AnimatedContent(
            targetState = when {
                isSearching                         -> "loading"
                query.length < 2                    -> "empty"
                searchResults.isEmpty()             -> "no_results"
                else                                -> "results"
            },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "search_body",
        ) { state ->
            when (state) {
                "loading" -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(color = PrimaryPurple, strokeWidth = 3.dp)
                            Text("Searching YouTube…", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
                "empty" -> SearchIdleView(isNetworkAvailable, onGenreClick = { selectedGenre ->
                    focusManager.clearFocus()
                    keyboard?.hide()
                    query = when (selectedGenre) {
                        "Pop" -> "Pop hit songs"
                        "Bollywood" -> "Bollywood hit songs"
                        "Phonk" -> "Phonk music"
                        "English" -> "Trending English songs"
                        "Lo-Fi" -> "Lo-Fi chill beats"
                        "EDM" -> "EDM dance hits"
                        "Punjabi" -> "Punjabi hit songs"
                        "Romantic" -> "Romantic Hindi songs"
                        "Rock" -> "Classic rock songs"
                        "Hip-Hop" -> "Hip Hop rap songs"
                        else -> "$selectedGenre songs"
                    }
                    keyboard?.hide()
                })
                "no_results" -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.MusicOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                            Text("No results for \"$query\"", color = TextSecondary)
                        }
                    }
                }
                "results" -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        item {
                            Text(
                                text = "${searchResults.size} results · ${if (searchMode == SearchMode.YouTube) "YouTube" else "Local Library"}",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                            )
                        }

                        items(searchResults, key = { it.id }) { song ->
                            SearchResultItem(
                                song           = song,
                                isPlaying      = currentSong?.id == song.id,
                                isDownloaded   = song.videoId != null && downloadedIds.contains(song.videoId),
                                isDownloading  = song.videoId != null && downloadingIds.contains(song.videoId),
                                onPlay         = { onPlaySong(song, searchResults) },
                                onDownload     = {
                                    song.videoId?.let { vid ->
                                        if (!downloadedIds.contains(vid) && !downloadingIds.contains(vid)) {
                                            downloadingIds = downloadingIds + vid
                                            SongDownloader.download(context, song)
                                            scope.launch {
                                                delay(3000)
                                                downloadingIds = downloadingIds - vid
                                                downloadedIds  = downloadedIds + vid
                                            }
                                        }
                                    }
                                },
                                onRadio        = { onStartRadio(song) },
                                onFavorite     = { onToggleFavorite(song) },
                            )
                        }
                        if (isLoadingMore) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = PrimaryPurple,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text("Loading more songs…", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                        }

                        item { Spacer(Modifier.height(100.dp)) }
                    }
                }
            }
        }
    }
}

// ── Search Result Row ──────────────────────────────────────────────────────────
@Composable
private fun SearchResultItem(
    song: Song,
    isPlaying: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onRadio: () -> Unit,
    onFavorite: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .background(if (isPlaying) PrimaryPurple.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Artwork
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceDark),
            contentAlignment = Alignment.Center,
        ) {
            if (song.artworkUri != null) {
                AsyncImage(
                    model = song.artworkUri,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Default.MusicNote, contentDescription = null, tint = TextSecondary)
            }
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PrimaryPurple.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        // Text info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = song.title,
                color = if (isPlaying) PrimaryPurple else TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp,
            )
            Text(
                text  = "${song.artist} · ${formatDuration(song.duration)}",
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp,
            )
        }

        // Action buttons
        if (song.sourceType == SourceType.ONLINE || song.videoId != null) {
            // Download button
            when {
                isDownloaded -> {
                    Icon(
                        Icons.Default.DownloadDone,
                        contentDescription = "Downloaded",
                        tint = Color(0xFF1DB954),
                        modifier = Modifier.size(22.dp),
                    )
                }
                isDownloading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = PrimaryPurple,
                        strokeWidth = 2.dp,
                    )
                }
                else -> {
                    IconButton(onClick = onDownload, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Download offline",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        IconButton(onClick = onFavorite, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

// ── Idle / welcome state ───────────────────────────────────────────────────────
@Composable
private fun SearchIdleView(isNetworkAvailable: Boolean, onGenreClick: (String) -> Unit) {
    val genres = listOf("Pop", "Phonk", "Bollywood", "English", "Hip-Hop", "Rock", "Lo-Fi", "EDM", "Punjabi", "Indie")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text("Browse Top Genres", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(12.dp))
        genres.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { genre ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PrimaryPurple.copy(alpha = 0.55f), SecondaryCyan.copy(alpha = 0.35f))
                                )
                            )
                            .clickable { onGenreClick(genre) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(genre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        if (!isNetworkAvailable) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDark)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color(0xFFFFA500), modifier = Modifier.size(20.dp))
                Text("Offline mode — only local music will appear in search results", color = TextSecondary, fontSize = 12.sp)
            }
        }
    }
}

private enum class SearchMode { Idle, YouTube, Local }

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
