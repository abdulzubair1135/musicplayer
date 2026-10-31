package com.zmusic.app.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ApiConfig {
    // Local server (adb reverse or same Wi-Fi) - instant playback, no YouTube restrictions
    const val LOCAL_URL = "http://127.0.0.1:3000/"
    // Cloud server on Render
    const val RENDER_URL = "https://musicplayer-blyl.onrender.com/"

    @Volatile
    var activeUrl: String = LOCAL_URL

    val BASE_URL: String
        get() = activeUrl

    const val CONNECT_TIMEOUT_SECONDS = 45L
    const val READ_TIMEOUT_SECONDS    = 60L
    const val WRITE_TIMEOUT_SECONDS   = 30L

    fun triggerEagerWakeup() {
        val client = OkHttpClient.Builder()
            .connectTimeout(50, TimeUnit.SECONDS)
            .readTimeout(50, TimeUnit.SECONDS)
            .build()
        val req = Request.Builder().url("${RENDER_URL}api/health").build()
        client.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.close()
            }
        })
    }

    suspend fun autoDetectFastestServer() = withContext(Dispatchers.IO) {
        val testClient = OkHttpClient.Builder()
            .connectTimeout(1500, TimeUnit.MILLISECONDS)
            .readTimeout(1500, TimeUnit.MILLISECONDS)
            .build()
        try {
            val req = Request.Builder().url("${LOCAL_URL}api/health").build()
            val resp = testClient.newCall(req).execute()
            if (resp.isSuccessful) {
                activeUrl = LOCAL_URL
                return@withContext
            }
        } catch (_: Exception) {}
        activeUrl = RENDER_URL
    }
}
