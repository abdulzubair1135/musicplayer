package com.zmusic.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zmusic.app.data.model.Song
import com.zmusic.app.data.model.SourceType

data class SongTag(
    val label: String,
    val containerColor: Color,
    val contentColor: Color
)

object SongTagHelper {

    fun getTags(song: Song): List<SongTag> {
        val tags = mutableListOf<SongTag>()
        val text = "${song.title} ${song.artist} ${song.genre}".lowercase()

        // 1. Detect Genre & Mood Tags
        when {
            text.contains("phonk") || text.contains("drift") || text.contains("sigma") || text.contains("brazilian") -> {
                tags.add(SongTag("🎧 Phonk", Color(0xFFEF4444).copy(alpha = 0.22f), Color(0xFFF87171)))
            }
            text.contains("arijit") || text.contains("t-series") || text.contains("bollywood") ||
            text.contains("pritam") || text.contains("shreya") || text.contains("jubin") ||
            text.contains("badshah") || text.contains("neha kakkar") || text.contains("kumar sanu") ||
            text.contains("udit narayan") || text.contains("alka yagnik") || text.contains("sonu nigam") ||
            text.contains("ranbir") || text.contains("shraddha") || text.contains("sushant") ||
            text.contains("zeemusic") || text.contains("yrf") -> {
                tags.add(SongTag("🎬 Bollywood", Color(0xFF8B5CF6).copy(alpha = 0.22f), Color(0xFFA78BFA)))
            }
            text.contains("punjabi") || text.contains("sidhu") || text.contains("moose") ||
            text.contains("dhillon") || text.contains("diljit") || text.contains("karan aujla") ||
            text.contains("shubh") || text.contains("honey singh") || text.contains("harnoor") ||
            text.contains("amrit maan") || text.contains("b praak") || text.contains("jassi") -> {
                tags.add(SongTag("🌾 Punjabi", Color(0xFFF59E0B).copy(alpha = 0.22f), Color(0xFFFBBF24)))
            }
            text.contains("sad") || text.contains("dard") || text.contains("broken") ||
            text.contains("alone") || text.contains("crying") || text.contains("heartbreak") ||
            text.contains("khairiyat") || text.contains("channa mereya") || text.contains("judai") ||
            text.contains("tuta") || text.contains("agar tum saath ho") -> {
                tags.add(SongTag("💔 Sad Vibe", Color(0xFF3B82F6).copy(alpha = 0.22f), Color(0xFF60A5FA)))
            }
            text.contains("romantic") || text.contains("love") || text.contains("ishq") ||
            text.contains("pyaar") || text.contains("mohabbat") || text.contains("humsafar") ||
            text.contains("tum hi ho") || text.contains("kesariya") || text.contains("raataan") -> {
                tags.add(SongTag("❤️ Romantic", Color(0xFFEC4899).copy(alpha = 0.22f), Color(0xFFF472B6)))
            }
            text.contains("lofi") || text.contains("lo-fi") || text.contains("chill") ||
            text.contains("slowed") || text.contains("reverb") || text.contains("midnight") -> {
                tags.add(SongTag("🌙 Lo-Fi", Color(0xFF10B981).copy(alpha = 0.22f), Color(0xFF34D399)))
            }
            text.contains("hip hop") || text.contains("hip-hop") || text.contains("rap") ||
            text.contains("emiway") || text.contains("divine") || text.contains("raftaar") ||
            text.contains("kr\$na") || text.contains("mc stan") || text.contains("drake") ||
            text.contains("eminem") || text.contains("travis") -> {
                tags.add(SongTag("🎤 Hip-Hop", Color(0xFFF97316).copy(alpha = 0.22f), Color(0xFFFB923C)))
            }
            text.contains("party") || text.contains("club") || text.contains("dj") ||
            text.contains("remix") || text.contains("dance") || text.contains("bhangra") -> {
                tags.add(SongTag("⚡ Party", Color(0xFF06B6D4).copy(alpha = 0.22f), Color(0xFF22D3EE)))
            }
            text.contains("bhajan") || text.contains("aarti") || text.contains("krishna") ||
            text.contains("ram") || text.contains("shiva") || text.contains("hanuman") || text.contains("bhakti") -> {
                tags.add(SongTag("🕉️ Devotional", Color(0xFFEAB308).copy(alpha = 0.22f), Color(0xFFFDE047)))
            }
            text.contains("taylor") || text.contains("weeknd") || text.contains("bieber") ||
            text.contains("alan walker") || text.contains("sheeran") || text.contains("pop") ||
            text.contains("billie") || text.contains("dua lipa") -> {
                tags.add(SongTag("🌍 Pop", Color(0xFF06B6D4).copy(alpha = 0.22f), Color(0xFF22D3EE)))
            }
            song.genre.isNotBlank() && song.genre != "General" && song.genre != "Online" -> {
                tags.add(SongTag("🏷️ ${song.genre}", Color(0xFF8B5CF6).copy(alpha = 0.22f), Color(0xFFA78BFA)))
            }
            else -> {
                tags.add(SongTag("🎵 Trending", Color(0xFF8B5CF6).copy(alpha = 0.22f), Color(0xFFA78BFA)))
            }
        }

        // 2. Audio Quality Tag
        tags.add(SongTag("⚡ 320k HD", Color(0xFF06B6D4).copy(alpha = 0.18f), Color(0xFF38BDF8)))

        // 3. Source Tag
        when (song.sourceType) {
            SourceType.DOWNLOADED -> tags.add(SongTag("📥 Offline", Color(0xFF10B981).copy(alpha = 0.20f), Color(0xFF4ADE80)))
            SourceType.ONLINE -> tags.add(SongTag("🌐 YouTube", Color(0xFFEF4444).copy(alpha = 0.18f), Color(0xFFF87171)))
            SourceType.LOCAL -> tags.add(SongTag("💾 Local", Color(0xFF64748B).copy(alpha = 0.22f), Color(0xFF94A3B8)))
        }

        return tags
    }
}

@Composable
fun SongTagBadge(
    tag: SongTag,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 10.sp
) {
    Surface(
        modifier = modifier,
        color = tag.containerColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = tag.label,
            color = tag.contentColor,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
