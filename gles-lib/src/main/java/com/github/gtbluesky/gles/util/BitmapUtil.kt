package com.github.gtbluesky.gles.util

import android.graphics.Bitmap
import android.graphics.Matrix
import java.io.BufferedOutputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.ByteBuffer

object BitmapUtil {

    @JvmStatic
    fun rotateBitmap(
        bitmap: Bitmap,
        rotate: Int,
        isRecycled: Boolean = true
    ): Bitmap {
        val matrix = Matrix().also {
            it.postRotate(rotate.toFloat())
        }
        return Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width,
            bitmap.height, matrix, true
        ).also {
            if (!bitmap.isRecycled && isRecycled) {
                bitmap.recycle()
            }
        }
    }

    @JvmStatic
    fun flipBitmap(
        bitmap: Bitmap,
        flipX: Boolean = false,
        flipY: Boolean = false,
        isRecycled: Boolean = true
    ): Bitmap {
        val matrix = Matrix().also {
            it.setScale(
                (if (flipX) -1 else 1).toFloat(),
                (if (flipY) -1 else 1).toFloat()
            )
            it.postTranslate(bitmap.width.toFloat(), 0f)
        }
        return Bitmap.createBitmap(
            bitmap,
            0, 0,
            bitmap.width,
            bitmap.height,
            matrix,
            false
        ).also {
            if (isRecycled && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    @JvmStatic
    fun scaleBitmap(
        bitmap: Bitmap,
        scale: Float,
        isRecycled: Boolean = true
    ): Bitmap {
        val matrix = Matrix().also {
            it.postScale(scale, scale)
        }
        return Bitmap.createBitmap(
            bitmap,
            0, 0,
            bitmap.width,
            bitmap.height,
            matrix,
            false
        ).also {
            if (isRecycled && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    @JvmStatic
    fun handleBitmapWithMatrix(
        bitmap: Bitmap,
        matrix: Matrix,
        isRecycled: Boolean = true
    ): Bitmap {
        return Bitmap.createBitmap(
            bitmap,
            0, 0,
            bitmap.width,
            bitmap.height,
            matrix,
            false
        ).also {
            if (isRecycled && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

    @JvmStatic
    fun saveBitmap(
        filePath: String,
        buffer: ByteBuffer,
        scale: Float = 1f,
        width: Int,
        height: Int,
        rotation: Int
    ) {
        var bos: BufferedOutputStream? = null
        try {
            bos = BufferedOutputStream(FileOutputStream(filePath))
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                it.copyPixelsFromBuffer(buffer)
            }.let {
                val matrix = Matrix().apply {
                    reset()
                    postScale(scale, -scale)
                    postRotate(rotation.toFloat())
                }
                handleBitmapWithMatrix(it, matrix)
            }.let {
                it.compress(Bitmap.CompressFormat.JPEG, 100, bos)
                it.recycle()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } finally {
            bos?.close()
        }
    }
}