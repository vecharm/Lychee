package com.vecharm.lychee.http.config.defaults

import android.util.Log
import com.vecharm.lychee.http.config.interfaces.ICoreConfig
import com.vecharm.lychee.http.config.interfaces.IRequestConfig
import com.vecharm.lychee.http.config.interfaces.IResponseHandler
import com.vecharm.lychee.http.core.CoreInterceptor
import com.vecharm.lychee.http.log.NewHttpLoggingInterceptor
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 提供默认的配置
 * 可以继承这个类，在这个基础上修改，也可以自己继承接口按照自己的想法实现
 * */
abstract class DefaultCoreConfig : ICoreConfig {

    /**
     * 方法返回值的处理
     * @see DefaultResponseHandler
     * */
    private val responseHandles = HashMap<Class<*>, Class<out IResponseHandler<*>>>()

    /**
     * 方法返回值的处理
     * @see DefaultResponseHandler
     * */
    override fun onInitClient(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        builder.connectTimeout(connectTimeout().toLong(), TimeUnit.SECONDS)
            .readTimeout(readTimeout().toLong(), TimeUnit.SECONDS)
            .writeTimeout(writeTimeout().toLong(), TimeUnit.SECONDS)
            .retryOnConnectionFailure(retryOnConnectionFailure())
        getCookieJar()?.also { builder.cookieJar(it) }
        getLogInterceptor()?.also { builder.addInterceptor(it) }
        return builder
    }

    override fun onInitRetrofit(builder: Retrofit.Builder): Retrofit.Builder {
        getHostString()?.also { builder.baseUrl(it) }
        getHostHttpUrl()?.also { builder.baseUrl(it) }
        getGsonConverterFactory()?.also { builder.addConverterFactory(it) }
        return builder
    }

    override fun getRequestConfig() = DefaultRequestConfig()


    /**
     * cookie缓存，默认为null
     * */
    open fun getCookieJar(): CookieJar? = null


    /**
     * 日志配置，建议使用
     * @see NewHttpLoggingInterceptor,该拦截器，处理了下载时，日志预先读取，导致进度延迟的现象。
     * 如果不需要日志，返回null即可
     * 如果需要自己写配置，
     * @see NewHttpLoggingInterceptor.doResponse
     * */
    open fun getLogInterceptor(): Interceptor? =
        NewHttpLoggingInterceptor(NewHttpLoggingInterceptor.Logger { Log.e("httpRequest", it) }).also {
            it.level = NewHttpLoggingInterceptor.Level.BODY
        }


    /**
     * 添加返回值处理者
     * */
    fun <T> registerResponseHandler(beanClass: Class<T>, handlerClass: Class<out IResponseHandler<T>>) {
        responseHandles[beanClass] = handlerClass
    }

    /**
     * 该方法必须返回处理器的新实例，不可共用一个
     * @see IResponseHandler.attachCallBack
     * */
    override fun <T> getResponseHandler(tClass: Class<T>): IResponseHandler<T> {
        val handlerClass = responseHandles.keys.find { it.isAssignableFrom(tClass) }?.let { responseHandles[it] }
            ?: DefaultResponseHandler::class.java
        return handlerClass.newInstance() as IResponseHandler<T>
    }

    open fun getGsonConverterFactory(): Converter.Factory? = GsonConverterFactory.create()


    /**
     * 断网重连
     * */
    open fun retryOnConnectionFailure() = true

    /**
     * 单位秒
     * */
    open fun writeTimeout() = 30

    /**
     * 单位秒
     * */
    open fun readTimeout() = 30

    /**
     * 单位秒
     * */
    open fun connectTimeout() = 30


    abstract fun getHostString(): String?

    open fun getHostHttpUrl(): HttpUrl? = null

}