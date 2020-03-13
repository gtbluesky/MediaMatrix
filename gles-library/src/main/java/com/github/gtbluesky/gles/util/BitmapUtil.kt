package com.github.gtbluesky.gles.util

import android.graphics.Bitmap
import android.graphics.Matrix
import java.io.BufferedOutputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class BitmapUtil {

    companion object {

        @JvmStatic
        fun rotateBitmap(
            bitmap: Bitmap,
            rotate: Int,
            isRecycled: Boolean = true
        ): Bitmap {
            val matrix = Matrix().also {
                it.reset()
                it.postRotate(rotate.toFloat())
            }
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width,
                bitmap.height, matrix, true
            )
            if (!bitmap.isRecycled && isRecycled) {
                bitmap.recycle()
            }
            return rotatedBitmap
        }

        @JvmStatic
        fun flipBitmap(
            bitmap: Bitmap,
            flipX: Boolean,
            flipY: Boolean,
            isRecycled: Boolean = true
        ): Bitmap {
            val matrix = Matrix().also {
                it.setScale((if (flipX) -1 else 1).toFloat(), (if (flipY) -1 else 1).toFloat())
                it.postTranslate(bitmap.width.toFloat(), 0f)
            }
            val result = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width,
                bitmap.height, matrix, false
            )
            if (isRecycled && !bitmap.isRecycled) {
                bitmap.recycle()
            }
            return result
        }

        @JvmStatic
        fun flipBitmap(bitmap: Bitmap, isRecycled: Boolean = true): Bitmap {
            return flipBitmap(bitmap, true, false, isRecycled)
        }

        @JvmStatic
        fun scaleBitmap(bitmap: Bitmap, scale: Float, isRecycled: Boolean = true): Bitmap {
            val matrix = Matrix().also {
                it.postScale(scale, scale)
            }
            val result = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width,
                bitmap.height, matrix, false
            )
            if (isRecycled && !bitmap.isRecycled) {
                bitmap.recycle()
            }
            return result
        }

        @JvmStatic
        fun saveBitmap(filePath: String, buffer: ByteBuffer, scale: Float = 1f, width: Int, height: Int) {
            var bos: BufferedOutputStream? = null
            try {
                bos = BufferedOutputStream(FileOutputStream(filePath))
                Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).let {
                    it.copyPixelsFromBuffer(buffer)
                    rotateBitmap(it, 180)
                }.let {
                    flipBitmap(it)
                }.let {
                    scaleBitmap(it, scale)
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
}