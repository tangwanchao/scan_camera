@file:Suppress("DEPRECATION", "unused")

package me.twc.scancamera.camera

import android.content.Context
import android.graphics.Rect
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.FrameLayout
import androidx.annotation.FloatRange
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import copy.com.google.zxing.client.android.camera.open.OpenCamera
import copy.com.google.zxing.client.android.camera.open.OpenCameraInterface
import copy.com.journeyapps.barcodescanner.camera.AutoFocusManager
import me.twc.impl.SimpleSurfaceTextureListener
import me.twc.scancamera.camera.decoration.CropRect
import me.twc.scancamera.camera.setting.Settings
import me.twc.utils.RotationUtil
import me.twc.utils.logD
import kotlin.math.max

/**
 * @author 唐万超
 * @date 2021/06/09
 */
@Suppress("MemberVisibilityCanBePrivate")
class CameraView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val mDisplayOrientationDetector = DisplayOrientationDetector(context)
    private val mCameraManager = CameraManager()
    private val mTextureView: TextureView = TextureView(context).apply {
        this.surfaceTextureListener = mCameraManager
        addView(this@apply, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }
    private lateinit var mSettings: Settings

    // 是否支持手动对焦
    private var mManualFocus = false

    override fun onAttachedToWindow() {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        mDisplayOrientationDetector.enable(wm.defaultDisplay)
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mDisplayOrientationDetector.disable()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        mCameraManager.onCameraViewSized(width, height)
        super.onLayout(changed, left, top, right, bottom)
    }

    /**
     * 获取相对于 [CameraView] 的裁剪区域
     */
    private fun getCropRect(): Rect? {
        fun findCropView(group: ViewGroup): View? {
            for (child in group.children) {
                if (child is CropRect) {
                    return child
                }
                if (child is ViewGroup) {
                    val find = findCropView(child)
                    if (find != null) {
                        return find
                    }
                }
            }
            return null
        }

        val cropViewOr: View? = findCropView(this)

        if (cropViewOr == null || !cropViewOr.isVisible || !cropViewOr.isEnabled) {
            return null
        }
        return (cropViewOr as CropRect).getRectInCameraView()
    }

    //<editor-fold desc="公开 API">
    fun setSettings(settings: Settings) {
        mSettings = settings
    }

    /**
     * 重新启动预览
     * 此方法可安全随时调用
     * 一般在 [takePicture] 收到结果后调用
     */
    fun restartPreview() = mCameraManager.restartPreview()

    fun create() = mCameraManager.startProcess()

    fun resume() = mCameraManager.startProcess()

    fun pause() = mCameraManager.stopProcess()

    fun destroy() {
        try {
            mCameraManager.destroy()
            mTextureView.surfaceTexture?.release()
        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }

    /**
     * 将 [CameraView] 和 [Lifecycle] 绑定
     * 绑定后 [CameraView] 将自动工作
     */
    fun bindLifecycle(lifecycle: Lifecycle, settings: Settings = Settings()) {
        setSettings(settings)
        val observer = CameraViewLifecycleObserver(this)
        lifecycle.addObserver(observer)
    }

    //<editor-fold desc="功能性 API">
    // 设置手电筒
    fun setTorch(on: Boolean) = mCameraManager.setTorch(on)

    // 设置手动对焦,只支持竖屏锁定下手动对焦
    fun setManualFocus(enable: Boolean) {
        mManualFocus = enable
    }

    // 预览缩放
    fun setScale(scale: Float) {
        mTextureView.scaleX = scale
        mTextureView.scaleY = scale
    }

    // 焦距
    fun setZoom(@FloatRange(from = 1.0) zoom: Float, callback: (success: Boolean) -> Unit) {
        mCameraManager.setZoom(zoom, callback)
    }

    // 扫码
    fun scanBarcode(callback: ScanBarcodeCallback) = mCameraManager.scanBarcode(callback)

    // 拍照
    fun takePicture(takePictureCallback: TakePictureCallback) =
        mCameraManager.takePicture(takePictureCallback)
    //</editor-fold>

    //<editor-fold desc="相机管理器">
    /**
     * [CameraManager] 负责控制相机相关功能
     * 比如打开相机,设置相机参数,开启预览等
     */
    inner class CameraManager(
        private val mCameraThread: CameraThread = CameraThread()
    ) : SimpleSurfaceTextureListener, Camera.PreviewCallback {
        //<editor-fold desc="私有属性">
        var mCamera: OpenCamera? = null
            private set
        var mCameraParametersWorker: CameraParametersWorker? = null
            private set
        private var mProcess: StartCameraPreviewProcess? = null
        private val mSizeHandler = SizeHandler()

        /**
         * 是否已经成功开启预览的标记
         */
        private var mStartedPreview: Boolean = false
        private var mAutoFocusManager: AutoFocusManager? = null
        private var mDecodeThread: BarcodeDecodeThread? = null
        private var mScanBarcodeCallback: ScanBarcodeCallback? = null

        //</editor-fold>

        //<editor-fold desc="相机相关操作">

        //<editor-fold desc="打开相机">
        private fun openCamera() = mCameraThread.enqueue {
            val camera = OpenCameraInterface.open(Camera.CameraInfo.CAMERA_FACING_BACK)
            mCamera = camera
            if (camera != null) {
                post { requestLayout() }
                logD("打开相机成功, camera = $mCamera")
            } else {
                logD("打开相机失败")
            }
        }
        //</editor-fold>

        //<editor-fold desc="设置相机参数">
        private fun setCamera(cameraParametersWorker: CameraParametersWorker) =
            mCameraThread.enqueue {
                if (mStartedPreview) {
                    logD("设置相机参数失败,已经开始预览")
                    return@enqueue
                }
                if (mCameraParametersWorker != null) {
                    logD("设置相机参数失败,参数已设置")
                    return@enqueue
                }
                val camera = mCamera
                if (camera == null) {
                    logD("设置相机参数失败,相机未打开")
                    return@enqueue
                }
                if (!cameraParametersWorker.set(camera)) {
                    return@enqueue
                }
                val bestSize = cameraParametersWorker.mBestPreviewSize!!
                mSizeHandler.setPreviewSize(previewSize = bestSize)
                mCameraParametersWorker = cameraParametersWorker
                logD("设置相机参数成功: parameters = $cameraParametersWorker")
            }
        //</editor-fold>

        //<editor-fold desc="开始预览">
        private fun startPreview(): Unit = mCameraThread.enqueue {
            if (mStartedPreview) {
                logD("开始预览失败: 已经开始预览")
                return@enqueue
            }
            val camera = mCamera
            if (camera == null) {
                logD("开始预览失败: 没有打开的相机")
                return@enqueue
            }
            val cameraParameterWorker = mCameraParametersWorker
            if (cameraParameterWorker == null) {
                logD("开始预览失败: 找不到相机参数")
                return@enqueue
            }
            val previewSize = mSizeHandler.getPreviewSize()
            if (previewSize == null || !mSizeHandler.ready()) {
                logD("开始预览失败: 启动相机所需参数未准备完毕")
                return@enqueue
            }
            camera.camera.let {
                val transform = cameraParameterWorker.calculatePreviewCenterCrop(previewSize)
                post { mTextureView.setTransform(transform) }
                it.setPreviewTexture(mTextureView.surfaceTexture)
                it.startPreview()
            }
            mStartedPreview = true
            mAutoFocusManager =
                AutoFocusManager(camera.camera, mSettings)
            mDecodeThread =
                BarcodeDecodeThread(cameraParameterWorker.getDecoder())
            mScanBarcodeCallback?.let(::scanBarcode)
            logD("开启预览成功")
        }
        //</editor-fold>

        //<editor-fold desc="重启预览">
        fun restartPreview() = mCameraThread.enqueue {
            val camera = mCamera
            if (camera == null) {
                logD("重启预览失败")
                return@enqueue
            }
            camera.camera.startPreview()
            mStartedPreview = true
            mScanBarcodeCallback?.let(::scanBarcode)
        }
        //</editor-fold>

        //<editor-fold desc="结束预览并退出相机">
        private fun closeCamera() = mCameraThread.enqueue {
            mAutoFocusManager?.stop()
            mAutoFocusManager = null

            mCamera?.camera?.let {
                it.setPreviewCallback(null)
                it.setPreviewDisplay(null)
                it.stopPreview()
                it.release()
                logD("退出预览并释放相机成功")
            }
            mCamera = null
            mCameraParametersWorker = null
            mProcess = null
            mStartedPreview = false
            mSizeHandler.setPreviewSize(previewSize = null)
            mDecodeThread?.quit()
            mDecodeThread = null
            mScanBarcodeCallback = null
        }
        //</editor-fold>
        //</editor-fold>

        //<editor-fold desc="相机启动后">

        //<editor-fold desc="开启/关闭闪光灯">
        fun setTorch(on: Boolean) = mCameraThread.enqueue {
            val camera = mCamera ?: return@enqueue
            val cameraParametersWorker = mCameraParametersWorker ?: return@enqueue
            cameraParametersWorker.setTorch(camera, on, mAutoFocusManager)
        }
        //</editor-fold>

        //<editor-fold desc="手动对焦">
        fun setManualFocusing(rect: Rect) = mCameraThread.enqueue {
            val camera = mCamera ?: return@enqueue
            val cameraParametersWorker = mCameraParametersWorker ?: return@enqueue
            cameraParametersWorker.setManualFocusing(camera, rect)
        }
        //</editor-fold>

        //<editor-fold desc="设置焦距">
        fun setZoom(@FloatRange(from = 1.0) zoom: Float, callback: (success: Boolean) -> Unit) = mCameraThread.enqueue {
            val camera = mCamera
            val cameraParametersWorker = mCameraParametersWorker
            if (camera == null || cameraParametersWorker == null) {
                callback(false)
                return@enqueue
            }
            val result = cameraParametersWorker.setZoom(camera, zoom)
            callback(result)
        }
        //</editor-fold>

        //<editor-fold desc="预览回调">
        override fun onPreviewFrame(data: ByteArray?, camera: Camera?) {
            val callback = mScanBarcodeCallback ?: return
            val cameraParametersWorker = mCameraParametersWorker
            val decodeThread = mDecodeThread
            val openCamera = mCamera
            if (data == null ||
                camera == null ||
                openCamera == null ||
                cameraParametersWorker == null ||
                decodeThread == null
            ) {
                if (callback.autoRetry()) {
                    scanBarcode(callback)
                }
                return
            }
            val cropRect = cameraParametersWorker.getCropPreviewRect(openCamera, getCropRect())
            val sourceData = cameraParametersWorker.createSourceData(data, camera, cropRect)
            if (sourceData == null) {
                logD("source data is null")
                return
            }
            decodeThread.decode(sourceData) {
                // 重新检查
                if (mScanBarcodeCallback != null) {
                    if (it != null) {
                        callback.onResult(it)
                    } else if (callback.autoRetry()) {
                        scanBarcode(callback)
                    }
                }
            }
        }
        //</editor-fold>

        //<editor-fold desc="扫码">
        fun scanBarcode(callback: ScanBarcodeCallback) = mCameraThread.enqueue {
            mScanBarcodeCallback = callback
            if (!mStartedPreview) {
                return@enqueue
            }
            mCamera?.camera?.setOneShotPreviewCallback(this)
        }
        //</editor-fold>

        //<editor-fold desc="拍照">
        fun takePicture(takePictureCallback: TakePictureCallback) = mCameraThread.enqueue {
            if (!mStartedPreview) {
                logD("未开启相机预览,无法拍照")
                takePictureCallback.takePictureError?.invoke()
                return@enqueue
            }
            val camera = mCamera
            if (camera == null) {
                logD("相机不存在,无法拍照")
                takePictureCallback.takePictureError?.invoke()
                return@enqueue
            } else {
                try {
                    mStartedPreview = false
                    mScanBarcodeCallback = null
                    camera.camera.setOneShotPreviewCallback(null)
                    val displayRotation = mDisplayOrientationDetector.getDisplayRotation()
                    val deviceRotation = mDisplayOrientationDetector.getDeviceRotation()
                    val captureRotation = RotationUtil.calculateCaptureRotation(
                        camera,
                        displayRotation,
                        deviceRotation
                    )
                    val info = PictureInfo(
                        displayRotation,
                        deviceRotation,
                        captureRotation
                    )
                    camera.camera.takePicture(
                        takePictureCallback.shutter,
                        takePictureCallback.raw,
                        takePictureCallback.postview
                    ) { data, _ ->
                        takePictureCallback.jpeg?.invoke(data, camera.camera, info)
                        if (takePictureCallback.autoStartPreview) {
                            restartPreview()
                        }
                    }

                } catch (th: Throwable) {
                    logD("拍照异常", th)
                    takePictureCallback.takePictureError?.invoke()
                    mStartedPreview = true
                    mScanBarcodeCallback?.let(::scanBarcode)
                }
            }
        }
        //</editor-fold>

        //</editor-fold>

        //<editor-fold desc="回调">

        //<editor-fold desc="SurfaceTextureListener">
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            super.onSurfaceTextureAvailable(surface, width, height)
            onSurfaceTextureSizeChanged(surface, width, height)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            super.onSurfaceTextureSizeChanged(surface, width, height)
            mSizeHandler.setCurrentSurfaceSize(currentSurfaceSize = Size(width, height))
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            mSizeHandler.setCurrentSurfaceSize(currentSurfaceSize = null)
            return false
        }
        //</editor-fold>

        //<editor-fold desc="SizeHandler 准备完毕回调">
        private fun onSizeHandlerReady() = mCameraThread.enqueue {
            val process = mProcess
            if (process == null) {
                logD("onSurfaceReady process is null")
                return@enqueue
            }
            process.onSizeHandlerReady()
        }
        //</editor-fold>

        //<editor-fold desc="CameraView 有了大小回调">
        fun onCameraViewSized(width: Int, height: Int) = mCameraThread.enqueue {
            val process = mProcess
            if (process == null) {
                logD("onCameraPreviewSized: mProcess == null")
                return@enqueue
            }
            val displayRotation = RotationUtil.getDisplayRotation(context)
            if (displayRotation == -1) {
                logD("onCameraPreviewSized: rotation == -1")
                return@enqueue
            }
            val camera = mCamera
            if (camera == null) {
                logD("onCameraPreviewSized: mCamera == null")
                return@enqueue
            }
            val cameraDisplayOrientation =
                RotationUtil.getCameraDisplayOrientation(camera, displayRotation)

            val parameters = CameraParametersWorker(
                Size(width, height),
                displayRotation,
                cameraDisplayOrientation,
                mSettings,
                getCropRect()
            )
            process.onCameraViewSized(parameters)
        }
        //</editor-fold>

        //</editor-fold>

        /**
         * 开始一个新的 [StartCameraPreviewProcess]
         * 如果 [mProcess] 存在,这次调用无效
         */
        fun startProcess() = mCameraThread.enqueue {
            if (mProcess != null) {
                logD("CameraStartPreviewProcess 正在运行")
                return@enqueue
            }
            mProcess = StartCameraPreviewProcess()
            logD("开启相机预览流程成功")
        }

        /**
         * 结束相机启动进程
         */
        fun stopProcess() = closeCamera()

        /**
         *
         * 表示启动相机预览的一个具体流程
         * 1.开启相机,相机成功开启触发 [CameraView] layout
         * 2.等待设置相机参数
         * 3.等待 [SizeHandler.ready]
         * 4.开启预览
         *
         * [openCamera]                                                               | CameraThread
         *      ↓↓↓
         * [CameraView.onLayout]                                                      | MainThread
         *      ↓↓↓
         * [CameraManager.onCameraViewSized]                                          | CameraThread
         *      ↓↓↓
         * [StartCameraPreviewProcess.onCameraViewSized]                              | CameraThread
         *      ↓↓↓
         * [CameraManager.setCamera]    | [CameraManager.onSurfaceTextureSizeChanged] | AnyThread
         *      ↓↓↓                              ↓↓↓
         * [SizeHandler.setPreviewSize] | [SizeHandler.setCurrentSurfaceSize]         | CameraThread
         *      ↓↓↓                              ↓↓↓
         * [SizeHandler.tryNotifyReady]                                               | CameraThread
         *      ↓↓↓
         * [CameraManager.onSizeHandlerReady]                                         | CameraThread
         *      ↓↓↓
         * [StartCameraPreviewProcess.onSizeHandlerReady]                             | CameraThread
         *      ↓↓↓
         * [CameraManager.startPreview]                                               | CameraThread
         */
        inner class StartCameraPreviewProcess {
            init {
                openCamera()
            }

            fun onCameraViewSized(setCameraParameters: CameraParametersWorker) {
                setCamera(setCameraParameters)
            }

            fun onSizeHandlerReady() {
                startPreview()
            }
        }

        //<editor-fold desc="SizeHandler">
        /**
         * [SizeHandler] 持有相机开启预览所需 Size
         *
         * @param mPreviewSize 相机设置的预览分辨率,存在表示相机预览已经设置完毕
         * @param mCurrentSurfaceSize 当前 Surface 的大小,存在表示 Surface 可用
         */
        inner class SizeHandler(
            private var mPreviewSize: Size? = null,
            private var mCurrentSurfaceSize: Size? = null
        ) {
            /**
             * @return [true : 开启预览所需要 Size 参数配置完毕]
             *         [false: 其他情况]
             */
            fun ready(): Boolean {
                mPreviewSize ?: return false
                mCurrentSurfaceSize ?: return false
                return mTextureView.isAvailable
            }

            fun setCurrentSurfaceSize(currentSurfaceSize: Size?) = mCameraThread.enqueue {
                mCurrentSurfaceSize = currentSurfaceSize
                tryNotifyReady()
            }

            fun setPreviewSize(previewSize: Size?) = mCameraThread.enqueue {
                mPreviewSize = previewSize
                tryNotifyReady()
            }

            private fun tryNotifyReady() {
                if (ready()) {
                    onSizeHandlerReady()
                } else {
                    logD("SizeHandler 未准备完毕 SizeHandler = $this mTextureView.isAvailable = ${mTextureView.isAvailable}")
                }
            }

            fun getPreviewSize() = mPreviewSize

            override fun toString(): String {
                return "mPreviewSize = $mPreviewSize, mCurrentSurfaceSize = $mCurrentSurfaceSize"
            }
        }
        //</editor-fold>

        fun useCameraThread(block: () -> Unit) = mCameraThread.enqueue {
            block()
        }

        /**
         * 销毁该相机管理器,销毁后任何调用无效
         */
        fun destroy() {
            mCameraThread.quit()
        }
    }
    //</editor-fold>

    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (mManualFocus && event.action == MotionEvent.ACTION_DOWN) {
            val cameraOrientation = mCameraManager.mCamera?.orientation
            if (cameraOrientation != null) {
                val rect = calculateTapArea(event.x, event.y, width, height)
                if (rect != null) {
                    when (cameraOrientation) {
                        90 -> {
                            rect.offset(1000, 1000)
                            rect.counterclockwiseRotation90()
                            rect.offset(-1000, -1000)
                        }
                        180 -> {
                            rect.offset(1000, 1000)
                            rect.counterclockwiseRotation90()
                            rect.counterclockwiseRotation90()
                            rect.offset(-1000, -1000)
                        }
                        270 -> {
                            rect.counterclockwiseRotation90()
                            rect.counterclockwiseRotation90()
                            rect.counterclockwiseRotation90()
                        }
                    }
                    mCameraManager.setManualFocusing(rect)
                }

            }
        }
        return super.onTouchEvent(event)
    }

    private fun Rect.counterclockwiseRotation90() {
        val newCenterX = this.centerY()
        val newCenterY = 2000 - this.centerX()
        val left = clamp(newCenterX - this.height() / 2,0,2000)
        val top = clamp(newCenterY - this.width() / 2,0,2000)
        val right = clamp(left + this.height(),0,2000)
        val bottom = clamp(top + this.width(),0,2000)
        this.set(left, top, right, bottom)
    }

    /**
     * 转换对焦区域
     * 范围(-1000, -1000, 1000, 1000)
     * x,y是坐标位置，width,height SurfaceView的宽高，coefficient是区域比例大小
     */
    private fun calculateTapArea(
        x: Float,
        y: Float,
        surfaceWidth: Int,
        surfaceHeight: Int,
        coefficient: Float = 0.5f
    ): Rect? {
        if (surfaceWidth <= 0 || surfaceHeight <= 0) {
            return null
        }
        val focusAreaSize = 200
        //这段代码可以看出coefficient的作用，只是为了扩展areaSize
        val areaSize = max(50, focusAreaSize * coefficient.toInt())
        //解释一下为什么*2000
        // 因为要把surfaceView的坐标转换为范围(-1000, -1000, 1000, 1000)
        // 则SurfaceView的中心点坐标会转化为（0,0)
        // x/surfaceWidth,得到当前x坐标占总宽度的比例,然后乘以2000就换算成了（0,0，2000,2000）的坐标范围内
        // 然后减去1000，就换算为了范围(-1000, -1000, 1000, 1000)的坐标。
        // 得到了x,y转换后的坐标，利用areaSize就可以得到聚焦区域。
        val centerX = (x / surfaceWidth * 2000 - 1000).toInt()
        val centerY = (y / surfaceHeight * 2000 - 1000).toInt()
        val left = clamp(centerX - (areaSize / 2))
        val top = clamp(centerY - (areaSize / 2))
        val right = clamp(left + areaSize)
        val bottom = clamp(top + areaSize)
        return Rect(left, top, right, bottom)
    }

    /**
     * 不大于最大值,不小于最小值
     */
    private fun clamp(x: Int, min: Int = -1000, max: Int = 1000): Int {
        if (x > max) {
            return max
        }
        if (x < min) {
            return min
        }
        return x
    }
    //</editor-fold>
}