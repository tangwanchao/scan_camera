package me.twc.scancamera.camera.setting

import androidx.annotation.Keep

/**
 * @author 唐万超
 * @date 2021/06/11
 */

/**
 * 相机对焦模式
 */
@Keep
enum class FocusMode {
    /**
     * 自动对焦
     */
    AUTO,

    /**
     * 连续自动对焦
     */
    CONTINUOUS,

    /**
     * 焦点设置在无限远
     */
    INFINITY,

    /**
     * 微距（特写）对焦模式
     */
    MACRO;
}