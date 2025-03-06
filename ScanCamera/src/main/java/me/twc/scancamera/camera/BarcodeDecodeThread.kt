package me.twc.scancamera.camera

import com.google.zxing.BinaryBitmap
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.GenericMultipleBarcodeReader
import copy.com.journeyapps.barcodescanner.Decoder
import copy.com.journeyapps.barcodescanner.SourceData


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

    fun decodeMultiple(
        sourceData: SourceData,
        callback: (result: Array<Result>?) -> Unit
    ) = mThread?.enqueue {
        val luminanceSource = sourceData.createSource()
        val multiReader = GenericMultipleBarcodeReader(mDecoder.reader)
        var decodeResult: Array<Result>? = null
        try {
            decodeResult = multiReader.decodeMultiple(BinaryBitmap(HybridBinarizer(luminanceSource)))
        } catch (th: Throwable) {
            th.printStackTrace()
        }
        callback(decodeResult)
    }

    fun quit() {
        mThread?.quit()
        mThread = null
    }
}