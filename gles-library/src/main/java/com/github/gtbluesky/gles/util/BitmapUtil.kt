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
        fun rotateBitmap(bitmap: Bitmap, rotate: Int, isRecycled: Boolean): Bitmap {
            val matrix = Matrix()
            matrix.reset()
            matrix.postRotate(rotate.toFloat())
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
            isRecycled: Boolean
        ): Bitmap {
            val matrix = Matrix()
            matrix.setScale((if (flipX) -1 else 1).toFloat(), (if (flipY) -1 else 1).toFloat())
            matrix.postTranslate(bitmap.width.toFloat(), 0f)
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
        fun flipBitmap(bitmap: Bitmap, isRecycled: Boolean): Bitmap {
            return flipBitmap(bitmap, true, false, isRecycled)
        }

        @JvmStatic
        fun saveBitmap(filePath: String, buffer: ByteBuffer, width: Int, height: Int) {
            var bos: BufferedOutputStream? = null
            try {
                bos = BufferedOutputStream(FileOutputStream(filePath))
                var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                bitmap = rotateBitmap(bitmap, 180, true)
                bitmap = flipBitmap(bitmap, true)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
                bitmap.recycle()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } finally {
                bos?.close()
            }
        }
    }
}