package com.github.keiki.displaytorch

import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Owns the UMP consent flow and the adaptive banner inside [container].
 *
 * The consent and privacy-options forms are dialogs in the activity window,
 * so they inherit its brightness override. The `onFormShown` /
 * `onFormDismissed` callbacks let the activity release and restore that
 * override around them.
 */
class AdsController(
    private val activity: AppCompatActivity,
    private val container: ViewGroup,
    private val adUnitId: String,
) {
    private val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
    private var adView: AdView? = null
    private var bannerRequested = false
    private var removed = false

    /** True when regulation requires an always-available way to change consent. */
    val isPrivacyOptionsRequired: Boolean
        get() = consentInfo.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun start(onFormShown: () -> Unit, onFormDismissed: () -> Unit) {
        consentInfo.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                val formRequired = consentInfo.consentStatus == ConsentInformation.ConsentStatus.REQUIRED
                if (formRequired) onFormShown()
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    if (formRequired) onFormDismissed()
                    if (consentInfo.canRequestAds()) loadBanner()
                }
            },
            {
                // Consent info unavailable (e.g. offline); a previous session's
                // consent may still allow ads.
                if (consentInfo.canRequestAds()) loadBanner()
            }
        )
        // Consent gathered in a previous session allows loading without waiting.
        if (consentInfo.canRequestAds()) loadBanner()
    }

    fun showPrivacyOptions(onFormShown: () -> Unit, onFormDismissed: () -> Unit) {
        onFormShown()
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { onFormDismissed() }
    }

    private fun loadBanner() {
        if (bannerRequested || removed) return
        bannerRequested = true
        // Google recommends initialising off the main thread; ad loads issued
        // before initialisation completes are queued by the SDK.
        activity.lifecycleScope.launch(Dispatchers.IO) { MobileAds.initialize(activity) }
        container.post {
            if (removed) return@post
            val adWidthDp = (container.width / activity.resources.displayMetrics.density).toInt()
            if (adWidthDp <= 0) return@post
            val banner = AdView(activity).apply {
                adUnitId = this@AdsController.adUnitId
                setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidthDp))
            }
            adView = banner
            container.addView(banner)
            banner.loadAd(AdRequest.Builder().build())
        }
    }

    /**
     * Tears down the banner and blocks any later load, including consent
     * callbacks that arrive after the remove-ads entitlement was restored.
     */
    fun remove() {
        removed = true
        destroyBanner()
        container.removeAllViews()
    }

    fun resume() {
        adView?.resume()
    }

    fun pause() {
        adView?.pause()
    }

    fun destroy() = destroyBanner()

    private fun destroyBanner() {
        adView?.destroy()
        adView = null
    }
}
