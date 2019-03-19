package com.vecharm.lychee.sample.api

import com.vecharm.lychee.http.config.interfaces.Download
import com.vecharm.lychee.http.config.interfaces.FileType
import com.vecharm.lychee.http.config.interfaces.MultiFileType
import com.vecharm.lychee.http.config.interfaces.Upload
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


    @FormUrlEncoded
    @POST("http://192.168.2.202:8888/service/upload/file")
    fun uploadApk(@Field("file") @FileType("apk") file: File): Call<ResultBean<UploadResult>>


    @Multipart
    @MultiFileType("apk")
    @POST("http://192.168.2.202:8888/service/upload/file")
    fun uploadMap(@PartMap map: MutableMap<String, Any>): Call<ResultBean<UploadResult>>

    @FormUrlEncoded
    @POST("http://192.168.2.202:8888/service/upload/file")
    @Upload
    fun upload(@Field("file") file: File): Call<ResultBean<UploadResult>>

}
