package me.twc.scancamera.camera.decoration

import android.graphics.Rect

/**
 * @author 唐万超
 * @date 2021/06/15
 */

/**
 * 实现此接口,表示图片需要裁剪
 */
interface CropRect {
    fun getRectInCameraView(): Rect?
}