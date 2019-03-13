package com.vecharm.lychee.sample.rxjava2.api

import com.vecharm.lychee.http.config.interfaces.ResultCallBack
import com.vecharm.lychee.http.config.interfaces.IResponseHandler
import com.vecharm.lychee.http.core.*
import io.reactivex.Observable
import io.reactivex.Observer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.io.RandomAccessFile


inline fun <reified T> Observable<T>.request(callBack: ResultCallBack<T>.() -> Unit): Disposable {
    return doRequest(callBack) { data, _ -> data }
}

inline fun <reified T> Observable<T>.request(saveFile: File, callBackC: ResultCallBack<T>.() -> Unit): Disposable {
    return request(RandomAccessFile(saveFile, "rwd"), callBackC)
}

inline fun <reified T> Observable<T>.request(file: RandomAccessFile?, callBack: ResultCallBack<T>.() -> Unit): Disposable {
    return doRequest(callBack) { data, it -> it.setDownloadListenerAndTransform(file, data, T::class.java) }
}


inline fun <reified T> Observable<T>.upload(callBack: ResultCallBack<T>.() -> Unit): Disposable {
    return doRequest(callBack) { data, it -> it.uploadListener(this.toString());data }

}

inline fun <reified T> Observable<T>.doRequest(callBack: ResultCallBack<T>.() -> Unit, crossinline transformer: (data: T, ResultCallBack<T>) -> T): Disposable {

    //回调
    val callback = ResultCallBack<T>().also { callBack.invoke(it) }

    val handler = RequestCore.getResponseHandler(T::class.java).also { it.attachCallBack(callback) }

    //开始请求
    return this.subscribeOn(Schedulers.io()).map { transformer.invoke(it, callback) }
        .observeOn(AndroidSchedulers.mainThread()).unsubscribeOn(Schedulers.io())
        .subscribeWith(ResponseObserver(handler))
}


class ResponseObserver<T>(private val response: IResponseHandler<T>) : Observer<T>, Disposable {
    override fun isDisposed(): Boolean {
        return disposable?.isDisposed ?: true
    }

    override fun dispose() {
        disposable?.dispose()
        disposable = null
    }

    private var disposable: Disposable? = null
    override fun onSubscribe(d: Disposable) {
        disposable = d
    }

    override fun onNext(responseBean: T) {
        if (response.isSucceeded(responseBean)) response.onSuccess(responseBean)
        else response.onError(responseBean)
    }

    override fun onError(e: Throwable) {
        response.onError(e)
    }

    override fun onComplete() {
        response.onCompleted()
    }

}