package me.twc.scandemo

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.twc.scandemo.databinding.ActMainBinding

/**
 * @author 唐万超
 * @date 2021/06/11
 */
class MainActivity:AppCompatActivity() {

    private val mBinding by lazy { ActMainBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mBinding.root)

        mBinding.btnStartCamera.setOnClickListener {
            startActivity(Intent(this,CameraActivity::class.java))
        }
    }
}