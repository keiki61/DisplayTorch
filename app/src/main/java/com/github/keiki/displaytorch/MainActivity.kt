package com.github.keiki.displaytorch

import android.graphics.Color
import androidx.core.content.edit
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.google.android.material.dialog.MaterialAlertDialogBuilder


private const val KEY_BRIGHTNESS_INDEX = "brightnessIndex"
private const val KEY_COLOR_WHITE = "colorWhite"
private const val PREF_NAME = "brightness_prefs"
private const val DEFAULT_INDEX = 0
private const val EDIT_BRIGHTNESS_STEP = 0.01f

class MainActivity : AppCompatActivity() {

    class BrightnessStep(
        var brightness: Float,
        val shadeWhite: Int,
        val shadeRed: Int = R.color.red
    )

    private val defaultBrightnessValues = listOf(0.02f, 0.1f, 0.3f, 0.5f, 1.0f)

    private val brightnessLevels = mutableListOf(
        BrightnessStep(defaultBrightnessValues[0], R.color.grey),
        BrightnessStep(defaultBrightnessValues[1], R.color.greyWhite),
        BrightnessStep(defaultBrightnessValues[2], R.color.greyWhite),
        BrightnessStep(defaultBrightnessValues[3], R.color.white),
        BrightnessStep(defaultBrightnessValues[4], R.color.white)
    )
    private var currentBrightnessIndex = DEFAULT_INDEX
    private var currentBackGroundColorWhite = true
    private var isEditMode = false
    private var twoFingerTouching = false

    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadBrightnessPrefs()

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (isEditMode) toggleEditMode() else toggleBrightness()
                return true
            }
            override fun onLongPress(e: MotionEvent) {
                toggleEditMode()
            }
        })

        val rootView: View = findViewById(android.R.id.content)
        rootView.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> if (event.pointerCount == 2) {
                    twoFingerTouching = true
                    // Cancel any pending single-tap in the gesture detector
                    val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                    gestureDetector.onTouchEvent(cancel)
                    cancel.recycle()
                }
                MotionEvent.ACTION_UP -> if (twoFingerTouching) {
                    twoFingerTouching = false
                    toggleColor()
                    return@setOnTouchListener true
                } else {
                    v.performClick()
                }
                MotionEvent.ACTION_CANCEL -> twoFingerTouching = false
            }
            if (!twoFingerTouching) gestureDetector.onTouchEvent(event)
            true
        }

        val resetMenuButton = findViewById<View>(R.id.resetMenuButton)
        resetMenuButton.setOnClickListener { showResetMenu(it) }
        ViewCompat.setOnApplyWindowInsetsListener(resetMenuButton) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val baseMargin = resources.getDimensionPixelSize(R.dimen.edit_menu_button_margin)
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top + baseMargin
            }
            insets
        }

        currentBrightnessIndex = savedInstanceState?.getInt(KEY_BRIGHTNESS_INDEX) ?: DEFAULT_INDEX
        currentBackGroundColorWhite = savedInstanceState?.getBoolean(KEY_COLOR_WHITE) ?: true
        setBrightnessIndex(currentBrightnessIndex)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_BRIGHTNESS_INDEX, currentBrightnessIndex)
        outState.putBoolean(KEY_COLOR_WHITE, currentBackGroundColorWhite)
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

    private fun toggleColor() {
        currentBackGroundColorWhite = !currentBackGroundColorWhite
        setScreenBrightness(brightnessLevels[currentBrightnessIndex])
        updateBrightnessText()
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        getRootView().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        findViewById<View>(R.id.resetMenuButton).visibility = if (isEditMode) View.VISIBLE else View.GONE
        updateBrightnessText()
    }

    private fun showResetMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.edit_mode_menu, menu)
            setOnMenuItemClickListener { item ->
                if (item.itemId == R.id.action_reset) {
                    showResetConfirmationDialog()
                    true
                } else {
                    false
                }
            }
            show()
        }
    }

    private fun showResetConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reset_confirm_title)
            .setMessage(R.string.reset_confirm_message)
            .setPositiveButton(R.string.action_reset) { _, _ -> resetBrightnessPrefs() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun adjustCurrentStepBrightness(delta: Float) {
        val step = brightnessLevels[currentBrightnessIndex]
        step.brightness = (step.brightness + delta).coerceIn(0.01f, 1.0f)
        saveBrightnessPrefs()
        setBrightnessIndex(currentBrightnessIndex)
    }

    private fun loadBrightnessPrefs() {
        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        brightnessLevels.forEachIndexed { i, step ->
            step.brightness = prefs.getFloat("brightness_$i", step.brightness)
        }
    }

    private fun saveBrightnessPrefs() {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit {
            brightnessLevels.forEachIndexed { i, step -> putFloat("brightness_$i", step.brightness) }
        }
    }

    private fun resetBrightnessPrefs() {
        brightnessLevels.forEachIndexed { i, step -> step.brightness = defaultBrightnessValues[i] }
        saveBrightnessPrefs()
        getRootView().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        setBrightnessIndex(currentBrightnessIndex)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun updateBrightnessText() {
        val brightnessTextView: TextView = findViewById(R.id.brightnessTextView)

        var debugText = ""
        if (BuildConfig.DEBUG) {
            val brightnessPercentage = (brightnessLevels[currentBrightnessIndex].brightness * 100).toInt()
            val backgroundColor = getRootView().getBackgroundColor()
            val colorHex = Color.valueOf(backgroundColor).toArgb().toHexString(HexFormat.Default).takeLast(6)
            debugText = " Brightness: $brightnessPercentage% ; Color : #$colorHex"
        }

        val editText = if (isEditMode) " [EDIT]" else ""
        brightnessTextView.text = getString(R.string.step_label, currentBrightnessIndex + 1, editText, debugText)
    }

    fun View.getBackgroundColor() = (background as? ColorDrawable?)?.color ?: Color.TRANSPARENT

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (isEditMode) adjustCurrentStepBrightness(EDIT_BRIGHTNESS_STEP)
                else { currentBrightnessIndex = (currentBrightnessIndex + 1) % brightnessLevels.size; setBrightnessIndex(currentBrightnessIndex) }
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (isEditMode) adjustCurrentStepBrightness(-EDIT_BRIGHTNESS_STEP)
                else { currentBrightnessIndex = (currentBrightnessIndex - 1 + brightnessLevels.size) % brightnessLevels.size; setBrightnessIndex(currentBrightnessIndex) }
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun getRootView(): View = findViewById(R.id.mainActivityLayout)
}
