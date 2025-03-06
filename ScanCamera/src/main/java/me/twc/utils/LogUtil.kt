package me.twc.utils

import android.util.Log
import me.twc.scancamera.BuildConfig

/**
 * @author 唐万超
 * @date 2021/06/08
 */
private const val TAG = "ScanCamera"

fun logD(message: String, th: Throwable? = null) {
    if (Log.isLoggable(TAG, Log.DEBUG) || BuildConfig.DEBUG) {
        Log.d(TAG, message, th)
    }
}