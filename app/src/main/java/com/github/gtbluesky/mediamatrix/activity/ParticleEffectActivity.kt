package com.github.gtbluesky.mediamatrix.activity

import android.graphics.Color
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import com.github.gtbluesky.gles.render.GLLooper
import com.github.gtbluesky.gles.shape.Cube
import com.github.gtbluesky.gles.shape.LightDemo
import com.github.gtbluesky.gles.shape.ParticleEffect
import com.github.gtbluesky.gles.shape.Sphere
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class ParticleEffectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val glView = GLSurfaceView(this)

        val shape = ParticleEffect(this)

        glView.setEGLContextClientVersion(2)
//        glView.holder.setFormat(PixelFormat.TRANSPARENT)
//        glView.setBackgroundColor(Color.TRANSPARENT)
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

        setContentView(glView)

//        glView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

//        glView.setOnTouchListener { v, event ->
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
////                    if (shape is Sphere) {
////                        shape.rotate(20f, 0f, 1f, 0f)
////                    }
//                }
//                MotionEvent.ACTION_UP -> {
//                    glView.requestRender()
//                }
//            }
//            true
//        }
    }

}
