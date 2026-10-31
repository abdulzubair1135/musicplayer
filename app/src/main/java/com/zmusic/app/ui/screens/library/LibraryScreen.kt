package com.zmusic.app.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zmusic.app.data.model.Playlist
import com.zmusic.app.data.model.Song
import com.zmusic.app.data.organizer.LocalMusicOrganizer
import com.zmusic.app.data.organizer.MusicCategory
import com.zmusic.app.ui.components.SongItem
import com.zmusic.app.ui.theme.PrimaryPurple
import com.zmusic.app.ui.theme.SecondaryCyan
import com.zmusic.app.ui.theme.SurfaceDark
import com.zmusic.app.ui.theme.SurfaceVariantDark
import com.zmusic.app.ui.theme.TextMuted
import com.zmusic.app.ui.theme.TextPrimary
import com.zmusic.app.ui.theme.TextSecondary

@Composable
fun LibraryScreen(
    playlists: List<Playlist>,
    favoriteSongs: List<Song>,
    allSongs: List<Song>,
    currentSong: Song?,
    onCreatePlaylist: (String) -> Unit,
    onDeletePlaylist: (Long) -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onStartRadio: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onSelectPlaylist: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    // Run the Local Organizer Bot on the device songs
    val organized = remember(allSongs) { LocalMusicOrganizer.organize(allSongs) }
    var selectedCategory by remember { mutableStateOf(MusicCategory.PHONK) }

    val activeSongList = remember(selectedCategory, organized, favoriteSongs) {
        when (selectedCategory) {
            MusicCategory.PHONK -> organized.phonkTracks
            MusicCategory.BOLLYWOOD -> organized.bollywoodTracks
            MusicCategory.ENGLISH -> organized.englishTracks
            MusicCategory.CALL_RECORDINGS -> organized.callRecordings
            MusicCategory.DOWNLOADED -> organized.downloadedTracks.ifEmpty { favoriteSongs }
            MusicCategory.ALL_MUSIC -> organized.allCleanMusic
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Local Library",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)
                )
            }

            // 🤖 AI Organizer Bot Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Brush.linearGradient(listOf(PrimaryPurple, SecondaryCyan))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SmartToy, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "AI Organizer Bot",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryCyan
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(PrimaryPurple.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("Active", fontSize = 10.sp, color = PrimaryPurple, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "${organized.musicTracks} Music Tracks Organized",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                "Quarantined ${organized.quarantinedCount} call & voice notes from songs",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            // Category Filter Pills
            item {
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(MusicCategory.values()) { category ->
                        val isSelected = category == selectedCategory
                        val count = when (category) {
                            MusicCategory.PHONK -> organized.phonkTracks.size
                            MusicCategory.BOLLYWOOD -> organized.bollywoodTracks.size
                            MusicCategory.ENGLISH -> organized.englishTracks.size
                            MusicCategory.CALL_RECORDINGS -> organized.callRecordings.size
                            MusicCategory.DOWNLOADED -> organized.downloadedTracks.size.let { if (it > 0) it else favoriteSongs.size }
                            MusicCategory.ALL_MUSIC -> organized.allCleanMusic.size
                        }

                        val bgColor = if (isSelected) PrimaryPurple else SurfaceVariantDark
                        val textColor = if (isSelected) Color.White else TextSecondary

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(bgColor)
                                .clickable { selectedCategory = category }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.iconEmoji, fontSize = 13.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                category.title,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "($count)",
                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Quarantined Alert if Call Recordings tab selected
            if (selectedCategory == MusicCategory.CALL_RECORDINGS) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E1A1A)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "These audio recordings are isolated from normal music queues so they never play during random shuffle.",
                                fontSize = 12.sp,
                                color = Color(0xFFFF8A80)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Songs List for Selected Category
            if (activeSongList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No songs found in ${selectedCategory.title}",
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(activeSongList) { song ->
                    SongItem(
                        song = song,
                        isCurrentPlaying = song.id == currentSong?.id,
                        onClick = { onPlaySong(song, activeSongList) },
                        onStartRadio = { onStartRadio(song) },
                        onToggleFavorite = { onToggleFavorite(song) }
                    )
                }
            }
        }
    }
}
