package me.twc.utils

import android.hardware.Camera
import android.util.Size

/**
 * @author 唐万超
 * @date 2021/06/09
 */
fun Size.rotation():Size = Size(height,width)

fun com.journeyapps.barcodescanner.Size.toSize() = Size(width, height)

fun Camera.Size.toSize() = Size(width, height)

fun Size.toSize() = com.journeyapps.barcodescanner.Size(width, height)