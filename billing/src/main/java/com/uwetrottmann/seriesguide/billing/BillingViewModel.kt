// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2025 Uwe Trottmann

package com.uwetrottmann.seriesguide.billing

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.uwetrottmann.seriesguide.billing.localdb.PlayUnlockState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Helps fetch current purchases and available products from Play billing provider.
 */
class BillingViewModel(
    application: Application,
    coroutineScope: CoroutineScope
) : AndroidViewModel(application) {

    val playUnlockStateLiveData: LiveData<PlayUnlockState>

    /**
     * A list of supported products filtered to only contain those
     * that are available on Google Play.
     */
    val availableProducts: Flow<List<SafeAugmentedProductDetails>>
    val entitlementRevokedEvent: LiveData<Void>
    val errorEvent: LiveData<BillingRepository.BillingError>

    private val repository: BillingRepository =
        BillingRepository.getInstance(application, coroutineScope)

    init {
        repository.startDataSourceConnections()
        playUnlockStateLiveData = repository.playUnlockStateLiveData
        availableProducts = repository.productDetails
            .map { products ->
                products.mapNotNull { product ->
                    if (product.productDetails != null) {
                        val subscriptionOfferDetails =
                            product.productDetails.subscriptionOfferDetails
                        if (subscriptionOfferDetails != null) {
                            return@mapNotNull SafeAugmentedProductDetails(
                                product.productId,
                                product.canPurchase,
                                product.productDetails,
                                subscriptionOfferDetails.flatMap { it.pricingPhases.pricingPhaseList }
                            )
                        }
                    }
                    return@mapNotNull null
                }
            }
            .flowOn(Dispatchers.IO)
        entitlementRevokedEvent = repository.entitlementRevokedEvent
        errorEvent = repository.errorEvent
    }

    fun makePurchase(activity: Activity, productDetails: SafeAugmentedProductDetails) {
        repository.launchBillingFlow(activity, productDetails)
    }

}

class BillingViewModelFactory(
    private val application: Application,
    private val coroutineScope: CoroutineScope
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BillingViewModel(application, coroutineScope) as T
    }

}
