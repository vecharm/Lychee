package com.vecharm.lychee.sample.api

import com.vecharm.lychee.http.config.interfaces.*
import com.vecharm.lychee.http.core.RANGE
import retrofit2.Call
import retrofit2.http.*
import java.io.File

interface API {


    @POST("hello")
    fun hello(): Call<ResultBean<String>>


    @Download
    @GET("https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk")
    fun download(): Call<DownloadBean>

    @GET
    @Download
    fun download(@Url url: String, @Header(RANGE) range: String): Call<DownloadBean>


    @Multipart
    @POST("http://192.168.2.202:8888/service/upload/file")
    fun uploadApk(@Part("file") @FileType("apk") file: File): Call<ResultBean<UploadResult>>


    @Multipart
    @MultiFileType("apk")
    @POST("http://192.168.2.202:8888/service/upload/file")
    fun uploadMap(@PartMap map: MutableMap<String, Any>): Call<ResultBean<UploadResult>>

    @Upload
    @FileLog
    @Multipart
    @POST("http://192.168.2.202:8888/service/upload/file")
    fun upload(@Part("file") file: File): Call<ResultBean<UploadResult>>

}
