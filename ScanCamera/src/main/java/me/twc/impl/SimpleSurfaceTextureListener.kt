package me.twc.impl

import android.graphics.SurfaceTexture
import android.view.TextureView

/**
 * @author 唐万超
 * @date 2021/06/10
 */
interface SimpleSurfaceTextureListener : TextureView.SurfaceTextureListener {
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {}
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
}