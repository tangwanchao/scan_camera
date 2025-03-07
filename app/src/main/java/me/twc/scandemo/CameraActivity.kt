package me.twc.scandemo

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.Result
import me.twc.scancamera.camera.ScanBarcodeCallback
import me.twc.scancamera.camera.TakePictureCallback
import me.twc.scancamera.camera.setting.FocusMode
import me.twc.scancamera.camera.setting.Settings
import me.twc.scandemo.databinding.ActCameraBinding
import me.twc.utils.logD
import java.io.File

/**
 * @author 唐万超
 * @date 2021/06/08
 */

class CameraActivity : AppCompatActivity() {

    private val mBinding by lazy { ActCameraBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)
        val settings = Settings(
            torch = false,
            focusMode = FocusMode.CONTINUOUS
        )
        mBinding.cameraView.bindLifecycle(lifecycle, settings)
        mBinding.cameraView.setManualFocus(true)

        mBinding.btnTorch.tag = settings.torch
        mBinding.btnTorch.setOnClickListener {
            val preOn = it.tag.toString().toBoolean()
            mBinding.cameraView.setTorch(!preOn)
            it.tag = !preOn
        }
        mBinding.btnScan.setOnClickListener {
            mBinding.cameraView.scanBarcode(object : ScanBarcodeCallback {
                override fun onResult(result: Result) {
                    logD("扫码结果:$result")
                }
            })
        }
        mBinding.btnMultipleScan.setOnClickListener {
            logD("开启多码扫码")
            mBinding.cameraView.scanBarcode(object : ScanBarcodeCallback {

                override fun isMultiple(): Boolean = true

                override fun onResult(result: Array<Result>) {
                    var needResult: String? = null
                    result.forEachIndexed { index, data ->
                        logD("多码扫码结果[$index]:${data.text}")
                        if (data.text.length != 15) {
                            needResult = data.text
                        }
                    }
                    if (needResult == null) {
                        mBinding.cameraView.scanBarcode(this)
                    }else{
                        Toast.makeText(this@CameraActivity, needResult, Toast.LENGTH_LONG).show()
                    }
                }
            })
        }
        mBinding.btnTake.setOnClickListener {
            mBinding.cameraView.takePicture(
                TakePictureCallback(
                    autoStartPreview = true,
                    jpeg = { bytes, camera, info ->
                        val bitmap = info.getBitmap(bytes)
                        File(cacheDir, "${System.currentTimeMillis()}.jpeg").writeBytes(info.bitmap2Bytes(bitmap))
                        logD("图片信息: $info")
                    })
            )
        }
    }
}