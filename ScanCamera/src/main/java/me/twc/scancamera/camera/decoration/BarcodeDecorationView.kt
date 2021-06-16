package me.twc.scancamera.camera.decoration

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import me.twc.scancamera.R
import me.twc.utils.dp

/**
 * @author 唐万超
 * @date 2020/03/17
 */
class BarcodeDecorationView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attributeSet, defStyleAttr), CropRect {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 角落装饰物的颜色
    private var cornerBoxColor: Int

    // 角落装饰物宽度,单位像素
    private var cornerBoxWidth: Float

    // 角落装饰物高度,单位像素
    private var cornerBoxHeight: Float

    // 边缘线条颜色
    private var rimLineColor: Int

    // 边缘线条高度
    private var rimLineHeight: Float

    private var layoutComplete: Boolean = false

    init {
        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.BarcodeDecorationView)
        cornerBoxColor = typedArray.getColor(
            R.styleable.BarcodeDecorationView_cornerBoxColor,
            Color.parseColor("#319BDD")
        )
        cornerBoxWidth = typedArray.getDimension(
            R.styleable.BarcodeDecorationView_cornerBoxWidth,
            3.dp
        )
        cornerBoxHeight = typedArray.getDimension(
            R.styleable.BarcodeDecorationView_cornerBoxHeight,
            30.dp
        )
        rimLineColor = typedArray.getColor(
            R.styleable.BarcodeDecorationView_rimLineColor,
            Color.parseColor("#319BDD")
        )
        rimLineHeight = typedArray.getDimension(
            R.styleable.BarcodeDecorationView_rimLineHeight,
            1.dp
        )
        typedArray.recycle()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        layoutComplete = true
    }

    private fun canDraw(): Boolean = layoutComplete

    override fun onDraw(canvas: Canvas) {
        if (canDraw()) {
            drawRimLine(canvas)
            drawCornerBox(canvas)
        }
    }


    /**
     * 绘制边沿线条
     */
    private fun drawRimLine(canvas: Canvas) {
        val halfCornerBoxWidth = cornerBoxWidth / 2f
        val h = rimLineHeight
        val halfH = h / 2f
        val l = halfCornerBoxWidth - halfH
        val t = halfCornerBoxWidth - halfH
        val r = width.toFloat() - halfCornerBoxWidth + halfH
        val b = height.toFloat() - halfCornerBoxWidth + halfH
        paint.color = rimLineColor
        // 左,上,右,下 四条边的绘制
        canvas.drawRect(l, t, l + h, b, paint)
        canvas.drawRect(l, t, r, t + h, paint)
        canvas.drawRect(r - h, t, r, b, paint)
        canvas.drawRect(l, b - h, r, b, paint)
    }

    /**
     * 绘制二维码预览框 4 个边脚
     */
    private fun drawCornerBox(canvas: Canvas) {
        val w = cornerBoxWidth
        val h = cornerBoxHeight
        val l = 0f
        val t = 0f
        val r = width.toFloat()
        val b = height.toFloat()
        paint.color = cornerBoxColor
        // 左上角
        canvas.drawRect(l, t, l + h, t + w, paint)
        canvas.drawRect(l, t, l + w, t + h, paint)
        // 右上角
        canvas.drawRect(r - h, t, r + 1, t + w, paint)
        canvas.drawRect(r - w, t, r, t + h, paint)
        // 坐下角
        canvas.drawRect(l, b - h, l + w, b, paint)
        canvas.drawRect(l, b - w, l + h, b, paint)
        // 右下角
        canvas.drawRect(r - w, b - h, r, b, paint)
        canvas.drawRect(r - h, b - w, r, b, paint)
    }

    override fun getRectInCameraView(): Rect? {
        return if (canDraw()) {
            Rect(left, top, right, bottom)
        } else null
    }
}