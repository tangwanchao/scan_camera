package me.twc.utils

import android.content.Context
import android.view.Surface
import android.view.WindowManager
import copy.com.google.zxing.client.android.camera.open.CameraFacing
import copy.com.google.zxing.client.android.camera.open.OpenCamera

/**
 * @author 唐万超
 * @date 2021/06/16
 */
@Suppress("DEPRECATION", "unused")
object RotationUtil {
    fun getDisplayRotation(context: Context): Int {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display = windowManager?.defaultDisplay
        if (display == null) {
            logD("display == null")
            return -1
        }
        return when (display.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
    }

    fun getCameraDisplayOrientation(openCamera: OpenCamera, displayRotation: Int): Int {
        return if (openCamera.facing == CameraFacing.FRONT) {
            val temp = (openCamera.orientation + displayRotation) % 360
            (360 - temp) % 360
        } else {
            (openCamera.orientation - displayRotation + 360) % 360
        }
    }

    fun calculateCaptureRotation(
        openCamera: OpenCamera,
        displayRotation: Int,
        deviceRotation: Int
    ): Int {
        var captureRotation = if (openCamera.facing == CameraFacing.FRONT) {
            (openCamera.orientation + displayRotation) % 360
        } else {  // back-facing camera
            (openCamera.orientation - displayRotation + 360) % 360
        }

        captureRotation = if (openCamera.facing == CameraFacing.FRONT) {
            (captureRotation - (displayRotation - deviceRotation) + 360) % 360
        } else {  // back-facing camera
            (captureRotation + (displayRotation - deviceRotation) + 360) % 360
        }
        return captureRotation
    }
}