package com.vecharm.lychee.sample.task

import android.widget.Toast
import com.vecharm.lychee.sample.api.API
import com.vecharm.lychee.sample.ui.App
import com.vecharm.lychee.http.config.defaults.getService
import com.vecharm.lychee.http.config.defaults.request
import com.vecharm.lychee.http.core.bytesRange
import com.vecharm.lychee.http.core.setRange
import com.vecharm.lychee.http.task.DefaultTask
import retrofit2.Call
import java.io.File

class DownloadTask : DefaultTask() {

    override fun onCancel() {
        service?.cancel()
    }

    override fun onResume(url: String, filePath: String) {
        download(url, File(filePath))
    }


    var service: Call<*>? = null

    fun download(url: String, saveFile: File) {
        setPathInfo(url, saveFile.absolutePath)

        service = getService<API>().download(url, range.bytesRange()).request(saveFile.setRange(range)) {
            onUpdateProgress = onUpdate
            onSuccess = { Toast.makeText(App.app, "${id}下载完成", Toast.LENGTH_LONG).show() }
        }
    }
}