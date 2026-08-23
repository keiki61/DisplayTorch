package com.github.keiki.displaytorch

import android.app.Activity
import android.content.Context
import androidx.core.content.edit
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val REMOVE_ADS_PRODUCT_ID = "remove_ads"
private const val PREF_NAME = "billing_prefs"
private const val KEY_ADS_REMOVED = "ads_removed"

/**
 * Owns the Play Billing connection for the non-consumable "remove_ads" product.
 * Entitlement is cached in SharedPreferences so it's known synchronously on
 * next launch, before the (async) connection to Play confirms it.
 */
class BillingManager(
    private val activity: Activity,
    private val scope: CoroutineScope,
    private val onAdsRemoved: () -> Unit
) : PurchasesUpdatedListener {

    private val prefs = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var adsRemoved: Boolean = prefs.getBoolean(KEY_ADS_REMOVED, false)
        private set(value) {
            val changed = value && !field
            field = value
            if (changed) {
                prefs.edit { putBoolean(KEY_ADS_REMOVED, true) }
                onAdsRemoved()
            }
        }

    private val billingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { restorePurchases() }
                }
            }

            override fun onBillingServiceDisconnected() {
                // enableAutoServiceReconnection() handles retries.
            }
        })
    }

    fun launchPurchaseFlow() {
        scope.launch {
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(REMOVE_ADS_PRODUCT_ID)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()
            val result = billingClient.queryProductDetails(params)
            val productDetails = result.productDetailsList?.firstOrNull() ?: return@launch
            val offerToken = productDetails.oneTimePurchaseOfferDetails?.offerToken ?: return@launch
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                )
                .build()
            billingClient.launchBillingFlow(activity, flowParams)
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        if (billingResult.responseCode != BillingClient.BillingResponseCode.OK || purchases == null) return
        scope.launch { purchases.forEach { handlePurchase(it) } }
    }

    private suspend fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        result.purchasesList.forEach { handlePurchase(it) }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED ||
            !purchase.products.contains(REMOVE_ADS_PRODUCT_ID)
        ) {
            return
        }
        adsRemoved = true
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params)
        }
    }

    fun end() {
        billingClient.endConnection()
    }
}
