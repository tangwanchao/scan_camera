package me.twc.utils

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Matrix
import androidx.annotation.WorkerThread
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import kotlin.math.min

/**
 * @author 唐万超
 * @date 2025/03/07
 */
object ImageUtil {

    /**
     * 旋转图片
     */
    @WorkerThread
    fun rotate(
        src: Bitmap,
        degrees: Int,
        px: Float = src.width / 2f,
        py: Float = src.height / 2f,
        recycle: Boolean = false,
    ): Bitmap {
        if (src.width == 0 || src.height == 0 || degrees == 0) return src
        val matrix = Matrix()
        matrix.setRotate(degrees.toFloat(), px, py)
        val ret = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        if (recycle && !src.isRecycled && ret != src) src.recycle()
        return ret
    }


    /**
     * 根据质量压缩图片
     */
    @WorkerThread
    fun compressByQuality(
        src: Bitmap,
        maxByteSize: Long,
        recycle: Boolean
    ): ByteArray {
        if (src.width == 0 || src.height == 0 || maxByteSize <= 0) return ByteArray(0)
        val baos = ByteArrayOutputStream()
        src.compress(CompressFormat.JPEG, 100, baos)
        val bytes: ByteArray
        if (baos.size() <= maxByteSize) {
            bytes = baos.toByteArray()
        } else {
            baos.reset()
            src.compress(CompressFormat.JPEG, 0, baos)
            if (baos.size() >= maxByteSize) {
                bytes = baos.toByteArray()
            } else {
                // find the best quality using binary search
                var st = 0
                var end = 100
                var mid = 0
                while (st < end) {
                    mid = (st + end) / 2
                    baos.reset()
                    src.compress(CompressFormat.JPEG, mid, baos)
                    val len = baos.size()
                    if (len.toLong() == maxByteSize) {
                        break
                    } else if (len > maxByteSize) {
                        end = mid - 1
                    } else {
                        st = mid + 1
                    }
                }
                if (end == mid - 1) {
                    baos.reset()
                    src.compress(CompressFormat.JPEG, st, baos)
                }
                bytes = baos.toByteArray()
            }
        }
        if (recycle && !src.isRecycled) src.recycle()
        return bytes
    }

    /**
     * 根据尺寸压缩图片
     */
    @WorkerThread
    fun compressBySize(src: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width =src.width
        val height = src.height
        if (src.width <= maxWidth && src.height <= maxHeight) return src
        var widthScale = 1f
        var heightScale = 1f
        if(width > maxWidth){
            widthScale = maxWidth.toFloat() / width.toFloat()
        }
        if(height > maxHeight){
            heightScale = maxHeight.toFloat() / height.toFloat()
        }
        val scale = min(widthScale,heightScale)
        val scaledWidth = (width * scale).toInt()
        val scaledHeight = (height * scale).toInt()
        return src.scale(scaledWidth,scaledHeight)
    }

    @WorkerThread
    fun bitmap2Bytes(
        bitmap: Bitmap,
        format: CompressFormat = CompressFormat.JPEG,
        quality: Int = 100
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        bitmap.compress(format, quality, baos)
        return baos.toByteArray()
    }
}