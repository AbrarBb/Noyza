package com.khatibstudio.noyza.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Play Billing manager.
 *
 * Product IDs:
 * - remove_ads          → One-time purchase to remove all ads (৳249)
 * - premium_monthly     → Monthly subscription (৳149/month)
 * - premium_yearly      → Annual subscription (৳999/year) — BEST VALUE
 * - premium_lifetime    → Lifetime one-time purchase (৳1999)
 *
 * Architecture:
 * - PurchasesUpdatedListener processes all purchase events
 * - Entitlements stored in DataStore (preferences) for instant reactive UI
 * - Restore purchases on connection
 */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: NoyZaPreferences
) : PurchasesUpdatedListener {

    companion object {
        private const val TAG = "BillingManager"

        // Product IDs — must match exactly what's registered in Play Console
        const val SKU_REMOVE_ADS = "remove_ads"
        const val SKU_PREMIUM_MONTHLY = "premium_monthly"
        const val SKU_PREMIUM_YEARLY = "premium_yearly"
        const val SKU_PREMIUM_LIFETIME = "premium_lifetime"

        private val SUBSCRIPTION_SKUS = listOf(SKU_PREMIUM_MONTHLY, SKU_PREMIUM_YEARLY)
        private val INAPP_SKUS = listOf(SKU_REMOVE_ADS, SKU_PREMIUM_LIFETIME)
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var billingClient: BillingClient

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _isPurchasing = MutableStateFlow(false)
    val isPurchasing: StateFlow<Boolean> = _isPurchasing.asStateFlow()

    private val _billingError = MutableStateFlow<String?>(null)
    val billingError: StateFlow<String?> = _billingError.asStateFlow()

    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing connected")
                    scope.launch {
                        queryProducts()
                        restorePurchases()
                    }
                } else {
                    Log.e(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing disconnected — will retry on next launch")
            }
        })
    }

    /**
     * Launch the billing flow for a given product.
     */
    fun launchBillingFlow(activity: Activity, productDetails: ProductDetails) {
        val productDetailsParamsList = if (productDetails.productType == BillingClient.ProductType.SUBS) {
            val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: return
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        } else {
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            )
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        _isPurchasing.value = true
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _isPurchasing.value = false
            _billingError.value = "Purchase failed: ${result.debugMessage}"
        }
    }

    /**
     * Restore existing purchases (call on app start).
     */
    suspend fun restorePurchases() {
        // Check subscriptions
        SUBSCRIPTION_SKUS.forEach { sku ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
            val result = billingClient.queryPurchasesAsync(params)
            result.purchasesList.forEach { handlePurchase(it) }
        }

        // Check in-app products
        val inappParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val inappResult = billingClient.queryPurchasesAsync(inappParams)
        inappResult.purchasesList.forEach { handlePurchase(it) }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        _isPurchasing.value = false
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    scope.launch { handlePurchase(purchase) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "User cancelled purchase")
            }
            else -> {
                _billingError.value = "Purchase error: ${billingResult.debugMessage}"
                Log.e(TAG, "Purchase error: ${billingResult.debugMessage}")
            }
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        val products = purchase.products
        val isPremium = products.any { it in SUBSCRIPTION_SKUS || it == SKU_PREMIUM_LIFETIME }
        val isAdsRemoved = products.any { it == SKU_REMOVE_ADS }

        if (isPremium) {
            preferences.setPremium(true, products.firstOrNull() ?: "")
            Log.d(TAG, "Premium granted: $products")
        }
        if (isAdsRemoved) {
            preferences.setAdsRemoved(true)
            Log.d(TAG, "Ads removed granted")
        }

        // Acknowledge the purchase if not already acknowledged
        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams) { result ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.e(TAG, "Acknowledge failed: ${result.debugMessage}")
                }
            }
        }
    }

    private suspend fun queryProducts() {
        val subscriptionProducts = SUBSCRIPTION_SKUS.map { sku ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(sku)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }

        val inappProducts = INAPP_SKUS.map { sku ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(sku)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        }

        val allProducts = subscriptionProducts + inappProducts
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(allProducts)
            .build()

        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _products.value = result.productDetailsList ?: emptyList()
        }
    }

    fun clearError() {
        _billingError.value = null
    }

    fun destroy() {
        scope.cancel()
        if (::billingClient.isInitialized) {
            billingClient.endConnection()
        }
    }
}
