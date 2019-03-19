package com.vecharm.lychee.sample.rxjava.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.vecharm.lychee.http.config.defaults.getService
import com.vecharm.lychee.http.config.defaults.request
import com.vecharm.lychee.http.task.SpeedTask
import com.vecharm.lychee.sample.rxjava.R
import com.vecharm.lychee.sample.rxjava.api.API
import com.vecharm.lychee.sample.rxjava.api.request
import com.vecharm.lychee.sample.rxjava.task.DownloadTask
import com.vecharm.lychee.sample.rxjava.task.UploadTask
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getService<API>().hello().request {
            onSuccess = { Toast.makeText(App.app, it.data ?: "", Toast.LENGTH_SHORT).show() }
        }

        getService<API>().download().request(File(App.app.externalCacheDir, "qq.apk")) {
            onSuccess = { Toast.makeText(App.app, "${it.downloadInfo?.fileName} 下载完成", Toast.LENGTH_SHORT).show() }
        }

        val adapter = object : BaseQuickAdapter<SpeedTask, BaseViewHolder>(R.layout.main_item_progress) {
            override fun convert(helper: BaseViewHolder, item: SpeedTask) {

                helper.getView<ToggleButton>(R.id.taskState).also {
                    if (item is DownloadTask) {
                        it.visibility = View.VISIBLE
                        it.isChecked = !item.isCancel
                        it.setOnCheckedChangeListener { _, _ -> item.toggle { } }
                    } else {
                        it.visibility = View.GONE
                    }
                }
                item.updateUI = {
                    helper.getView<TextView>(R.id.taskName).text = "任务:${item.id}"
                    helper.getView<TextView>(R.id.speed).text = "${item.getSpeed()}"
                    helper.getView<TextView>(R.id.progress).text = "${item.progress}%"
                    helper.getView<ProgressBar>(R.id.progressBar).also {
                        it.max = 100
                        it.progress = item.progress
                    }
                }
            }

        }

        addDownloadTaskButton.setOnClickListener {
            val downloadTask = DownloadTask()
            val file = File(App.app.externalCacheDir, "qq${adapter.data.size + 1}.apk")
            downloadTask.download("https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk", file)
            adapter.addData(downloadTask)

        }

        addUploadTaskButton.setOnClickListener {
            val uploadTask = UploadTask()
            uploadTask.upload(File(App.app.externalCacheDir, "qq.apk"))
            adapter.addData(uploadTask)
        }



        listView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        listView.adapter = adapter
    }
}
