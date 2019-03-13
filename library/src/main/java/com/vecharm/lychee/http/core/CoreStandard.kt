package com.vecharm.lychee.http.core

import com.vecharm.lychee.http.config.defaults.attachDownloadInfo
import com.vecharm.lychee.http.config.interfaces.ResultCallBack
import okhttp3.internal.Util
import java.io.File
import java.io.RandomAccessFile
import java.lang.Exception
import java.security.MessageDigest


/**
 * 扩展方法，用于断点续传
 * */
fun CoreResponseBody.readData(progressListener: ProgressHelper.ProgressListener?, reader: CoreResponseBody.IBytesReader) {
    read(progressListener, reader)
}

/**
 * 扩展方法，将CoreResponseBody转成file
 * */
fun CoreResponseBody.saveFile(file: File, progressListener: ProgressHelper.ProgressListener?) {
    saveRandomAccessFile(RandomAccessFile(file, "rwd"), progressListener)
}

/**
 * 扩展方法，用于断点续传
 * */
fun CoreResponseBody.saveRandomAccessFile(file: RandomAccessFile, progressListener: ProgressHelper.ProgressListener?) {
    readData(progressListener, object : CoreResponseBody.IBytesReader {
        override fun onUpdate(sink: ByteArray, len: Int): Int {
            file.write(sink, 0, len)
            return len
        }

        override fun onClose() {
            Util.closeQuietly(file)
        }
    })
}


/**
 * 注册上传监听器
 * @param token 返回值的实例
 * 也可以自己实现
 * @see UploadFilePostStation.registerProgressCallback
 * */
fun <T> ResultCallBack<T>.uploadListener(token: String) {
    UploadFilePostStation.registerProgressCallback(token, object : ProgressHelper.ProgressListener {
        override fun onUpdate(fileName: String, currLen: Long, size: Long, speed: Long, progress: Int) {
            //将回调进度的回调给使用者
            runOnUI { onUpdateProgress?.invoke(fileName, currLen, size, speed, progress) }
        }
    })
}

/**
 * 设置下载监听器
 * @param data CoreResponseBody
 * 也可以自己实现
 * @see CoreResponseBody.saveRandomAccessFile
 * */
fun <T> ResultCallBack<T>.setDownloadListener(saveFile: File, data: CoreResponseBody) {
    setDownloadListener(RandomAccessFile(saveFile, "rwd"), data)
}

/**
 * 设置下载监听器
 * @param data CoreResponseBody
 * 也可以自己实现
 * @see CoreResponseBody.saveRandomAccessFile
 * */
fun <T> ResultCallBack<T>.setDownloadListener(saveFile: RandomAccessFile, data: CoreResponseBody?) {
    data?.saveRandomAccessFile(saveFile, object : ProgressHelper.ProgressListener {
        override fun onUpdate(fileName: String, currLen: Long, size: Long, speed: Long, progress: Int) {
            runOnUI { onUpdateProgress?.invoke(fileName, currLen, size, speed, progress) }
        }
    })
}

/**
 * 设置下载监听器 和 转换 IDownloadBean
 * @see com.vecharm.lychee.http.config.defaults.IDownloadBean
 * */
fun <T> ResultCallBack<T>.setDownloadListenerAndTransform(saveFile: RandomAccessFile?, data: T, tClass: Class<T>): T {
    if (saveFile != null) this.setDownloadListener(saveFile, data as? CoreResponseBody)
    return attachDownloadInfo(data as? CoreResponseBody, tClass)
}


fun Long.bytesRange() = "bytes=$this-"

fun runOnUI(block: () -> Unit) {
    RequestCore.runOnUI(Runnable { block.invoke() })
}

fun md5(srcStr: String): String {
    val hash: ByteArray
    return try {
        hash = MessageDigest.getInstance("MD5").digest(srcStr.toByteArray(Charsets.UTF_8))
        hash.joinToString("") { String.format("%02x", it) }
    } catch (e: Exception) {
        "MD5加密异常:${e.message}"
    }
}


