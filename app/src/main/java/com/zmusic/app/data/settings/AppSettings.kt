package com.zmusic.app.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppSettings private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("zmusic_app_settings", Context.MODE_PRIVATE)

    private val _allowLongTracks = MutableStateFlow(prefs.getBoolean(KEY_ALLOW_LONG, false))
    val allowLongTracks: StateFlow<Boolean> = _allowLongTracks.asStateFlow()

    fun setAllowLongTracks(allowed: Boolean) {
        prefs.edit().putBoolean(KEY_ALLOW_LONG, allowed).apply()
        _allowLongTracks.value = allowed
    }

    companion object {
        private const val KEY_ALLOW_LONG = "allow_long_tracks"

        @Volatile
        private var instance: AppSettings? = null

        fun getInstance(context: Context): AppSettings {
            return instance ?: synchronized(this) {
                instance ?: AppSettings(context.applicationContext).also { instance = it }
            }
        }
    }
}
