package com.gtbluesky.codec

import android.media.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * An AudioProcessor that uses the Sonic library to modify audio speed/pitch/sample rate.
 */
class SonicAudioProcessor : AudioProcessor() {
    private var speed = 1f
    private var pitch = 1f
    private var channelCount = NO_VALUE
    private var sampleRate = NO_VALUE

    private var pendingOutputSampleRate = SAMPLE_RATE_NO_CHANGE
    private var outputSampleRateHz = NO_VALUE

    private var pendingSonicRecreation = false
    private var sonic: Sonic? = null
    private var buffer: ByteBuffer
    private var shortBuffer: ShortBuffer
    private var outputBuffer: ByteBuffer
    private var inputBytes: Long = 0
    private var outputBytes: Long = 0
    private var inputEnded = false

    companion object {
        /**
         * The maximum allowed playback speed in [.setSpeed].
         */
        const val MAXIMUM_SPEED = 8.0f
        /**
         * The minimum allowed playback speed in [.setSpeed].
         */
        const val MINIMUM_SPEED = 0.1f
        /**
         * The maximum allowed pitch in [.setPitch].
         */
        const val MAXIMUM_PITCH = 8.0f
        /**
         * The minimum allowed pitch in [.setPitch].
         */
        const val MINIMUM_PITCH = 0.1f
        /**
         * Indicates that the output sample rate should be the same as the input.
         */
        const val SAMPLE_RATE_NO_CHANGE = -1

        /**
         * The threshold below which the difference between two pitch/speed factors is negligible.
         */
        private const val CLOSE_THRESHOLD = 0.01f

        /**
         * The minimum number of output bytes at which the speedup is calculated using the input/output
         * byte counts, rather than using the current playback parameters speed.
         */
        private const val MIN_BYTES_FOR_SPEEDUP_CALCULATION = 1024
    }

    /**
     * Creates a new Sonic audio processor.
     */
    init {
        buffer = emptyBuffer
        shortBuffer = buffer.asShortBuffer()
        outputBuffer = emptyBuffer
    }

    /**
     * Sets the playback speed. This method may only be called after draining data through the
     * processor. The value returned by [.isActive] may change, and the processor must be
     * [flushed][.flush] before queueing more data.
     *
     * @param speed The requested new playback speed.
     * @return The actual new playback speed.
     */
    fun setSpeed(speed: Float): Float {
        return constrainValue(speed, MINIMUM_SPEED, MAXIMUM_SPEED).also {
            if (this.speed != it) {
                this.speed = it
                pendingSonicRecreation = true
            }
        }
    }

    /**
     * Sets the playback pitch. This method may only be called after draining data through the
     * processor. The value returned by [.isActive] may change, and the processor must be
     * [flushed][.flush] before queueing more data.
     *
     * @param pitch The requested new pitch.
     * @return The actual new pitch.
     */
    fun setPitch(pitch: Float): Float {
        return constrainValue(pitch, MINIMUM_PITCH, MAXIMUM_PITCH).also {
            if (this.pitch != it) {
                this.pitch = it
                pendingSonicRecreation = true
            }
        }
    }

    /**
     * Sets the sample rate for output audio, in Hertz. Pass [.SAMPLE_RATE_NO_CHANGE] to output
     * audio at the same sample rate as the input. After calling this method, call [ ][.configure] to configure the processor with the new sample rate.
     *
     * @param sampleRateHz The sample rate for output audio, in Hertz.
     * @see .configure
     */
    fun setOutputSampleRateHz(sampleRateHz: Int) {
        pendingOutputSampleRate = sampleRateHz
    }

    /**
     * Returns the specified duration scaled to take into account the speedup factor of this instance,
     * in the same units as `duration`.
     *
     * @param duration The duration to scale taking into account speedup.
     * @return The specified duration scaled to take into account speedup, in the same units as
     * `duration`.
     */
    fun scaleDurationForSpeedup(duration: Long): Long {
        return if (outputBytes >= MIN_BYTES_FOR_SPEEDUP_CALCULATION) {
            if (outputSampleRateHz == sampleRate) {
                scaleLargeTimestamp(
                    duration,
                    inputBytes,
                    outputBytes
                )
            } else {
                scaleLargeTimestamp(
                    duration,
                    inputBytes * outputSampleRateHz,
                    outputBytes * sampleRate
                )
            }
        } else {
            (speed.toDouble() * duration).toLong()
        }
    }

    @Throws(UnhandledAudioFormatException::class)
    override fun configure(sampleRate: Int, channelCount: Int, encoding: Int): Boolean {
        if (encoding != AudioFormat.ENCODING_PCM_16BIT) {
            throw UnhandledAudioFormatException(sampleRate, channelCount, encoding)
        }
        val outputSampleRateHz = if (pendingOutputSampleRate == SAMPLE_RATE_NO_CHANGE) {
            sampleRate
        } else {
            pendingOutputSampleRate
        }
        if (this.sampleRate == sampleRate
            && this.channelCount == channelCount
            && this.outputSampleRateHz == outputSampleRateHz
        ) {
            return false
        }
        this.sampleRate = sampleRate
        this.channelCount = channelCount
        this.outputSampleRateHz = outputSampleRateHz
        return true
    }

    override fun isActive(): Boolean {
        return (outputSampleRateHz != NO_VALUE
                && (abs(speed - 1f) >= CLOSE_THRESHOLD
                || abs(pitch - 1f) >= CLOSE_THRESHOLD
                || outputSampleRateHz != sampleRate))
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        sonic?.let {
            if (inputBuffer.hasRemaining()) {
                val shortBuffer: ShortBuffer = inputBuffer.asShortBuffer()
                val inputSize: Int = inputBuffer.remaining()
                inputBytes += inputSize.toLong()
                it.queueInput(shortBuffer)
                inputBuffer.position(inputBuffer.position() + inputSize)
            }
            val outputSize = it.getOutputSize()
            if (outputSize > 0) {
                if (buffer.capacity() < outputSize) {
                    buffer = ByteBuffer.allocateDirect(outputSize).order(ByteOrder.nativeOrder())
                    shortBuffer = buffer.asShortBuffer()
                } else {
                    buffer.clear()
                    shortBuffer.clear()
                }
                it.getOutput(shortBuffer)
                outputBytes += outputSize.toLong()
                buffer.limit(outputSize)
                outputBuffer = buffer
            }
        }
    }

    override fun queueEndOfStream() {
        sonic?.queueEndOfStream()
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val outputBuffer = outputBuffer
        this.outputBuffer = emptyBuffer
        return outputBuffer
    }

    override fun isEnded(): Boolean {
        return inputEnded && (sonic == null || sonic?.getOutputSize() == 0)
    }

    override fun flush() {
        if (isActive()) {
            if (pendingSonicRecreation) {
                sonic = Sonic(
                    sampleRate,
                    channelCount,
                    speed,
                    pitch,
                    outputSampleRateHz
                )
            } else {
                sonic?.flush()
            }
        }
        outputBuffer = emptyBuffer
        inputBytes = 0
        outputBytes = 0
        inputEnded = false
    }

    override fun reset() {
        speed = 1f
        pitch = 1f
        channelCount = NO_VALUE
        sampleRate = NO_VALUE
        outputSampleRateHz = NO_VALUE
        buffer = emptyBuffer
        shortBuffer = buffer.asShortBuffer()
        outputBuffer = emptyBuffer
        pendingOutputSampleRate = SAMPLE_RATE_NO_CHANGE
        pendingSonicRecreation = false
        sonic = null
        inputBytes = 0
        outputBytes = 0
        inputEnded = false
    }

    /**
     * Constrains a value to the specified bounds.
     *
     * @param value The value to constrain.
     * @param min The lower bound.
     * @param max The upper bound.
     * @return The constrained value `Math.max(min, Math.min(value, max))`.
     */
    private fun constrainValue(
        value: Float,
        min: Float,
        max: Float
    ): Float {
        return max(min, min(value, max))
    }

    /**
     * Scales a large timestamp.
     *
     *
     * Logically, scaling consists of a multiplication followed by a division. The actual operations
     * performed are designed to minimize the probability of overflow.
     *
     * @param timestamp The timestamp to scale.
     * @param multiplier The multiplier.
     * @param divisor The divisor.
     * @return The scaled timestamp.
     */
    private fun scaleLargeTimestamp(
        timestamp: Long,
        multiplier: Long,
        divisor: Long
    ): Long {
        return if (divisor >= multiplier && divisor % multiplier == 0L) {
            val divisionFactor = divisor / multiplier
            timestamp / divisionFactor
        } else if (divisor < multiplier && multiplier % divisor == 0L) {
            val multiplicationFactor = multiplier / divisor
            timestamp * multiplicationFactor
        } else {
            val multiplicationFactor = multiplier.toDouble() / divisor
            (timestamp * multiplicationFactor).toLong()
        }
    }
}