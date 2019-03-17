package com.vecharm.lychee.http.core

import com.vecharm.lychee.http.config.interfaces.IRequestConfig
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 核心拦截器。主要进行请求参数的处理，以及返回的值的处理
 *
 * */
class CoreInterceptor(private val requestConfig: IRequestConfig) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newRequestBuild = when (request.method()) {
            "POST" -> requestConfig.newPostRequest(request)
            "GET" -> requestConfig.newGetRequest(request)
            else -> requestConfig.newOtherRequest(request)
        }
        requestConfig.addHeaders(newRequestBuild, request)

        val response = chain.proceed(newRequestBuild.build())
        return response.newBuilder().body(CoreResponseBody(response.body()!!, response)).build()
    }
}

