package com.gtbluesky.codec

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Looper
import android.util.Log
import java.io.IOException
import java.nio.ByteBuffer

/**
 * 基于 MediaExtractor + MediaMuxer 实现
 */
class HwVideoCombiner(
    private val videoList: List<String>,
    private val destPath: String,
    private val combineListener: CombineListener? = null
) {

    private var muxer: MediaMuxer? = null
    private var readBuf: ByteBuffer
    private var outAudioTrackIndex =
        INVALID_TRACK_INDEX
    private var outVideoTrackIndex =
        INVALID_TRACK_INDEX
    private var audioFormat: MediaFormat? = null
    private var videoFormat: MediaFormat? = null

    companion object {
        private val TAG = HwVideoCombiner::class.java.simpleName
        private const val INVALID_TRACK_INDEX = -1
        private const val MAX_BUFFER_SIZE = 1 shl 20
    }

    init {
        readBuf = ByteBuffer.allocate(MAX_BUFFER_SIZE)
    }

    fun combine() {
        check(Looper.getMainLooper() != Looper.myLooper()) {
            "Please use this method in work thread!"
        }
        var hasAudioFormat = false
        var hasVideoFormat = false
        val videoIterator = videoList.iterator()
        // 开始合并
        combineListener?.onCombineStart()
        // MediaExtractor 获取多媒体信息
        // 获取到第一个可用的音频和视频的 MediaFormat
        while (videoIterator.hasNext()) {
            val videoPath = videoIterator.next()
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(videoPath)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            var trackIndex: Int
            if (!hasVideoFormat) {
                trackIndex = selectTrack(extractor, "video/")
                if (trackIndex > INVALID_TRACK_INDEX) {
                    extractor.selectTrack(trackIndex)
                    videoFormat = extractor.getTrackFormat(trackIndex)
                    hasVideoFormat = true
                } else {
                    Log.e(TAG, "No video track found in $videoPath")
                }
            }
            if (!hasAudioFormat) {
                trackIndex = selectTrack(extractor, "audio/")
                if (trackIndex > INVALID_TRACK_INDEX) {
                    extractor.selectTrack(trackIndex)
                    audioFormat = extractor.getTrackFormat(trackIndex)
                    hasAudioFormat = true
                } else {
                    Log.e(TAG, "No audio track found in $videoPath")
                }
            }
            extractor.release()
            if (hasVideoFormat && hasAudioFormat) {
                break
            }
        }
        // MediaMuxer 创建文件
        try {
            muxer = MediaMuxer(destPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        // 以获取到的音频和视频的 MediaFormat 作为输出视频和音频的格式
        if (hasVideoFormat) {
            outVideoTrackIndex = muxer?.addTrack(videoFormat!!) ?: INVALID_TRACK_INDEX
        }
        if (hasAudioFormat) {
            outAudioTrackIndex = muxer?.addTrack(audioFormat!!) ?: INVALID_TRACK_INDEX
        }
        muxer?.start()
        // MediaExtractor 遍历读取帧，MediaMuxer 写入帧，并记录帧信息
        var ptsOffset = 0L
        val trackIndex = videoList.iterator()
        var currentVideo = 0
        var combineResult = true
        while (trackIndex.hasNext()) {
            // 监听当前合并第几个视频
            combineListener?.onCombineProcessing(++currentVideo, videoList.size)
            val videoPath = trackIndex.next()
            var hasVideo = false
            var hasAudio = false
            // 选择视频轨道
            val videoExtractor = MediaExtractor()
            try {
                videoExtractor.setDataSource(videoPath)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val inVideoTrackIndex = selectTrack(videoExtractor, "video/")
            if (inVideoTrackIndex > INVALID_TRACK_INDEX) {
                hasVideo = true
            }
            videoExtractor.selectTrack(inVideoTrackIndex)
            // 选择音频轨道
            val audioExtractor = MediaExtractor()
            try {
                audioExtractor.setDataSource(videoPath)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val inAudioTrackIndex = selectTrack(audioExtractor, "audio/")
            if (inAudioTrackIndex > INVALID_TRACK_INDEX) {
                hasAudio = true
            }
            audioExtractor.selectTrack(inAudioTrackIndex)
            // 如果存在视频轨道和音频轨道都不存在，则合并失败，文件出错
            if (!hasVideo && !hasAudio) {
                combineResult = false
                videoExtractor.release()
                audioExtractor.release()
                break
            }
            var presentationTimeUs: Long
            var audioPts = 0L
            var videoPts = 0L
            var hasVideoData = true
            var hasAudioData = true
            while (hasVideoData || hasAudioData) {
                var outTrackIndex: Int
                var extractor: MediaExtractor
                var currentTrackIndex: Int
                if ((!hasVideoData || audioPts - videoPts <= 50000L) && hasAudioData) {
                    currentTrackIndex = inAudioTrackIndex
                    outTrackIndex = outAudioTrackIndex
                    extractor = audioExtractor
                } else {
                    currentTrackIndex = inVideoTrackIndex
                    outTrackIndex = outVideoTrackIndex
                    extractor = videoExtractor
                }
                Log.d(TAG, "currentTrackIndex: $currentTrackIndex, outTrackIndex: $outTrackIndex")
                readBuf.rewind()
                // 读取数据帧
                val frameSize = extractor.readSampleData(readBuf, 0)
                if (frameSize < 0) {
                    if (currentTrackIndex == inVideoTrackIndex) {
                        hasVideoData = false
                    } else if (currentTrackIndex == inAudioTrackIndex) {
                        hasAudioData = false
                    }
                } else {
                    if (extractor.sampleTrackIndex != currentTrackIndex) {
                        Log.e(TAG, "got sample from track ${extractor.sampleTrackIndex}, expected $currentTrackIndex")
                    }
                    // 读取帧的pts
                    presentationTimeUs = extractor.sampleTime
                    if (currentTrackIndex == inVideoTrackIndex) {
                        videoPts = presentationTimeUs
                    } else {
                        audioPts = presentationTimeUs
                    }
                    // 帧信息
                    val info = MediaCodec.BufferInfo().also {
                        it.offset = 0
                        it.size = frameSize
                        it.presentationTimeUs = ptsOffset + presentationTimeUs
                        if (extractor.sampleFlags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0) {
                            it.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
                        }
                    }
                    readBuf.rewind()
                    Log.d(
                        TAG, String.format(
                            "write sample track %d, size %d, pts %d flag %d",
                            Integer.valueOf(outTrackIndex),
                            Integer.valueOf(info.size),
                            java.lang.Long.valueOf(info.presentationTimeUs),
                            Integer.valueOf(info.flags)
                        )
                    )
                    // 将读取到的数据写入文件
                    muxer?.writeSampleData(outTrackIndex, readBuf, info)
                    extractor.advance()
                }
            }
            // 当前文件最后一帧的PTS，用作下一个视频的PTS
            ptsOffset += if (videoPts > audioPts) videoPts else audioPts
            // 当前文件最后一帧和下一帧的间隔差40ms，默认录制25fps的视频，帧间隔时间就是40ms
            // 但由于使用MediaCodec录制完之后，后面又写入了一个OES的帧，导致前面解析的时候会有时间差
            // 这里设置10ms效果比40ms的要好些。
            ptsOffset += 10000
            Log.d(TAG, "finish one file, ptsOffset $ptsOffset")
            // 释放资源
            videoExtractor.release()
            audioExtractor.release()
        }
        // 释放复用器
        try {
            muxer?.stop()
            muxer?.release()
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Muxer close error. No data was written")
        } finally {
            muxer = null
        }
        Log.d(TAG, "video combine finished")
        // 合并结束
        combineListener?.onCombineFinished(combineResult)
    }


    /**
     * 选择轨道
     * @param extractor     MediaExtractor
     * @param mimePrefix    音频轨或视频轨
     * @return
     */
    private fun selectTrack(
        extractor: MediaExtractor,
        mimePrefix: String
    ): Int {
        // 获取轨道总数
        val numTracks = extractor.trackCount
        // 遍历查找包含mimePrefix的轨道
        for (i in 0 until numTracks) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString("mime")
            if (mime?.startsWith(mimePrefix) == true) {
                return i
            }
        }
        return INVALID_TRACK_INDEX
    }

    interface CombineListener {
        /**
         * 合并开始
         */
        fun onCombineStart()

        /**
         * 合并过程
         * @param current 当前合并的视频
         * @param sum   合并视频总数
         */
        fun onCombineProcessing(current: Int, sum: Int)

        /**
         * 合并结束
         * @param success   是否合并成功
         */
        fun onCombineFinished(success: Boolean)
    }
}