//
// Created by gtbluesky on 18-9-15.
//

#include "FFmpegController.h"

FFmpegController::FFmpegController(PlayStatus *playStatus, NativeCallJava *callJava, const char *url) {
    this->status = playStatus;
    this->callJava = callJava;
    this->url = url;
    pthread_mutex_init(&init_mutex, NULL);
    pthread_mutex_init(&seek_mutex, NULL);
}


FFmpegController::~FFmpegController() {
    release();
}

void *decodeFFmpeg(void *data) {
    FFmpegController *fFmpegController = (FFmpegController *) data;
    fFmpegController->decodeFFmpegThread();
    pthread_exit(&fFmpegController->decodeThread);
}

void FFmpegController::prepared() {

    pthread_create(&decodeThread, NULL, decodeFFmpeg, this);

}

int av_format_callback(void *ctx) {
    FFmpegController *fFmpegController = (FFmpegController *) ctx;
    if (fFmpegController->status->is_exited) {
        return AVERROR_EOF;
    }
    return 0;
}

void FFmpegController::decodeFFmpegThread() {

    pthread_mutex_lock(&init_mutex);
    //1.注册解码器并初始化网络
    // 4.0不需要注册可以直接使用
//    av_register_all();
    avformat_network_init();
    LogUtil::d("ver=%s", av_version_info());
    //2.打开文件或网络流
    pFormatCtx = avformat_alloc_context();
    pFormatCtx->interrupt_callback.callback = av_format_callback;
    pFormatCtx->interrupt_callback.opaque = this;
    int ret = avformat_open_input(&pFormatCtx, url, NULL, NULL);
    if (ret != 0) {
        LogUtil::e("can not open url :%s", url);
        char* buf = new char[1024];
        av_strerror(ret, buf, 1024);
        LogUtil::e("ret :%d, error:%s", ret, buf);
        callJava->onCallError(NATIVE_THREAD, 1001, "can not open url");
        decode_exit = true;
        pthread_mutex_unlock(&init_mutex);
        return;
    }
    //3.获取流信息
    ret = avformat_find_stream_info(pFormatCtx, NULL);
    if (ret < 0) {
        LogUtil::e("can not find streams from %s", url);
        char* buf = new char[1024];
        av_strerror(ret, buf, 1024);
        LogUtil::e("ret :%d, error:%s", ret, buf);
        callJava->onCallError(NATIVE_THREAD, 1002, "can not find streams from url");
        decode_exit = true;
        pthread_mutex_unlock(&init_mutex);
        return;
    }
    //4.获取音频和视频流
    for (int i = 0; i < pFormatCtx->nb_streams; i++) {
        //得到音频流
        if (pFormatCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            if (!audioInfo) {
                audioInfo = new AudioInfo(status, pFormatCtx->streams[i]->codecpar->sample_rate,
                                          callJava);
                audioInfo->streamIndex = i;
                audioInfo->codecPar = pFormatCtx->streams[i]->codecpar;
                audioInfo->duration = static_cast<int>(1000 * pFormatCtx->duration / AV_TIME_BASE); //ms
                audioInfo->time_base = pFormatCtx->streams[i]->time_base;
                duration = audioInfo->duration;
            }
        } else if (pFormatCtx->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            if (!videoInfo) {
                videoInfo = new VideoInfo(status, callJava);
                videoInfo->stream_index = i;
                videoInfo->codecpar = pFormatCtx->streams[i]->codecpar;
                videoInfo->time_base = pFormatCtx->streams[i]->time_base;
                int num = pFormatCtx->streams[i]->avg_frame_rate.num;
                int den = pFormatCtx->streams[i]->avg_frame_rate.den;
                if(num != 0 && den != 0) {
                    int fps = num / den;//[25 / 1]
                    videoInfo->defaultDelayTime = 1000.0 / fps;
                }
            }
        }
    }

    if (audioInfo) {
        getCodecContext(audioInfo->codecPar, &audioInfo->avCodecContext);
    }
    if (videoInfo) {
        getCodecContext(videoInfo->codecpar, &videoInfo->avCodecContext);
    }

    if (callJava) {
        if (status && !status->is_exited) {
            callJava->onCallPrepared(NATIVE_THREAD);
        } else {
            decode_exit = true;
        }
    }
    pthread_mutex_unlock(&init_mutex);
}

void FFmpegController::start() {

    if (!audioInfo) {
        LogUtil::e("audio is null");
        callJava->onCallError(NATIVE_THREAD, 1006, "audio is null");
        return;
    }

    audioInfo->play();
    if (videoInfo) {
        videoInfo->play();
        videoInfo->audioInfo = audioInfo;
    }
    while (status && !status->is_exited) {
        //seek时停止解码
        if (status->is_seeking) {
            av_usleep(100 * 1000);
            continue;
        }

        //解码队列缓存不宜太多
        if (audioInfo->queue->getQueueSize() > 100) {
            av_usleep(100 * 1000);
            continue;
        }
        //8.读取音频帧
        AVPacket *avPacket = av_packet_alloc();
        pthread_mutex_lock(&seek_mutex);
        int ret = av_read_frame(pFormatCtx, avPacket);
        pthread_mutex_unlock(&seek_mutex);
        if (ret == 0) {
            if (avPacket->stream_index == audioInfo->streamIndex) {
                //解码操作
                audioInfo->queue->putAVPacket(avPacket);
            } else if (avPacket->stream_index == videoInfo->stream_index) {
                videoInfo->queue->putAVPacket(avPacket);
            } else {
                av_packet_free(&avPacket);
                av_free(avPacket);
                avPacket = NULL;
            }
        } else {
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            while (status && !status->is_exited) {
                if (audioInfo->queue->getQueueSize() > 0) {
                    av_usleep(100 * 1000);
                    continue;
                } else {
                    status->is_exited = true;
                    break;
                }
            }
        }
    }
    if (callJava) {
        callJava->onCallComplete(NATIVE_THREAD);
    }
    decode_exit = true;
}

void FFmpegController::pause() {
    if (audioInfo) {
        audioInfo->pause();
    }
}

void FFmpegController::resume() {
    if (audioInfo) {
        audioInfo->resume();
    }
}

void FFmpegController::release() {
    status->is_exited = true;
    pthread_mutex_lock(&init_mutex);
    int sleep_count = 0;
    while (!decode_exit) {
        if (sleep_count > 1000) {
            decode_exit = true;
        }
        LogUtil::e("wait ffmpeg exit %d", sleep_count);
        sleep_count++;
        av_usleep(1000 * 10);//10ms
    }
    LogUtil::e("释放Audio");
    if (audioInfo) {
        delete audioInfo;
        audioInfo = NULL;
    }
    LogUtil::e("释放Video");
    if (videoInfo) {
        delete videoInfo;
        videoInfo = NULL;
    }
    LogUtil::e("释放封装格式上下文");
    if (pFormatCtx) {
        avformat_close_input(&pFormatCtx);
        avformat_free_context(pFormatCtx);
        pFormatCtx = NULL;
    }
    LogUtil::e("释放 nativecalljava");
    if (callJava) {
        callJava = NULL;
    }
    LogUtil::e("释放 playstatus");
    if (status) {
        status = NULL;
    }
    pthread_mutex_unlock(&init_mutex);
    pthread_mutex_destroy(&init_mutex);
    pthread_mutex_destroy(&seek_mutex);
}

void FFmpegController::seek(int64_t milliseconds) {
    if (duration <= 0) {
        return;
    }
    if (milliseconds >= 0 && milliseconds <= duration) {
        if (audioInfo) {
            status->is_seeking = true;
            audioInfo->queue->clearAvpacket();
            audioInfo->clock = 0;
            audioInfo->last_time = 0;
            pthread_mutex_lock(&seek_mutex);
            int64_t ts = milliseconds * AV_TIME_BASE / 1000;
            avcodec_flush_buffers(audioInfo->avCodecContext);
            avformat_seek_file(pFormatCtx, -1, INT64_MIN, ts, INT64_MAX, 0);
            pthread_mutex_unlock(&seek_mutex);
            status->is_seeking = false;
        }
    }
}

void FFmpegController::setVolume(int percent) {
    if (audioInfo) {
        audioInfo->setVolume(percent);
    }
}

void FFmpegController::setSoundChannel(int sound_channle) {
    if (audioInfo) {
        audioInfo->setSoundChannel(sound_channle);
    }
}

void FFmpegController::setPitch(float pitch) {
    if (audioInfo) {
        audioInfo->setPitch(pitch);
    }
}

void FFmpegController::setSpeed(float speed) {
    if (audioInfo) {
        audioInfo->setSpeed(speed);
    }
}

int FFmpegController::getSampleRate() {
    if (audioInfo) {
        return audioInfo->sample_rate;
    }
    return 0;
}

void FFmpegController::controlRecord(bool start) {
    if (audioInfo) {
        audioInfo->controlRecord(start);
    }
}

int FFmpegController::getCodecContext(AVCodecParameters *params, AVCodecContext **ctx) {
    //5.获取解码器
    AVCodec *dec = avcodec_find_decoder(params->codec_id);
    if (!dec) {
        LogUtil::e("can not find decoder");
        callJava->onCallError(NATIVE_THREAD, 1003, "can not find decoder");
        decode_exit = true;
        pthread_mutex_unlock(&init_mutex);
        return -1;
    }

    //6.利用解码器创建解码器上下文
    *ctx = avcodec_alloc_context3(dec);
    if (!audioInfo->avCodecContext) {
        LogUtil::e("can not alloc new decodecctx");
        callJava->onCallError(NATIVE_THREAD, 1004, "can not alloc new decodecctx");
        decode_exit = true;
        pthread_mutex_unlock(&init_mutex);
        return -1;
    }

    if (avcodec_parameters_to_context(*ctx, params) < 0) {
        LogUtil::e("can not fill decodecctx");
        callJava->onCallError(NATIVE_THREAD, 1005, "ccan not fill decodecctx");
        decode_exit = true;
        pthread_mutex_unlock(&init_mutex);
        return -1;
    }
    //7.打开解码器
    if (avcodec_open2(*ctx, dec, 0) != 0) {
        LogUtil::e("cant not open audio strames");
        callJava->onCallError(NATIVE_THREAD, 1006, "cant not open audio strames");
        decode_exit = true;
        pthread_mutex_unlock(&init_mutex);
        return -1;
    }
    return 0;
}

