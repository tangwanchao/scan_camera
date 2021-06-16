package me.twc.scancamera.camera

import androidx.lifecycle.LifecycleOwner
import me.twc.impl.FullLifecycleObserver

/**
 * @author 唐万超
 * @date 2021/06/11
 */
class CameraViewLifecycleObserver(
    private val mCameraView: CameraView
) : FullLifecycleObserver {
    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        mCameraView.create()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        mCameraView.resume()
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        mCameraView.pause()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        mCameraView.destroy()
    }
}