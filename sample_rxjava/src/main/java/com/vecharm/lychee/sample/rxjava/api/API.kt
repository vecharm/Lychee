package com.vecharm.lychee.sample.rxjava.api

import com.vecharm.lychee.http.config.interfaces.Download
import com.vecharm.lychee.http.config.interfaces.FileType
import com.vecharm.lychee.http.config.interfaces.MultiFileType
import retrofit2.Call
import retrofit2.http.*
import rx.Observable
import java.io.File

interface API {

    @POST("hello")
    fun hello(): Observable<ResultBean<String>>

    @Download
    @GET("https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk")
    fun download(): Observable<DownloadBean>

    @GET
    @Download
    fun download(@Url url: String, @Header("RANGE") range: String): Observable<DownloadBean>


    @FormUrlEncoded
    @POST("http://192.168.2.202:8888/service/upload/file")
    fun upload(@Field("file") @FileType("apk") file: File): Observable<ResultBean<UploadResult>>


    @Multipart
    @MultiFileType("apk")
    @POST("http://192.168.2.202:8888/service/upload/file")
    fun uploadMap(@PartMap map: MutableMap<String, Any>): Observable<ResultBean<UploadResult>>
}
