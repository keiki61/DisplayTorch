package com.github.keiki.displaytorch

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.github.keiki.displaytorch.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

private const val PREF_NAME = "brightness_prefs"
private const val KEY_SEEN_ONBOARDING = "seenOnboarding"
private const val KEY_STEP_INDEX = "brightnessIndex"
private const val KEY_COLOR_WHITE = "colorWhite"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var state: TorchState
    private lateinit var gestureDetector: GestureDetector
    private lateinit var billingManager: BillingManager

    /** Null until ads are started; stays null when the remove-ads entitlement is cached. */
    private var ads: AdsController? = null
    private var isEditMode = false
    private var twoFingerTouching = false

    /**
     * Number of dialog-like overlays currently open (tutorial, menu, dialogs,
     * consent forms). While it is above zero the window brightness override is
     * released, because at step 1 (2%) none of them would be readable.
     */
    private var overlayDepth = 0

    /** Background per step in white mode, dim grey up to pure white. Red mode always uses [R.color.red]. */
    private val whiteShades = listOf(R.color.grey, R.color.greyWhite, R.color.greyWhite, R.color.white, R.color.white)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        showOverLockScreen()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
        state = TorchState(PreferencesBrightnessStore(prefs))
        savedInstanceState?.let {
            val color = if (it.getBoolean(KEY_COLOR_WHITE, true)) LightColor.WHITE else LightColor.RED
            state.restore(it.getInt(KEY_STEP_INDEX, 0), color)
        }

        setupGestures()
        setupInsets()
        binding.editMenuButton.setOnClickListener { showEditMenu(it) }
        binding.onboardingDismissButton.setOnClickListener { dismissOnboarding() }
        applyCurrentStep()

        billingManager = BillingManager(this, lifecycleScope, ::onPurchaseEvent)
        billingManager.start()

        // First run: the tutorial comes alone; consent and ads follow once it is dismissed.
        if (prefs.getBoolean(KEY_SEEN_ONBOARDING, false)) startAdsIfEligible() else showOnboarding()
    }

    override fun onResume() {
        super.onResume()
        ads?.resume()
    }

    override fun onPause() {
        ads?.pause()
        super.onPause()
    }

    override fun onDestroy() {
        ads?.destroy()
        billingManager.end()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_STEP_INDEX, state.stepIndex)
        outState.putBoolean(KEY_COLOR_WHITE, state.color == LightColor.WHITE)
    }

    // ---- Setup ---------------------------------------------------------------

    private fun showOverLockScreen() {
        // The manifest declares showWhenLocked for API 27+; API 26 needs the window flag.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
    }

    /**
     * Click and long-click listeners are the single source of the tap and hold
     * actions, so they work both from touch (via the gesture detector) and
     * from accessibility services such as TalkBack. The two-finger tap has no
     * accessibility equivalent and is exposed as a custom action instead.
     */
    @SuppressLint("ClickableViewAccessibility") // performClick is issued from the gesture detector.
    private fun setupGestures() {
        val root = binding.root
        root.setOnClickListener {
            if (isEditMode) {
                toggleEditMode()
                it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            } else {
                cycleStep(forward = true)
            }
        }
        root.setOnLongClickListener {
            toggleEditMode()
            true // performLongClick adds the haptic feedback for a handled long click.
        }
        ViewCompat.addAccessibilityAction(root, getString(R.string.a11y_toggle_color)) { _, _ ->
            toggleColor()
            true
        }
        root.contentDescription = getString(R.string.app_name)

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean = root.performClick()

            override fun onLongPress(e: MotionEvent) {
                root.performLongClick()
            }
        })

        root.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> if (event.pointerCount == 2) {
                    twoFingerTouching = true
                    // Cancel any pending single-tap in the gesture detector.
                    val cancel = MotionEvent.obtain(event).apply { action = MotionEvent.ACTION_CANCEL }
                    gestureDetector.onTouchEvent(cancel)
                    cancel.recycle()
                }
                MotionEvent.ACTION_UP -> if (twoFingerTouching) {
                    twoFingerTouching = false
                    toggleColor()
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_CANCEL -> twoFingerTouching = false
            }
            if (!twoFingerTouching) gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.editMenuButton) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val baseMargin = resources.getDimensionPixelSize(R.dimen.edit_menu_button_margin)
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top + baseMargin
            }
            insets
        }

        // The ad container sits above the navigation bar whether or not a
        // banner is ever added, so the edit-mode frame (whose bottom is
        // constrained to the container's top) is never hidden behind the bar.
        ViewCompat.setOnApplyWindowInsetsListener(binding.adContainer) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> { bottomMargin = systemBars.bottom }
            insets
        }

        // Keep the edit-mode frame inside the system bars on the other three sides.
        ViewCompat.setOnApplyWindowInsetsListener(binding.editModeBorder) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val baseMargin = resources.getDimensionPixelSize(R.dimen.edit_mode_border_margin)
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top + baseMargin
                leftMargin = systemBars.left + baseMargin
                rightMargin = systemBars.right + baseMargin
            }
            insets
        }
    }

    // ---- Torch ---------------------------------------------------------------

    private fun cycleStep(forward: Boolean) {
        if (forward) state.nextStep() else state.previousStep()
        applyCurrentStep()
    }

    private fun toggleColor() {
        state.toggleColor()
        applyCurrentStep()
    }

    /** Pushes the selected step and colour to the window, the background and the readout. */
    private fun applyCurrentStep() {
        window.attributes = window.attributes.apply {
            screenBrightness =
                if (overlayDepth == 0) state.currentBrightness
                else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        val shade = if (state.color == LightColor.WHITE) whiteShades[state.stepIndex] else R.color.red
        binding.root.setBackgroundColor(getColor(shade))
        updateReadout()
    }

    private fun overlayOpened() {
        overlayDepth++
        applyCurrentStep()
    }

    private fun overlayClosed() {
        overlayDepth = (overlayDepth - 1).coerceAtLeast(0)
        applyCurrentStep()
    }

    private fun updateReadout() {
        val percent = (state.currentBrightness * 100).toInt()
        binding.brightnessTextView.text = getString(R.string.step_label, state.stepIndex + 1, state.stepCount)
        val colorName = getString(if (state.color == LightColor.WHITE) R.string.light_white else R.string.light_red)
        ViewCompat.setStateDescription(
            binding.root,
            getString(R.string.torch_state_description, state.stepIndex + 1, state.stepCount, colorName)
        )

        if (BuildConfig.DEBUG) {
            val hex = "%06X".format(binding.root.backgroundColor() and 0xFFFFFF)
            binding.debugInfoTextView.text = getString(R.string.debug_info_label, percent, hex)
            binding.debugInfoTextView.isVisible = true
        }

        binding.editInfoTextView.isVisible = isEditMode
        if (isEditMode) {
            binding.editInfoTextView.text = getString(R.string.edit_mode_label, percent)
            binding.editInfoTextView.contentDescription =
                getString(R.string.edit_mode_brightness_description, percent)
            binding.editInfoTextView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                brightnessIconFor(state.currentBrightness), 0, 0, 0
            )
        }
    }

    private fun brightnessIconFor(brightness: Float) = when {
        brightness < 0.34f -> R.drawable.ic_brightness_low
        brightness < 0.67f -> R.drawable.ic_brightness_medium
        else -> R.drawable.ic_brightness_high
    }

    private fun View.backgroundColor() = (background as? ColorDrawable)?.color ?: Color.TRANSPARENT

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val volumeKey = keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
        if (!volumeKey) return super.onKeyDown(keyCode, event)

        // Swallow the volume keys while the tutorial is up, so brightness does
        // not change behind it.
        if (binding.onboardingOverlay.isVisible) return true

        val up = keyCode == KeyEvent.KEYCODE_VOLUME_UP
        if (isEditMode) {
            state.adjustCurrentStep(if (up) TorchState.EDIT_STEP else -TorchState.EDIT_STEP)
            applyCurrentStep()
        } else {
            cycleStep(forward = up)
        }
        return true
    }

    // ---- Edit mode -----------------------------------------------------------

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        binding.editMenuButton.isVisible = isEditMode
        binding.editModeBorder.isVisible = isEditMode
        binding.editHintTextView.isVisible = isEditMode
        updateReadout()
    }

    private fun showEditMenu(anchor: View) {
        overlayOpened()
        PopupMenu(this, anchor).apply {
            setOnDismissListener { overlayClosed() }
            menuInflater.inflate(R.menu.edit_mode_menu, menu)
            menu.findItem(R.id.action_remove_ads).isVisible = !billingManager.adsRemoved
            menu.findItem(R.id.action_privacy_options).isVisible = ads?.isPrivacyOptionsRequired == true
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_how_it_works -> showOnboarding()
                    R.id.action_reset -> showResetConfirmationDialog()
                    R.id.action_remove_ads -> billingManager.launchPurchaseFlow()
                    R.id.action_privacy_options -> ads?.showPrivacyOptions(
                        onFormShown = ::overlayOpened,
                        onFormDismissed = ::overlayClosed
                    )
                    else -> return@setOnMenuItemClickListener false
                }
                true
            }
            show()
        }
    }

    private fun showResetConfirmationDialog() {
        overlayOpened()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.reset_confirm_title)
            .setMessage(R.string.reset_confirm_message)
            .setPositiveButton(R.string.action_reset) { _, _ -> resetSteps() }
            .setNegativeButton(android.R.string.cancel, null)
            .setOnDismissListener { overlayClosed() }
            .show()
    }

    private fun resetSteps() {
        state.resetSteps()
        binding.root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        applyCurrentStep()
    }

    // ---- Onboarding ----------------------------------------------------------

    private fun showOnboarding() {
        if (binding.onboardingOverlay.isVisible) return
        binding.onboardingOverlay.isVisible = true
        overlayOpened()
    }

    private fun dismissOnboarding() {
        binding.onboardingOverlay.isVisible = false
        getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit { putBoolean(KEY_SEEN_ONBOARDING, true) }
        overlayClosed()
        startAdsIfEligible()
    }

    // ---- Ads & billing -------------------------------------------------------

    private fun startAdsIfEligible() {
        if (ads != null || billingManager.adsRemoved) return
        ads = AdsController(this, binding.adContainer, BuildConfig.BANNER_AD_UNIT_ID).also {
            it.start(onFormShown = ::overlayOpened, onFormDismissed = ::overlayClosed)
        }
    }

    private fun onPurchaseEvent(event: PurchaseEvent) {
        val message = when (event) {
            PurchaseEvent.AdsRemoved -> {
                ads?.remove()
                R.string.purchase_ads_removed
            }
            PurchaseEvent.Pending -> R.string.purchase_pending
            PurchaseEvent.Unavailable -> R.string.purchase_unavailable
            PurchaseEvent.Failed -> R.string.purchase_failed
            PurchaseEvent.Cancelled -> return
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
