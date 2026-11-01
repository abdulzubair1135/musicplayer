package com.zmusic.app.data.api

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ApiConfig {
    const val LOCAL_URL = "http://127.0.0.1:3000/"
    const val DEFAULT_CLOUD_URL = "https://musicplayer-blyl.onrender.com/"

    @Volatile
    var activeUrl: String = LOCAL_URL

    val BASE_URL: String
        get() = activeUrl

    const val CONNECT_TIMEOUT_SECONDS = 12L
    const val READ_TIMEOUT_SECONDS    = 15L
    const val WRITE_TIMEOUT_SECONDS   = 10L

    private const val PREFS_NAME = "zmusic_api_prefs"
    private const val KEY_CUSTOM_URL = "custom_server_url"

    fun getSavedServerUrl(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_URL, null)?.takeIf { it.isNotBlank() }
    }

    fun saveServerUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val clean = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString(KEY_CUSTOM_URL, clean).apply()
        activeUrl = clean
    }

    fun triggerEagerWakeup() {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        val target = activeUrl.trimEnd('/') + "/api/health"
        val req = Request.Builder().url(target).build()
        client.newCall(req).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                response.close()
            }
        })
    }

    suspend fun checkServerHealth(url: String): Boolean = withContext(Dispatchers.IO) {
        val testClient = OkHttpClient.Builder()
            .connectTimeout(3000, TimeUnit.MILLISECONDS)
            .readTimeout(3000, TimeUnit.MILLISECONDS)
            .build()
        try {
            val target = url.trimEnd('/') + "/api/health"
            val req = Request.Builder().url(target).build()
            val resp = testClient.newCall(req).execute()
            val isOk = resp.isSuccessful && (resp.body?.string()?.contains("ok") == true)
            resp.close()
            isOk
        } catch (_: Exception) {
            false
        }
    }

    suspend fun autoDetectFastestServer(context: Context? = null) = withContext(Dispatchers.IO) {
        // 1. Check custom URL if saved
        if (context != null) {
            val saved = getSavedServerUrl(context)
            if (!saved.isNullOrBlank()) {
                if (checkServerHealth(saved)) {
                    activeUrl = saved
                    return@withContext
                }
            }
        }

        // 2. Check local server (adb reverse tcp:3000 tcp:3000 or same machine)
        if (checkServerHealth(LOCAL_URL)) {
            activeUrl = LOCAL_URL
            return@withContext
        }

        // 3. Fallback to cloud
        activeUrl = DEFAULT_CLOUD_URL
    }
}
