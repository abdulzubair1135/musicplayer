package com.zmusic.app.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zmusic.app.data.model.Song
import com.zmusic.app.ui.components.SongItem
import com.zmusic.app.ui.theme.PrimaryPurple
import com.zmusic.app.ui.theme.SecondaryCyan
import com.zmusic.app.ui.theme.SurfaceDark
import com.zmusic.app.ui.theme.TextPrimary
import com.zmusic.app.ui.theme.TextSecondary

@Composable
fun SearchScreen(
    allLocalSongs: List<Song>,
    onlineSongs: List<Song>,
    isNetworkAvailable: Boolean,
    currentSong: Song?,
    onPlaySong: (Song, List<Song>) -> Unit,
    onStartRadio: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredLocalSongs = remember(searchQuery, allLocalSongs) {
        if (searchQuery.isBlank()) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            allLocalSongs.filter { song ->
                song.title.lowercase().contains(q) ||
                song.artist.lowercase().contains(q) ||
                song.album.lowercase().contains(q) ||
                song.genre.lowercase().contains(q)
            }
        }
    }

    val filteredOnlineSongs = remember(searchQuery, onlineSongs, isNetworkAvailable) {
        if (searchQuery.isBlank() || !isNetworkAvailable) emptyList()
        else {
            val q = searchQuery.trim().lowercase()
            onlineSongs.filter { song ->
                song.title.lowercase().contains(q) ||
                song.artist.lowercase().contains(q) ||
                song.album.lowercase().contains(q) ||
                song.genre.lowercase().contains(q)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp)
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search songs, artists, albums, genres...", color = TextSecondary) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = PrimaryPurple
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = TextSecondary
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = PrimaryPurple,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (searchQuery.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Type to search local library & online music catalog",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        } else if (filteredLocalSongs.isEmpty() && filteredOnlineSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No songs found for '$searchQuery'",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // LOCAL RESULTS
                if (filteredLocalSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = "LOCAL RESULTS (${filteredLocalSongs.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = PrimaryPurple,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(filteredLocalSongs) { song ->
                        SongItem(
                            song = song,
                            isCurrentPlaying = song.id == currentSong?.id,
                            onClick = { onPlaySong(song, filteredLocalSongs) },
                            onStartRadio = { onStartRadio(song) },
                            onToggleFavorite = { onToggleFavorite(song) }
                        )
                    }
                }

                // ONLINE RESULTS
                if (filteredOnlineSongs.isNotEmpty()) {
                    item {
                        Text(
                            text = "ONLINE RESULTS (${filteredOnlineSongs.size})",
                            style = MaterialTheme.typography.labelMedium,
                            color = SecondaryCyan,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    }
                    items(filteredOnlineSongs) { song ->
                        SongItem(
                            song = song,
                            isCurrentPlaying = song.id == currentSong?.id,
                            onClick = { onPlaySong(song, filteredOnlineSongs) },
                            onStartRadio = { onStartRadio(song) },
                            onToggleFavorite = { onToggleFavorite(song) }
                        )
                    }
                }
            }
        }
    }
}
