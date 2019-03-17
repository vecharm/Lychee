package com.vecharm.lychee.http.core

import android.net.Uri
import android.text.TextUtils
import okhttp3.internal.Util
import okio.*
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.http.PartMap
import java.io.File
import java.io.IOException
import java.lang.reflect.Type
import okio.ForwardingSource
import okio.Okio
import okio.BufferedSource
import android.util.Log
import com.vecharm.lychee.http.config.interfaces.Download
import com.vecharm.lychee.http.config.interfaces.FileType
import com.vecharm.lychee.http.config.interfaces.MultiFileType
import com.vecharm.lychee.http.config.interfaces.Upload
import okhttp3.*


/**
 * 核心Cover类
 * */
class CoreCoverFactory : Converter.Factory() {
    companion object {
        fun create() = CoreCoverFactory()
    }


    /**
     *
     * 获取真实的ResponseCover，处理非下载情况返回值的转换
     * 如果是Download注解的方法，则认为这是一个下载方法
     * */
    override fun responseBodyConverter(type: Type, annotations: Array<Annotation>, retrofit: Retrofit): Converter<ResponseBody, *>? {
        var delegateResponseCover: Converter<*, *>? = null
        retrofit.converterFactories().filter { it != this }.find {
            delegateResponseCover = it.responseBodyConverter(type, annotations, retrofit); delegateResponseCover != null
        }
        return CoreResponseCover(annotations.find { it is Download } != null, delegateResponseCover as Converter<ResponseBody, Any>)
    }

    /**
     * 获取真实的ResponseCover，处理非文件参数的转换
     * 对于文件类型的参数创建有MediaType的RequestBody
     * @see com.vecharm.lychee.http.config.interfaces.FileType
     * @see com.vecharm.lychee.http.config.interfaces.MultiFileType
     * */
    override fun requestBodyConverter(type: Type, parameterAnnotations: Array<Annotation>, methodAnnotations: Array<Annotation>, retrofit: Retrofit): Converter<*, RequestBody>? {
        return when {
            type == File::class.java -> FileCover(parameterAnnotations.findFileType(), methodAnnotations.findMultiType(), methodAnnotations.isIncludeUpload())
            parameterAnnotations.find { it is PartMap } != null -> {
                var delegateCover: Converter<*, *>? = null
                retrofit.converterFactories().filter { it != this }.find {
                    delegateCover =
                        it.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit); delegateCover != null
                }
                if (delegateCover == null) return null
                return MapParamsCover(parameterAnnotations, methodAnnotations, delegateCover as Converter<Any, RequestBody>)
            }
            else -> null
        }
    }
}

/**
 * map类型的参数处理
 * */
class MapParamsCover<T>(private val parameterAnnotations: Array<Annotation>, private val methodAnnotations: Array<Annotation>, val delegatePartMapCover: Converter<T, RequestBody>) :
    Converter<T, RequestBody> {
    override fun convert(value: T): RequestBody {
        return if (value is File) FileCover(parameterAnnotations.findFileType(), methodAnnotations.findMultiType(), methodAnnotations.isIncludeUpload()).convert(value)
        else delegatePartMapCover.convert(value)
    }
}

/**
 * 所有Response的body都经过这里
 * */
class CoreResponseCover(private val isDownloadMethodCover: Boolean, private val delegateResponseCover: Converter<ResponseBody, Any>) :
    Converter<ResponseBody, Any> {

    override fun convert(value: ResponseBody): Any {

        var responseBody: ResponseBody? = null
        try {
            //取出OkHttpCall&ExceptionCatchingRequestBody 中的 delegateRespondBody
            value::class.java.declaredFields.forEach {
                if (it.type == ResponseBody::class.java) {
                    it.isAccessible = true
                    responseBody = it.get(value) as? ResponseBody
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (responseBody == null) responseBody = value

        //非下载的情况
        if (!isDownloadMethodCover) {
            (responseBody as? CoreResponseBody).also {
                it?.startRead()
                it?.notifyRead()
            }
            return delegateResponseCover.convert(value)
        } else {
            //下载的情况
            return responseBody ?: value
        }
    }
}


class FileCover(private val fileType: FileType?, private val multiFileType: MultiFileType?, private val isAutoWried: Boolean) :
    Converter<File, RequestBody> {
    override fun convert(value: File): RequestBody {
        var type = fileType?.value ?: multiFileType?.value
        if (isAutoWried) if (type == null) type = value.name?.substringAfterLast(".", "")
        if (type == null || type.isEmpty()) return create(null, value)
        return create(MediaType.parse(type), value)
    }
}


fun create(contentType: MediaType?, file: File?): RequestBody {
    if (file == null) throw NullPointerException("content == null")
    return FileRequestBody(contentType, file)
}

fun Array<Annotation>.findFileType() = find { it is FileType } as? FileType
fun Array<Annotation>.findMultiType() = find { it is MultiFileType } as? MultiFileType
fun Array<Annotation>.isIncludeUpload() = find { it is Upload } != null

//callAdapter-> parseParams -> cover -> requestBuild - >request
//这里是到 cover这一步 构建请求的RequestBody 这时候拿到的file已经是被我改造的UploadFile
class FileRequestBody(private val contentType: MediaType?, val file: File) : RequestBody() {
    val uploadFile: UploadFile = if (file is UploadFile) file else UploadFile(file)

    override fun contentType() = contentType

    override fun contentLength() = file.length()

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        var source: Source? = null
        try {
            source = Okio.source(file)
            if (sink is Buffer) sink.writeAll(source)//处理被日志读取的情况
            else sink.writeAll(warpSource(source))
        } finally {
            Util.closeQuietly(source)
        }
    }

    private fun warpSource(source: Source) = object : ForwardingSource(source) {
        private var currLen = 0L
        val speedComputer = ProgressHelper.uploadSpeedComputer?.newInstance()
        override fun read(sink: Buffer, byteCount: Long): Long {
            val len = super.read(sink, byteCount)
            currLen += if (len != -1L) len else 0
            speedComputer ?: return len
            if (speedComputer.isUpdate(currLen, contentLength())) {
                uploadFile.progressListener?.onUpdate(file.name, currLen, contentLength(), speedComputer.computer(currLen, contentLength()), speedComputer.progress(currLen, contentLength()))
            }
            return len
        }
    }
}

/**
 * 所有Response的body都经过这里
 * */
class CoreResponseBody(private val responseBody: ResponseBody, val response: Response? = null) : ResponseBody() {


    private var isCanRead = false

    private var bufferedSource: BufferedSource? = null

    private var pendingReadList = ArrayList<(() -> Unit)>()

    var progressCallBack: ProgressCallBack? = null

    var rangeStart = 0L
        private set

    var rangeEnd = -1L
        private set

    var downloadInfo: DownloadInfo? = null
        private set

    init {
        parseResponseRangeFormHeader()

        val fileName = getFileNameByHeader()
        if (fileName != null) {
            val fileType = fileName.substringAfterLast(".", "")
            val url = response?.request()?.url()?.toString() ?: ""
            downloadInfo = DownloadInfo(url, fileName, rangeEnd, fileType)
        }
    }

    override fun contentType(): MediaType? {
        return responseBody.contentType()
    }

    override fun contentLength(): Long {
        return responseBody.contentLength()
    }

    override fun source(): BufferedSource {
        return bufferedSource ?: Okio.buffer(source(responseBody.source())).also { bufferedSource = it }
    }

    fun getFileNameByHeader(): String? {
        var fileName: String? = null
        var dispositionHeader = response?.header("Content-Disposition")
        if (dispositionHeader?.isNotEmpty() == true) {
            dispositionHeader = dispositionHeader.replace("attachment;filename=", "").replace("filename*=utf-8", "")
            val strings = dispositionHeader.split("; ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (strings.size > 1) {
                dispositionHeader = strings[1].replace("filename=", "")
                dispositionHeader = dispositionHeader.replace("\"", "")
                fileName = dispositionHeader
            }
        }
        if (fileName?.isNotEmpty() == true) {
            response?.also {
                val uri = Uri.parse(it.request().url().toString())
                fileName = uri.lastPathSegment
            }
        }
        return fileName
    }

    /**
     * 处理Response的Header中的Content-Range
     * 获取本次下载的区间
     * */
    private fun parseResponseRangeFormHeader() {
        val contentRange = response?.header("Content-Range")
        val rangeStartString = contentRange?.replace("bytes ", "")?.substringBefore("-")
        val rangeEndString = contentRange?.substringAfter("/")

        try {
            if (rangeStartString?.trim()?.isNotEmpty() == true) {
                rangeStart = rangeStartString.toLong()
            }
            if (rangeEndString?.trim()?.isNotEmpty() == true) {
                rangeEnd = rangeEndString.toLong()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startRead() {
        isCanRead = true
    }

    fun isCanRead(): Boolean {
        val code = response?.code() ?: 0
        if (code < 200 || code >= 300) {
            isCanRead = true
        }
        return isCanRead
    }

    /**
     * 通知拦截器可以读取
     * */
    fun notifyRead() {
        if (!isCanRead) return
        pendingReadList.forEach { it.invoke() }
        pendingReadList.clear()
    }

    /**
     * 调解与下载冲突的日志拦截器
     * */
    fun waitRead(callback: () -> Unit) {
        if (!pendingReadList.contains(callback)) pendingReadList.add(callback)
    }


    /**
     * 处理非法获取数据的情况
     * @see startRead 调用此方法才允许读取
     * */
    private fun source(source: Source): Source {

        return object : ForwardingSource(source) {

            @Throws(IOException::class)
            override fun read(sink: Buffer, byteCount: Long): Long {
                if (!isCanRead()) throw UnsupportedOperationException("不支持提前处理数据")
                // read() returns the number of bytes read, or -1 if this source is exhausted.
                return super.read(sink, byteCount)
            }
        }
    }

    /**
     *
     *  使用这个方法读取ResponseBody的数据
     * */
    fun read(callback: ProgressHelper.ProgressListener?, dataOutput: IBytesReader? = null) {
        var currLen = rangeStart

        try {
            val fileName = downloadInfo?.fileName ?: ""
            progressCallBack = object : CoreResponseBody.ProgressCallBack {
                val speedComputer = ProgressHelper.downloadSpeedComputer?.newInstance()
                override fun onUpdate(isExhausted: Boolean, currLen: Long, size: Long) {
                    speedComputer ?: return
                    if (speedComputer.isUpdate(currLen, size)) {
                        callback?.onUpdate(fileName, currLen, size, speedComputer.computer(currLen, size), speedComputer.progress(currLen, size))
                    }
                }
            }
            startRead()
            val source = source()
            val sink = ByteArray(1024 * 4)
            var len = 0
            while (source.read(sink).also { len = it } != -1) {
                currLen += dataOutput?.onUpdate(sink, len) ?: 0
                progressCallBack?.onUpdate(false, currLen, rangeEnd)
            }
            progressCallBack?.onUpdate(true, currLen, rangeEnd)
            //通知日志读取，由于日志已经在上面消费完了，所以在只能获取头部信息
            notifyRead()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        } finally {
            Util.closeQuietly(source())
            dataOutput?.onClose()
        }
    }


    /**
     * 进度回调接口
     * */
    interface ProgressCallBack {
        fun onUpdate(isExhausted: Boolean, currLen: Long, size: Long)
    }


    /**
     * 数据读取接口
     * */
    interface IBytesReader {
        fun onUpdate(sink: ByteArray, len: Int): Int
        fun onClose()
    }


    data class DownloadInfo(val url: String, val fileName: String, val size: Long, val fileType: String)
}


