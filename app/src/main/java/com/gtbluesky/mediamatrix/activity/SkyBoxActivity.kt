package com.gtbluesky.mediamatrix.activity

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import com.gtbluesky.gles.render.GLLooper
import com.gtbluesky.gles.shape.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class SkyBoxActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private lateinit var rotationSensor: Sensor
    private val rotateMatrix = FloatArray(16)
    private lateinit var shape: BaseShape
    private lateinit var glView: GLSurfaceView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        Matrix.setIdentityM(rotateMatrix, 0)

        glView = GLSurfaceView(this)

        shape = SkyBox(this)

        setContentView(glView)
        glView.setEGLContextClientVersion(2)
        glView.setEGLConfigChooser(8,8,8,8,16,8)
        glView.holder.setFormat(PixelFormat.TRANSPARENT)
        glView.setBackgroundColor(Color.TRANSPARENT)
        glView.setRenderer(object : GLSurfaceView.Renderer{
            override fun onDrawFrame(gl: GL10?) {
                shape.drawFrame()
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                shape.change(width, height)
            }

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                shape.init()
            }

        })
        glView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        SensorManager.getRotationMatrixFromVector(rotateMatrix, event?.values)
        if (shape is SkyBox) {
            (shape as SkyBox).setRotateMatrix(rotateMatrix)
            glView.requestRender()
        }
    }
}
