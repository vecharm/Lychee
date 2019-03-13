package com.vecharm.lychee.http.config.defaults

import com.vecharm.lychee.http.config.interfaces.ResultCallBack
import com.vecharm.lychee.http.config.interfaces.IResponseHandler
import com.vecharm.lychee.http.core.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

val schedulerIO: ExecutorService = Executors.newCachedThreadPool()

inline fun <reified T> getService(): T {
    return RequestCore.getRetrofit().create(T::class.java)
}


inline fun <reified T> Call<T>.request(callBack: ResultCallBack<T>.() -> Unit): Call<T> {
    doRequest(callBack) { ResponseCallBack(it) }
    return this
}

inline fun <reified T> Call<T>.request(saveFile: File, callBackC: ResultCallBack<T>.() -> Unit): Call<T> {
    return request(RandomAccessFile(saveFile, "rwd"), callBackC)
}

inline fun <reified T> Call<T>.request(saveFile: RandomAccessFile, callBack: ResultCallBack<T>.() -> Unit): Call<T> {
    doRequest(callBack) { DownloadResponseCallBack(T::class.java, saveFile, it) }
    return this
}


inline fun <reified T> Call<T>.upload(callBack: ResultCallBack<T>.() -> Unit): Call<T> {
    doRequest(callBack) { ResponseCallBack(it) }.uploadListener(this.toString())
    return this
}

inline fun <reified T> Call<T>.doRequest(callBack: ResultCallBack<T>.() -> Unit, block: (IResponseHandler<T>) -> Callback<T>): ResultCallBack<T> {
//回调
    val callback = ResultCallBack<T>().also { callBack.invoke(it) }
    val handler = RequestCore.getResponseHandler(T::class.java).also { it.attachCallBack(callback) }
    enqueue(block.invoke(handler))
    return callback
}

open class ResponseCallBack<T>(private val handler: IResponseHandler<T>) : Callback<T> {

    var call: Call<T>? = null
    override fun onFailure(call: Call<T>, t: Throwable) {
        this.call = call
        handler.onError(t)
        handler.onCompleted()
    }

    override fun onResponse(call: Call<T>, response: Response<T>) {
        this.call = call
        try {
            val data = response.body()
            if (response.isSuccessful) {
                if (data == null) handler.onError(HttpException(response))
                else onHandler(data)
            } else handler.onError(HttpException(response))

        } catch (t: Throwable) {
            handler.onError(t)
        }
        handler.onCompleted()
    }


    open fun onHandler(data: T) {
        if (call?.isCanceled == true) return
        if (handler.isSucceeded(data)) handler.onSuccess(data)
        else handler.onError(data)
    }
}

class DownloadResponseCallBack<T>(val tClass: Class<T>, val file: RandomAccessFile, val handler: IResponseHandler<T>) :
    ResponseCallBack<T>(handler) {
    override fun onHandler(data: T) {
        if (data is CoreResponseBody) {
            schedulerIO.execute {
                handler.getCallBack()?.also {
                    val bean = it.setDownloadListenerAndTransform(file, data, tClass)
                    runOnUI { super.onHandler(bean) }
                }
            }
        } else super.onHandler(data)
    }
}

fun <T> attachDownloadInfo(data: CoreResponseBody?, tClass: Class<T>): T {
    val bean = tClass.newInstance()
    if (IDownloadBean::class.java.isAssignableFrom(tClass)) {
        data?.downloadInfo?.also { (bean as IDownloadBean).attachDownloadInfo(it) }
    }
    return bean
}

interface IDownloadBean {
    fun attachDownloadInfo(info: CoreResponseBody.DownloadInfo)
}