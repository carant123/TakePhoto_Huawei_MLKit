package com.huawei.mlkitcarlos.camera


open class CameraConfiguration {
    var fps = 20.0f
    var previewHeight = MAX_HEIGHT
    val isAutoFocus = true

    @Synchronized
    fun setCameraFacing(facing: Int) {
        require(!(facing != CAMERA_FACING_BACK && facing != CAMERA_FACING_FRONT)) { "Invalid camera: $facing" }
        cameraFacing = facing
    }

    companion object {
        val CAMERA_FACING_BACK: Int = android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK
        val CAMERA_FACING_FRONT: Int = android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT
        const val DEFAULT_WIDTH = 480
        const val DEFAULT_HEIGHT = 360
        const val MAX_WIDTH = 960
        const val MAX_HEIGHT = 720

        @get:Synchronized
        var cameraFacing = CAMERA_FACING_BACK
            protected set
    }
}