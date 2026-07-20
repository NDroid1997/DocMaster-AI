package com.ndroid.docmasterai.premium

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.ndroid.docmasterai.data.local.preferences.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumManager @Inject constructor(
    @ApplicationContext private val context: Context,
    val preferences: AppPreferences,
) : PurchasesUpdatedListener {

    companion object {
        const val PREMIUM_MONTHLY = "docmaster_premium_monthly"
        const val PREMIUM_YEARLY = "docmaster_premium_yearly"
        const val PREMIUM_LIFETIME = "docmaster_premium_lifetime"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _trialStatus = MutableStateFlow<TrialStatus>(TrialStatus.NotStarted)
    val trialStatus: StateFlow<TrialStatus> = _trialStatus.asStateFlow()

    private val _availableProducts = MutableStateFlow<List<ProductDetails>>(emptyList())
    val availableProducts: StateFlow<List<ProductDetails>> = _availableProducts.asStateFlow()

    private var billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    init {
        connectBillingClient()
        scope.launch {
            preferences.isPremium.collect { premium ->
                _isPremium.value = premium
            }
        }
        scope.launch {
            preferences.trialStartDate.collect { startDate ->
                if (startDate == 0L) {
                    preferences.hasUsedTrial.collect { hasUsed ->
                        _trialStatus.value = if (hasUsed) TrialStatus.Expired else TrialStatus.NotStarted
                    }
                } else {
                    val currentTime = System.currentTimeMillis()
                    val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L
                    if (currentTime - startDate < sevenDaysInMillis) {
                        val remainingDays = 7 - ((currentTime - startDate) / (24 * 60 * 60 * 1000L)).toInt()
                        _trialStatus.value = TrialStatus.Active(remainingDays)
                    } else {
                        _trialStatus.value = TrialStatus.Expired
                        preferences.setHasUsedTrial(true)
                    }
                }
            }
        }
    }

    private fun connectBillingClient() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    queryExistingPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retry connection
                connectBillingClient()
            }
        })
    }

    private fun queryProducts() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_MONTHLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_YEARLY)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _availableProducts.value = queryResult.productDetailsList
            }
        }
    }

    private fun queryExistingPurchases() {
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasPremium = purchases.any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                scope.launch {
                    preferences.setPremium(hasPremium)
                    _isPremium.value = hasPremium
                }
            }
        }

        // Also check one-time purchases (lifetime)
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val hasLifetime = purchases.any { purchase ->
                    purchase.products.contains(PREMIUM_LIFETIME) &&
                            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                if (hasLifetime) {
                    scope.launch {
                        preferences.setPremium(true)
                        _isPremium.value = true
                    }
                }
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity? = null, productId: String = PREMIUM_MONTHLY) {
        val product = _availableProducts.value.find { it.productId == productId } ?: return
        val act = activity ?: return

        val offerToken = product.subscriptionOfferDetails?.firstOrNull()?.offerToken

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(product)
                .apply { offerToken?.let { setOfferToken(it) } }
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        billingClient.launchBillingFlow(act, billingFlowParams)
    }

    fun launchPurchaseFlow() {
        // No-op without activity reference, called from ViewModel
        // In production, pass activity via a callback or shared state
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            // Acknowledge the purchase
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(params) { result ->
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        scope.launch {
                            preferences.setPremium(true)
                            _isPremium.value = true
                        }
                    }
                }
            } else {
                scope.launch {
                    preferences.setPremium(true)
                    _isPremium.value = true
                }
            }
        }
    }

    fun isFeatureAvailable(feature: PremiumFeature): Boolean {
        return _isPremium.value || (_trialStatus.value is TrialStatus.Active) || feature.isFreeFeature
    }

    fun isSubFeatureAvailable(parentFeature: PremiumFeature, subFeature: String): Boolean {
        if (_isPremium.value || (_trialStatus.value is TrialStatus.Active)) return true
        
        return when (parentFeature) {
            PremiumFeature.ADVANCED_PDF_TOOLS -> {
                subFeature in listOf("merge", "split")
            }
            PremiumFeature.BASIC_CONVERT -> {
                subFeature in listOf("image_to_pdf", "pdf_to_image")
            }
            PremiumFeature.EDITOR -> {
                subFeature in listOf("crop", "rotate", "filter")
            }
            PremiumFeature.PREVIEW_EXPORT -> {
                subFeature in listOf("view", "share")
            }
            else -> false
        }
    }

    fun startTrial() {
        scope.launch {
            val hasUsed = false // We should check actual preference value here if not already handled by flow
            if (_trialStatus.value == TrialStatus.NotStarted) {
                val startTime = System.currentTimeMillis()
                preferences.setTrialStartDate(startTime)
                preferences.setHasUsedTrial(true)
                _trialStatus.value = TrialStatus.Active(7)
            }
        }
    }
}

sealed class TrialStatus {
    object NotStarted : TrialStatus()
    data class Active(val daysRemaining: Int) : TrialStatus()
    object Expired : TrialStatus()
}

enum class PremiumFeature(val isFreeFeature: Boolean) {
    BASIC_SCAN(true),
    BASIC_OCR(true),           // Limited to 5/day in free
    BASIC_CONVERT(false),      // Granular
    UNLIMITED_OCR(false),
    BATCH_CONVERSION(false),
    ADVANCED_PDF_TOOLS(false), // Granular
    AI_ENHANCEMENT(false),
    NO_ADS(false),
    CLOUD_BACKUP(false),
    EDITOR(false),             // Granular
    PREVIEW_EXPORT(false),     // Granular
}
