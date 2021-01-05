package com.gtbluesky.camera.listener

interface OnCompletionListener {
    /**
     * 注意：回调在子线程
     */
    fun onCompletion(savePath: String)
}