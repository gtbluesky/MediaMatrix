package com.gtbluesky.mediamatrix.player.util

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader

object TextResourceReader {
    private val TAG = TextResourceReader::class.java.simpleName

    fun readTextFileFromResource(context: Context, resourceId: Int): String {
        val body = StringBuilder()
        val inputStream = context.resources.openRawResource(resourceId)
        val inputStreamReader = InputStreamReader(inputStream)
        val bufferedReader = BufferedReader(inputStreamReader)
        var nextLine: String?
        try {
            while (bufferedReader.readLine().also { nextLine = it } != null) {
                body.append(nextLine)
                body.append('\n')
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return body.toString()
    }

    fun readTextFileFromAsset(context: Context, filename: String): String? {
        var result: String? = null
        try {
            val inputStream = context.assets.open(filename)
            var byteData: Int
            val outputStream = ByteArrayOutputStream()
            while (inputStream.read().also { byteData = it } != -1) {
                outputStream.write(byteData)
            }
            val buff = outputStream.toByteArray()
            outputStream.close()
            inputStream.close()
            result = String(buff, Charsets.UTF_8)
            result = result.replace("\\r\\n", "\n")
            Log.d(TAG, "read result is $result")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return result
    }
}