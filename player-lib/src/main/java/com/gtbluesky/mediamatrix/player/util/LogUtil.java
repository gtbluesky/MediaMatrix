package com.gtbluesky.mediamatrix.player.util;

import android.util.Log;

public class LogUtil {

    private static final boolean SHOW_LOG = true;
    private static final String TAG = "gtbluesky";

    public static void v(String msg) {
        if (!SHOW_LOG) {
            return;
        }
        Log.v(TAG, msg);
    }

    public static void d(String msg) {
        if (!SHOW_LOG) {
            return;
        }
        Log.d(TAG, msg);
    }

    public static void i(String msg) {
        if (!SHOW_LOG) {
            return;
        }
        Log.i(TAG, msg);
    }

    public static void w(String msg) {
        if (!SHOW_LOG) {
            return;
        }
        Log.w(TAG, msg);
    }

    public static void e(String msg) {
        if (!SHOW_LOG) {
            return;
        }
        Log.e(TAG, msg);
    }
}
