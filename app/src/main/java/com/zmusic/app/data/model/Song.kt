package com.zmusic.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SourceType {
    LOCAL,
    ONLINE
}

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long = 0,
    val album: String,
    val albumId: Long = 0,
    val duration: Long,
    val contentUri: String,
    val artworkUri: String?,
    val genre: String = "General",
    val language: String = "English",
    val year: Int = 0,
    val sourceType: SourceType = SourceType.LOCAL,
    val streamUri: String? = null,
    val dateAdded: Long = System.currentTimeMillis()
)

data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val artworkUri: String?,
    val songCount: Int
)

data class Genre(
    val name: String,
    val songCount: Int
)
