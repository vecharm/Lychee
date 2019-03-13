package com.vecharm.lychee.task

import android.widget.Toast
import com.vecharm.lychee.api.API
import com.vecharm.lychee.http.config.defaults.getService
import com.vecharm.lychee.http.config.defaults.upload
import com.vecharm.lychee.ui.App
import com.vecharm.lychee.http.task.SpeedTask
import java.io.File

class UploadTask : SpeedTask() {


    var onUpdate = { fileName: String, currLen: Long, size: Long, speed: Long, progress: Int ->
        this.speed = speed
        setTaskProgress(progress, currLen)
        setFileInfo(fileName, size)
        this.updateUI?.invoke() ?: Unit
    }


    fun cache() {
        //todo 将任务信息保存到本地
    }


    fun upload(file: File) {
        getService<API>().uploadMap(hashMapOf("file" to file)).upload {
            onUpdateProgress = onUpdate
            onSuccess = { Toast.makeText(App.app, "${id}上传完成", Toast.LENGTH_LONG).show() }
        }
    }
}