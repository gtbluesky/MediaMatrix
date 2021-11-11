package com.gtbluesky.mediamatrix.player.util

import com.gtbluesky.mediamatrix.player.util.LogUtil.e
import java.text.SimpleDateFormat
import java.util.*

object TimeUtil {
    fun getFormatTime(time: Int): String {
        return getFormatTime(time, "mm:ss")
    }

    fun getFormatTime(time: Int, pattern: String?): String {
        return try {
            val formatter = SimpleDateFormat(pattern)
            formatter.timeZone = TimeZone.getTimeZone("GMT+00:00")
            formatter.format(time)
        } catch (e: Exception) {
            e(e.message)
            "00:00"
        }
    }
}