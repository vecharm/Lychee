package com.vecharm.lychee.http.config.interfaces

import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit


/**
 * 整个请求库的核心配置接口
 * 默认配置
 * @see com.vecharm.lychee.http.config.defaults.DefaultCoreConfig
 * */
interface ICoreConfig {

    /**
     * 初始化Okhttp
     * */
    fun onInitClient(builder: OkHttpClient.Builder): OkHttpClient.Builder

    /**
     * 初始化Retrofit
     * */
    fun onInitRetrofit(builder: Retrofit.Builder): Retrofit.Builder

    /**
     * 获取请求 请求配置 DefaultRequestConfig为默认配置
     * @see com.vecharm.lychee.http.config.defaults.DefaultRequestConfig
     * */
    fun getRequestConfig(): IRequestConfig

    /**
     * 获取返回值处理者 DefaultResponseHandler为默认配置
     * @see com.vecharm.lychee.http.config.defaults.DefaultResponseHandler
     * */
    fun <T> getResponseHandler(tClass: Class<T>): IResponseHandler<T>

}

/**
 *  请求配置接口，默认配置 主要功能有文件上传的配置，添加默认参数，处理参数签名 md5 加密
 * @see com.vecharm.lychee.http.config.defaults.DefaultRequestConfig
 * */
interface IRequestConfig {

    /**
     * 对于post请求的处理
     * @see com.vecharm.lychee.http.config.defaults.DefaultRequestConfig.newPostRequest
     * */
    fun newPostRequest(oldRequest: Request): Request.Builder

    /**
     * 对于get请求的处理
     * @see com.vecharm.lychee.http.config.defaults.DefaultRequestConfig.newGetRequest
     * */
    fun newGetRequest(oldRequest: Request): Request.Builder

    /**
     * 目前只做了post和get的处理，其他的扩展由这个接口配置 默认按照get处理
     * @see com.vecharm.lychee.http.config.defaults.DefaultRequestConfig.newOtherRequest
     * */
    fun newOtherRequest(oldRequest: Request): Request.Builder

    /**
     * 添加默认的头部参数
     * @see com.vecharm.lychee.http.config.defaults.DefaultRequestConfig.addHeaders
     * */
    fun addHeaders(newRequestBuild: Request.Builder, oldRequest: Request)
}

/**
 * 返回值处理者的接口，可以配置不同的返回值处理，但一般情况都是只有一个
 * 其他的返回都是它的子类
 * @see com.vecharm.lychee.http.config.defaults.ResponseBean
 * @see com.vecharm.lychee.http.config.defaults.DefaultResponseHandler
 * */
interface IResponseHandler<T> {

    /**
     * 用于判断是否成功，不同的业务可能有不同的判断方式
     * */
    fun isSucceeded(bean: T): Boolean

    fun onCompleted()

    fun onSuccess(data: T)

    fun onError(data: T)

    fun onError(e: Throwable?)

    /**
     * 请求回调通过这个方法依附在这个处理器
     * */
    fun attachCallBack(callBack: ResultCallBack<T>)

    fun getCallBack(): ResultCallBack<T>?

}

/**
 * 请求的回调
 * */
class ResultCallBack<T> {

    var onCompleted: (() -> Unit)? = null

    var onSuccess: ((data: T) -> Unit)? = null

    var onErrorMessage: ((message: String?) -> Unit)? = null

    var onError: ((errorCode: Int, message: String?) -> Unit)? = null

    var onUpdateProgress: ((fileName: String, currLen: Long, size: Long, speed: Long, progress: Int) -> Unit)? = null

}

/**
 * 参数注解
 * 上传时定义该api的方法参数的文件类型
 * */
@kotlin.annotation.Retention
@kotlin.annotation.Target(AnnotationTarget.VALUE_PARAMETER)
annotation class FileType(
    val value: String = "")

/**
 * 方法注解
 * 上传时定义api的所有文件的类型
 * */
@kotlin.annotation.Retention
@kotlin.annotation.Target(AnnotationTarget.FUNCTION)
annotation class MultiFileType(
    val value: String = "")


/*
* 上传用的注解，使用这个注解，可根据文件的后缀名设置MediaType
* */
@kotlin.annotation.Retention
@kotlin.annotation.Target(AnnotationTarget.FUNCTION)
annotation class Upload(
    val value: String = "downloadRequest")


/**
 *
 * 下载用的注解，下载的api必须用这个
 * */
@kotlin.annotation.Retention
@kotlin.annotation.Target(AnnotationTarget.FUNCTION)
annotation class Download(
    val value: String = "downloadRequest")