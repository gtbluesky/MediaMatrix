package com.gtbluesky.gles.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLUtils
import android.opengl.Matrix
import android.text.TextUtils
import android.util.Log
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer


object GLHelper {

    private val TAG = GLHelper::class.java.simpleName

    @JvmField
    val IDENTITY_MATRIX = FloatArray(16)

    init {
        Matrix.setIdentityM(IDENTITY_MATRIX, 0)
    }

    /**
     * Creates a new program from the supplied vertex and fragment shaders.
     *
     * @return A handle to the program, or 0 on failure.
     */
    @JvmStatic
    fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = loadShader(
            GLES30.GL_VERTEX_SHADER,
            vertexSource
        )
        if (vertexShader == 0) {
            return 0
        }

        val fragmentShader = loadShader(
            GLES30.GL_FRAGMENT_SHADER,
            fragmentSource
        )
        if (fragmentShader == 0) {
            GLES30.glDeleteShader(vertexShader)
            return 0
        }

        var program = GLES30.glCreateProgram()
        checkGlError("glCreateProgram")
        if (program == 0) {
            Log.e(TAG, "Could not create program")
            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)
            return 0
        }
        GLES30.glAttachShader(program, vertexShader)
        checkGlError("glAttachShader")
        GLES30.glAttachShader(program, fragmentShader)
        checkGlError("glAttachShader")
        GLES30.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES30.GL_TRUE) {
            Log.e(TAG, "Could not link program: ")
            Log.e(TAG, GLES30.glGetProgramInfoLog(program))
            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)
            GLES30.glDeleteProgram(program)
            program = 0
        }
        return program
    }

    /**
     * Compiles the provided shader source.
     *
     * @return A handle to the shader, or 0 on failure.
     */
    @JvmStatic
    fun loadShader(shaderType: Int, source: String): Int {
        var shader = GLES30.glCreateShader(shaderType)
        checkGlError("glCreateShader type=$shaderType")
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Could not compile shader $shaderType:")
            Log.e(TAG, " " + GLES30.glGetShaderInfoLog(shader))
            GLES30.glDeleteShader(shader)
            shader = 0
        }
        return shader
    }

    /**
     * Checks to see if a GLES error has been raised.
     */
    @JvmStatic
    fun checkGlError(op: String) {
        val error = GLES30.glGetError()
        if (error != GLES30.GL_NO_ERROR) {
            val msg = "$op: glError 0x${Integer.toHexString(error)}"
            Log.e(TAG, msg)
            throw RuntimeException(msg)
        }
    }

    /**
     * Checks to see if the location we obtained is valid.  GLES returns -1 if a label
     * could not be found, but does not set the GL error.
     *
     *
     * Throws a RuntimeException if the location is invalid.
     */
    @JvmStatic
    fun checkLocation(location: Int, label: String) {
        if (location < 0) {
            throw RuntimeException("Unable to locate '$label' in program")
        }
    }

    /**
     * Creates a texture from raw data.
     *
     * @param data Image data, in a "direct" ByteBuffer.
     * @param width Texture width, in pixels (not bytes).
     * @param height Texture height, in pixels.
     * @param format Image data format (use constant appropriate for glTexImage2D(), e.g. GL_RGBA).
     * @return Handle to texture.
     */
    @JvmStatic
    fun createImageTexture(data: ByteBuffer, width: Int, height: Int, format: Int): Int {
        val textureHandles = IntArray(1)
        val textureHandle: Int

        GLES30.glGenTextures(1, textureHandles, 0)
        textureHandle = textureHandles[0]
        checkGlError("glGenTextures")

        // Bind the texture handle to the 2D texture target.
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureHandle)

        // Configure min/mag filtering, i.e. what scaling method do we use if what we're rendering
        // is smaller or larger than the source image.
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_LINEAR
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_2D,
            GLES30.GL_TEXTURE_MAG_FILTER,
            GLES30.GL_LINEAR
        )
        checkGlError("loadImageTexture")

        // Load the data from the buffer into the texture handle.
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            format,
            width,
            height,
            0,
            format,
            GLES30.GL_UNSIGNED_BYTE,
            data
        )
        checkGlError("loadImageTexture")

        return textureHandle
    }

    @JvmStatic
    fun createTexture(textureType: Int): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        val textureId = textures[0]
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(textureType, textureId)
        GLES30.glTexParameteri(
            textureType,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_NEAREST
        )
        GLES30.glTexParameteri(
            textureType,
            GLES30.GL_TEXTURE_MAG_FILTER,
            GLES30.GL_LINEAR
        )
        GLES30.glTexParameteri(
            textureType,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glTexParameteri(
            textureType,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glBindTexture(textureType, 0)
        return textureId
    }

    @JvmStatic
    fun createTexture(textureType: Int, context: Context, resId: Int): Int {
        val textureId = createTexture(textureType)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(textureType, textureId)
        val bitmap = BitmapFactory.decodeResource(context.resources, resId)
        GLUtils.texImage2D(textureType, 0, bitmap, 0)
        GLES30.glBindTexture(textureType, GLES30.GL_NONE)
        bitmap.recycle()
        return textureId
    }

    @JvmStatic
    fun createOESTexture(): Int {
        return createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
    }

    @JvmStatic
    fun createCubeTexture(context: Context, resourceIds: IntArray): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        val textureId = textures[0]

        val options = BitmapFactory.Options()

        val cubeBitmaps = arrayOfNulls<Bitmap>(6)

        for ( i in 0 until 6) {
            cubeBitmaps[i] = BitmapFactory.decodeResource(context.resources, resourceIds[i], options)
            if (cubeBitmaps[i] == null) {
                Log.e(TAG, "Resource ID ${resourceIds[i]} cannot be decoded.")
                GLES30.glDeleteTextures(1, textures, 0)
                return GLES30.GL_ZERO
            }
        }

        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, textureId)
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_CUBE_MAP,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_LINEAR
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_CUBE_MAP,
            GLES30.GL_TEXTURE_MAG_FILTER,
            GLES30.GL_LINEAR
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_CUBE_MAP,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_CLAMP_TO_EDGE
        )
        GLES30.glTexParameteri(
            GLES30.GL_TEXTURE_CUBE_MAP,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_CLAMP_TO_EDGE
        )

        GLUtils.texImage2D(GLES30.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, cubeBitmaps[0], 0)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, cubeBitmaps[1], 0)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, cubeBitmaps[2], 0)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, cubeBitmaps[3], 0)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, cubeBitmaps[4], 0)
        GLUtils.texImage2D(GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, cubeBitmaps[5], 0)

        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_NONE)

        cubeBitmaps.forEach {
            it?.recycle()
        }
        return textureId
    }

    @JvmStatic
    fun createFrameBuffer(frameBuffers: IntArray, frameBufferTextures: IntArray, width: Int, height: Int) {
        GLES30.glGenFramebuffers(frameBuffers.size, frameBuffers, 0)
        GLES30.glGenTextures(frameBufferTextures.size, frameBufferTextures, 0)
        for (i in frameBufferTextures.indices) {
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, frameBufferTextures[i])
            GLES30.glTexImage2D(
                GLES30.GL_TEXTURE_2D,
                0,
                GLES30.GL_RGBA,
                width,
                height,
                0,
                GLES30.GL_RGBA,
                GLES30.GL_UNSIGNED_BYTE,
                null
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_LINEAR
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_S,
                GLES30.GL_CLAMP_TO_EDGE
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_T,
                GLES30.GL_CLAMP_TO_EDGE
            )
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, frameBuffers[i])
            GLES30.glFramebufferTexture2D(
                GLES30.GL_FRAMEBUFFER,
                GLES30.GL_COLOR_ATTACHMENT0,
                GLES30.GL_TEXTURE_2D,
                frameBufferTextures[i],
                0
            )
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
        }
        checkError("createFrameBuffer")
    }

    /**
     * Allocates a direct float buffer, and populates it with the float array data.
     */
    @JvmStatic
    fun createFloatBuffer(array: FloatArray): FloatBuffer {
        return ByteBuffer
            .allocateDirect(array.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(array)
            .apply {
                position(0)
            }
    }

    /**
     * Allocates a direct short buffer, and populates it with the short array data.
     */
    @JvmStatic
    fun createShortBuffer(array: ShortArray): ShortBuffer {
        return ByteBuffer
            .allocateDirect(array.size * 2)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(array).apply {
                position(0)
            }
    }

    /**
     * Allocates a direct byte buffer, and populates it with the byte array data.
     */
    @JvmStatic
    fun createByteBuffer(array: ByteArray): ByteBuffer {
        return ByteBuffer
            .allocateDirect(array.size)
            .order(ByteOrder.nativeOrder())
            .put(array).apply {
                position(0)
            }
    }

    @JvmStatic
    fun getCurrentFrame(width: Int, height: Int): ByteBuffer {
        return ByteBuffer
            .allocateDirect(width * height * 4)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                GLES30.glReadPixels(
                    0, 0, width, height,
                    GLES30.GL_RGBA,
                    GLES30.GL_UNSIGNED_BYTE,
                    this
                )
                checkError("glReadPixels")
                rewind()
            }

    }

    @JvmStatic
    fun getShaderFromFile(filePath: String): String? {
        if (TextUtils.isEmpty(filePath)) {
            return null
        }
        val file = File(filePath)
        if (file.isDirectory) {
            return null
        }
        var inputStream: InputStream? = null
        try {
            inputStream = FileInputStream(file)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return getShaderFromStream(inputStream)
    }

    @JvmStatic
    fun getShaderFromAssets(
        context: Context,
        path: String
    ): String? {
        var inputStream: InputStream? = null
        try {
            inputStream = context.resources.assets.open(path)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return getShaderFromStream(inputStream)
    }

    private fun getShaderFromStream(inputStream: InputStream?): String? {
        if (inputStream == null) {
            return null
        }
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val builder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                builder.append(line).append("\n")
            }
            reader.close()
            return builder.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    /**
     * Writes GL version info to the log.
     */
    @JvmStatic
    fun logVersionInfo() {
        Log.i(TAG, "vendor  : ${GLES30.glGetString(GLES30.GL_VENDOR)}")
        Log.i(TAG, "renderer: ${GLES30.glGetString(GLES30.GL_RENDERER)}")
        Log.i(TAG, "version : ${GLES30.glGetString(GLES30.GL_VERSION)}")

        val values = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, values, 0)
        val majorVersion = values[0]
        GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, values, 0)
        val minorVersion = values[0]
        if (GLES30.glGetError() == GLES30.GL_NO_ERROR) {
            Log.i(TAG, "iversion: $majorVersion.$minorVersion")
        }
    }

    @JvmStatic
    fun checkError(op: String) {
        val error = GLES30.glGetError()
        if (error != GLES30.GL_NO_ERROR) {
            Log.e(TAG, "$op: glError 0x${Integer.toHexString(error)}")
        }
    }

}