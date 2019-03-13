package com.vecharm.lychee.http.core

import java.io.File




class UploadFile(file: File) : File(file.absolutePath) {
    var progressListener: ProgressHelper.ProgressListener? = null
}

