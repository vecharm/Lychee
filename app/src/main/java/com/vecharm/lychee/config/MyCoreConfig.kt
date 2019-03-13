package com.vecharm.lychee.config

import android.app.Application
import android.content.pm.PackageManager
import android.widget.Toast
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.vecharm.lychee.ui.App
import com.vecharm.lychee.BuildConfig

import com.vecharm.lychee.http.config.defaults.DefaultCoreConfig
import com.vecharm.lychee.http.config.defaults.DefaultRequestConfig
import com.vecharm.lychee.http.config.defaults.DefaultResponseHandler
import com.vecharm.lychee.http.config.defaults.ResponseBean
import com.vecharm.lychee.http.core.md5
import okhttp3.Request
import java.util.*

class MyCoreConfig(val context: Application) : DefaultCoreConfig() {

    override fun getHostString() = "https://www.easy-mock.com/mock/5bffc142bfe3e5204cd3a84c/app/"

    override fun getCookieJar() = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context))

    override fun getRequestConfig() = MyRequestConfig()

    init {
        /*
        * 注册自定义的返回值处理
        * */
        registerResponseHandler(ResponseBean::class.java, MyResponseHandler::class.java)
    }

}

class MyRequestConfig : DefaultRequestConfig() {

    /**
     * 添加默认头部参数
     * */
    override fun addHeaders(newRequestBuild: Request.Builder, oldRequest: Request) {
        newRequestBuild.addHeader("Accept", "application/json")
        newRequestBuild.addHeader("Accept-Language", "zh")
    }

    /**
     * 添加通用参数
     * */
    override fun onAddCommonParams(map: MutableMap<String, String>) {
        map["app_version"] = BuildConfig.VERSION_CODE.toString()
        map["nonce"] = map["nonce"] ?: randomUUID()
        map["timestamp"] = System.currentTimeMillis().div(1000).toString()
        map["pkg_name"] = App.app.packageName
        map["app_sign"] = getAppSign()
    }

    /**
     * 参数签名
     * */
    override fun onSignParams(map: MutableMap<String, String>) {
        super.onSignParams(map)
        map.remove("app_sign")
    }

    private var appSign = ""
    private fun getAppSign(): String {
        if (appSign.isNotEmpty()) return appSign
        return try {
            val packageInfo = App.app.packageManager.getPackageInfo(App.app.packageName, PackageManager.GET_SIGNATURES)
            md5(String(packageInfo.signatures[0].toByteArray())).also { appSign = it }
        } catch (e: Exception) {
            ""
        }
    }

}

/**
 *
 * 自定义返回值处理
 * */
class MyResponseHandler : DefaultResponseHandler() {

    override fun onError(status: Int, message: String?) {
        if (10001 == status) {//没有登陆
            Toast.makeText(App.app, "没有登陆", Toast.LENGTH_LONG).show()
        }
        super.onError(status, message)
        Toast.makeText(App.app, "$status:$message", Toast.LENGTH_LONG).show()
    }
}

fun randomUUID() = UUID.randomUUID().toString().replace("-", "")