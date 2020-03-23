package com.gtbluesky.camera.controller

import android.content.Context
import android.hardware.SensorManager
import android.util.Log
import android.view.OrientationEventListener
import com.gtbluesky.camera.listener.OnRotationChangeListener

class OrientationController(context: Context) :
    OrientationEventListener(context, SensorManager.SENSOR_DELAY_UI) {

    var onRotationChangeListener: OnRotationChangeListener? = null

    companion object {
        private val TAG = OrientationController::class.java.simpleName
        const val ROTATION_0 = 0
        const val ROTATION_90 = 90
        const val ROTATION_180 = 180
        const val ROTATION_270 = 270
    }

    override fun onOrientationChanged(orientation: Int) {
        Log.e(TAG, "orientation=$orientation")
        val rotation = when {
            orientation < 45 || orientation >= (270 + 45) -> {
                ROTATION_0
            }
            orientation >= 45 && orientation < (90 + 45) -> {
                ROTATION_90
            }
            orientation >= (90 + 45) && orientation < (180 + 45) -> {
                ROTATION_180
            }
            orientation >= (180 + 45) && orientation < (270 + 45) -> {
                ROTATION_270
            }
            else -> ROTATION_0
        }
        Log.e(TAG, "rotation=$rotation")
        onRotationChangeListener?.onRotationChange(rotation)
    }
}