package com.github.gtbluesky.gles.constant

class FilterConstant {
    companion object {
        val VERTEX_COORDS = floatArrayOf(
            -1.0f, -1.0f,  // 0 bottom left
            1.0f, -1.0f,   // 1 bottom right
            -1.0f,  1.0f,  // 2 top left
            1.0f,  1.0f    // 3 top right
        )

        val OES_TEXTURE_COORDS = floatArrayOf(
            0.0f, 0.0f,     // 0 bottom left
            1.0f, 0.0f,     // 1 bottom right
            0.0f, 1.0f,     // 2 top left
            1.0f, 1.0f      // 3 top right
        )

        val TEXTURE_COORDS = floatArrayOf(
            0.0f, 1.0f,     // 0 bottom left
            1.0f, 1.0f,     // 1 bottom right
            0.0f, 0.0f,     // 2 top left
            1.0f, 0.0f      // 3 top right
        )
    }
}