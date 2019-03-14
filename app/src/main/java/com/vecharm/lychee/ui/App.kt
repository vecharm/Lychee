package com.vecharm.lychee.ui

import android.app.Application
import com.vecharm.lychee.config.MyCoreConfig
import com.vecharm.lychee.http.core.RequestCore

class App : Application() {
    companion object {
        lateinit var app: App
    }

    override fun onCreate() {
        super.onCreate()
        app = this
        RequestCore.init(MyCoreConfig(this))
    }
}