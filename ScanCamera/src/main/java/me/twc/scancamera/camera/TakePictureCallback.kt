package me.twc.scancamera.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.hardware.Camera
import androidx.annotation.WorkerThread
import me.twc.utils.ImageUtil
import me.twc.utils.logD

/**
 * @author 唐万超
 * @date 2021/06/15
 *
 */

data class PictureInfo(
    val displayRotation: Int,
    val deviceRotation: Int,
    // jpeg 图片需要旋转的角度
    val captureRotation: Int,
    // jpeg 图片旋转后可以根据该 rect 进行裁剪,裁剪后将得到预览框中图片
    // 注意: 仅预览回调能使用该方法裁剪,拍照回调裁剪可能在错误的位置裁剪
    val cropImageRect: Rect?
) {

    /**
     * @param bytes jpeg byteArray
     */
    @WorkerThread
    fun getBitmap(bytes: ByteArray, clip: Boolean = false): Bitmap {
        val image = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val rotatedImage = ImageUtil.rotate(image, captureRotation)
        if (cropImageRect == null || !clip) {
            return rotatedImage
        }
        return Bitmap.createBitmap(
            rotatedImage,
            cropImageRect.left, cropImageRect.top,
            cropImageRect.width(), cropImageRect.height()
        )
    }

    @WorkerThread
    fun bitmap2Bytes(src: Bitmap): ByteArray {
        return ImageUtil.bitmap2Bytes(src)
    }

    @WorkerThread
    fun compress(
        src: Bitmap,
        maxWidth: Int = 2000,
        maxHeight: Int = 2000,
        maxSize: Long = 795648L
    ): ByteArray {
        val compressedBitmap = ImageUtil.compressBySize(src, maxWidth, maxHeight)
        return ImageUtil.compressByQuality(compressedBitmap, maxSize, false)
    }
}

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
