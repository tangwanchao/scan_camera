package me.twc.scancamera.camera.decoration

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.core.view.children

/**
 * @author 唐万超
 * @date 2021/06/17
 */
class ConstraintShadowLayout @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attributeSet, defStyleAttr) {

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = "#b2000000".toColorInt()
    }

    init {
        setWillNotDraw(false)
    }

    override fun onDraw(canvas: Canvas) {
        val rect = findCropRect() ?: return
        val rl = rect.left.toFloat()
        val rt = rect.top.toFloat()
        val rr = rect.right.toFloat()
        val rb = rect.bottom.toFloat()
        canvas.drawRect(0f, 0f, rl, height.toFloat(), mPaint)
        canvas.drawRect(rl, 0f, width.toFloat(), rt, mPaint)
        canvas.drawRect(rr, rt, width.toFloat(), height.toFloat(), mPaint)
        canvas.drawRect(rl, rb, rr, height.toFloat(), mPaint)
        super.onDraw(canvas)
    }

    private fun findCropRect(): Rect? {
        val cropRect = children.find { it is CropRect } as CropRect
        return cropRect.getRectInCameraView()
    }
}