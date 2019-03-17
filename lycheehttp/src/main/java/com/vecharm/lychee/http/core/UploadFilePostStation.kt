package com.vecharm.lychee.http.core

import java.util.*


/**
 * 用于关联请求回调和文件上传进度的回调
 * 需要注意的是，需要setCallBack执行后才调用registerProgressCallback
 * */
object UploadFilePostStation {

    val map = WeakHashMap<String, ArrayList<UploadFile>>()


    // first be executed
    fun setCallBack(callBackToken: String, callbackFile: UploadFile) {
        val list = map[callBackToken] ?: ArrayList()
        if (!list.contains(callbackFile)) list.add(callbackFile)
        map[callBackToken] = list
    }

    // second
    fun registerProgressCallback(callBackToken: String, listener: ProgressHelper.ProgressListener) {
        map[callBackToken]?.forEach { it.progressListener = listener }
        map[callBackToken]?.clear()
        map.remove(callBackToken)
    }


}