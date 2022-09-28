package com.eratart.bounnce

import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import com.eratart.bounnce.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val activityRootView by lazy { binding.root }
    private val playView by lazy { binding.playView }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activityRootView)
        addOnLayoutReadyListener()
    }

    private fun addOnLayoutReadyListener() {
        val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                onLayoutReady()
                activityRootView.viewTreeObserver?.removeOnGlobalLayoutListener(this)
            }
        }
        activityRootView.viewTreeObserver?.addOnGlobalLayoutListener(listener)
    }

    private fun onLayoutReady() {
        playView.setup()
    }
}