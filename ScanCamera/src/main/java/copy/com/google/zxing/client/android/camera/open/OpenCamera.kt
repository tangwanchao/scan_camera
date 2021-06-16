/*
 * Copyright (C) 2015 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package copy.com.google.zxing.client.android.camera.open

import android.hardware.Camera

/**
 * 打开的相机实例和该相机的一些原始数据
 *
 * @param mIndex 相机 id
 * @param camera 相机具体实例
 * @param facing 相机朝向
 * @param orientation 相机角度
 */
@Suppress("DEPRECATION","MemberVisibilityCanBePrivate")
class OpenCamera(
    private val mIndex: Int,
    val camera: Camera,
    val facing: CameraFacing,
    val orientation: Int
) {
    override fun toString(): String {
        return "Camera #$mIndex : $facing,$orientation"
    }
}