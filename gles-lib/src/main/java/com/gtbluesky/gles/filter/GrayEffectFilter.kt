package com.gtbluesky.gles.filter

import android.content.Context
import android.opengl.GLES30
import com.gtbluesky.gles.util.GLHelper

class GrayEffectFilter(context: Context) : NormalFilter(
    fragmentShader = GLHelper.getShaderFromAssets(
        context,
        "shader/gray_effect.frag"
    ) ?: ""
)