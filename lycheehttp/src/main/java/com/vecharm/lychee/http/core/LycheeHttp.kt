package com.vecharm.lychee.http.core

import android.os.Handler
import android.os.Looper
import com.vecharm.lychee.http.config.interfaces.ICoreConfig
import com.vecharm.lychee.http.config.interfaces.IResponseHandler
import okhttp3.OkHttpClient
import retrofit2.Retrofit

object LycheeHttp {


    private var retrofit: Retrofit? = null

    private var coreConfig: ICoreConfig? = null

    private var mainHandler = Handler(Looper.getMainLooper())

    /**
     * 使用前需要先初始化
     * 建议在App初始化时调用
     * */
    fun init(coreConfig: ICoreConfig) {
        LycheeHttp.coreConfig = coreConfig
        val client = OkHttpClient.Builder().also { initClient(it, coreConfig) }.build()
        retrofit = Retrofit.Builder().client(client).also { initRetrofit(it, coreConfig) }.build()
    }


    private fun initClient(builder: OkHttpClient.Builder, coreConfig: ICoreConfig) {
        coreConfig.onInitClient(builder)
        builder.addInterceptor(CoreInterceptor(coreConfig.getRequestConfig()))
    }

    private fun initRetrofit(builder: Retrofit.Builder, coreConfig: ICoreConfig) {
        builder.addConverterFactory(CoreCoverFactory.create())
        builder.addCallAdapterFactory(CoreCallAdapter())
        coreConfig.onInitRetrofit(builder)
    }


    fun getRetrofit(): Retrofit {
        return retrofit ?: throw NullPointerException("请先调用 LycheeHttp::init 初始化")
    }

    /**
     *
     * 根据返回值类型获取返回值处理器
     * @see com.vecharm.lychee.http.config.defaults.DefaultResponseHandler
     * */
    fun <T> getResponseHandler(tClass: Class<T>): IResponseHandler<T> {
        return coreConfig?.getResponseHandler(tClass) ?: throw java.lang.NullPointerException("responseHandler 不能为空")
    }


    fun runOnUI(r: Runnable) {
        mainHandler.post(r)
    }
}