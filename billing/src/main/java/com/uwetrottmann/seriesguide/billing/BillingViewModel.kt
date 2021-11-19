package com.uwetrottmann.seriesguide.billing

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.uwetrottmann.seriesguide.billing.localdb.AugmentedSkuDetails
import com.uwetrottmann.seriesguide.billing.localdb.GoldStatus
import kotlinx.coroutines.CoroutineScope

class BillingViewModel(
    application: Application,
    coroutineScope: CoroutineScope
) : AndroidViewModel(application) {

    val goldStatusLiveData: LiveData<GoldStatus>
    val subsSkuDetailsListLiveData: LiveData<List<AugmentedSkuDetails>>
    val entitlementRevokedEvent: LiveData<Void>
    val errorEvent: LiveData<String>

    private val repository: BillingRepository =
        BillingRepository.getInstance(application, coroutineScope)

    init {
        repository.startDataSourceConnections()
        goldStatusLiveData = repository.goldStatusLiveData
        subsSkuDetailsListLiveData = repository.subsSkuDetailsListLiveData
        entitlementRevokedEvent = repository.entitlementRevokedEvent
        errorEvent = repository.errorEvent
    }

    fun makePurchase(activity: Activity, augmentedSkuDetails: AugmentedSkuDetails) {
        repository.launchBillingFlow(activity, augmentedSkuDetails)
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
