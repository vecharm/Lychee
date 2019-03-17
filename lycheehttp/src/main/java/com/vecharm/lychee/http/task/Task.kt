package com.vecharm.lychee.http.task

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*

open class Task : Serializable {

    @SerializedName("id")
    val id = UUID.randomUUID()

    @SerializedName("createTime")
    var createTime = System.currentTimeMillis()

    @SerializedName("range")
    var range = 0L

    @SerializedName("progress")
    var progress = 0

    @SerializedName("fileName")
    var fileName: String? = null

    @SerializedName("fileSize")
    var fileSize = 0L

    @SerializedName("url")
    var url: String? = null

    @SerializedName("filePath")
    var filePath: String? = null

    open fun setTaskProgress(progress: Int, range: Long) {
        this.progress = progress
        this.range = range

    }

    open fun setPathInfo(url: String, filePath: String) {
        this.url = url
        this.filePath = filePath
    }

    open fun setFileInfo(fileName: String, fileSize: Long) {
        this.fileName = fileName
        this.fileSize = fileSize
    }


}