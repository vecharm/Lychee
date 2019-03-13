package com.vecharm.lychee.http.core

/**
 * 速度处理帮助类
 * */
object ProgressHelper {

    /**
     * 下载速度计算器,可初始化时改变这个值
     * */
    var downloadSpeedComputer: Class<out ISpeedComputer>? = DefaultSpeedComputer::class.java

    /**
     * 上传速度计算器,可初始化时改变这个值
     * */
    var uploadSpeedComputer: Class<out ISpeedComputer>? = downloadSpeedComputer


    /**
     * 默认的速度计算器
     * */
    class DefaultSpeedComputer : ISpeedComputer {

        private var lastRefreshTime = 0L
        private var lastLength = 0L
        private var lastProgress = 0
        private var lastSpeed = 0L

        override fun computer(currLen: Long, contentLen: Long): Long {
            val time = System.currentTimeMillis()

            if (time - lastRefreshTime > 200) {
                val speed = (currLen - lastLength) * 1000 / (time - lastRefreshTime) //转换成秒
                lastRefreshTime = time
                lastLength = currLen
                lastSpeed = speed
                return speed
            }
            return lastSpeed

        }

        override fun progress(currLen: Long, contentLen: Long): Int {
            return if (contentLen > 0) (currLen * 100 / contentLen).toInt() else 0
        }

        /**
         * 当进度有变化或者时间超过200ms才更新
         * */
        override fun isUpdate(currLen: Long, contentLen: Long): Boolean {
            val time = System.currentTimeMillis()
            val progress = progress(currLen, contentLen)
            if (progress - lastProgress > 0 || time - lastRefreshTime >= 200) {
                lastProgress = progress
                return true
            }
            return false
        }

    }


    /**
     * 速度计算器接口
     * @see FileRequestBody.warpSource 上传
     * @see CoreResponseBody.read 下载
     * */
    interface ISpeedComputer {

        /**
         * 获取速度
         * */
        fun computer(currLen: Long, contentLen: Long): Long

        /**
         * 获取进度
         * */
        fun progress(currLen: Long, contentLen: Long): Int

        /**
         * 是否允许回调
         * */
        fun isUpdate(currLen: Long, contentLen: Long): Boolean
    }

    /**
     * 进度监听器
     * */
    interface ProgressListener {
        fun onUpdate(fileName: String, currLen: Long, size: Long, speed: Long, progress: Int)
    }
}