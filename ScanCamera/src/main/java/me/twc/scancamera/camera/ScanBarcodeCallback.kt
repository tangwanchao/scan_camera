package me.twc.scancamera.camera

import com.google.zxing.Result
/**
 * @author 唐万超
 * @date 2021/06/15
 */
interface ScanBarcodeCallback {

    fun autoRetry(): Boolean = true

    fun onResult(result: Result)
}