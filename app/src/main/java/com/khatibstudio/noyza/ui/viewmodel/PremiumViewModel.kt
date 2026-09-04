package com.khatibstudio.noyza.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khatibstudio.noyza.billing.BillingManager
import com.khatibstudio.noyza.data.preferences.NoyZaPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PremiumUiState(
    val selectedPlan: Int = 1, // 0=monthly, 1=yearly, 2=lifetime
    val isPurchasing: Boolean = false,
    val isPremium: Boolean = false
)

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val preferences: NoyZaPreferences
) : ViewModel() {

    private val _selectedPlan = MutableStateFlow(1)
    private val _uiState = MutableStateFlow(PremiumUiState())
    val uiState: StateFlow<PremiumUiState> = _uiState.asStateFlow()

    private var currentActivity: Activity? = null

    init {
        viewModelScope.launch {
            combine(
                _selectedPlan,
                billingManager.isPurchasing,
                preferences.isPremium
            ) { plan, purchasing, premium ->
                PremiumUiState(plan, purchasing, premium)
            }.collect { state -> _uiState.value = state }
        }
    }

    fun setActivity(activity: Activity) {
        currentActivity = activity
    }

    fun selectPlan(index: Int) {
        _selectedPlan.value = index
    }

    fun startPurchase() {
        val activity = currentActivity ?: return
        val products = billingManager.products.value
        val skuToFind = when (_selectedPlan.value) {
            0 -> BillingManager.SKU_PREMIUM_MONTHLY
            1 -> BillingManager.SKU_PREMIUM_YEARLY
            2 -> BillingManager.SKU_PREMIUM_LIFETIME
            else -> BillingManager.SKU_PREMIUM_YEARLY
        }
        val product = products.firstOrNull { it.productId == skuToFind } ?: return
        billingManager.launchBillingFlow(activity, product)
    }

    fun purchaseRemoveAds() {
        val activity = currentActivity ?: return
        val product = billingManager.products.value
            .firstOrNull { it.productId == BillingManager.SKU_REMOVE_ADS } ?: return
        billingManager.launchBillingFlow(activity, product)
    }

    fun restorePurchases() {
        viewModelScope.launch {
            billingManager.restorePurchases()
        }
    }
}
