package com.vecharm.lychee.sample.ui

import android.app.Application
import com.vecharm.lychee.sample.config.MyCoreConfig
import com.vecharm.lychee.http.core.LycheeHttp

class App : Application() {
    companion object {
        lateinit var app: App
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        LycheeHttp.init(MyCoreConfig(this))
    }
}