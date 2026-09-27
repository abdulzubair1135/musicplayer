package com.zmusic.app

import android.app.Application
import com.zmusic.app.data.repository.MusicRepository
import com.zmusic.app.player.MusicPlayerController

class ZMusicApp : Application() {

    lateinit var repository: MusicRepository
        private set

    lateinit var playerController: MusicPlayerController
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        repository = MusicRepository(this)
        playerController = MusicPlayerController.getInstance(this)
    }

    companion object {
        lateinit var instance: ZMusicApp
            private set
    }
}
