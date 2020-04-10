package com.gtbluesky.codec

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Interface for audio processors, which take audio data as input and transform it, potentially
 * modifying its channel count, encoding and/or sample rate.
 *
 * <p>In addition to being able to modify the format of audio, implementations may allow parameters
 * to be set that affect the output audio and whether the processor is active/inactive.
 */
abstract class AudioProcessor {

    companion object {
        const val NO_VALUE = -1
    }
    /** Exception thrown when a processor can't be configured for a given input audio format.  */
    class UnhandledAudioFormatException(sampleRate: Int, channelCount: Int, encoding: Int) :
        Exception("Unhandled format: $sampleRate Hz, $channelCount channels in encoding $encoding")

    /** An empty, direct [ByteBuffer].  */
    protected var emptyBuffer: ByteBuffer =
        ByteBuffer.allocateDirect(0).order(ByteOrder.nativeOrder())

    /**
     * Configures the processor to process input audio with the specified format. After calling this
     * method, call [.isActive] to determine whether the audio processor is active. Returns
     * the configured output audio format if this instance is active.
     *
     *
     * After calling this method, it is necessary to [.flush] the processor to apply the
     * new configuration. Before applying the new configuration, it is safe to queue input and get
     * output in the old input/output formats. Call [.queueEndOfStream] when no more input
     * will be supplied in the old input format.
     *
     * @param sampleRate The sample rate of input audio in Hz.
     * @param channelCount The number of interleaved channels in input audio.
     * @param encoding The encoding of input audio.
     * @return The configured output audio format if this instance is [active][.isActive].
     * @throws UnhandledAudioFormatException Thrown if the specified format can't be handled as input.
     */
    @Throws(UnhandledAudioFormatException::class)
    abstract fun configure(sampleRate: Int, channelCount: Int, encoding: Int): Boolean

    /** Returns whether the processor is configured and will process input buffers.  */
    abstract fun isActive(): Boolean

    /**
     * Queues audio data between the position and limit of the input `buffer` for processing.
     * `buffer` must be a direct byte buffer with native byte order. Its contents are treated as
     * read-only. Its position will be advanced by the number of bytes consumed (which may be zero).
     * The caller retains ownership of the provided buffer. Calling this method invalidates any
     * previous buffer returned by [.getOutput].
     *
     * @param buffer The input buffer to process.
     */
    abstract fun queueInput(buffer: ByteBuffer)

    /**
     * Queues an end of stream signal. After this method has been called,
     * [.queueInput] may not be called until after the next call to
     * [.flush]. Calling [.getOutput] will return any remaining output data. Multiple
     * calls may be required to read all of the remaining output data. [.isEnded] will return
     * `true` once all remaining output data has been read.
     */
    abstract fun queueEndOfStream()

    /**
     * Returns a buffer containing processed output data between its position and limit. The buffer
     * will always be a direct byte buffer with native byte order. Calling this method invalidates any
     * previously returned buffer. The buffer will be empty if no output is available.
     *
     * @return A buffer containing processed output data between its position and limit.
     */
    abstract fun getOutput(): ByteBuffer

    /**
     * Returns whether this processor will return no more output from [.getOutput] until it
     * has been [.flush]ed and more input has been queued.
     */
    abstract fun isEnded(): Boolean

    /**
     * Clears any buffered data and pending output. If the audio processor is active, also prepares
     * the audio processor to receive a new stream of input in the last configured (pending) format.
     */
    abstract fun flush()

    /** Resets the processor to its unconfigured state, releasing any resources.  */
    abstract fun reset()
}