package com.marlerino.rainbowgoldstation.view

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.DisplayMetrics
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.RotateAnimation
import android.view.animation.TranslateAnimation
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.Chronometer
import android.widget.Chronometer.OnChronometerTickListener
import android.widget.ImageView
import android.widget.TextView.OnEditorActionListener
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import androidx.recyclerview.widget.RecyclerView.SmoothScroller
import com.marlerino.rainbowgoldstation.viewmodel.DataManager
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.marlerino.rainbowgoldstation.R
import com.marlerino.rainbowgoldstation.viewmodel.DeskColumnAdapter
import java.util.concurrent.TimeUnit
import kotlin.random.Random


class GameActivity : AppCompatActivity() {
    private var gameEnded = false
    private var code = ""
    private lateinit var deck: ConstraintLayout
    private val targetPositions = mutableListOf(0, 0, 0)
    private val readyColumns = mutableListOf(false, false, false)
    private val stopedColumns = mutableListOf(false, false, false)
    private var toStop = false
    private val icons = listOf(
        R.drawable.well,
        R.drawable.clover,
        R.drawable.horseshoe,
        R.drawable.hat,
        R.drawable.wild,
        R.drawable.well,
        R.drawable.clover,
    )
    private lateinit var btnHint: Button
    private lateinit var bottomMenu: ConstraintLayout
    private lateinit var chronometer: Chronometer
    private lateinit var btnStop: Button
    private lateinit var qrMenu: ConstraintLayout
    private lateinit var qrImage: ImageView
    private lateinit var textInput: TextInputLayout
    private lateinit var textInputEditText: TextInputEditText
    private val stopCheckHandler = Handler()
    private val checkToStop = object : Runnable {
        override fun run() {
            if (!stopedColumns.contains(false)) {
                showQR()
                stopCheckHandler.removeCallbacks(this)
            } else {
                stopCheckHandler.postDelayed(this, 500)
            }
        }
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        window.decorView.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility == 0) {
                hideSystemUI()
            }
        }
        hideSystemUI()

        deck = findViewById(R.id.deck)
        bottomMenu = findViewById(R.id.bottom_menu)

        val recyclingViews = listOf<RecyclerView>(
            findViewById(R.id.left_recycling_view),
            findViewById(R.id.center_recycling_view),
            findViewById(R.id.right_recycling_view)
        )
        initRecyclingViews(
            recyclingViews
        )

        chronometer = findViewById(R.id.chronometer)
        initChronometer(0)
        chronometer.start()
        btnStop = findViewById(R.id.button_stop)
        qrMenu = findViewById(R.id.qr_menu)
        qrMenu.setOnClickListener {
            hideKeyboard()
            textInputEditText.clearFocus()
        }
        qrImage = findViewById(R.id.qr_image)
        textInput = findViewById(R.id.text_input_layout)
        btnStop.setOnClickListener {
            onClickStop()
            btnStop.setOnClickListener {}
        }

        btnHint = findViewById(R.id.button_hint)
        val scaleXAnimator: ObjectAnimator = ObjectAnimator.ofFloat(btnHint, "scaleX", 1f, 7f)
        val scaleYAnimator: ObjectAnimator = ObjectAnimator.ofFloat(btnHint, "scaleY", 1f, 7f)
        val alphaAnimator = ValueAnimator.ofFloat(0f, 1f)
        scaleXAnimator.duration = 500
        scaleYAnimator.duration = 500
        alphaAnimator.duration = 250
        alphaAnimator.startDelay = 250
        alphaAnimator.addUpdateListener { animation ->
            btnHint.text = this.getString(R.string.press_stop)
            btnHint.alpha = animation.animatedValue as Float
        }
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator)
        btnHint.setOnClickListener {
            animatorSet.start()
            btnHint.setOnClickListener {}
        }
        textInputEditText = findViewById(R.id.text_input_edit_text)
        textInputEditText.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val text: String = textInputEditText.text.toString()
                if (text == code){
                    DataManager(this).clearSession()
                    gameEnded = true
                    startActivity(Intent(this, StartActivity::class.java))
                }else{
                    textInputEditText.setText("")
                    hideKeyboard()
                    textInputEditText.clearFocus()
                }
                return@OnEditorActionListener true
            }
            false
        })
        textInputEditText.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                textInput.hint = null
                ObjectAnimator.ofFloat(qrMenu, "translationY", -(qrMenu.height.toFloat())).apply {
                    duration = 500
                    start()
                }
            }else{
                textInput.hint = getText(R.string.code)
                ObjectAnimator.ofFloat(qrMenu, "translationY", -((qrMenu.height.toFloat() / 790 * 457))).apply {
                    duration = 500
                    start()
                }
            }
        }
    }
    private fun initChronometer(startTime: Long) {
        chronometer.onChronometerTickListener =
            OnChronometerTickListener { cArg ->
                val time = SystemClock.elapsedRealtime() - cArg.base
                val h = (time / 3600000).toInt()
                val m = (time - h * 3600000).toInt() / 60000
                val s = (time - h * 3600000 - m * 60000).toInt() / 1000
                val hh = if (h < 10) "0$h" else h.toString() + ""
                val mm = if (m < 10) "0$m" else m.toString() + ""
                val ss = if (s < 10) "0$s" else s.toString() + ""
                cArg.text = "$hh:$mm:$ss"
            }
        chronometer.base = SystemClock.elapsedRealtime() - startTime
    }

    override fun onStop() {
        super.onStop()
        if(!gameEnded){
            DataManager(this).saveSession(chronometer.text.toString(), code, targetPositions)
        }
    }

    override fun onResume() {
        super.onResume()
        val session = DataManager(this).loadSession()
        if (session.time != "") {
            val parts: List<String> =
                session.time.split(":")
            var totalTime = 0L
            for(i in parts.indices){
                var multiplyer = 1
                for (j in 1 until parts.size - i){
                    multiplyer *= 60
                }
                totalTime += parts[i].toLong() * multiplyer
            }
            chronometer.stop()
            initChronometer(TimeUnit.SECONDS.toMillis(totalTime))
        }
        else{
            initChronometer(0)
        }
        if (session.positions.isEmpty() ||session.positions.contains(0)) {
            chronometer.start()
        }
        if (session.positions.isNotEmpty() && !session.positions.contains(0) && !stopedColumns.contains(true)) {
            btnStop.setOnClickListener {}
            if (session.code != "") {
                code = session.code
            }
            for (i in 0 until session.positions.size) {
                targetPositions[i] = session.positions[i]
            }
            stopDeck()
        }
    }

    override fun onPause() {
        super.onPause()
        if(!gameEnded){
            DataManager(this).saveSession(chronometer.text.toString(), code, targetPositions)
        }
    }

    private fun showQR() {
        val computerCode = DataManager(this).getCurrentComputer()
        val time = chronometer.text.toString()
        if (code == "") {
            val symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890"
            val codeBuilder = StringBuilder()
            for (i in 0..6) {
                codeBuilder.append(symbols[Random.nextInt(symbols.length)])
            }
            code = codeBuilder.toString()
        }
        val json = "{\"computer\":\"$computerCode\",\"timer\":\"$time\",\"code\":\"$code\"}"
        val bitmap = generateQRCode(json)
        qrImage.setImageBitmap(bitmap)

        val alphaAnimationHintBtn = AlphaAnimation(1f, 0f)
        alphaAnimationHintBtn.duration = 1000
        alphaAnimationHintBtn.fillAfter = true


        ObjectAnimator.ofFloat(bottomMenu, "translationY", (bottomMenu.height.toFloat())).apply {
            duration = 500
            start()
        }
        ObjectAnimator.ofFloat(
            chronometer,
            "translationY",
            (chronometer.height.toFloat() + bottomMenu.height.toFloat())
        ).apply {
            duration = 500
            start()
        }
        ObjectAnimator.ofFloat(qrMenu, "translationY", -(qrMenu.height.toFloat() / 790 * 457))
            .apply {
                duration = 1000
                startDelay = 500
                start()
            }
        val animationSetDeck = AnimationSet(true)
        animationSetDeck.interpolator = AccelerateInterpolator()
        val rotateAnimationDeck = RotateAnimation(
            0f,
            20f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        rotateAnimationDeck.duration = 500
        rotateAnimationDeck.startOffset = 1000
        rotateAnimationDeck.fillAfter = true
        val translateAnimationDeck = TranslateAnimation(0f, 0f, 0f, -(deck.height.toFloat()))
        translateAnimationDeck.duration = 500
        translateAnimationDeck.startOffset = 1000
        translateAnimationDeck.fillAfter = true
        animationSetDeck.addAnimation(rotateAnimationDeck)
        animationSetDeck.addAnimation(translateAnimationDeck)
        animationSetDeck.fillAfter = true

        btnHint.startAnimation(alphaAnimationHintBtn)
        deck.startAnimation(animationSetDeck)
    }

    private fun onClickStop() {
        for (i in 0..2) {
            targetPositions[i] = Random.nextInt(1, 6)
        }
        stopDeck()
    }

    private fun stopDeck() {
        toStop = true
        chronometer.stop()
        stopCheckHandler.postDelayed(checkToStop, 500)
    }

    private fun initRecyclingViews(recyclerViews: List<RecyclerView>) {
        val onScrollListeners = mutableListOf<OnScrollListener>()
        val smoothScrollers = mutableListOf<SmoothScroller>()
        val startPositions = mutableListOf<Int>(0, 0, 0)
        for (i: Int in 0..2) {
            var num = 0
            while (startPositions.contains(num)) {
                num = Random.nextInt(1, 5)
            }
            startPositions[i] = num
            smoothScrollers.add(
                object : LinearSmoothScroller(
                    applicationContext
                ) {
                    override fun getVerticalSnapPreference(): Int {
                        return SNAP_TO_START
                    }

                    override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                        return 300f * 3 / displayMetrics.densityDpi
                    }
                }
            )
            onScrollListeners.add(
                object : OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                        if (layoutManager.findLastVisibleItemPosition() == 1) {
                            recyclerView.scrollToPosition(layoutManager.itemCount - 1)
                            if (toStop) {
                                readyColumns[i] = true
                                smoothScrollers[i].targetPosition = targetPositions[i]
                                recyclerView.layoutManager!!.startSmoothScroll(smoothScrollers[i])
                            }
                        }
                        if (readyColumns[i] && layoutManager.findFirstCompletelyVisibleItemPosition() != targetPositions[i] && smoothScrollers[i].targetPosition != targetPositions[i]) {
                            smoothScrollers[i].targetPosition = targetPositions[i]
                            recyclerView.layoutManager!!.startSmoothScroll(smoothScrollers[i])
                        }
                        if (readyColumns[i] && layoutManager.findFirstCompletelyVisibleItemPosition() == targetPositions[i] && (!stopedColumns[i] || layoutManager.findFirstVisibleItemPosition() == targetPositions[i])) {
                            val recyclerViewHeight = recyclerView.height
                            val firstVisibleView =
                                layoutManager.findViewByPosition(layoutManager.findFirstCompletelyVisibleItemPosition())
                            val firstVisibleViewHeight = firstVisibleView?.height ?: 0
                            val offset = (recyclerViewHeight - firstVisibleViewHeight) / 2
                            layoutManager.scrollToPositionWithOffset(targetPositions[i], offset)
                            stopedColumns[i] = true
                        }
                        if (!toStop && smoothScrollers[i].targetPosition != 0) {
                            smoothScrollers[i].targetPosition = 0
                            recyclerView.layoutManager!!.startSmoothScroll(smoothScrollers[i])
                        }
                        if (layoutManager.findLastCompletelyVisibleItemPosition() == layoutManager.itemCount - 1) {
                            smoothScrollers[i].targetPosition = layoutManager.itemCount - 2
                            recyclerView.layoutManager!!.startSmoothScroll(smoothScrollers[i])
                        }
                    }
                }
            )
            recyclerViews[i].adapter = DeskColumnAdapter(icons)
            recyclerViews[i].layoutManager = LinearLayoutManager(this)
            recyclerViews[i].addOnScrollListener(onScrollListeners[i])
            recyclerViews[i].scrollToPosition(
                startPositions[i]
            )
            smoothScrollers[i].targetPosition = 0
            recyclerViews[i].layoutManager!!.startSmoothScroll(smoothScrollers[i])
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

    private fun generateQRCode(json: String): Bitmap? {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(json, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x,
                    y,
                    if (bitMatrix.get(x, y)) getColor(R.color.main) else getColor(R.color.second)
                )
            }
        }
        return bitmap
    }

}