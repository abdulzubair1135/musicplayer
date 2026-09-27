package com.zmusic.app.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.zmusic.app.data.model.Playlist
import com.zmusic.app.data.model.Song
import com.zmusic.app.ui.components.SongItem
import com.zmusic.app.ui.theme.PrimaryPurple
import com.zmusic.app.ui.theme.SurfaceDark
import com.zmusic.app.ui.theme.TextPrimary
import com.zmusic.app.ui.theme.TextSecondary

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    playlistSongs: List<Song>,
    allSongs: List<Song>,
    currentSong: Song?,
    onBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onAddSongToPlaylist: (Long) -> Unit,
    onRemoveSongFromPlaylist: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddSongsDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = playlist.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "${playlistSongs.size} tracks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Action Buttons Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { if (playlistSongs.isNotEmpty()) onPlaySong(playlistSongs.first(), playlistSongs) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play All", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { if (playlistSongs.isNotEmpty()) onShufflePlay(playlistSongs) },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = null, tint = PrimaryPurple)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Shuffle", color = TextPrimary)
            }

            IconButton(onClick = { showAddSongsDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Track", tint = PrimaryPurple)
            }
        }

        if (playlistSongs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "This playlist is empty. Tap '+' to add tracks!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                items(playlistSongs) { song ->
                    SongItem(
                        song = song,
                        isCurrentPlaying = song.id == currentSong?.id,
                        onClick = { onPlaySong(song, playlistSongs) },
                        onAddToPlaylist = { onRemoveSongFromPlaylist(song.id) }
                    )
                }
            }
        }

        // Add Songs Selector Dialog
        if (showAddSongsDialog) {
            val availableSongs = allSongs.filter { s -> playlistSongs.none { it.id == s.id } }
            AlertDialog(
                onDismissRequest = { showAddSongsDialog = false },
                title = { Text("Add Songs to Playlist", color = TextPrimary) },
                text = {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(availableSongs) { song ->
                            SongItem(
                                song = song,
                                onClick = {
                                    onAddSongToPlaylist(song.id)
                                }
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddSongsDialog = false }) {
                        Text("Done", color = PrimaryPurple)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}
