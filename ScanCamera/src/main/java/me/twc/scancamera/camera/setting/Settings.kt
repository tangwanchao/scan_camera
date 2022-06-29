package me.twc.scancamera.camera.setting

import android.util.Size
import androidx.annotation.Keep
import com.google.zxing.BarcodeFormat

/**
 * @author 唐万超
 * @date 2021/06/11
 */


/**
 * @param focusMode 相机对焦模式
 * @param torch 是否打开手电筒
 * @param invertColor 反转深色浅色
 * @param barcodeSceneModeEnabled 相机设置为条码场景
 * @param exposureEnabled 启用曝光
 * @param decodeFormats 支持的解码格式
 */
@Keep
data class Settings(
    val focusMode: FocusMode = FocusMode.AUTO,
    val torch: Boolean = false,
    val invertColor: Boolean = false,
    val barcodeSceneModeEnabled: Boolean = false,
    val exposureEnabled: Boolean = false,
    val decodeFormats: List<BarcodeFormat> = listOf(
        BarcodeFormat.CODE_128,
        BarcodeFormat.QR_CODE
    ),
    val minPhotoSize: Size = Size(1080, 1080)
) {
    val autoFocusEnable = focusMode == FocusMode.AUTO
}