package com.gtbluesky.gles.filter

import android.content.Context
import com.gtbluesky.gles.util.GLHelper

class BeautyFilter(context: Context) : NormalFilter(
    fragmentShader = GLHelper.getShaderFromAssets(
        context,
        "shader/beauty_smooth.frag.frag"
    ) ?: ""
) {

}