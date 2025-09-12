package org.github.keiki.displaytorch

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object{
        const val DEBUG: Boolean = true
    }

    class BrightnessStep(
        val brightness: Float,
        val shadeWhite: Int,
        val shadeRed: Int = R.color.red
    )

    private val brightnessLevels = listOf(
        BrightnessStep(0.02f, R.color.grey),
        BrightnessStep(0.1f, R.color.greyWhite),
        BrightnessStep(0.3f, R.color.greyWhite),
        BrightnessStep(0.5f, R.color.white),
        BrightnessStep(1.0f, R.color.white)
    )
    private var currentBrightnessIndex = 0
    private var currentBackGroundColorWhite = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rootView: View = findViewById(android.R.id.content)

        rootView.setOnClickListener {
            toggleBrightness()
        }

        // Update the initial text
        updateBrightnessText()

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
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

    private fun setScreenBrightness(brightnessStep: BrightnessStep) {

        val layoutParams = window.attributes
        layoutParams.screenBrightness = brightnessStep.brightness
        val rootView = getRootView()
        if (currentBackGroundColorWhite) {
            rootView.setBackgroundColor(getColor(brightnessStep.shadeWhite))
        } else {
            rootView.setBackgroundColor(getColor(brightnessStep.shadeRed))
        }
        window.attributes = layoutParams
    }

    private fun resetScreenBrightness() {
        val layoutParams = window.attributes
        layoutParams.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        window.attributes = layoutParams
    }

    private fun updateBrightnessText() {
        val brightnessTextView: TextView = findViewById(R.id.brightnessTextView)
        val brightnessPercentage = (brightnessLevels[currentBrightnessIndex].brightness * 100).toInt()

        var debugText = ""

        if (DEBUG) {
            debugText =     " Brightness: $brightnessPercentage%"
        }

        brightnessTextView.text = "Step ${currentBrightnessIndex + 1}" + debugText
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {

                val rootView: View = getRootView()

                if (currentBackGroundColorWhite) {
                    rootView.setBackgroundColor(getColor(R.color.red))
                    currentBackGroundColorWhite = false
                } else {
                    rootView.setBackgroundColor(getColor(brightnessLevels[currentBrightnessIndex].shadeWhite))
                    currentBackGroundColorWhite = true
                }
                true // Indicate event is handled
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun getRootView(): View = findViewById(R.id.mainActivityLayout)
}