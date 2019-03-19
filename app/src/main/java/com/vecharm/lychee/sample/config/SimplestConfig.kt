package com.vecharm.lychee.sample.config

import com.vecharm.lychee.http.config.defaults.DefaultCoreConfig
import com.vecharm.lychee.http.config.defaults.DefaultRequestConfig

class SimplestConfig:DefaultCoreConfig() {
    override fun getHostString() = "https://host:port/"

    override fun isShowLog(): Boolean {
        return super.isShowLog()
    }

    class RequestConfig:DefaultRequestConfig(){




    }

}