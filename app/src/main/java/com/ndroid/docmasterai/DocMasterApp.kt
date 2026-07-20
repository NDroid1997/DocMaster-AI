package com.ndroid.docmasterai

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import org.opencv.android.OpenCVLoader

@HiltAndroidApp
class DocMasterApp : Application() {
	override fun onCreate() {
		super.onCreate()

		// Initialize OpenCV. initDebug() is suitable for development / debug builds.
		val ok = OpenCVLoader.initDebug()
		if (!ok) {
			Log.w("DocMasterApp", "OpenCV initialization failed (initDebug)")
		} else {
			Log.d("DocMasterApp", "OpenCV initialized successfully")
		}
	}
}
