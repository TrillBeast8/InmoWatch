package com.example.inmocontrol_v2.utils

import android.util.Log
import com.example.inmocontrol_v2.BuildConfig

/**
 * Optimized logging utility that eliminates logging overhead in release builds
 * and reduces string concatenation warnings
 */
object Logger {
    private const val GLOBAL_TAG = "InmoControl"

    // Only log in debug builds to eliminate performance overhead in release
    private val isDebugBuild = BuildConfig.DEBUG

    @JvmStatic
    fun d(tag: String, message: String) {
        if (isDebugBuild) {
            Log.d("$GLOBAL_TAG:$tag", message)
        }
    }

    @JvmStatic
    fun d(tag: String, message: () -> String) {
        if (isDebugBuild) {
            Log.d("$GLOBAL_TAG:$tag", message())
        }
    }

    @JvmStatic
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e("$GLOBAL_TAG:$tag", message, throwable)
        } else {
            Log.e("$GLOBAL_TAG:$tag", message)
        }
    }

    @JvmStatic
    fun w(tag: String, message: String) {
        Log.w("$GLOBAL_TAG:$tag", message)
    }

    @JvmStatic
    fun i(tag: String, message: String) {
        if (isDebugBuild) {
            Log.i("$GLOBAL_TAG:$tag", message)
        }
    }
}
