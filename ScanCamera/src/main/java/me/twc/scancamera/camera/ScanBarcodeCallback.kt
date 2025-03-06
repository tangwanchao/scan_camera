package me.twc.scancamera.camera

import com.google.zxing.Result

/**
 * @author 唐万超
 * @date 2021/06/15
 */
interface ScanBarcodeCallback {

    // 是否自动重试
    fun autoRetry(): Boolean = true

    // 是否支持多个条码
    fun isMultiple(): Boolean = false

    fun onResult(result: Result) {}

    fun onResult(result: Array<Result>) {}
}