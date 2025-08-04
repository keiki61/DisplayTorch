package org.github.keiki.displaytorch

import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val brightnessLevels = listOf(0.03f,0.1f,0.3f, 0.5f, 1.0f)
    private var currentBrightnessIndex = 0
    private var currentBackGroundColorWhite = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rootView:View = findViewById(android.R.id.content)

        rootView.setOnClickListener {
            toggleBrightness()
        }

        // Update the initial text
        updateBrightnessText()
    }

    private fun toggleBrightness() {
        currentBrightnessIndex = (currentBrightnessIndex + 1) % brightnessLevels.size
        setBrightnessIndex(currentBrightnessIndex)
    }

    private fun setBrightnessIndex(index: Int) {
        setScreenBrightness(brightnessLevels[index])
        updateBrightnessText()
    }

    override fun onResume() {
        super.onResume()
        setBrightnessIndex(0)
    }

    override fun onPause() {
        super.onPause()
        resetScreenBrightness()
    }

    private fun setScreenBrightness(brightness: Float) {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightness
        window.attributes = layoutParams
    }

    private fun resetScreenBrightness() {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = layoutParams
    }

    private fun updateBrightnessText() {
        val brightnessTextView: TextView = findViewById(R.id.brightnessTextView)
        val brightnessPercentage = (brightnessLevels[currentBrightnessIndex] * 100).toInt()
        brightnessTextView.text = "Brightness: $brightnessPercentage%"
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {

                val rootView:View = findViewById(R.id.mainActivityLayout)

                if (currentBackGroundColorWhite) {
                    rootView.setBackgroundColor(Color.rgb(139.0f, 0.0f, 0.0f))
                    currentBackGroundColorWhite = false
                } else {
                    rootView.setBackgroundColor(ContextCompat.getColor(this,R.color.greyLight))
                    currentBackGroundColorWhite = true
                }
                true // Indicate event is handled
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}