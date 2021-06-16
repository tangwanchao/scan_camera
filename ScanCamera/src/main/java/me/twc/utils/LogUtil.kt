package me.twc.utils

import android.util.Log

/**
 * @author 唐万超
 * @date 2021/06/08
 */
private const val TAG = "ScanCamera"

fun logD(message: String, th: Throwable? = null) {
    if (Log.isLoggable(TAG, Log.DEBUG)) {
        Log.d(TAG, message, th)
    }
}