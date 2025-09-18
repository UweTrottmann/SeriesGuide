// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2025 Uwe Trottmann

package com.battlelancer.seriesguide.billing

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.battlelancer.seriesguide.billing.localdb.PlayUnlockState
import com.battlelancer.seriesguide.billing.localdb.UnlockState
import com.battlelancer.seriesguide.util.PackageTools
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

/**
 * Helps fetch current purchases and available products from Play billing provider.
 */
class BillingViewModel(
    application: Application,
    coroutineScope: CoroutineScope
) : AndroidViewModel(application) {

    data class AugmentedUnlockState(
        val unlockState: UnlockState,
        /**
         * May be `null` if there is no active purchase.
         */
        val playUnlockState: PlayUnlockState?,
        val hasAllAccessPass: Boolean
    )

    val augmentedUnlockState: Flow<AugmentedUnlockState>

    /**
     * A list of supported products filtered to only contain those
     * that are available on Google Play.
     */
    val availableProducts: Flow<List<SafeAugmentedProductDetails>>
    val errorEvent: LiveData<BillingRepository.BillingError>

    private val repository: BillingRepository =
        BillingRepository.getInstance(application, coroutineScope)

    init {
        repository.startDataSourceConnections()

        augmentedUnlockState = combine(
            BillingTools.unlockStateReadOnly,
            repository.createUnlockStateFlow()
        ) { unlockState, playUnlockState ->
            // Note: this will not show the all access in-app pass if the user also has a sub
            val hasAllAccessPass =
                (playUnlockState?.entitled == true && !playUnlockState.isSub)
                        || PackageTools.hasUnlockKeyInstalled(getApplication())
            AugmentedUnlockState(
                unlockState,
                playUnlockState,
                hasAllAccessPass
            )
        }
            // Share to avoid re-creating on config change;
            // no StateFlow as UI doesn't need initial value, it will display a wait indicator.
            .shareIn(viewModelScope, SharingStarted.Lazily, replay = 1)

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
