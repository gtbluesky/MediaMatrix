package com.gtbluesky.mediamatrix.player.util

import android.util.Log

object LogUtil {
    private const val SHOW_LOG = true
    private const val TAG = "LogUtil"

    fun v(msg: String?) {
        if (!SHOW_LOG) {
            return
        }
        if (msg.isNullOrEmpty()) {
            return
        }
        Log.v(TAG, msg)
    }

    fun d(msg: String?) {
        if (!SHOW_LOG) {
            return
        }
        if (msg.isNullOrEmpty()) {
            return
        }
        Log.d(TAG, msg)
    }

    fun i(msg: String?) {
        if (!SHOW_LOG) {
            return
        }
        if (msg.isNullOrEmpty()) {
            return
        }
        Log.i(TAG, msg)
    }

    fun w(msg: String?) {
        if (!SHOW_LOG) {
            return
        }
        if (msg.isNullOrEmpty()) {
            return
        }
        Log.w(TAG, msg)
    }

    fun e(msg: String?) {
        if (!SHOW_LOG) {
            return
        }
        if (msg.isNullOrEmpty()) {
            return
        }
        Log.e(TAG, msg)
    }
}