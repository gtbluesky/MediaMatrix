package com.gtbluesky.mediamatrix.activity

import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import com.gtbluesky.gles.render.GLLooper
import com.gtbluesky.gles.shape.Cube
import com.gtbluesky.gles.shape.LightDemo
import com.gtbluesky.gles.shape.Sphere
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class EGLTestActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private lateinit var textureView: TextureView
    private lateinit var glLooper: GLLooper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val shape = Sphere()

//        val glView = GLSurfaceView(this)
//        setContentView(glView)
//        glView.setEGLContextClientVersion(2)
//        glView.setEGLConfigChooser(8,8,8,8,16,8)
//        glView.holder.setFormat(PixelFormat.TRANSPARENT)
//        glView.setBackgroundColor(Color.TRANSPARENT)
//        glView.setRenderer(object : GLSurfaceView.Renderer{
//            override fun onDrawFrame(gl: GL10?) {
//                shape.drawFrame()
//            }
//
//            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
//                shape.change(width, height)
//            }
//
//            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
//                shape.init()
//            }
//
//        })
//        glView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
//
//        glView.setOnTouchListener { v, event ->
//            when (event.action) {
//                MotionEvent.ACTION_DOWN -> {
//                    if (shape is Sphere) {
//                        shape.rotate(20f, 0f, 1f, 0f)
//                    }
//                }
//                MotionEvent.ACTION_UP -> {
//                    glView.requestRender()
//                }
//            }
//            true
//        }

        glLooper = GLLooper()
//        surfaceView = SurfaceView(this)
//        setContentView(surfaceView)
//        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
//
//            override fun surfaceCreated(p0: SurfaceHolder?) {
//                glLooper.postMessage(GLLooper.MSG_INIT, obj = p0?.surface)
//            }
//
//            override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {
//                glLooper.postMessage(GLLooper.MSG_CHANGE, p2, p3)
//            }
//
//            override fun surfaceDestroyed(p0: SurfaceHolder?) {
//                glLooper.postMessage(GLLooper.MSG_DESTROY)
//            }
//
//        })

        textureView = TextureView(this)
        setContentView(textureView)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture?, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture?) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture?): Boolean {
                glLooper.postMessage(GLLooper.MSG_DESTROY)
                return true
            }

            override fun onSurfaceTextureAvailable(p0: SurfaceTexture?, p1: Int, p2: Int) {
                glLooper.postMessage(GLLooper.MSG_INIT, obj = Surface(p0))
                glLooper.postMessage(GLLooper.MSG_CHANGE, p1, p2)
            }

        }

        window.decorView.postDelayed({
            glLooper.postMessage(GLLooper.MSG_DRAW)
        }, 1000)
    }

//    override fun onDestroy() {
//        glLooper.release()
//        super.onDestroy()
//    }
}
