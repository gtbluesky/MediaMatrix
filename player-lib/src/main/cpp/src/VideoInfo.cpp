//
// Created by gtbluesky on 18-10-6.
//

#include "VideoInfo.h"

VideoInfo::VideoInfo(PlayStatus *status, NativeCallJava *nativeCallJava) {
    this->status = status;
    this->callJava = nativeCallJava;
    queue = new PacketQueue(status);
}

VideoInfo::~VideoInfo() {
    release();
}

void *playVideo(void *data) {
    VideoInfo *video = (VideoInfo *) data;
    while (video->status && !video->status->is_exited) {
        if (video->status->is_seeking) {
            av_usleep(100 * 1000);
            continue;
        }
        if (!video->queue->getQueueSize()) {
            if (!video->status->is_loading) {
                video->status->is_loading = true;
                video->callJava->onCallLoad(NATIVE_THREAD, true);
            }
            av_usleep(100 * 1000);
            continue;
        } else {
            if (video->status->is_loading) {
                video->status->is_loading = false;
                video->callJava->onCallLoad(NATIVE_THREAD, false);
            }
        }
        AVPacket *avPacket = av_packet_alloc();
        if (video->queue->getAVPacket(avPacket)) {
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            continue;
        }
        if (avcodec_send_packet(video->avCodecContext, avPacket)) {
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            continue;
        }
        AVFrame *avFrame = av_frame_alloc();
        if (avcodec_receive_frame(video->avCodecContext, avFrame)) {
            av_frame_free(&avFrame);
            av_free(avFrame);
            avFrame = NULL;
            av_packet_free(&avPacket);
            av_free(avPacket);
            avPacket = NULL;
            continue;
        }
        LogUtil::e("解码一个Video AvFrame成功");
        if (avFrame->format == AV_PIX_FMT_YUV420P) {
            LogUtil::e("当前视频是YUV420P格式");
            double diff = video->getFrameDiffTime(avFrame);
            LogUtil::d("diff is %f", diff);

            av_usleep(video->getDelayTime(diff) * 1000);
            video->callJava->onCallRenderYUV(
                    video->avCodecContext->width,
                    video->avCodecContext->height,
                    avFrame->data[0],
                    avFrame->data[1],
                    avFrame->data[2]
            );
        } else {
            LogUtil::e("当前视频不是YUV420P格式");
            AVFrame *pFrameYUV420P = av_frame_alloc();
            int num = av_image_get_buffer_size(
                    AV_PIX_FMT_YUV420P,
                    video->avCodecContext->width,
                    video->avCodecContext->height,
                    1
            );
            uint8_t *buffer = (uint8_t *) av_malloc(num * sizeof(uint8_t));
            av_image_fill_arrays(
                    pFrameYUV420P->data,
                    pFrameYUV420P->linesize,
                    buffer,
                    AV_PIX_FMT_YUV420P,
                    video->avCodecContext->width,
                    video->avCodecContext->height,
                    1
            );
            SwsContext *sws_ctx = sws_getContext(
                    video->avCodecContext->width,
                    video->avCodecContext->height,
                    video->avCodecContext->pix_fmt,
                    video->avCodecContext->width,
                    video->avCodecContext->height,
                    AV_PIX_FMT_YUV420P,
                    SWS_BICUBIC,
                    NULL,
                    NULL,
                    NULL
            );
            if (!sws_ctx) {
                av_frame_free(&pFrameYUV420P);
                av_free(pFrameYUV420P);
                av_free(buffer);
                continue;
            }
            sws_scale(
                    sws_ctx,
                    (const uint8_t *const *) avFrame->data,
                    avFrame->linesize,
                    0,
                    avFrame->height,
                    pFrameYUV420P->data,
                    pFrameYUV420P->linesize
            );
            video->callJava->onCallRenderYUV(
                    video->avCodecContext->width,
                    video->avCodecContext->height,
                    pFrameYUV420P->data[0],
                    pFrameYUV420P->data[1],
                    pFrameYUV420P->data[2]
            );
            av_frame_free(&pFrameYUV420P);
            av_free(pFrameYUV420P);
            av_free(buffer);
            sws_freeContext(sws_ctx);
        }
        av_frame_free(&avFrame);
        av_free(avFrame);
        avFrame = NULL;
        av_packet_free(&avPacket);
        av_free(avPacket);
        avPacket = NULL;
    }

    pthread_exit(&video->thread_play);
}

void VideoInfo::play() {
    pthread_create(&thread_play, NULL, playVideo, this);
}

void VideoInfo::release() {
    if (queue) {
        delete queue;
        queue = NULL;
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

double VideoInfo::getFrameDiffTime(AVFrame *avFrame) {

    double pts = avFrame->pts;
    if(pts == AV_NOPTS_VALUE)
    {
        pts = 0;
    }
    pts *= av_q2d(time_base);
    pts *= 1000;

    if(pts > 0)
    {
        clock = pts;
    }

    return (audioInfo->clock - clock);
}

double VideoInfo::getDelayTime(double diff) {

    if(diff > 3)
    {
        delayTime = delayTime * 2 / 3;
        if(delayTime < defaultDelayTime / 2)
        {
            delayTime = defaultDelayTime * 2 / 3;
        }
        else if(delayTime > defaultDelayTime * 2)
        {
            delayTime = defaultDelayTime * 2;
        }
        LogUtil::e(">3");
    }
    else if(diff < -3)
    {
        delayTime = delayTime * 3 / 2;
        if(delayTime < defaultDelayTime / 2)
        {
            delayTime = defaultDelayTime * 2 / 3;
        }
        else if(delayTime > defaultDelayTime * 2)
        {
            delayTime = defaultDelayTime * 2;
        }
        LogUtil::e("<3");
    }
    else if(diff == 3)
    {

    }
    if(diff >= 500)
    {
        delayTime = 0;
    }
    else if(diff <= -500)
    {
        delayTime = defaultDelayTime * 2;
    }

    if(fabs(diff) >= 10000)
    {
        delayTime = defaultDelayTime;
    }
    return delayTime;
}
