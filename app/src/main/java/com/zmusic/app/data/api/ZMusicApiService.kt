package com.zmusic.app.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ── Retrofit Interface ─────────────────────────────────────────────────────────
interface ZMusicApiService {

    @GET("api/health")
    suspend fun health(): Response<HealthResponse>

    @GET("api/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("limit") limit: Int = 20,
    ): Response<SearchResponse>

    @GET("api/discover")
    suspend fun discover(
        @Query("genre") genre: String? = null,
    ): Response<DiscoverResponse>

    @GET("api/trending")
    suspend fun trending(
        @Query("genre") genre: String? = null,
    ): Response<TrendingResponse>

    @GET("api/radio")
    suspend fun radio(
        @Query("videoId") videoId: String? = null,
        @Query("title") title: String? = null,
        @Query("artist") artist: String? = null,
    ): Response<RadioResponse>

    @GET("api/info/{videoId}")
    suspend fun songInfo(
        @Path("videoId") videoId: String,
    ): Response<SongInfoResponse>

    companion object {
        @Volatile
        private var INSTANCE: ZMusicApiService? = null

        fun getInstance(): ZMusicApiService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: create().also { INSTANCE = it }
            }
        }

        private fun create(): ZMusicApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(ApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .build()

            return Retrofit.Builder()
                .baseUrl(ApiConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ZMusicApiService::class.java)
        }

        /** Build the full streaming URL for a given videoId */
        fun streamUrl(videoId: String): String {
            return "${ApiConfig.BASE_URL.trimEnd('/')}api/stream/$videoId"
        }
    }
}
