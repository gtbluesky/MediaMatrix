package com.gtbluesky.mediamatrix.player.listener

import com.gtbluesky.mediamatrix.player.bean.TimeInfoBean

interface OnTimeInfoListener {
    fun onTimeInfo(bean: TimeInfoBean)
}