//
// Created by gtbluesky on 18-9-15.
//

#ifndef MEDIAPLAYER_AUDIOINFO_H
#define MEDIAPLAYER_AUDIOINFO_H

#include "PacketQueue.h"
#include "PlayStatus.h"
#include "NativeCallJava.h"
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <SoundTouch.h>
#include "PcmBufferQueue.h"
#include "PcmBean.h"

using namespace soundtouch;

#ifdef __cplusplus
extern "C" {
#endif
#include <libavcodec/avcodec.h>
#include <libswresample/swresample.h>
#include <libavutil/time.h>

class AudioInfo {

public:
    int streamIndex = -1;
    AVCodecContext *avCodecContext = NULL;
    AVCodecParameters *codecPar = NULL;
    PacketQueue *queue = NULL;
    PlayStatus *status = NULL;
    NativeCallJava *callJava = NULL;

    pthread_t thread_play;
    AVPacket *avPacket = NULL;
    AVFrame *avFrame = NULL;
    int ret = -1;
    uint8_t *buffer = NULL;
    int dataSize = 0;
    int sample_rate = 0;

    int duration = 0; //单位ms
    AVRational time_base;
    double clock = 0;//总的播放时长:ms
    double now_time = 0;//当前frame时间
    double last_time = 0; //上一次调用时间
    int volumePercent = 100;
    int sound_channel = 2;
    float pitch = 1.0f;
    float speed = 1.0f;
    bool is_recording = false; //正在PCM转AAC
    bool isFrameFinished = true; //一个frame处理结束

    // 引擎接口
    SLObjectItf engineObject = NULL;
    SLEngineItf engineEngine = NULL;

    //混音器
    SLObjectItf outputMixObject = NULL;
    SLEnvironmentalReverbItf outputMixEnvironmentalReverb = NULL;
    SLEnvironmentalReverbSettings reverbSettings = SL_I3DL2_ENVIRONMENT_PRESET_STONECORRIDOR;

    //pcm
    SLObjectItf pcmPlayerObject = NULL;
    SLPlayItf pcmPlayerPlay = NULL;
    SLVolumeItf  pcmVolumePlay = NULL;
    SLMuteSoloItf pcmMutePlay = NULL;

    //缓冲器队列接口
    SLAndroidSimpleBufferQueueItf simpleBufferQueue = NULL;

    //SoundTouch
    SoundTouch *soundTouch = NULL;
    SAMPLETYPE *sampleBuffer = NULL;
    bool handle_finished = true; //soundtouch对一个avframe的数据全部处理完成
    uint8_t *out_buffer = NULL;
    int re_sample_num = 0; //重采样后每个avframe中的采样数
    int st_sample_num = 0; //soundtouch处理后每个avframe中的采样数

    pthread_t pcm_buffer_thread;
    PcmBufferQueue *pcm_buffer_queue = NULL;
    int default_pcm_size = 4096;

public:
    AudioInfo(PlayStatus *playStatus, int sample_rate, NativeCallJava *callJava);

    ~AudioInfo();

    void play();

    void pause();

    void resume();

    void stop();

    void release();

    void setVolume(int percent);

    void setSoundChannel(int sound_channel);

    int getSoundTouchData();

    void setPitch(float pitch);

    void setSpeed(float speed);

    int getPcmDb(char *pcmcata, size_t pcmsize);

    void controlRecord(bool start);

    int reSampleAudio(void **pcmbuf);

    void initOpenSLES();

    int getCurrentSampleRateForOpensles(int sampleRate);
};

#ifdef __cplusplus
}
#endif

#endif //MEDIAPLAYER_AUDIOINFO_H
