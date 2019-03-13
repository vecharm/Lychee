package com.vecharm.lychee.sample.rxjava2.ui

import android.app.Application
import com.vecharm.lychee.http.core.RequestCore
import com.vecharm.lychee.sample.rxjava2.config.MyCoreConfig

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