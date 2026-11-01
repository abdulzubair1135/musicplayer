package com.zmusic.app.ui.screens.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zmusic.app.data.model.Song
import com.zmusic.app.ui.components.SongTagBadge
import com.zmusic.app.ui.components.SongTagHelper
import com.zmusic.app.ui.theme.PrimaryPurple
import com.zmusic.app.ui.theme.PrimaryPurpleVariant
import com.zmusic.app.ui.theme.SecondaryCyan
import com.zmusic.app.ui.theme.SurfaceDark
import com.zmusic.app.ui.theme.SurfaceVariantDark
import com.zmusic.app.ui.theme.TextMuted
import com.zmusic.app.ui.theme.TextPrimary
import com.zmusic.app.ui.theme.TextSecondary

data class ArtistCardData(
    val name: String,
    val imageUrl: String,
    val searchQuery: String
)

val curatedPopularArtists = listOf(
    ArtistCardData("Arijit Singh", "https://i.ytimg.com/vi/VAdGW7QDJiU/hqdefault.jpg", "Arijit Singh"),
    ArtistCardData("Diljit Dosanjh", "https://i.ytimg.com/vi/cl0a3i2wFcc/hqdefault.jpg", "Diljit Dosanjh"),
    ArtistCardData("The Weeknd", "https://i.ytimg.com/vi/4NRXx6U8ABQ/hqdefault.jpg", "The Weeknd"),
    ArtistCardData("Taylor Swift", "https://i.ytimg.com/vi/ic8j13piAhQ/hqdefault.jpg", "Taylor Swift"),
    ArtistCardData("Ed Sheeran", "https://i.ytimg.com/vi/JGwWNGJdvx8/hqdefault.jpg", "Ed Sheeran"),
    ArtistCardData("Billie Eilish", "https://i.ytimg.com/vi/d5gf9dXbPi0/hqdefault.jpg", "Billie Eilish"),
    ArtistCardData("AP Dhillon", "https://i.ytimg.com/vi/VNs_cCtdbPc/hqdefault.jpg", "AP Dhillon"),
    ArtistCardData("Shreya Ghoshal", "https://i.ytimg.com/vi/zRtPUIumXcY/hqdefault.jpg", "Shreya Ghoshal"),
    ArtistCardData("Sidhu Moose Wala", "https://i.ytimg.com/vi/NvrIwnvTLHM/hqdefault.jpg", "Sidhu Moose Wala"),
    ArtistCardData("Alan Walker", "https://i.ytimg.com/vi/60ItHLz5WEA/hqdefault.jpg", "Alan Walker"),
    ArtistCardData("Atif Aslam", "https://i.ytimg.com/vi/SAcpESN_Fk4/hqdefault.jpg", "Atif Aslam"),
    ArtistCardData("Justin Bieber", "https://i.ytimg.com/vi/tQ0yjYUFKAE/hqdefault.jpg", "Justin Bieber")
)

@Composable
fun HomeScreen(
    recentlyPlayed: List<Song>,
    onlineDiscover: List<Song>,
    onlineTrending: List<Song>,
    artists: List<String>,
    isNetworkAvailable: Boolean,
    onPlaySong: (Song, List<Song>) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // App Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Z-Music",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Online Stream & Discovery",
                            fontSize = 13.sp,
                            color = PrimaryPurpleVariant
                        )
                    }
                }
            }

            // Offline Banner (if no internet)
            if (!isNetworkAvailable) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.WifiOff, null, tint = SecondaryCyan, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "You are currently offline. Check Library tab for your local songs.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else if (onlineDiscover.isEmpty() && onlineTrending.isEmpty()) {
                // Loading banner when fetching online music
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 20.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = PrimaryPurple,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Loading trending music…", color = TextSecondary, fontSize = 14.sp)
                    }
                }
            }

            // Hero Banner: "You May Like"
            val heroSong = onlineDiscover.firstOrNull() ?: onlineTrending.firstOrNull()
            if (heroSong != null) {
                item {
                    HeroYouMayLikeCard(
                        song = heroSong,
                        onClick = { onPlaySong(heroSong, onlineDiscover + onlineTrending) }
                    )
                }
            }

            // 🔥 Trending Right Now
            if (onlineTrending.isNotEmpty()) {
                item {
                    HomeSectionHeader(title = "Trending Right Now", icon = Icons.Default.TrendingUp)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(onlineTrending) { song ->
                            OnlineSongCard(
                                song = song,
                                onClick = { onPlaySong(song, onlineTrending) }
                            )
                        }
                    }
                }
            }

            // 🏆 Best of This Month / Discover
            if (onlineDiscover.isNotEmpty()) {
                item {
                    HomeSectionHeader(title = "Best of This Month", icon = Icons.Default.EmojiEvents)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(onlineDiscover) { song ->
                            OnlineSongCard(
                                song = song,
                                onClick = { onPlaySong(song, onlineDiscover) }
                            )
                        }
                    }
                }
            }

            // Recently Played Online
            if (recentlyPlayed.isNotEmpty()) {
                item {
                    HomeSectionHeader(title = "Jump Back In", icon = Icons.Default.AutoAwesome)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(recentlyPlayed.take(8)) { song ->
                            OnlineSongCard(
                                song = song,
                                onClick = { onPlaySong(song, recentlyPlayed) }
                            )
                        }
                    }
                }
            }

            // Popular Artists (Global & Trending Online Stars with Photos)
            item {
                HomeSectionHeader(title = "Popular Artists", icon = Icons.Default.Person)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(curatedPopularArtists) { artist ->
                        ArtistCircle(artist = artist, onClick = { onNavigateToArtist(artist.searchQuery) })
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroYouMayLikeCard(song: Song, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = song.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.92f),
                                Color.Black.copy(alpha = 0.45f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
                            tint = SecondaryCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "YOU MAY LIKE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryCyan,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = song.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(6.dp))
                    val heroTags = remember(song) { SongTagHelper.getTags(song) }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        heroTags.take(3).forEach { tag ->
                            SongTagBadge(tag = tag, fontSize = 9.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(30.dp))
                        .background(PrimaryPurple)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Play Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = PrimaryPurple, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

@Composable
private fun OnlineSongCard(song: Song, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Card(
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.size(140.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = song.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                val primaryTag = remember(song) { SongTagHelper.getTags(song).firstOrNull() }
                if (primaryTag != null) {
                    SongTagBadge(
                        tag = primaryTag,
                        fontSize = 8.sp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = song.title,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = song.artist,
            color = TextSecondary,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ArtistCircle(artist: ArtistCardData, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(82.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(PrimaryPurple, SecondaryCyan))),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = artist.imageUrl,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = artist.name,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
