package com.vecharm.lychee.sample.rxjava.api

import com.vecharm.lychee.http.config.interfaces.ResultCallBack
import com.vecharm.lychee.http.config.interfaces.IResponseHandler
import com.vecharm.lychee.http.core.*
import rx.Observable
import rx.Observer
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.io.File
import java.io.RandomAccessFile

inline fun <reified T> Observable<T>.request(callBack: ResultCallBack<T>.() -> Unit): Subscription {
    return doRequest(callBack) { data, _ -> data }
}

inline fun <reified T> Observable<T>.request(saveFile: File, callBackC: ResultCallBack<T>.() -> Unit): Subscription {
    return request(RandomAccessFile(saveFile, "rwd"), callBackC)
}

inline fun <reified T> Observable<T>.request(file: RandomAccessFile?, callBack: ResultCallBack<T>.() -> Unit): Subscription {
    return doRequest(callBack) { data, it -> it.setDownloadListenerAndTransform(file, data, T::class.java) }
}


inline fun <reified T> Observable<T>.upload(callBack: ResultCallBack<T>.() -> Unit): Subscription {
    return doRequest(callBack) { data, it -> it.uploadListener(this.toString());data }
}


inline fun <reified T> Observable<T>.doRequest(callBack: ResultCallBack<T>.() -> Unit, crossinline transformer: (data: T, ResultCallBack<T>) -> T): Subscription {
    //回调
    val callback = ResultCallBack<T>().also { callBack.invoke(it) }
    val handler = LycheeHttp.getResponseHandler(T::class.java).also { it.attachCallBack(callback) }
    //开始请求
    return this.subscribeOn(Schedulers.io()).map { transformer.invoke(it, callback) }
        .observeOn(AndroidSchedulers.mainThread()).unsubscribeOn(Schedulers.io()).subscribe(ResponseObserver(handler))
}


class ResponseObserver<T>(private val response: IResponseHandler<T>) : Observer<T> {

    override fun onNext(responseBean: T) {
        if (response.isSucceeded(responseBean)) response.onSuccess(responseBean)
        else response.onError(responseBean)
    }

    override fun onError(e: Throwable?) {
        response.onError(e)
    }

    override fun onCompleted() {
        response.onCompleted()
    }

}