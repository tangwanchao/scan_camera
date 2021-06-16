package me.twc.scancamera.camera

import com.journeyapps.barcodescanner.SourceData
import com.google.zxing.Result

/**
 * @author 唐万超
 * @date 2021/06/15
 */
class BarcodeDecodeThread(
    private val mDecoder: com.journeyapps.barcodescanner.Decoder
) {

    private var mThread: CameraThread? = CameraThread("BarcodeDecodeThread")

    /**
     * 解码 [SourceData]
     *
     * @param sourceData SourceData
     */
    fun decode(
        sourceData: com.journeyapps.barcodescanner.SourceData,
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