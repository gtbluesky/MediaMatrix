//
// Created by gtbluesky on 18-9-15.
//

#include "AudioInfo.h"

AudioInfo::AudioInfo(PlayStatus *playStatus, int sample_rate, NativeCallJava *callJava) {
    this->status = playStatus;
    this->sample_rate = sample_rate;
    this->callJava = callJava;
    queue = new PacketQueue(playStatus);
    pcm_buffer_queue = new PcmBufferQueue(status);
    buffer = (uint8_t *) av_malloc(sample_rate * 2 * 2);

    sampleBuffer = (SAMPLETYPE *) (malloc(sample_rate * 2 * 2));
    soundTouch = new SoundTouch();
    soundTouch->setSampleRate(sample_rate);
    soundTouch->setChannels(2);
    soundTouch->setPitch(pitch);
    soundTouch->setTempo(speed);
}

AudioInfo::~AudioInfo() {
    release();
}

void *splitPcmBuffer(void *data) {
    AudioInfo *audio = (AudioInfo *) data;
    while (audio->status != NULL && !audio->status->is_exited) {
        PcmBean *pcmBean = NULL;
        audio->pcm_buffer_queue->getBuffer(&pcmBean);
        if (pcmBean == NULL) {
            continue;
        }

        LogUtil::d("pcmbean buffer size is %d", pcmBean->buffer_size);

        if (pcmBean->buffer_size <= audio->default_pcm_size)//不用分包
        {
            if (audio->is_recording) {
                audio->callJava->onCallPCM2AAC(NATIVE_THREAD, pcmBean->buffer_size,
                                               pcmBean->buffer);
            }
        } else {

            int pack_num = pcmBean->buffer_size / audio->default_pcm_size;
            int pack_sub = pcmBean->buffer_size % audio->default_pcm_size;

            for (int i = 0; i < pack_num; i++) {
                char *bf = (char *)(malloc(static_cast<size_t>(audio->default_pcm_size)));
                memcpy(bf, pcmBean->buffer + i * audio->default_pcm_size,
                       static_cast<size_t>(audio->default_pcm_size));
                if (audio->is_recording) {
                    audio->callJava->onCallPCM2AAC(NATIVE_THREAD, audio->default_pcm_size, bf);
                }
                free(bf);
                bf = NULL;
            }

            if (pack_sub > 0) {
                char *bf = (char *)(malloc(pack_sub));
                memcpy(bf, pcmBean->buffer + pack_num * audio->default_pcm_size, pack_sub);
                if (audio->is_recording) {
                    audio->callJava->onCallPCM2AAC(NATIVE_THREAD, pack_sub, bf);
                }
            }
        }
        delete pcmBean;
        pcmBean = NULL;
    }
    pthread_exit(&audio->pcm_buffer_thread);
}

void *decodePlay(void *data) {
    AudioInfo *audioInfo = (AudioInfo *) data;
    audioInfo->initOpenSLES();
    pthread_exit(&audioInfo->thread_play);
}

void AudioInfo::play() {
    pthread_create(&thread_play, NULL, decodePlay, this);
    pthread_create(&pcm_buffer_thread, NULL, splitPcmBuffer, this);
}


void AudioInfo::pause() {
    if (pcmPlayerPlay) {
        (*pcmPlayerPlay)->SetPlayState(pcmPlayerPlay, SL_PLAYSTATE_PAUSED);
    }
}

void AudioInfo::resume() {
    if (pcmPlayerPlay) {
        (*pcmPlayerPlay)->SetPlayState(pcmPlayerPlay, SL_PLAYSTATE_PLAYING);
    }
}


void AudioInfo::stop() {
    if (pcmPlayerPlay) {
        (*pcmPlayerPlay)->SetPlayState(pcmPlayerPlay, SL_PLAYSTATE_STOPPED);
    }
}

void AudioInfo::release() {
    stop();
    if (pcm_buffer_queue) {
        pcm_buffer_queue->noticeThread();
        pthread_join(pcm_buffer_thread, NULL);
        delete pcm_buffer_queue;
        pcm_buffer_thread = NULL;
    }
    if (queue) {
        delete queue;
        queue = NULL;
    }
    if (pcmPlayerObject) {
        (*pcmPlayerObject)->Destroy(pcmPlayerObject);
        pcmPlayerObject = NULL;
        pcmPlayerPlay = NULL;
        simpleBufferQueue = NULL;
        pcmMutePlay = NULL;
        pcmVolumePlay = NULL;
    }
    if (outputMixObject) {
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixObject = NULL;
        outputMixEnvironmentalReverb = NULL;
    }
    if (engineObject) {
        (*engineObject)->Destroy(engineObject);
        engineObject = NULL;
        engineEngine = NULL;
    }
    if (buffer) {
        free(buffer);
        buffer = NULL;
    }
    if (out_buffer) {
        out_buffer = NULL;
    }
    if (soundTouch) {
        delete soundTouch;
        soundTouch = NULL;
    }
    if (sampleBuffer) {
        free(sampleBuffer);
        sampleBuffer = NULL;
    }
    if (avCodecContext) {
        avcodec_close(avCodecContext);
        avcodec_free_context(&avCodecContext);
        avCodecContext = NULL;
    }
    if (status) {
        status = NULL;
    }
    if (callJava) {
        callJava = NULL;
    }
}

//FILE *outFIle = fopen("/storage/emulated/0/music.pcm", "w");

int AudioInfo::reSampleAudio(void **pcmbuf) {
    while (status && !status->is_exited) {
        if (status->is_seeking) {
            av_usleep(100 * 1000);
            continue;
        }
        if (!queue->getQueueSize()) {
            if (!status->is_loading) {
                status->is_loading = true;
                callJava->onCallLoad(NATIVE_THREAD, true);
            }
            av_usleep(100 * 1000);
            continue;
        } else {
            if (status->is_loading) {
                status->is_loading = false;
                callJava->onCallLoad(NATIVE_THREAD, false);
            }
        }
        if (isFrameFinished) {
            avPacket = av_packet_alloc();
            if (queue->getAVPacket(avPacket) != 0) {
                av_packet_free(&avPacket);
                av_free(avPacket);
                avPacket = NULL;
                continue;
            }
            ret = avcodec_send_packet(avCodecContext, avPacket);
            if (ret != 0) {
                av_packet_free(&avPacket);
                av_free(avPacket);
                avPacket = NULL;
                continue;
            }
        }
        avFrame = av_frame_alloc();
        ret = avcodec_receive_frame(avCodecContext, avFrame);
        if (ret == 0) {
            isFrameFinished = false;
            if (avFrame->channels > 0 && avFrame->channel_layout == 0) {
                avFrame->channel_layout = av_get_default_channel_layout(avFrame->channels);
            } else if (avFrame->channels == 0 && avFrame->channel_layout > 0) {
                avFrame->channels = av_get_channel_layout_nb_channels(avFrame->channel_layout);
            }
            SwrContext *swrCtx = swr_alloc_set_opts(
                    NULL,
                    AV_CH_LAYOUT_STEREO,
                    AV_SAMPLE_FMT_S16,
                    avFrame->sample_rate,
                    avFrame->channel_layout,
                    (AVSampleFormat) (avFrame->format),
                    avFrame->sample_rate,
                    NULL, NULL
            );
            if (!swrCtx || swr_init(swrCtx) < 0) {
                av_packet_free(&avPacket);
                av_free(avPacket);
                avPacket = NULL;
                av_frame_free(&avFrame);
                av_free(avFrame);
                avFrame = NULL;
                if (swrCtx) {
                    swr_free(&swrCtx);
                    swrCtx = NULL;
                }
                isFrameFinished = true;
                continue;
            }

            re_sample_num = swr_convert(
                    swrCtx,
                    &buffer,
                    avFrame->nb_samples,
                    (const uint8_t **) avFrame->data,
                    avFrame->nb_samples
            );

            int outChannels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
            int bytesPerSample = av_get_bytes_per_sample(AV_SAMPLE_FMT_S16);
            dataSize = re_sample_num * outChannels * bytesPerSample;

            now_time = avFrame->pts * av_q2d(time_base) * 1000; //单位: ms
            LogUtil::e("now_time: %f", now_time);
            LogUtil::e("clock: %f", clock);
//            if (now_time < clock) {
//                now_time = clock;
//            }
            clock = now_time;
            *pcmbuf = buffer;
//            fwrite(buffer, 1, dataSize, outFIle);
//            av_packet_free(&avPacket);
//            av_free(avPacket);
//            avPacket = NULL;
            av_frame_free(&avFrame);
            av_free(avFrame);
            avFrame = NULL;
            swr_free(&swrCtx);
            swrCtx = NULL;
            break;
        } else {
            isFrameFinished = true;
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            av_frame_free(&avFrame);
            av_free(avFrame);
            avFrame = NULL;
            continue;
        }
    }
//    fclose(outFIle);
    return dataSize;
}

void pcmBufferCallBack(SLAndroidSimpleBufferQueueItf bf, void *context) {
    AudioInfo *audioInfo = (AudioInfo *) context;
    if (audioInfo) {
        int outChannels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
        int bytesPerSample = av_get_bytes_per_sample(AV_SAMPLE_FMT_S16);
        int bufferSize = audioInfo->getSoundTouchData() * outChannels * bytesPerSample;
        if (bufferSize > 0) {
            audioInfo->clock += 1000 * bufferSize / ((double) (audioInfo->sample_rate * outChannels * bytesPerSample));
            if (audioInfo->clock - audioInfo->last_time >= 100) { //100ms
                audioInfo->last_time = audioInfo->clock;
                audioInfo->callJava->onCallTimeInfo(NATIVE_THREAD, audioInfo->clock,
                                                    audioInfo->duration);
            }
//            if (audioInfo->is_recording) {
//                audioInfo->callJava->onCallPCM2AAC(NATIVE_THREAD, bufferSize, audioInfo->sampleBuffer);
//            }
            audioInfo->pcm_buffer_queue->putBuffer(audioInfo->sampleBuffer, bufferSize);
            audioInfo->callJava->onCallVolumeDB(NATIVE_THREAD,
                                                audioInfo->getPcmDb(
                                                        (char *) audioInfo->sampleBuffer,
                                                        bufferSize));
            (*audioInfo->simpleBufferQueue)->Enqueue(audioInfo->simpleBufferQueue,
                                                     audioInfo->sampleBuffer,
                                                     bufferSize);
        }
    }
}

void AudioInfo::initOpenSLES() {

    SLresult result;
    result = slCreateEngine(&engineObject, 0, 0, 0, 0, 0);
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);

    //第二步，创建混音器
    const SLInterfaceID mids[1] = {SL_IID_ENVIRONMENTALREVERB};
    const SLboolean mreq[1] = {SL_BOOLEAN_FALSE};
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 1, mids, mreq);
    (void) result;
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    (void) result;
    result = (*outputMixObject)->GetInterface(outputMixObject, SL_IID_ENVIRONMENTALREVERB,
                                              &outputMixEnvironmentalReverb);
    if (SL_RESULT_SUCCESS == result) {
        result = (*outputMixEnvironmentalReverb)->SetEnvironmentalReverbProperties(
                outputMixEnvironmentalReverb, &reverbSettings);
        (void) result;
    }
    SLDataLocator_OutputMix outputMix = {
            SL_DATALOCATOR_OUTPUTMIX,
            outputMixObject
    };
    SLDataSink audioSnk = {&outputMix, 0};


    // 第三步，配置PCM格式信息
    SLDataLocator_AndroidSimpleBufferQueue android_queue = {
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
            2
    };

    SLDataFormat_PCM pcm = {
            SL_DATAFORMAT_PCM,//播放pcm格式的数据
            2,//2个声道（立体声）
            (SLuint32) getCurrentSampleRateForOpensles(sample_rate),//44100hz的频率
            SL_PCMSAMPLEFORMAT_FIXED_16,//位数16位
            SL_PCMSAMPLEFORMAT_FIXED_16,//和位数一致就行
            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,//立体声（前左前右）
            SL_BYTEORDER_LITTLEENDIAN//结束标志
    };
    SLDataSource slDataSource = {&android_queue, &pcm};

    const SLInterfaceID ids[4] = {
            SL_IID_BUFFERQUEUE,
            SL_IID_VOLUME,
            SL_IID_PLAYBACKRATE,
            SL_IID_MUTESOLO
    };
    const SLboolean req[4] = {
            SL_BOOLEAN_TRUE,
            SL_BOOLEAN_TRUE,
            SL_BOOLEAN_TRUE,
            SL_BOOLEAN_TRUE
    };

    (*engineEngine)->CreateAudioPlayer(engineEngine, &pcmPlayerObject, &slDataSource, &audioSnk, 4,
                                       ids, req);
    //初始化播放器
    (*pcmPlayerObject)->Realize(pcmPlayerObject, SL_BOOLEAN_FALSE);

//    得到接口后调用  获取Player接口
    (*pcmPlayerObject)->GetInterface(pcmPlayerObject, SL_IID_PLAY, &pcmPlayerPlay);
//    获取声音接口
    (*pcmPlayerObject)->GetInterface(pcmPlayerObject, SL_IID_VOLUME, &pcmVolumePlay);
//    获取声道接口
    (*pcmPlayerObject)->GetInterface(pcmPlayerObject, SL_IID_MUTESOLO, &pcmMutePlay);
//    注册回调缓冲区 获取缓冲队列接口
    (*pcmPlayerObject)->GetInterface(pcmPlayerObject, SL_IID_BUFFERQUEUE, &simpleBufferQueue);
    setVolume(volumePercent);
    setSoundChannel(sound_channel);
    //缓冲接口回调
    (*simpleBufferQueue)->RegisterCallback(simpleBufferQueue, pcmBufferCallBack, this);
//    获取播放状态接口
    (*pcmPlayerPlay)->SetPlayState(pcmPlayerPlay, SL_PLAYSTATE_PLAYING);
    pcmBufferCallBack(simpleBufferQueue, this);


}

int AudioInfo::getCurrentSampleRateForOpensles(int sampleRate) {
    int rate = 0;
    switch (sampleRate) {
        case 8000:
            rate = SL_SAMPLINGRATE_8;
            break;
        case 11025:
            rate = SL_SAMPLINGRATE_11_025;
            break;
        case 12000:
            rate = SL_SAMPLINGRATE_12;
            break;
        case 16000:
            rate = SL_SAMPLINGRATE_16;
            break;
        case 22050:
            rate = SL_SAMPLINGRATE_22_05;
            break;
        case 24000:
            rate = SL_SAMPLINGRATE_24;
            break;
        case 32000:
            rate = SL_SAMPLINGRATE_32;
            break;
        case 44100:
            rate = SL_SAMPLINGRATE_44_1;
            break;
        case 48000:
            rate = SL_SAMPLINGRATE_48;
            break;
        case 64000:
            rate = SL_SAMPLINGRATE_64;
            break;
        case 88200:
            rate = SL_SAMPLINGRATE_88_2;
            break;
        case 96000:
            rate = SL_SAMPLINGRATE_96;
            break;
        case 192000:
            rate = SL_SAMPLINGRATE_192;
            break;
        default:
            rate = SL_SAMPLINGRATE_44_1;
            break;
    }
    return rate;
}

void AudioInfo::setVolume(int percent) {
    volumePercent = percent;
    if (pcmVolumePlay) {
        if (percent > 30) {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -20);
        } else if (percent > 25) {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -22);
        } else if (percent > 20) {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -25);
        } else if (percent > 15) {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -28);
        } else if (percent > 10) {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -30);
        } else if (percent > 5) {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -34);
        } else if (percent > 3) {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -37);
        } else if (percent > 0) {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -40);
        } else {
            (*pcmVolumePlay)->SetVolumeLevel(pcmVolumePlay, (100 - percent) * -100);
        }
    }
}

void AudioInfo::setSoundChannel(int sound_channel) {
    this->sound_channel = sound_channel;
    if (pcmMutePlay) {
        if (sound_channel == 0) {
            //right
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 1, SL_BOOLEAN_FALSE);
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 0, SL_BOOLEAN_TRUE);
        } else if (sound_channel == 1) {
            //left
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 1, SL_BOOLEAN_TRUE);
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 0, SL_BOOLEAN_FALSE);
        } else if (sound_channel == 2) {
            //center
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 1, SL_BOOLEAN_FALSE);
            (*pcmMutePlay)->SetChannelMute(pcmMutePlay, 0, SL_BOOLEAN_FALSE);
        }
    }
}

int AudioInfo::getSoundTouchData() {
    while (status && !status->is_exited) {
        out_buffer = NULL;
        if (handle_finished) {
            handle_finished = false;
            dataSize = reSampleAudio((void **) &out_buffer);
            if (dataSize > 0) {
                //FFmpeg 8bit pcm 转 SoundTouch 16bit pcm
                for (int i = 0; i < dataSize / 2 + 1; ++i) {
                    sampleBuffer[i] = (out_buffer[i * 2] | (out_buffer[i * 2 + 1] << 8));
                }
                soundTouch->putSamples(sampleBuffer, re_sample_num);
                st_sample_num = soundTouch->receiveSamples(sampleBuffer, re_sample_num);
            } else {
                soundTouch->flush();
            }
        }
        if (st_sample_num == 0) {
            handle_finished = true;
            continue;
        } else {
            //一个avframe的数据未处理完的情况
            if (!out_buffer) {
                st_sample_num = soundTouch->receiveSamples(sampleBuffer, re_sample_num);
                if (st_sample_num == 0) {
                    handle_finished = true;
                    continue;
                }
            }
            return st_sample_num;
        }
    }
    return 0;
}

void AudioInfo::setPitch(float pitch) {
    this->pitch = pitch;
    if (soundTouch) {
        soundTouch->setPitch(pitch);
    }
}

void AudioInfo::setSpeed(float speed) {
    this->speed = speed;
    if (soundTouch) {
        soundTouch->setTempo(speed);
    }
}

int AudioInfo::getPcmDb(char *pcmcata, size_t pcmsize) {
    int db = 0;
    short int pervalue = 0;
    double sum = 0;
    for (int i = 0; i < pcmsize; i += 2) {
        memcpy(&pervalue, pcmcata + i, 2);
        sum += abs(pervalue);
    }
    sum = sum / (pcmsize / 2);
    if (sum > 0) {
        db = (int) 20.0 * log10(sum);
    }
    return db;
}

void AudioInfo::controlRecord(bool start) {
    this->is_recording = start;
}
