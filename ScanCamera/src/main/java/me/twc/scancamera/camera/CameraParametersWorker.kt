@file:Suppress("DEPRECATION")

package me.twc.scancamera.camera

import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.Camera
import android.util.Size
import android.view.Display
import android.view.TextureView
import androidx.annotation.FloatRange
import com.google.zxing.DecodeHintType
import copy.com.google.zxing.client.android.camera.CameraConfigurationUtils
import copy.com.google.zxing.client.android.camera.open.OpenCamera
import copy.com.journeyapps.barcodescanner.Decoder
import copy.com.journeyapps.barcodescanner.DecoderFactory
import copy.com.journeyapps.barcodescanner.DefaultDecoderFactory
import copy.com.journeyapps.barcodescanner.SourceData
import copy.com.journeyapps.barcodescanner.camera.AutoFocusManager
import copy.com.journeyapps.barcodescanner.camera.CenterCropStrategy
import me.twc.scancamera.camera.setting.FocusMode
import me.twc.scancamera.camera.setting.Settings
import me.twc.utils.logD
import me.twc.utils.rotation
import me.twc.utils.toSize
import kotlin.math.min

/**
 * @author 唐万超
 * @date 2021/06/09
 *
 */

/**
 * 用于设置相机参数
 *
 * @param cameraViewSize 预览容器的大小
 * @param displayRotation [Display.getRotation]
 * @param cameraDisplayOrientation [Camera.setDisplayOrientation]
 * @param settings 相机相关设置
 * @param mCropRectInCameraView 裁剪区域
 */
data class CameraParametersWorker(
    val cameraViewSize: Size,
    val displayRotation: Int,
    val cameraDisplayOrientation: Int,
    val settings: Settings,
    val mCropRectInCameraView: Rect? = null,
    val decoderFactory: DecoderFactory = DefaultDecoderFactory()
) {
    private val mPreviewScalingStrategy = CenterCropStrategy()

    /**
     * [set] 成功后,该值必然存在
     */
    var mBestPreviewSize: Size? = null
        private set

    /**
     * 设置已经开启的相机的参数
     *
     * @return [true : 参数设置成功]
     *         [false: 其他情况]
     */
    fun set(openCamera: OpenCamera): Boolean {
        val parameters = openCamera.camera.parameters
        if (parameters == null) {
            logD("设备异常: 无法获取相机参数")
            return false
        }
        val bestPreviewSize = getBestPreviewSize(parameters)
        if (bestPreviewSize == null) {
            logD("设备异常: 无法获取相机分辨率")
            return false
        }
        mBestPreviewSize = bestPreviewSize
        openCamera.camera.setDisplayOrientation(cameraDisplayOrientation)
        parameters.let {
            try {
                setInternal(parameters, bestPreviewSize, false)
            } catch (th: Throwable) {
                try {
                    setInternal(parameters, bestPreviewSize, true)
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
            }
            openCamera.camera.parameters = it
        }
        return true
    }

    /**
     * @param safeMode 安全模式下将尽可能少的设置参数
     */
    private fun setInternal(
        parameters: Camera.Parameters,
        previewSize: Size,
        safeMode: Boolean
    ) {
        setFocus(parameters, settings.focusMode, safeMode)
        if (!safeMode) {
            setTorchInternal(parameters, settings.torch)
            if (settings.invertColor) {
                setInvertColor(parameters)
            }
            if (settings.barcodeSceneModeEnabled) {
                setBarcodeSceneMode(parameters)
            }
        }
        parameters.setPreviewSize(previewSize.width, previewSize.height)
        if (!safeMode) {
            val photoSize = getBestPhotoSize(parameters, previewSize, settings.minPhotoSize)
            if (photoSize != null) {
                parameters.setPictureSize(photoSize.width, photoSize.height)
            }
        }
    }

    private fun getBestPreviewSize(parameter: Camera.Parameters): Size? {
        val sizes = parameter.supportedPreviewSizes ?: mutableListOf()
        if (sizes.isEmpty()) {
            parameter.previewSize?.let(sizes::add)
        }
        if (sizes.isEmpty()) return null
        sizes.forEach {
            logD("支持的预览大小:${it.width}-${it.height}")
        }
        val desired = if (isCameraRotated()) cameraViewSize.rotation() else cameraViewSize
        val best = mPreviewScalingStrategy.getBestPreviewSize(
            sizes.map { copy.com.journeyapps.barcodescanner.Size(it.width, it.height) },
            desired.toSize()
        ).toSize()
        logD("最佳预览大小:${best.width}-${best.height}")
        return best
    }

    private fun getBestPhotoSize(parameters: Camera.Parameters, previewSize: Size, min: Size): Size? {
        val sizes = parameters.supportedPictureSizes ?: mutableListOf()
        if (sizes.isEmpty()) {
            parameters.pictureSize?.let(sizes::add)
        }
        if (sizes.isEmpty()) return null
        sizes.forEach {
            logD("支持的照片大小:${it.width}-${it.height}")
        }
        val best = mPreviewScalingStrategy.getBestPhotoSize(
            sizes.map { copy.com.journeyapps.barcodescanner.Size(it.width, it.height) },
            previewSize.toSize(),
            min.toSize()
        ).toSize()
        logD("最佳照片大小:${best.width}-${best.height}")
        return best

    }

    /**
     * 预览/拍照旋转后图片裁剪区域
     */
    fun getCropPreviewRect(
        openCamera: OpenCamera,
        cropRectInCameraView: Rect?
    ): Rect? {
        if (cropRectInCameraView == null) return null

        if (cropRectInCameraView.width() <= 0 || cropRectInCameraView.height() <= 0) {
            logD("无效的裁剪区域")
            return null
        }

        var previewSize = openCamera.camera.parameters.previewSize.toSize()
        if (isCameraRotated()) {
            previewSize = previewSize.rotation()
        }
        val scalePreviewRect = mPreviewScalingStrategy.scalePreview(
            previewSize.toSize(),
            cameraViewSize.toSize()
        )
        cropRectInCameraView.offset(-scalePreviewRect.left, -scalePreviewRect.top)
        val resultRect = Rect(
            cropRectInCameraView.left * previewSize.width / scalePreviewRect.width(),
            cropRectInCameraView.top * previewSize.height / scalePreviewRect.height(),
            cropRectInCameraView.right * previewSize.width / scalePreviewRect.width(),
            cropRectInCameraView.bottom * previewSize.height / scalePreviewRect.height(),
        )
        // 精度损失粗暴修复
        if (cropRectInCameraView.width() == cropRectInCameraView.height() && resultRect.width() != resultRect.height()) {
            val fixedSize = (resultRect.width() + resultRect.height()) / 2
            resultRect.right = resultRect.left + fixedSize
            resultRect.bottom = resultRect.top + fixedSize
        }
        return resultRect
    }

    /**
     * @return [true : 相机旋转垂直于当前显示旋转]
     */
    private fun isCameraRotated(): Boolean {
        return cameraDisplayOrientation % 180 != 0
    }

    /**
     * 计算 [TextureView] 的变换
     * [Matrix] 会导致预览被放大/缩小以填充 [TextureView]
     *
     * @param previewSize 相机预览分辨率
     */
    fun calculatePreviewCenterCrop(previewSize: Size): Matrix {
        val rotationSize = if (isCameraRotated()) previewSize.rotation() else previewSize
        val ratioTexture = cameraViewSize.width.toFloat() / cameraViewSize.height.toFloat()
        val ratioPreview = rotationSize.width.toFloat() / rotationSize.height.toFloat()
        val scaleX: Float
        val scaleY: Float

        if (ratioTexture < ratioPreview) {
            scaleX = ratioPreview / ratioTexture
            scaleY = 1f
        } else {
            scaleX = 1f
            scaleY = ratioTexture / ratioPreview
        }
        val matrix = Matrix()
        matrix.setScale(scaleX, scaleY)

        // Center the preview
        val scaledWidth: Float = cameraViewSize.width * scaleX
        val scaledHeight: Float = cameraViewSize.height * scaleY
        val dx: Float = (cameraViewSize.width - scaledWidth) / 2
        val dy: Float = (cameraViewSize.height - scaledHeight) / 2

        // Perform the translation on the scaled preview
        matrix.postTranslate(dx, dy)
        return matrix
    }

    //<editor-fold desc="设置对焦">
    private fun setFocus(
        parameters: Camera.Parameters,
        focusModeSetting: FocusMode,
        safeMode: Boolean
    ) {
        val supportedFocusModes = parameters.supportedFocusModes
        var focusMode: String? = null
        if (safeMode || focusModeSetting === FocusMode.AUTO) {
            focusMode = findSettableValue(
                "focus mode",
                supportedFocusModes,
                Camera.Parameters.FOCUS_MODE_AUTO
            )
        } else if (focusModeSetting === FocusMode.CONTINUOUS) {
            focusMode = findSettableValue(
                "focus mode",
                supportedFocusModes,
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE,
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO,
                Camera.Parameters.FOCUS_MODE_AUTO
            )
        } else if (focusModeSetting === FocusMode.INFINITY) {
            focusMode = findSettableValue(
                "focus mode",
                supportedFocusModes,
                Camera.Parameters.FOCUS_MODE_INFINITY
            )
        } else if (focusModeSetting === FocusMode.MACRO) {
            focusMode = findSettableValue(
                "focus mode",
                supportedFocusModes,
                Camera.Parameters.FOCUS_MODE_MACRO
            )
        }

        // Maybe selected auto-focus but not available, so fall through here:
        if (!safeMode && focusMode == null) {
            focusMode = findSettableValue(
                "focus mode",
                supportedFocusModes,
                Camera.Parameters.FOCUS_MODE_MACRO,
                Camera.Parameters.FOCUS_MODE_EDOF
            )
        }
        if (focusMode != null && parameters.focusMode != focusMode) {
            parameters.focusMode = focusMode
        }
        logD("对焦模式为 $focusMode")
    }
    //</editor-fold>

    //<editor-fold desc="设置手电筒">
    private fun setTorchInternal(parameters: Camera.Parameters, on: Boolean) =
        CameraConfigurationUtils.setTorch(parameters, on)

    private fun isTorchOn(parameters: Camera.Parameters): Boolean {
        return parameters.flashMode == Camera.Parameters.FLASH_MODE_ON ||
                parameters.flashMode == Camera.Parameters.FLASH_MODE_TORCH
    }

    fun setTorch(camera: OpenCamera, on: Boolean, autoFocusManager: AutoFocusManager? = null) {
        try {
            val parameter = camera.camera.parameters
            val isTorchOn = isTorchOn(parameter)
            if (isTorchOn == on) return
            autoFocusManager?.stop()
            setTorchInternal(parameter, on)
            if (settings.exposureEnabled) {
                CameraConfigurationUtils.setBestExposure(parameter, on)
            }
            camera.camera.parameters = parameter
            autoFocusManager?.start()
        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }
    //</editor-fold>

    //<editor-fold desc="设置对焦">
    fun setManualFocusing(camera: OpenCamera, rect: Rect) {
        try {
            val parameter = camera.camera.parameters
            camera.camera.cancelAutoFocus()
            if (parameter.maxNumFocusAreas > 0) {
                parameter.focusAreas = listOf(Camera.Area(rect, 1000))
            }
            val focusMode = parameter.focusMode
            parameter.focusMode = Camera.Parameters.FOCUS_MODE_MACRO
            camera.camera.parameters = parameter
            camera.camera.autoFocus { success, _ ->
                logD("对焦结果: $success $rect")
                try {
                    val p = camera.camera.parameters
                    parameter.focusMode = focusMode
                    camera.camera.parameters = p
                } catch (th: Throwable) {
                    th.printStackTrace()
                }
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }
    //</editor-fold>

    //<editor-fold desc="设置焦距">
    /**
     * @param zoom 放大倍数
     *
     * @return [true : 设置成功]
     *         [false: 设置失败]
     */
    fun setZoom(camera: OpenCamera, @FloatRange(from = 1.0) zoom: Float): Boolean {
        try {
            val parameters = camera.camera.parameters
            if (!parameters.isZoomSupported) return false
            val zoomRatios = parameters.zoomRatios
            var selectIndex = zoomRatios.size - 1
            for ((index, value) in zoomRatios.withIndex()) {
                if (value >= zoom * 100) {
                    selectIndex = index
                    break
                }
            }
            parameters.zoom = min(selectIndex, parameters.maxZoom)
            camera.camera.parameters = parameters
            return true
        } catch (th: Throwable) {
            th.printStackTrace()
        }
        return false
    }
    //</editor-fold>

    //<editor-fold desc="反正深色/浅色">
    private fun setInvertColor(parameter: Camera.Parameters) =
        CameraConfigurationUtils.setInvertColor(parameter)
    //</editor-fold>

    //<editor-fold desc="设置相机为条码场景模式">
    private fun setBarcodeSceneMode(parameter: Camera.Parameters) =
        CameraConfigurationUtils.setBarcodeSceneMode(parameter)
    //</editor-fold>

    private fun findSettableValue(
        @Suppress("UNUSED_PARAMETER", "SameParameterValue") name: String,
        supportedValues: Collection<String>?,
        vararg desiredValues: String
    ): String? {
        if (supportedValues == null) return null
        for (desiredValue in desiredValues) {
            if (supportedValues.contains(desiredValue)) {
                return desiredValue
            }
        }
        return null
    }

    //<editor-fold desc="获取 SourceData">
    fun createSourceData(
        data: ByteArray?,
        camera: Camera?,
        cropRect: Rect? = null
    ): SourceData? {
        if (data == null || camera == null) return null
        val previewFormat = camera.parameters.previewFormat
        val previewSize = camera.parameters.previewSize
        return SourceData(
            data,
            previewSize.width,
            previewSize.height,
            previewFormat,
            cameraDisplayOrientation
        ).apply {
            this.cropRect = cropRect
        }
    }
    //</editor-fold>

    //<editor-fold desc="获取 Decoder">
    fun getDecoder(): Decoder {
        val decodeHintTypes = mutableMapOf<DecodeHintType, Any>()
        decodeHintTypes[DecodeHintType.POSSIBLE_FORMATS] = settings.decodeFormats
        return decoderFactory.createDecoder(decodeHintTypes)
    }
    //</editor-fold>
}