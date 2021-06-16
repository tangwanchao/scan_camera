package me.twc.utils

import android.content.res.Resources

/**
 * @author 唐万超
 * @date 2021/06/16
 */
val Number.dp: Float
    get() {
        val scale = Resources.getSystem().displayMetrics.density
        return (this.toFloat() * scale + 0.5f)
    }