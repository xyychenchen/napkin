package com.imageflow.app

import android.app.Application
import com.imageflow.app.data.ImageFlowDatabase

class ImageFlowApplication : Application() {
    val database: ImageFlowDatabase by lazy { ImageFlowDatabase.getInstance(this) }
}
