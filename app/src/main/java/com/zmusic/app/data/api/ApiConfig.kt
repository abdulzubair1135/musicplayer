package com.zmusic.app.data.api

object ApiConfig {
    // ──────────────────────────────────────────────────────────────
    // IMPORTANT: After deploying to Render, replace this URL with
    // your actual Render service URL, e.g.:
    //   https://zmusic-backend.onrender.com
    // For local development (PC and phone on same Wi-Fi):
    //   http://192.168.x.x:3000  (your PC's local IP)
    // ──────────────────────────────────────────────────────────────
    const val BASE_URL = "https://musicplayer-blyl.onrender.com/"

    const val CONNECT_TIMEOUT_SECONDS = 30L
    const val READ_TIMEOUT_SECONDS    = 60L
    const val WRITE_TIMEOUT_SECONDS   = 30L
}
