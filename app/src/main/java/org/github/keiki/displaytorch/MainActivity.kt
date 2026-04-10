package org.github.keiki.displaytorch

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


private const val KEY_BUNDLE = "brightnessIndex"

private const val DEFAULT_INDEX = 0

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
    private var currentBrightnessIndex = DEFAULT_INDEX
    private var currentBackGroundColorWhite = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rootView: View = findViewById(android.R.id.content)

        rootView.setOnClickListener {
            toggleBrightness()
        }
        rootView.setOnLongClickListener {
            toggleColor()
            true
        }
        currentBrightnessIndex = savedInstanceState?.getInt(KEY_BUNDLE) ?: DEFAULT_INDEX
        setBrightnessIndex(currentBrightnessIndex)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_BUNDLE, currentBrightnessIndex)
    }
    private fun toggleBrightness() {
        currentBrightnessIndex = (currentBrightnessIndex + 1) % brightnessLevels.size
        setBrightnessIndex(currentBrightnessIndex)
    }

    private fun setBrightnessIndex(index: Int) {
        setScreenBrightness(brightnessLevels[index])
        updateBrightnessText()
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

    @OptIn(ExperimentalStdlibApi::class)
    private fun updateBrightnessText() {
        val brightnessTextView: TextView = findViewById(R.id.brightnessTextView)

        var debugText = ""

        if (DEBUG) {
            val brightnessPercentage = (brightnessLevels[currentBrightnessIndex].brightness * 100).toInt()

            val backgroundColor = getRootView().getBackgroundColor()
            val colorHex = Color.valueOf(backgroundColor).toArgb().toHexString(HexFormat.Default).takeLast(6)
            debugText =     " Brightness: $brightnessPercentage% ; Color : #$colorHex"
        }

        brightnessTextView.text = "Step ${currentBrightnessIndex + 1}" + debugText
    }

    fun View.getBackgroundColor() = (background as? ColorDrawable?)?.color ?: Color.TRANSPARENT

    private fun toggleColor() {
        currentBackGroundColorWhite = !currentBackGroundColorWhite
        setScreenBrightness(brightnessLevels[currentBrightnessIndex])
        updateBrightnessText()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                currentBrightnessIndex = (currentBrightnessIndex + 1) % brightnessLevels.size
                setBrightnessIndex(currentBrightnessIndex)
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                currentBrightnessIndex = (currentBrightnessIndex - 1 + brightnessLevels.size) % brightnessLevels.size
                setBrightnessIndex(currentBrightnessIndex)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun getRootView(): View = findViewById(R.id.mainActivityLayout)
}