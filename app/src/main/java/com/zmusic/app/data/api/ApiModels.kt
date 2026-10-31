package com.zmusic.app.data.api

import com.google.gson.annotations.SerializedName

// ── API Response Models ────────────────────────────────────────────────────────

data class ApiSong(
    @SerializedName("videoId")          val videoId: String,
    @SerializedName("title")            val title: String,
    @SerializedName("artist")           val artist: String,
    @SerializedName("album")            val album: String = "YouTube",
    @SerializedName("duration")         val duration: Long = 0L,         // ms
    @SerializedName("durationFormatted") val durationFormatted: String = "0:00",
    @SerializedName("thumbnail")        val thumbnail: String = "",
    @SerializedName("genre")            val genre: String = "Online",
    @SerializedName("sourceType")       val sourceType: String = "YOUTUBE",
)

data class SearchResponse(
    @SerializedName("results") val results: List<ApiSong>,
    @SerializedName("total")   val total: Int = 0,
    @SerializedName("query")   val query: String = "",
)

data class DiscoverResponse(
    @SerializedName("results") val results: List<ApiSong>,
)

data class TrendingResponse(
    @SerializedName("results") val results: List<ApiSong>,
    @SerializedName("genre")   val genre: String = "",
)

data class RadioResponse(
    @SerializedName("results") val results: List<ApiSong>,
)

data class SongInfoResponse(
    @SerializedName("videoId")   val videoId: String,
    @SerializedName("title")     val title: String,
    @SerializedName("artist")    val artist: String,
    @SerializedName("duration")  val duration: Long,
    @SerializedName("thumbnail") val thumbnail: String,
    @SerializedName("streamUrl") val streamUrl: String,
    @SerializedName("bitrate")   val bitrate: Int = 0,
)

data class HealthResponse(
    @SerializedName("status")    val status: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("version")   val version: String,
)
