package me.twc.scancamera.camera

import android.content.Context
import android.hardware.SensorManager
import android.view.Display
import android.view.OrientationEventListener
import android.view.Surface

/**
 * @author 唐万超
 * @date 2021/06/16
 */
class DisplayOrientationDetector(
    context: Context,
    rate: Int = SensorManager.SENSOR_DELAY_NORMAL
) {

    companion object {
        private const val DISPLAY_ROTATION_KNOWN = 0
        private const val DEVICE_ORIENTATION_KNOWN = 0
    }

    private var mDisplay: Display? = null
    private var mDisplayRotation = DISPLAY_ROTATION_KNOWN
    private var mDeviceRotation = DEVICE_ORIENTATION_KNOWN

    private val mOrientationEventListener = object : OrientationEventListener(context, rate) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == ORIENTATION_UNKNOWN) return
            mDisplay?.let {
                mDisplayRotation = when (it.rotation) {
                    Surface.ROTATION_90 -> 90
                    Surface.ROTATION_180 -> 180
                    Surface.ROTATION_270 -> 270
                    else -> 0
                }
            }
            // 同 display.rotation 方式
            mDeviceRotation = when (orientation) {
                in 60..140 -> 270
                in 140..270 -> 180
                in 220..300 -> 90
                else -> 0
            }
        }
    }

    fun enable(display: Display) {
        mDisplayRotation = display.rotation
        mDisplay = display
        mOrientationEventListener.enable()
    }

    fun disable() {
        mOrientationEventListener.disable()
        mDisplayRotation = DISPLAY_ROTATION_KNOWN
        mDisplay = null
    }

    fun getDisplayRotation() = mDisplayRotation
    fun getDeviceRotation() = mDeviceRotation
}