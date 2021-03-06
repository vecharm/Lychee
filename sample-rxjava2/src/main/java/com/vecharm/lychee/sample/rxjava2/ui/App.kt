package com.vecharm.lychee.sample.rxjava2.ui

import android.app.Application
import com.vecharm.lychee.http.core.LycheeHttp
import com.vecharm.lychee.sample.rxjava2.config.MyCoreConfig

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