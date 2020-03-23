package com.gtbluesky.mediamatrix.view

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import com.gtbluesky.mediamatrix.R

class FocusImageView : AppCompatImageView {

    private var focusingResId = 0
    private var successResId = 0
    private var failResId = 0
    private val focusAnimation: Animation by lazy {
        AnimationUtils.loadAnimation(context, R.anim.focusview_show)
    }

    constructor(context: Context): super(context) {
        focusingResId = R.drawable.focus_focusing
        successResId = R.drawable.focus_success
        failResId = R.drawable.focus_focus_failed
    }

    constructor(context: Context, attrs: AttributeSet): super(context, attrs) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.FocusImageView)
        focusingResId = a.getResourceId(R.styleable.FocusImageView_focusingDrawable, R.drawable.focus_focusing)
        successResId = a.getResourceId(R.styleable.FocusImageView_focusingDrawable, R.drawable.focus_success)
        failResId = a.getResourceId(R.styleable.FocusImageView_focusingDrawable, R.drawable.focus_focus_failed)
        a.recycle()
    }

    fun startFocus(point: Point) {
        val params = layoutParams as RelativeLayout.LayoutParams
        var topMargin = point.y - height / 2
        var leftMargin = point.x - width / 2
        val parentHeight = (parent as ViewGroup).height
        val parentWidth = (parent as ViewGroup).width
        if (parentHeight - topMargin < height) {
            topMargin = parentHeight - height
        }
        if (parentWidth - leftMargin < width) {
            leftMargin = parentWidth - width
        }
        params.topMargin = Math.max(0, topMargin)
        params.leftMargin = Math.max(0, leftMargin)
        layoutParams = params
        visibility = View.VISIBLE
        setImageResource(focusingResId)
        startAnimation(focusAnimation)
        postDelayed({ visibility = View.GONE }, 3500)
    }

    fun focusSuccess() {
        setImageResource(successResId)
        postDelayed({ visibility = View.GONE }, 1000)
    }

    fun focusFailed() {
        setImageResource(failResId)
        postDelayed({ visibility = View.GONE }, 1000)
    }
}