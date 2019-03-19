package com.vecharm.lychee.http.task


abstract class DefaultTask : SpeedTask() {


    var onUpdate = { fileName: String, currLen: Long, size: Long, speed: Long, progress: Int ->
        this.speed = speed
        setTaskProgress(progress, currLen)
        setFileInfo(fileName, size)
        this.updateUI?.invoke() ?: Unit
    }

    var isCancel = false
        protected set

    open fun cancel() {
        isCancel = true
        onCancel()
    }

    open fun resume() {
        if (!isCancel) return
        val url = url ?: return
        val filePath = filePath ?: return
        isCancel = false
        onResume(url, filePath)
    }

    protected abstract fun onCancel()
    protected abstract fun onResume(url: String, filePath: String)

    open fun toggle(callBack: () -> Unit) {
        if (isCancel) resume() else cancel()
        callBack.invoke()
    }

    open fun cache() {}


}