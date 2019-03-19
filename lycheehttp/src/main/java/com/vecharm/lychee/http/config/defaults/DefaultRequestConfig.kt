package com.vecharm.lychee.http.config.defaults

import com.vecharm.lychee.http.core.FileRequestBody
import com.vecharm.lychee.http.config.interfaces.IRequestConfig
import com.vecharm.lychee.http.core.md5
import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okio.Buffer
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.*
import kotlin.collections.ArrayList

/**
 * 请求的默认配置
 * 可以继承这个类，在这个基础上修改，也可以自己继承接口按照自己的想法实现
 * @see com.vecharm.lychee.http.core.CoreInterceptor
 * */
open class DefaultRequestConfig : IRequestConfig {


    /**
     * 处理get方法
     * 获取->加入通用参数->排序->签名(临时去掉不参与签名的参数)->重新生成request
     * */
    override fun newGetRequest(oldRequest: Request): Request.Builder {
        val oldUrl = oldRequest.url()
        val map = getSortMap()
        val keys = oldUrl.queryParameterNames()
        keys.forEach { oldUrl.queryParameter(it)?.also { value -> map[it] = value } }
        //添加默认参数和签名
        addCommonParamsAndSign(map)
        //构建新的请求
        val commonParamsUrlBuilder =
            oldRequest.url().newBuilder().scheme(oldRequest.url().scheme()).host(oldRequest.url().host())
        map.filter { it.value.isNotEmpty() }
            .forEach { if (!keys.contains(it.key)) commonParamsUrlBuilder.addQueryParameter(it.key, it.value) }
        return oldRequest.newBuilder().method(oldRequest.method(), oldRequest.body())
            .url(commonParamsUrlBuilder.build())
    }


    /**
     * 处理post方法
     * 获取->加入通用参数->排序->签名(临时去掉不参与签名的参数)->重新生成request
     * */
    override fun newPostRequest(oldRequest: Request): Request.Builder {
        return when (oldRequest.body()) {
            is FormBody -> newFormBodyRequest(oldRequest)
            is MultipartBody -> newMultipartBodyRequest(oldRequest)
            else -> newGetRequest(oldRequest)
        }
    }

    /**
     * 对表单请求的处理
     * 获取->加入通用参数->排序->签名(临时去掉不参与签名的参数)->重新生成request
     * */
    open fun newFormBodyRequest(oldRequest: Request): Request.Builder {
        val oldPostBody = oldRequest.body() as FormBody
        val map = getSortMap()
        //获取原始请求的参数，放入有序的map中
        (0 until oldPostBody.size()).forEach { map[oldPostBody.encodedName(it)] = oldPostBody.encodedValue(it) }
        //添加默认参数和签名
        addCommonParamsAndSign(map)
        return oldRequest.newBuilder().post(createNewFormBody(map))
    }

    /**
     * 对Multipart请求的处理
     * 获取->加入通用参数->排序->签名(临时去掉不参与签名的参数)->重新生成request
     * 对于文件的签名，只是获取他的数据进行md5加密。
     * */
    open fun newMultipartBodyRequest(oldRequest: Request): Request.Builder {

        //获取不需要参与签名的key
        val unSignParamName = unSignParamNames()

        val multipartBody = oldRequest.body() as MultipartBody
        val oldParts = multipartBody.parts()
        //创建一个新的builder
        val bodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)

        val newParts = ArrayList<MultipartBody.Part>()
        val map = getSortMap()

        //用来保存文件参数的key
        val fileKeys = ArrayList<String>()
        oldParts.forEach { part ->
            val headers = part.headers()
            headers?.names()?.mapIndexed { index, _ -> headers.value(index) }?.forEach {
                val key = getMultipartBodyKey(it)
                key?.also {
                    //如果不是文件类型参数，将需要参与签名的参数取出来
                    if (part.body() !is FileRequestBody) {
                        if (unSignParamName?.contains(key) != true) map[key] = bodyToString(part.body())
                    } else {
                        val fileBody = part.body() as FileRequestBody
                        fileKeys.add(key)
                        //对于文件的签名，只是获取他的数据进行md5加密。
                        newParts.add(MultipartBody.Part.createFormData(key, fileBody.file.name, fileBody))
                        if (unSignParamName?.contains(key) != true) map[key] = part.body().md5()
                    }
                }
            }
        }
        //添加默认参数和签名
        addCommonParamsAndSign(map)
        //因为文件参数的真实值并没有取处理，map只是保存了它的md5，签名完了之后，把它排除开
        map.filter { it.value.isNotEmpty() && !fileKeys.contains(it.key) }
            .forEach { newParts.add(createNewPart(it.key, it.value)) }
        newParts.forEach { bodyBuilder.addPart(it) }
        //创建新的请求
        return oldRequest.newBuilder().post(bodyBuilder.build())
    }


    /**
     * 获取Multipart的参数key
     * */
    open fun getMultipartBodyKey(headerValue: String): String? {
        val replace = "form-data; name="
        return if (headerValue.contains(replace)) headerValue.replace(replace, "").replace("\"".toRegex(), "") else null
    }

    open fun createNewPart(key: String, value: String) = MultipartBody.Part.createFormData(key, value)!!

    open fun createNewFormBody(map: MutableMap<String, String>): FormBody {
        val formBodyBuilder = FormBody.Builder()
        //排除值为空的参数
        map.filter { it.value.isNotEmpty() }
            .forEach { formBodyBuilder.addEncoded(it.key, URLEncoder.encode(it.value, Charsets.UTF_8.name())) }
        return formBodyBuilder.build()
    }


    override fun newOtherRequest(oldRequest: Request) = newGetRequest(oldRequest)

    override fun addHeaders(newRequestBuild: Request.Builder, oldRequest: Request) {}

    private fun addCommonParamsAndSign(map: MutableMap<String, String>) {
        onAddCommonParams(map)
        //默认签名方式是按照参考名称排序
        if (isSignParam()) onSignParams(map)
    }

    open fun onAddCommonParams(map: MutableMap<String, String>) {}

    /**
     * 参数签名
     * */
    open fun onSignParams(map: MutableMap<String, String>) {
        map["sign"] = sign(LinkedHashMap(map).filter { unSignParamNames()?.contains(it.key) != true })
    }

    /**
     * 不需要参与签名的字段
     * */
    open fun unSignParamNames(): Array<String>? = null

    /**
     * 是否参数签名
     * */
    open fun isSignParam() = false

    /**
     * 示例->签名规则: 所有变量按字母顺序value组合
     * */
    open fun sign(map: Map<String, String>) =
        md5(StringBuilder().also { sb -> map.forEach { sb.append(it.value) } }.toString())

    open fun getSortMap(): MutableMap<String, String> = TreeMap()

    open fun bodyToString(request: RequestBody?) = try {
        Buffer().also { request?.writeTo(it) }.readUtf8()!!
    } catch (e: Exception) {
        e.printStackTrace()
        ""
    }

    private fun RequestBody.md5(): String {
        if (contentLength() <= 0L) return ""
        Buffer().also { writeTo(it) }.readByteArray().let {
            val md = MessageDigest.getInstance("MD5")
            it.forEach { md.update(it) }
            return md.digest().joinToString("") { String.format("%02x", it) }
        }
    }

}