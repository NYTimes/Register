package com.nytimes.android.external.registerlib

import android.app.Activity
import android.content.Context
import androidx.annotation.UiThread
import com.android.billingclient.api.*

class GoogleServiceProviderTesting(
        context: Context,
        enablePendingPurchases: Boolean,
        listener: PurchasesUpdatedListener
) : GoogleServiceProvider() {

    private val billingClient: BillingClientTesting = BillingClientTesting(
            context,
            enablePendingPurchases,
            listener
    )

    override fun isReady(): Boolean {
        return billingClient.isReady
    }

    override fun isFeatureSupported(feature: String): BillingResult {
        return billingClient.isFeatureSupported(feature)
    }

    override fun startConnection(listener: BillingClientStateListener) {
        billingClient.startConnection(listener)
    }

    override fun endConnection() {
        billingClient.endConnection()
    }

    override fun launchBillingFlow(activity: Activity, params: BillingFlowParams): BillingResult {
        return billingClient.launchBillingFlow(activity, params)
    }

    override fun queryPurchases(skuType: String): Purchase.PurchasesResult {
        return billingClient.queryPurchases(skuType)
    }

    override fun querySkuDetailsAsync(params: SkuDetailsParams, listener: SkuDetailsResponseListener) {
        billingClient.querySkuDetailsAsync(params, listener)
    }

    override fun consumeAsync(params: ConsumeParams, listener: ConsumeResponseListener) {
        billingClient.consumeAsync(params, listener)
    }

    override fun queryPurchaseHistoryAsync(skuType: String, listener: PurchaseHistoryResponseListener) {
        billingClient.queryPurchaseHistoryAsync(skuType, listener)
    }

    override fun acknowledgePurchase(params: AcknowledgePurchaseParams, listener: AcknowledgePurchaseResponseListener) {
        billingClient.acknowledgePurchase(params, listener)
    }

    companion object {

        /**
         * Constructs a new [BillingClient.Builder] instance.
         *
         * @param context It will be used to get an application context to bind to the in-app billing
         * service.
         */
        @UiThread
        @JvmStatic
        fun newBuilder(context: Context): GoogleServiceProvider.Builder {
            return GoogleServiceProvider.Builder(context)
        }
    }
}
