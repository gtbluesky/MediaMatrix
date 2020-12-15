package com.gtbluesky.camera.controller

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.gtbluesky.camera.listener.StartFocusCallback
import kotlin.math.abs
import kotlin.math.sqrt

class SensorController(context: Context?) : SensorEventListener {

    private var lastX = 0
    private var lastY = 0
    private var lastZ = 0
    private var canFocusIn = false
    private var lastTimeStamp = 0L
    private var status = STATUS_NONE
    var startFocusCallback: StartFocusCallback? = null
    private val sensorManager: SensorManager by lazy {
        context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val sensor: Sensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    companion object {
        private const val DELEY_DURATION = 500
        private const val STATUS_NONE = 0
        private const val STATUS_STATIC = 1
        private const val STATUS_MOVE = 2

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor == null) {
            return
        }
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) {
            return
        }
        val x = event.values[0].toInt()
        val y = event.values[1].toInt()
        val z = event.values[2].toInt()
        val stamp = System.currentTimeMillis()
        if (status != STATUS_NONE) {
            val px: Int = abs(lastX - x)
            val py: Int = abs(lastY - y)
            val pz: Int = abs(lastZ - z)
            val value = sqrt(px * px + py * py + (pz * pz).toDouble())
            if (value > 1.4) {
                status = STATUS_MOVE
            } else {
                if (status == STATUS_MOVE) {
                    lastTimeStamp = stamp
                    canFocusIn = true
                }
                if (canFocusIn) {
                    if (stamp - lastTimeStamp > DELEY_DURATION) {
                        //移动后静止一段时间，可以发生对焦行为
                        canFocusIn = false
                        startFocusCallback?.onStart()
                    }
                }
                status = STATUS_STATIC
            }
        } else {
            lastTimeStamp = stamp
            status = STATUS_STATIC
        }
        lastX = x
        lastY = y
        lastZ = z
    }

    fun register() {
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    fun unregister() {
        sensorManager.unregisterListener(this, sensor)
    }
}