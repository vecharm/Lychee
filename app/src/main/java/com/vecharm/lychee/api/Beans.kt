package com.vecharm.lychee.api

import com.google.gson.annotations.SerializedName
import com.vecharm.lychee.http.config.defaults.IDownloadBean
import com.vecharm.lychee.http.config.defaults.ResponseBean
import com.vecharm.lychee.http.core.CoreResponseBody

class ResultBean<T> : ResponseBean() {
    @SerializedName("data")
    var data: T? = null
}

class DownloadBean(override var status: Int = 10000) : ResponseBean(), IDownloadBean {
    var downloadInfo: CoreResponseBody.DownloadInfo? = null
    override fun attachDownloadInfo(info: CoreResponseBody.DownloadInfo) {
        downloadInfo = info
    }
}


class UploadResult(@SerializedName("filename") val fileName: String)
