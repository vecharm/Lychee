# Lychee
## 介绍
`Lychee` 是一个基于Retrofit实现的轻量级网络请求库，支持多任务上传，下载，断点续传，使用简单，方便。使用注解简化了Retrofit原有的文件上传方式，支持添加**通用请求参数**，**参数签名**(文件使用md5)

## 用法

#### 初始化配置
```kotlin
    override fun onCreate() {
        super.onCreate()
        RequestCore.init(MyCoreConfig(this))
    }
```
#### 使用
```kotlin
      //普通请求
      getService<API>().hello().request {
          onSuccess = { Toast.makeText(App.app, it.data ?: "", Toast.LENGTH_SHORT).show() }
          onErrorMessage = {
          onCompleted = {}
      }

      //单个文件下载
      getService<API>().download().request(File(App.app.externalCacheDir, "qq.apk")) {
          onSuccess = { Toast.makeText(App.app, "${it.downloadInfo?.fileName} 下载完成", Toast.LENGTH_SHORT).show() }
          onErrorMessage = {
          onCompleted = {}
      }
        
      //多任务下载
      addDownloadTaskButton.setOnClickListener {
          val downloadTask = DownloadTask()
          val file = File(App.app.externalCacheDir, "qq${adapter.data.size + 1}.apk"
          downloadTask.download("https://qd.myapp.com/myapp/qqteam/AndroidQQ/mobileqq_android.apk", file)
          adapter.addData(downloadTask)
      }
        
      //多任务上传
      addUploadTaskButton.setOnClickListener {
          val uploadTask = UploadTask()
          uploadTask.upload(File(App.app.externalCacheDir, "qq${adapter.data.size + 1}.apk"))
          adapter.addData(uploadTask)
      }
        
```
### 下载的API定义
下载只需要使用 `Download` 注解API就可以啦
```kotlin
    @Download
    @GET("https://xxxx/xxxx.apk")
    fun download(): Call<DownloadBean>
```
### 上传的API定义
上传时使用 `FileType` 和 `MultiFileType` 声明文件的类型就可以啦，MultiFileType是同时声明API所有的文件参数的类型，也可以两个都写FileType会覆盖MultiFileType
```kotlin
    @FormUrlEncoded
    @POST("http://XXX/XXX")
    fun upload(@Field("file") @FileType("apk") file: File): Call<UploadResult>

    @Multipart
    @MultiFileType("apk")
    @POST("http://XXXX/XXXX")
    fun upload(@PartMap map: MutableMap<String, Any>): Call<UploadResult>
```

以上是用法，接下来的是配置
***
## 配置
配置总归还是要有滴，因为总得有个地方配置Host的吧。为了方便我也准备了一些默认的配置
#### 总配置
`ICoreConfig`默认配置的为`DefaultCoreConfig` ，DefaultCoreConfig是一个抽象类，只有继承它配置一下Host就可以用了哟
```kotlin
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
```
## API兼容
如果你不喜欢用RxJava，只用kotlin 可以这样定义
```kotlin
    @POST("hello")
    fun hello(): Call<ResultBean<String>>
```
如果你喜欢用RxJava或者RxJava2 
```kotlin
    @POST("hello")
    fun hello(): Call<ResultBean<String>>
```
#### 简约，轻奢才是代码之美，这样写代码才像极了爱情。喜欢的话给个star鼓励一下我哟，感谢各位大大。

