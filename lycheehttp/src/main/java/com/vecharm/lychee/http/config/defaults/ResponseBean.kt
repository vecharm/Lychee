package com.vecharm.lychee.http.config.defaults

import com.google.gson.annotations.SerializedName
import java.io.Serializable

open class ResponseBean : Serializable {
    @SerializedName("code")
    open var status = 0

    @SerializedName("msg")
    open var desc: String? = null
}