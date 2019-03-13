package com.vecharm.lychee.http.core

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.io.File
import java.lang.reflect.Proxy
import java.lang.reflect.Type

/**
 * 返回值的处理方法，它的主要实现了将请求的回调和上传进度关联
 * @see UploadFile 保存回调实例
 * @see FileRequestBody 上传进度的实现
 * @see UploadFilePostStation 将UploadFile与Call关联的临时站
 * 这里只是做一个代理，不负责生产，最终还是调用 retrofit.callAdapterFactories()获取真实的CallAdapter
 * */
class CoreCallAdapter : CallAdapter.Factory() {

    //CallAdapter.Factory是Retrofit的一个构建返回值的工厂类。比如 fun upload(@Part("bid") bid: Int, @Part("file") file: File): Data<UploadResult>
    //这个Data<UploadResult> 就是在这里时候实例化出来的
    override fun get(returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit): CallAdapter<*, *>? {

        var adapter: CallAdapter<*, *>? = null

        //获取真实的 adapterFactory
        retrofit.callAdapterFactories().filter { it != this }
            .find { adapter = it.get(returnType, annotations, retrofit);adapter != null }

        return adapter?.let {

            //所以使用动态代理 获取生产的对象
            Proxy.newProxyInstance(CallAdapter::class.java.classLoader, arrayOf(CallAdapter::class.java)) { _, method, arrayOfAnys ->

                val returnObj = when (arrayOfAnys?.size) { // 这里 Retrofit调用 CallAdapter.adapt 获得返回值对象 在此截获
                    null, 0 -> method.invoke(adapter)
                    1 -> method.invoke(adapter, arrayOfAnys[0])
                    else -> method.invoke(adapter, arrayOfAnys)
                }
                //从参数中把OkHttpCall拿出 OkHttpCall是Retrofit 封装的一个请求call 里面放了本次请求的所有参数
                val okHttpCall = arrayOfAnys?.getOrNull(0) as? Call<*>
                okHttpCall?.also {

                    var list = ArrayList<UploadFile>()
                    try {
                        //由于Retrofit的调用链接是 callAdapter-> parseParams -> requestCover -> requestBuild - >request
                        //利用这个关系 一开始在callAdapter中把okHttpCall里面所有的File封装成UploadFile
                        val callClass = okHttpCall::class.java
                        //找出保存参数的 Object[] args 成员
                        callClass.declaredFields.forEach {
                            if (it.type.isArray) {
                                it.isAccessible = true
                                val args = it.get(okHttpCall) as? Array<Any>
                                args?.forEachIndexed { index, data ->
                                    //拿出File文件 用UploadFile包裹
                                    args[index] = parseData(data, list)
                                }
                                return@forEach
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        //如果上面的操作失败了，就用request()，最终执行一遍Cover生成
                        list = getFileRequestBody(okHttpCall.request()?.body())
                    }
                    //将返回值对象的toString 和 WrapFile 关联起来，因为一次可能上传多个文件就用数组了
                    list.forEach { UploadFilePostStation.setCallBack(returnObj.toString(), it) }
                }

                return@newProxyInstance returnObj

            } as CallAdapter<*, *>
        }
    }

    /**
     *
     * 解析参数，拿出File文件 用UploadFile包裹
     * @see UploadFile
     * */
    private fun parseData(data: Any, list: ArrayList<UploadFile>): Any {
        if (data::class.java == File::class.java) {
            return UploadFile(data as File).also { list.add(it) }
        } else if (data is Map<*, *>) { //mapPart
            val oldMap = data as Map<String, Any>
            val map = LinkedHashMap<String, Any>()
            oldMap.forEach { map[it.key] = it.value }
            oldMap.forEach {
                if (it.value::class.java == File::class.java) {
                    map[it.key] = UploadFile(it.value as File).also { list.add(it) }
                }
            }
            return map
        }
        return data
    }

    /**
     * 获取 UploadFile
     * @see FileRequestBody
     * */
    private fun getFileRequestBody(body: RequestBody?): ArrayList<UploadFile> {
        val list = ArrayList<UploadFile>()
        val multipartBody = body as? MultipartBody ?: return list
        multipartBody.parts().forEach { it.body()?.also { if (it is FileRequestBody) list.add(it.uploadFile) } }
        return list
    }

}