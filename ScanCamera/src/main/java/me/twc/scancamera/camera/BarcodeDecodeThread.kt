package me.twc.scancamera.camera

import copy.com.journeyapps.barcodescanner.SourceData
import com.google.zxing.Result
import copy.com.journeyapps.barcodescanner.Decoder

/**
 * @author 唐万超
 * @date 2021/06/15
 */
class BarcodeDecodeThread(
    private val mDecoder: Decoder
) {

    private var mThread: CameraThread? = CameraThread("BarcodeDecodeThread")

    /**
     * 解码 [SourceData]
     *
     * @param sourceData SourceData
     */
    fun decode(
        sourceData: SourceData,
        callback: (result: Result?) -> Unit
    ) = mThread?.enqueue {
        val luminanceSource = sourceData.createSource()
        val decodeResult = mDecoder.decode(luminanceSource)
        callback(decodeResult)
    }

    fun quit() {
        mThread?.quit()
        mThread = null
    }
}