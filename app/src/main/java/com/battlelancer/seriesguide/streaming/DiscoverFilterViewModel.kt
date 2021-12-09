package com.battlelancer.seriesguide.streaming

import android.app.Application
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.model.SgWatchProvider
import com.battlelancer.seriesguide.model.SgWatchProvider.Type
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.uwetrottmann.tmdb2.entities.WatchProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DiscoverFilterViewModel(
    application: Application,
    private val type: Type
) : AndroidViewModel(application) {

    val allWatchProvidersFlow = Pager(
        // Configure how data is loaded by passing additional properties to
        // PagingConfig, such as prefetchDistance.
        PagingConfig(pageSize = 20)
    ) {
        SgRoomDatabase.getInstance(getApplication()).sgWatchProviderHelper()
            .allWatchProvidersPagingSource(type.id)
    }.flow
        .cachedIn(viewModelScope)

    init {
        val watchRegion = StreamingSearch.getCurrentRegionOrNull(getApplication())
        if (watchRegion != null) {
            updateWatchProviders(watchRegion)
        }
    }

    fun updateWatchProviders(watchRegion: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val tmdb = SgApp.getServicesComponent(getApplication()).tmdb()
                val language = when (type) {
                    Type.SHOWS -> DisplaySettings.getShowsSearchLanguage(getApplication())
                    Type.MOVIES -> DisplaySettings.getMoviesLanguage(getApplication())
                }
                val newProviders = when (type) {
                    Type.SHOWS -> TmdbTools2().getShowWatchProviders(tmdb, language, watchRegion)
                    Type.MOVIES -> TmdbTools2().getMovieWatchProviders(tmdb, language, watchRegion)
                }
                if (newProviders != null) {
                    val dbHelper =
                        SgRoomDatabase.getInstance(getApplication()).sgWatchProviderHelper()
                    val oldProviders = dbHelper.getAllWatchProviders(type.id).toMutableList()

                    val diff = calculateProviderDiff(newProviders, oldProviders, type)

                    dbHelper.updateWatchProviders(
                        diff.inserts,
                        diff.updates,
                        diff.deletes
                    )
                }
            }
        }
    }

    companion object {

        /**
         * Create inserts, updates and deletes to minimize database writes
         * at the cost of CPU and memory. Only pass providers of one type.
         */
        @VisibleForTesting
        fun calculateProviderDiff(
            newProviders: List<WatchProviders.WatchProvider>,
            oldProviders: List<SgWatchProvider>,
            type: Type
        ): ProviderDiff {
            val inserts = mutableListOf<SgWatchProvider>()
            val updates = mutableListOf<SgWatchProvider>()
            val deletes = oldProviders.associateByTo(mutableMapOf()) { it.provider_id }

            newProviders.forEach { newProvider ->
                val providerId = newProvider.provider_id
                val providerName = newProvider.provider_name
                if (providerId != null && providerName != null) {
                    // Do not delete this provider
                    deletes.remove(providerId)
                    val existingProvider =
                        oldProviders.find { it.provider_id == providerId }
                    if (existingProvider != null) {
                        // Only update if different
                        val update = existingProvider.copy(
                            provider_name = providerName,
                            display_priority = newProvider.display_priority ?: 0,
                            logo_path = newProvider.logo_path ?: "",
                            type = type.id
                        )
                        if (update != existingProvider) updates.add(update)
                    } else {
                        inserts.add(
                            SgWatchProvider(
                                provider_id = providerId,
                                provider_name = providerName,
                                display_priority = newProvider.display_priority ?: 0,
                                logo_path = newProvider.logo_path ?: "",
                                type = type.id,
                                enabled = false
                            )
                        )
                    }
                }
            }

            return ProviderDiff(inserts, updates, deletes.values.toList())
        }
    }

}

data class ProviderDiff(
    val inserts: List<SgWatchProvider>,
    val updates: List<SgWatchProvider>,
    val deletes: List<SgWatchProvider>
)

class DiscoverFilterViewModelFactory(
    private val application: Application,
    private val type: Type
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DiscoverFilterViewModel(application, type) as T
    }
}
