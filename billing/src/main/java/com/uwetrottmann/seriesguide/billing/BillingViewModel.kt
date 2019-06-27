package com.uwetrottmann.seriesguide.billing

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.uwetrottmann.seriesguide.billing.localdb.AugmentedSkuDetails
import com.uwetrottmann.seriesguide.billing.localdb.GoldStatus
import timber.log.Timber

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    val goldStatusLiveData: LiveData<GoldStatus>
    val subsSkuDetailsListLiveData: LiveData<List<AugmentedSkuDetails>>

    private val repository: BillingRepository = BillingRepository.getInstance(application)

    init {
        repository.startDataSourceConnections()
        goldStatusLiveData = repository.goldStatusLiveData
        subsSkuDetailsListLiveData = repository.subsSkuDetailsListLiveData
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("onCleared")
        repository.endDataSourceConnections()
    }

    fun makePurchase(activity: Activity, augmentedSkuDetails: AugmentedSkuDetails) {
        repository.launchBillingFlow(activity, augmentedSkuDetails)
    }

}