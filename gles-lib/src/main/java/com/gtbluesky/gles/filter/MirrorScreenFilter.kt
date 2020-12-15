package com.gtbluesky.gles.filter

import android.content.Context
import com.gtbluesky.gles.util.GLHelper

class MirrorScreenFilter(context: Context, isMirrorX: Boolean = true) : NormalFilter(
    fragmentShader = GLHelper.getShaderFromAssets(
        context,
        if (isMirrorX) {
            "shader/mirror_screen_x.frag"
        } else {
            "shader/mirror_screen_y.frag"
        }
    ) ?: ""
)