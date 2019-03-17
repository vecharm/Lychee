package com.vecharm.lychee.http.config.defaults

import com.vecharm.lychee.http.config.interfaces.ResultCallBack
import com.vecharm.lychee.http.config.interfaces.IResponseHandler
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 提供默认的返回值处理器，可以继承这个类，在这个基础上修改，也可以自己继承接口按照自己的想法实现
 * */
open class DefaultResponseHandler : IResponseHandler<ResponseBean> {

    enum class HttpStatus(val CODE: Int) {
        //未知错误
        UNKNOWN_ERROR(12001),
        //连接超市
        SOCKET_TIME_OUT(12002),
        //连接错误
        CONNECT_ERROR(12029),
    }

    var callback: ResultCallBack<ResponseBean>? = null
    override fun attachCallBack(callBack: ResultCallBack<ResponseBean>) {
        this.callback = callBack
    }

    override fun getCallBack() = callback

    /**
     * 默认返回10000为成功
     * */
    override fun isSucceeded(bean: ResponseBean) = bean.status == 10000

    override fun onCompleted() {
        callback?.onCompleted?.invoke()
    }

    override fun onSuccess(data: ResponseBean) {
        callback?.onSuccess?.invoke(data)
    }

    override fun onError(data: ResponseBean) {
        onError(data.status, data.desc)
    }

    override fun onError(e: Throwable?) {
        e?.printStackTrace()
        when (e) {
            is UnknownHostException -> onError(HttpStatus.UNKNOWN_ERROR.CODE, e.message)
            is ConnectException -> onError(HttpStatus.CONNECT_ERROR.CODE, e.message)
            is SocketTimeoutException -> onError(HttpStatus.SOCKET_TIME_OUT.CODE, e.message)
            is retrofit2.HttpException -> onError(e.code(), e.message())
            else -> onError(HttpStatus.UNKNOWN_ERROR.CODE, e?.message)
        }
        callback?.onErrorMessage?.invoke(e?.message)
        callback?.onCompleted?.invoke()
    }


    open fun onError(status: Int, message: String?) {
        callback?.onError?.invoke(status, message)
        callback?.onErrorMessage?.invoke(message)
    }
}