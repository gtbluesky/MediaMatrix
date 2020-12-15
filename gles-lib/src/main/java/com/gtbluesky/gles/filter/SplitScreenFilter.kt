package com.gtbluesky.gles.filter

import android.content.Context
import com.gtbluesky.gles.util.GLHelper

class SplitScreenFilter(context: Context, screenNum: Int = 2) : NormalFilter(
    fragmentShader = GLHelper.getShaderFromAssets(
        context,
        when (screenNum) {
            3 -> "shader/split_screen_3.frag"
            4 -> "shader/split_screen_4.frag"
            9 -> "shader/split_screen_9.frag"
            else -> "shader/split_screen_2.frag"
        }
    ) ?: ""
)