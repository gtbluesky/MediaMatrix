package com.gtbluesky.mediamatrix.player;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

import com.gtbluesky.mediamatrix.player.listener.OnRecordTimeListener;
import com.gtbluesky.mediamatrix.player.util.LogUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class AACEncode {
    private MediaFormat encoderFormat = null;
    private MediaCodec encoder = null;
    private FileOutputStream outputStream = null;
    private MediaCodec.BufferInfo info = null;
    private int perpcmsize = 0;
    private byte[] outByteBuffer = null;
    private int aacSampleRate = 4;
    private double recordTime = 0; //ms
    private int audioSampleRate = 0;
    private OnRecordTimeListener mOnRecordTimeListener;

    public void initMediacodec(int samperate, File outfile) {
        try {
            aacSampleRate = getAdtsSampleRate(samperate);
            encoderFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, samperate, 2);
            encoderFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
            encoderFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            encoderFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 4096);
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            info = new MediaCodec.BufferInfo();
            if (encoder == null) {
                LogUtil.d("craete encoder wrong");
                return;
            }
            recordTime = 0;
            encoder.configure(encoderFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            outputStream = new FileOutputStream(outfile);
            encoder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void convertPcmToAac(int size, byte[] buffer) {
        LogUtil.d("buffer size is: " + size);
        if (buffer != null && encoder != null) {
            recordTime += size * 1000f / (audioSampleRate * 2 * 2);
            if (mOnRecordTimeListener != null) {
                mOnRecordTimeListener.onRecordTime((int) recordTime);
            }
            int inputBufferIndex = encoder.dequeueInputBuffer(0);
            if (inputBufferIndex >= 0) {
                ByteBuffer byteBuffer;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    byteBuffer = encoder.getInputBuffer(inputBufferIndex);
                } else {
                    byteBuffer = encoder.getInputBuffers()[inputBufferIndex];
                }
                byteBuffer.clear();
                byteBuffer.put(buffer);
                encoder.queueInputBuffer(inputBufferIndex, 0, size, 0, 0);
            }

            int index = encoder.dequeueOutputBuffer(info, 0);
            while (index >= 0) {
                try {
                    perpcmsize = info.size + 7;
                    outByteBuffer = new byte[perpcmsize];

                    ByteBuffer byteBuffer;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        byteBuffer = encoder.getOutputBuffer(index);
                    } else {
                        byteBuffer = encoder.getOutputBuffers()[index];
                    }
                    byteBuffer.position(info.offset);
                    byteBuffer.limit(info.offset + info.size);

                    addAdtsHeader(outByteBuffer, perpcmsize, aacSampleRate);

                    byteBuffer.get(outByteBuffer, 7, info.size);
                    byteBuffer.position(info.offset);
                    outputStream.write(outByteBuffer, 0, perpcmsize);

                    encoder.releaseOutputBuffer(index, false);
                    index = encoder.dequeueOutputBuffer(info, 0);
                    outByteBuffer = null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void addAdtsHeader(byte[] packet, int packetLen, int samplerate) {
        int profile = 2; // AAC LC
        int freqIdx = samplerate; // samplerate
        int chanCfg = 2; // CPE

        packet[0] = (byte) 0xFF; // 0xFFF(12bit) 这里只取了8位，所以还差4位放到下一个里面
        packet[1] = (byte) 0xF9; // 第一个t位放F
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    private int getAdtsSampleRate(int sampleRate) {
        int rate = 4;
        switch (sampleRate) {
            case 96000:
                rate = 0;
                break;
            case 88200:
                rate = 1;
                break;
            case 64000:
                rate = 2;
                break;
            case 48000:
                rate = 3;
                break;
            case 44100:
                rate = 4;
                break;
            case 32000:
                rate = 5;
                break;
            case 24000:
                rate = 6;
                break;
            case 22050:
                rate = 7;
                break;
            case 16000:
                rate = 8;
                break;
            case 12000:
                rate = 9;
                break;
            case 11025:
                rate = 10;
                break;
            case 8000:
                rate = 11;
                break;
            case 7350:
                rate = 12;
                break;
            default:
                break;
        }
        return rate;
    }

    public void releaseMediacodec() {
        if (encoder == null) {
            return;
        }
        try {
            recordTime = 0;
            outputStream.close();
            outputStream = null;
            encoder.stop();
            encoder.release();
            encoder = null;
            encoderFormat = null;
            info = null;
            LogUtil.d("录制完成...");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                outputStream = null;
            }
        }
    }

    public void setOnRecordTimeListener(OnRecordTimeListener listener) {
        this.mOnRecordTimeListener = listener;
    }
}
