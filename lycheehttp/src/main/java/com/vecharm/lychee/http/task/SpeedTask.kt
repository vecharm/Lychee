package com.vecharm.lychee.http.task


open class SpeedTask : Task() {

    var speed = 0L


    open var updateUI: (() -> Unit)? = null
        set(value) {
            field = value
            value?.invoke()
        }

    open fun getSpeed(): String {
        if (speed < 1024) return "${speed}B/s"
        val kb = speed / 1024
        if (kb < 1024) return "${kb}KB/s"
        return "${kb / 1024}MB/s"
    }


}