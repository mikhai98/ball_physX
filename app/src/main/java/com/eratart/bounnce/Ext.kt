package com.eratart.bounnce

import android.animation.ValueAnimator
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View

fun Context?.vibrateTick(duration: Long = 10L) {
    this?.apply {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        v?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

fun Context?.vibrateReject(duration: Long = 75L) {
    this?.apply {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        val delay = 100L
        val pattern = longArrayOf(0, duration, delay, duration)
        v?.vibrate(pattern, -1)
    }
}

fun Context?.vibrateSuccess(duration: Long = 75L) {
    this?.apply {
        val v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        v?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}

fun ValueAnimator.startAnimation(view: View, listener: (value: Float) -> Unit) {
    removeAllUpdateListeners()
    addUpdateListener {
        val value = it.animatedValue
        if (value is Float) {
            listener.invoke(value)
        }
        view.postInvalidateOnAnimation()
    }
    start()
}
