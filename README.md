# Lychee
## 介绍
`Lychee` 是一个基于Retrofit2实现的轻量级网络请求库，支持多任务上传，下载，断点续传，使用简单，方便。使用注解简化了Retrofit2原有的文件上传方式，支持添加**通用请求参数**，**参数签名**(文件使用md5)

### 相关文章
* [基于Retrofit2实现的LycheeHttp](https://juejin.im/post/5c905dcaf265da60da704c25)
* [基于Retrofit2实现的LycheeHttp-使用动态代理实现上传](https://juejin.im/post/5c8db0d0e51d45798322ba54)
* [基于Retrofit2实现的LycheeHttp-多任务下载的实现](https://juejin.im/post/5c8eef6ee51d4563614b183e)

## 用法

#### 初始化配置
```kotlin
    override fun onCreate() {
        super.onCreate()
        LycheeHttp.init(MyCoreConfig(this))
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
1. 根据文件名称的后缀名获取，使用`Upload` 进行注解
```kotlin
    @Upload
    @Multipart
    @POST("http://xxx/xxx")
    fun upload(@Part("file") file: File): Call<ResultBean<UploadResult>>
```
2. 对某个file进行注解，使用`FileType("png")` 或者`FileType("image/png")`
```kotlin
    @Multipart
    @POST("http:/xxx/xxx")
    fun upload(@Part("file") @FileType("png") file: File): Call<ResultBean<UploadResult>>
```
3. 对整个方法的所有file参数进行注解，使用`MultiFileType("png")`或者`MultiFileType("image/png")`
```kotlin
    @Multipart
    @MultiFileType("png")
    @POST("http://xxx/xxx")
    fun upload(@PartMap map: MutableMap<String, Any>): Call<ResultBean<UploadResult>>
    fun upload(@PartMap map: MutableMap<String, Any>): Call<UploadResult>
```
#### 使用
```kotlin
      //普通请求
      getService<API>().hello().request {
          onSuccess = { Toast.makeText(App.app, it.data ?: "", Toast.LENGTH_SHORT).show() }
          onErrorMessage = {}
          onCompleted = {}
      }

      //单个文件下载
      getService<API>().download().request(File(App.app.externalCacheDir, "qq.apk")) {
          onSuccess = { Toast.makeText(App.app, "${it.downloadInfo?.fileName} 下载完成", Toast.LENGTH_SHORT).show() }
          onErrorMessage = {}
          onCompleted = {}
      }
        
      //多任务下载
      addDownloadTaskButton.setOnClickListener {
          val downloadTask = DownloadTask()
          val file = File(App.app.externalCacheDir, "qq${adapter.data.size + 1}.apk"
          downloadTask.download("https://xxx/xxx.apk", file)
          adapter.addData(downloadTask)
      }
        
      //多任务上传
      addUploadTaskButton.setOnClickListener {
          val uploadTask = UploadTask()
          uploadTask.upload(File(App.app.externalCacheDir, "qq${adapter.data.size + 1}.apk"))
          adapter.addData(uploadTask)
      }
        
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
    fun hello(): Observable<ResultBean<String>>
```
#### 简约，才是代码之美，这样写代码才像极了爱情。喜欢的话给个star鼓励一下我哟，感谢各位大大。

