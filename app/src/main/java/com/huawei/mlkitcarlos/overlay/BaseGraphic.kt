package com.huawei.mlkitcarlos.overlay

import android.graphics.Canvas
import com.huawei.hms.mlsdk.common.LensEngine


abstract class BaseGraphic(private val graphicOverlay: GraphicOverlay) {
    abstract fun draw(canvas: Canvas?)
    fun scaleX(x: Float): Float {
        return x * graphicOverlay.widthScaleValue
    }

    fun scaleY(y: Float): Float {
        return y * graphicOverlay.heightScaleValue
    }

    fun translateX(x: Float): Float {
        return if (graphicOverlay.cameraFacing == LensEngine.FRONT_LENS) {
            graphicOverlay.width - scaleX(x)
        } else {
            scaleX(x)
        }
    }

    fun translateY(y: Float): Float {
        return scaleY(y)
    }

}
