package com.gtbluesky.gles.filter

import android.content.Context
import android.graphics.Point
import android.graphics.PointF
import android.opengl.GLES20
import android.opengl.GLES30
import com.gtbluesky.gles.util.GLHelper
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.*

class ToneCurveFilter(
    context: Context,
    assetPath: String = "",
    rawId: Int = 0
) : NormalFilter(
    fragmentShader = GLHelper.getShaderFromAssets(
        context,
        "shader/tone_curve.frag"
    ) ?: ""
) {
    private var toneCurveTextureId = GLES30.GL_NONE
    private val toneCurveTextureUnit = 1
    private var toneCurveTextureUnitHandle = GLES30.GL_NONE

    private var toneCurveBuffer: ByteBuffer? = null

    override fun initProgram() {
        super.initProgram()
        toneCurveTextureUnitHandle = GLES30.glGetUniformLocation(program, "uToneCurveTextureUnit")
    }

    init {
        try {
            val inputStream = if (rawId != 0) {
                context.resources.openRawResource(rawId)
            } else {
                context.assets.open(assetPath)
            }
            val version = readShort(inputStream).toInt()
            val totalCurves = readShort(inputStream).toInt()
            val curves = ArrayList<Array<PointF>>(totalCurves)
            val pointRate = 1.0f / 255
            for (i in 0 until totalCurves) {
                // 2 bytes, Count of points in the curve (short integer from 2...19)
                val pointCount = readShort(inputStream)
                val points = Array(pointCount.toInt()) {
                    // point count * 4
                    // Curve points. Each curve point is a pair of short integers where
                    // the first number is the output value (vertical coordinate on the
                    // Curves dialog graph) and the second is the input value. All coordinates have range 0 to 255.
                    val y = readShort(inputStream)
                    val x = readShort(inputStream)
                    PointF(x * pointRate, y * pointRate)
                }
                curves.add(points)
            }
            inputStream.close()
            val rgbCompositeCurve = createSplineCurve(curves[0])
            val redCurve = createSplineCurve(curves[1])
            val greenCurve = createSplineCurve(curves[2])
            val blueCurve = createSplineCurve(curves[3])

            if (redCurve!!.size >= 256 && greenCurve!!.size >= 256 && blueCurve!!.size >= 256 && rgbCompositeCurve!!.size >= 256) {
                val toneCurveByteArray = ByteArray(256 * 4)
                for (currentCurveIndex in 0..255) {
                    // BGRA for upload to texture
                    toneCurveByteArray[currentCurveIndex * 4 + 2] = (clamp(
                        currentCurveIndex + blueCurve[currentCurveIndex] + rgbCompositeCurve[currentCurveIndex]
                    ).toInt() and 0xff).toByte()
                    toneCurveByteArray[currentCurveIndex * 4 + 1] = (clamp(
                        currentCurveIndex + greenCurve[currentCurveIndex] + rgbCompositeCurve[currentCurveIndex]
                    ).toInt() and 0xff).toByte()
                    toneCurveByteArray[currentCurveIndex * 4] = (clamp(
                        currentCurveIndex + redCurve[currentCurveIndex] + rgbCompositeCurve[currentCurveIndex]
                    ).toInt() and 0xff).toByte()
                    toneCurveByteArray[currentCurveIndex * 4 + 3] = 0xff.toByte()
                }
                toneCurveBuffer = ByteBuffer.wrap(toneCurveByteArray)
                toneCurveTextureId =
                    GLHelper.createTexture(textureType, toneCurveTextureUnit)
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + toneCurveTextureUnit)
                GLES30.glBindTexture(textureType, toneCurveTextureId)
                GLES20.glTexImage2D(
                    textureType,
                    0,
                    GLES20.GL_RGBA,
                    256,
                    1,
                    0,
                    GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE,
                    toneCurveBuffer
                )
                GLES30.glBindTexture(textureType, GLES30.GL_NONE)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun preDraw() {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + toneCurveTextureUnit)
        GLES30.glBindTexture(textureType, toneCurveTextureId)
        GLES30.glUniform1i(toneCurveTextureUnitHandle, toneCurveTextureUnit)
    }

    override fun destroy() {
        super.destroy()
        if (toneCurveTextureId != GLES30.GL_NONE) {
            GLES30.glDeleteTextures(1, intArrayOf(toneCurveTextureId), 0)
            toneCurveTextureId = GLES30.GL_NONE
        }
    }

    @Throws(IOException::class)
    private fun readShort(input: InputStream): Short {
        return (input.read() shl 8 or input.read()).toShort()
    }

    private fun createSplineCurve(points: Array<PointF>): ArrayList<Float>? {
        if (points.isEmpty()) {
            return null
        }
        // Sort the array
        val sortedPoints = points.clone()
        Arrays.sort(sortedPoints) { point1, point2 ->
            sign(point1.x - point2.x).toInt()
        }
        // Convert from (0, 1) to (0, 255).
        val convertedPoints = Array(sortedPoints.size) {
            val point = sortedPoints[it]
            Point((point.x * 255).toInt(), (point.y * 255).toInt())
        }
        val splinePoints = createSplineCurve2(convertedPoints)
        // If we have a first point like (0.3, 0) we'll be missing some points at the beginning
        // that should be 0.
        val firstSplinePoint = splinePoints!![0]
        if (firstSplinePoint.x > 0) {
            for (i in firstSplinePoint.x downTo 0) {
                splinePoints.add(0, Point(i, 0))
            }
        }
        // Insert points similarly at the end, if necessary.
        val lastSplinePoint = splinePoints[splinePoints.size - 1]
        if (lastSplinePoint.x < 255) {
            for (i in lastSplinePoint.x + 1..255) {
                splinePoints.add(Point(i, 255))
            }
        }
        // Prepare the spline points.
        val preparedSplinePoints =
            ArrayList<Float>(splinePoints.size)
        for (newPoint in splinePoints) {
            val origPoint = Point(newPoint.x, newPoint.x)
            var distance = sqrt(
                (origPoint.x - newPoint.x).toDouble().pow(2.0) + (origPoint.y - newPoint.y).toDouble().pow(
                    2.0
                )
            ).toFloat()
            if (origPoint.y > newPoint.y) {
                distance = -distance
            }
            preparedSplinePoints.add(distance)
        }
        return preparedSplinePoints
    }

    private fun createSplineCurve2(points: Array<Point>): ArrayList<Point>? {
        val sdA = createSecondDerivative(points)
        // Is [points count] equal to [sdA count]?
        // int n = [points count];
        val n = sdA?.size ?: 0
        if (n < 1) {
            return null
        }
        val sd = DoubleArray(n)
        // From NSMutableArray to sd[n];
        for (i in 0 until n) {
            sd[i] = sdA?.get(i)!!
        }
        val output = ArrayList<Point>(n + 1)
        for (i in 0 until n - 1) {
            val cur = points[i]
            val next = points[i + 1]
            for (x in cur.x until next.x) {
                val t = (x - cur.x).toDouble() / (next.x - cur.x)
                val a = 1 - t
                val h = next.x - cur.x.toDouble()
                var y =
                    a * cur.y + t * next.y + h * h / 6 * ((a * a * a - a) * sd[i] + (t * t * t - t) * sd[i + 1])
                if (y > 255.0) {
                    y = 255.0
                } else if (y < 0.0) {
                    y = 0.0
                }
                output.add(Point(x, y.roundToInt()))
            }
        }
        // If the last point is (255, 255) it doesn't get added.
        if (output.size == 255) {
            output.add(points[points.size - 1])
        }
        return output
    }

    private fun createSecondDerivative(points: Array<Point>): ArrayList<Double>? {
        val n = points.size
        if (n <= 1) {
            return null
        }
        val matrix = Array(n) { DoubleArray(3) }
        val result = DoubleArray(n)
        matrix[0][1] = 1.0
        // What about matrix[0][1] and matrix[0][0]? Assuming 0 for now (Brad L.)
        matrix[0][0] = 0.0
        matrix[0][2] = 0.0
        for (i in 1 until n - 1) {
            val p1 = points[i - 1]
            val p2 = points[i]
            val p3 = points[i + 1]
            matrix[i][0] = (p2.x - p1.x).toDouble() / 6
            matrix[i][1] = (p3.x - p1.x).toDouble() / 3
            matrix[i][2] = (p3.x - p2.x).toDouble() / 6
            result[i] =
                (p3.y - p2.y).toDouble() / (p3.x - p2.x) - (p2.y - p1.y).toDouble() / (p2.x - p1.x)
        }
        // What about result[0] and result[n-1]? Assuming 0 for now (Brad L.)
        result[0] = 0.0
        result[n - 1] = 0.0
        matrix[n - 1][1] = 1.0
        // What about matrix[n-1][0] and matrix[n-1][2]? For now, assuming they are 0 (Brad L.)
        matrix[n - 1][0] = 0.0
        matrix[n - 1][2] = 0.0
        // solving pass1 (up->down)
        for (i in 1 until n) {
            val k = matrix[i][0] / matrix[i - 1][1]
            matrix[i][1] -= k * matrix[i - 1][2]
            matrix[i][0] = 0.0
            result[i] -= k * result[i - 1]
        }
        // solving pass2 (down->up)
        for (i in n - 2 downTo 0) {
            val k = matrix[i][2] / matrix[i + 1][1]
            matrix[i][1] -= k * matrix[i + 1][0]
            matrix[i][2] = 0.0
            result[i] -= k * result[i + 1]
        }
        val output = ArrayList<Double>(n)
        for (i in 0 until n) output.add(result[i] / matrix[i][1])
        return output
    }

    private fun clamp(value: Float, minValue: Float = 0f, maxValue: Float = 255f): Float {
        return min(max(value, minValue), maxValue)
    }

}