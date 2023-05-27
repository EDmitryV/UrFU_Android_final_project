package com.marlerino.rainbowgoldstation.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.marlerino.rainbowgoldstation.viewmodel.DataManager
import com.marlerino.rainbowgoldstation.R
import com.marlerino.rainbowgoldstation.view.JoinActivity


class StartActivity : AppCompatActivity() {
    private lateinit var btnStart:Button
    private lateinit var btnBack:Button
    private lateinit var computerCode:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)
        val dataManager = DataManager(this)
        computerCode = findViewById(R.id.textCode)
        computerCode.text = dataManager.getCurrentComputer()
        hideSystemUI()
        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility == 0) {
                hideSystemUI()
            }
        }
        btnBack = findViewById(R.id.backButton)
        btnStart = findViewById(R.id.button_start)
        val scaleXAnimator:ObjectAnimator = ObjectAnimator.ofFloat(btnBack, "scaleX", 1f, 7f)
        val scaleYAnimator:ObjectAnimator = ObjectAnimator.ofFloat(btnBack, "scaleY", 1f, 7f)
        val alphaAnimator = ValueAnimator.ofFloat(0f, 1f)
        scaleXAnimator.duration = 500
        scaleYAnimator.duration = 500
        alphaAnimator.duration = 250
        alphaAnimator.startDelay = 250
        alphaAnimator.addUpdateListener { animation ->
            btnBack.text = this.getString(R.string.back)
            btnBack.alpha = animation.animatedValue as Float
        }
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator)
        btnBack.setOnClickListener {
            animatorSet.start()
            btnBack.setOnClickListener {
                startActivity(Intent(this, JoinActivity::class.java))
            }
        }
        btnStart.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }
}