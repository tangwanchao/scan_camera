package me.twc.scandemo

import android.os.Bundle
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
        mBinding.cameraView.scanBarcode(object : ScanBarcodeCallback {
            override fun onResult(result: Result) {
                logD("扫码结果:$result")
            }
        })

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
        mBinding.btnTake.setOnClickListener {
            mBinding.cameraView.takePicture(
                TakePictureCallback(
                    autoStartPreview = true,
                    jpeg = { bytes, camera, info ->
                        File(cacheDir, "${System.currentTimeMillis()}.jpeg").writeBytes(bytes)
                        logD("图片信息: $info")
                    })
            )
        }
    }
}