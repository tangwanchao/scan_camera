package me.twc.scancamera.camera

import android.hardware.Camera

/**
 * @author 唐万超
 * @date 2021/06/15
 *
 */

data class PictureInfo(
    val displayRotation: Int,
    val deviceRotation: Int,
    // jpeg 图片需要旋转的角度
    val captureRotation: Int
)

typealias PictureWithParamCallback = (data: ByteArray, camera: Camera, info: PictureInfo) -> Unit

/**
 * @param autoStartPreview 是否在调用 [CameraView.takePicture] 自动重新开启预览
 */
class TakePictureCallback(
    val autoStartPreview: Boolean = false,
    val shutter: Camera.ShutterCallback? = null,
    val raw: Camera.PictureCallback? = null,
    val postview: Camera.PictureCallback? = null,
    val takePictureError: (() -> Unit)? = null,
    val jpeg: PictureWithParamCallback? = null
)
