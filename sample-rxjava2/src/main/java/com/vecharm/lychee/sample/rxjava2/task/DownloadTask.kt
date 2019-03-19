package com.vecharm.lychee.sample.rxjava2.task

import android.widget.Toast
import com.vecharm.lychee.http.config.defaults.getService
import com.vecharm.lychee.http.core.bytesRange
import com.vecharm.lychee.http.core.setRange
import com.vecharm.lychee.http.task.DefaultTask
import com.vecharm.lychee.http.task.SpeedTask
import com.vecharm.lychee.sample.rxjava2.api.API
import com.vecharm.lychee.sample.rxjava2.ui.App
import com.vecharm.lychee.sample.rxjava2.api.request
import io.reactivex.disposables.Disposable
import java.io.File
import java.io.RandomAccessFile

class DownloadTask : DefaultTask() {

    override fun onCancel() {
        service?.dispose()
    }

    override fun onResume(url: String, filePath: String) {
        download(url, File(filePath))
    }

    var service: Disposable? = null

    fun download(url: String, saveFile: File) {
        setPathInfo(url, saveFile.absolutePath)

        service = getService<API>().download(url, range.bytesRange()).request(saveFile.setRange(range)) {
            onUpdateProgress = onUpdate
            onSuccess = { Toast.makeText(App.app, "${id}下载完成", Toast.LENGTH_LONG).show() }
        }
    }
}