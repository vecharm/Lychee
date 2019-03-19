package com.vecharm.lychee.sample.rxjava2.task

import android.widget.Toast
import com.vecharm.lychee.http.config.defaults.getService
import com.vecharm.lychee.http.task.DefaultTask
import com.vecharm.lychee.http.task.SpeedTask
import com.vecharm.lychee.sample.rxjava2.api.API
import com.vecharm.lychee.sample.rxjava2.api.upload
import com.vecharm.lychee.sample.rxjava2.ui.App
import java.io.File

class UploadTask : DefaultTask() {
    override fun onCancel() {}

    override fun onResume(url: String, filePath: String) {}


    fun upload(file: File) {
        getService<API>().upload(file).upload {
            onUpdateProgress = onUpdate
            onSuccess = { Toast.makeText(App.app, "${id}上传完成", Toast.LENGTH_LONG).show() }
        }
    }
}