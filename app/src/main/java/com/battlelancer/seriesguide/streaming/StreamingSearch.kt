// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2025 Uwe Trottmann

package com.battlelancer.seriesguide.streaming

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableStringBuilder
import android.text.SpannedString
import android.text.style.TextAppearanceSpan
import android.view.View
import android.widget.Button
import androidx.annotation.StringRes
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.edit
import androidx.core.text.buildSpannedString
import androidx.core.text.inSpans
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.movies.MoviesSettings
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.shows.ShowsSettings
import com.battlelancer.seriesguide.sync.SgSyncAdapter
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.util.WebTools
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.uwetrottmann.tmdb2.entities.WatchProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import kotlin.coroutines.CoroutineContext

object StreamingSearch {

    val regionLiveData = MutableLiveData<String?>()

    data class WatchInfo(val tmdbId: Int?, val region: String?)

    /** if null = not configured, also used in settings_basic.xml */
    const val KEY_SETTING_REGION = "com.uwetrottmann.seriesguide.watch.region"

    val supportedRegions = listOf(
        "AD", // Andorra
        "AE", // United Arab Emirates
        "AL", // Albania
        "AO", // Angola
        "AR", // Argentina
        "AT", // Austria
        "AU", // Australia
        "AZ", // Azerbaijan
        "BA", // Bosnia and Herzegovina
        "BE", // Belgium
        "BF", // Burkina Faso
        "BG", // Bulgaria
        "BH", // Bahrain
        "BM", // Bermuda
        "BO", // Bolivia
        "BR", // Brazil
        "BY", // Belarus
        "BZ", // Belize
        "CA", // Canada
        "CH", // Switzerland
        "CI", // Ivory Coast
        "CL", // Chile
        "CM", // Cameroon
        "CO", // Colombia
        "CR", // Costa Rica
        "CV", // Cape Verde
        "CY", // Cyprus
        "CZ", // Czech Republic
        "DE", // Germany
        "DK", // Denmark
        "DZ", // Algeria
        "EC", // Ecuador
        "EE", // Estonia
        "EG", // Egypt
        "ES", // Spain
        "FI", // Finland
        "FJ", // Fiji
        "FR", // France
        "GB", // United Kingdom // TMDB (correctly) uses GB instead of UK
        "GF", // French Guiana
        "GH", // Ghana
        "GI", // Gibraltar
        "GG", // Guernsey
        "GQ", // Equatorial Guinea
        "GR", // Greece
        "GT", // Guatemala
        "GY", // Guyana
        "HK", // Hong Kong
        "HN", // Honduras
        "HR", // Croatia
        "HU", // Hungary
        "ID", // Indonesia
        "IE", // Ireland
        "IL", // Israel
        "IN", // India
        "IQ", // Iraq
        "IS", // Iceland
        "IT", // Italy
        "JO", // Jordan
        "JP", // Japan
        "KE", // Kenya
        "KR", // Korea
        "KW", // Kuwait
        "LB", // Lebanon
        "LI", // Liechtenstein
        "LT", // Lithuania
        "LU", // Luxembourg
        "LV", // Latvia
        "LY", // Libya
        "MA", // Morocco
        "MC", // Monaco
        "MD", // Moldova
        "ME", // Montenegro
        "MG", // Madagascar
        "MK", // Macedonia
        "ML", // Mali
        "MT", // Malta
        "MU", // Mauritius
        "MW", // Malawi
        "MX", // Mexico
        "MY", // Malaysia
        "MZ", // Mozambique
        "NE", // Niger
        "NG", // Nigeria
        "NI", // Nicaragua
        "NL", // Netherlands
        "NO", // Norway
        "NZ", // New Zealand
        "OM", // Oman
        "PA", // Panama
        "PE", // Peru
        "PF", // French Polynesia
        "PG", // Papua New Guinea
        "PH", // Philippines
        "PK", // Pakistan
        "PL", // Poland
        "PS", // Palestine
        "PT", // Portugal
        "PY", // Paraguay
        "QA", // Qatar
        "RO", // Romania
        "RS", // Serbia
        "RU", // Russia
        "SA", // Saudi Arabia
        "SC", // Seychelles
        "SE", // Sweden
        "SI", // Slovenia
        "SG", // Singapore
        "SK", // Slovakia
        "SM", // San Marino
        "SN", // Senegal
        "SV", // El Salvador
        "TD", // Chad
        "TH", // Thailand
        "TN", // Tunisia
        "TR", // Türkiye
        "TW", // Taiwan
        "TZ", // Tanzania
        "UA", // Ukraine
        "UG", // Uganda
//        "UK", // TMDB (correctly) uses GB instead of UK
        "UY", // Uruguay
        "US", // United States
        "VA", // Vatican City
        "VE", // Venezuela
        "XK", // Kosovo
        "YE", // Yemen
        "ZA", // South Africa
        "ZM", // Zambia
        "ZW", // Zimbabwe
    )

    fun initRegionLiveData(context: Context) {
        regionLiveData.value = getCurrentRegionOrNull(context)
    }

    fun getWatchInfoMediator(tmdbId: LiveData<Int>): MediatorLiveData<WatchInfo> {
        return MediatorLiveData<WatchInfo>().apply {
            addSource(tmdbId) { value = WatchInfo(it, value?.region) }
            addSource(regionLiveData) { value = WatchInfo(value?.tmdbId, it) }
        }
    }

    /**
     * Defaults to show providers, set [isMovie] to get for a movie.
     */
    fun getWatchProviderLiveData(
        watchInfo: LiveData<WatchInfo>,
        viewModelContext: CoroutineContext,
        context: Context,
        isMovie: Boolean = false
    ): LiveData<TmdbTools2.WatchInfo> {
        return watchInfo.switchMap {
            liveData(context = viewModelContext + Dispatchers.IO) {
                if (it.tmdbId != null && it.region != null) {
                    val tmdbTools = TmdbTools2()
                    val providers = if (isMovie) {
                        tmdbTools.getWatchProvidersForMovie(
                            it.tmdbId,
                            it.region,
                            context
                        )
                    } else {
                        tmdbTools.getWatchProvidersForShow(
                            it.tmdbId,
                            it.region,
                            context
                        )
                    }
                    emit(tmdbTools.buildWatchInfo(providers))
                }
            }
        }
    }

    @JvmStatic
    fun initButtons(linkButton: View, configureButton: View, fragmentManager: FragmentManager) {
        linkButton.setOnClickListener {
            StreamingSearchInfoDialog.show(fragmentManager)
        }
        TooltipCompat.setTooltipText(configureButton, configureButton.contentDescription)
        configureButton.setOnClickListener {
            StreamingSearchInfoDialog.show(fragmentManager)
        }
    }

    /**
     * Set [hideExternalLink] to not display a link to TMDB with direct links to providers.
     */
    @SuppressLint("SetTextI18n")
    fun configureButton(
        button: Button,
        watchInfo: TmdbTools2.WatchInfo,
        hideExternalLink: Boolean
    ) {
        val topProviderOrNull = watchInfo.topProvider
        return if (topProviderOrNull != null) {
            val context = button.context
            val moreText = if (watchInfo.countMore > 0) {
                " + " + NumberFormat.getIntegerInstance().format(watchInfo.countMore)
            } else ""
            val providerText = topProviderOrNull + moreText
            button.text = providerText
            button.setOnClickListener {
                showWatchProviderInfoDialog(context, watchInfo, hideExternalLink)
            }
            button.isEnabled = true
        } else {
            button.setText(R.string.action_stream)
            button.isEnabled = false
        }
    }

    private fun showWatchProviderInfoDialog(
        context: Context,
        watchInfo: TmdbTools2.WatchInfo,
        hideExternalLink: Boolean
    ) {
        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.action_stream)
            .setMessage(watchInfo.toSpannedString(context))
            .setPositiveButton(R.string.dismiss, null /* Just dismiss */)
        if (!hideExternalLink && !watchInfo.url.isNullOrEmpty()) {
            builder.setNeutralButton(R.string.more_information) { _, _ ->
                WebTools.openInApp(context, watchInfo.url)
            }
        }
        builder.show()
    }

    private fun TmdbTools2.WatchInfo.toSpannedString(context: Context): SpannedString {
        return buildSpannedString {
            // Display "cheapest" options first
            addWatchProvidersSection(context, subscription, R.string.title_stream_subscription)
            addWatchProvidersSection(context, free, R.string.title_stream_free)
            addWatchProvidersSection(context, withAds, R.string.title_stream_with_ads)
            addWatchProvidersSection(context, rent, R.string.title_stream_rent)
            addWatchProvidersSection(context, buy, R.string.title_stream_buy)
            inSpans(TextAppearanceSpan(context, R.style.TextAppearance_SeriesGuide_Caption)) {
                appendLine()
                append(context.getString(R.string.powered_by_justwatch))
            }
        }
    }

    private fun SpannableStringBuilder.addWatchProvidersSection(
        context: Context,
        providers: List<String>,
        @StringRes titleRes: Int
    ) {
        if (providers.isNotEmpty()) {
            if (isNotEmpty()) appendLine()
            inSpans(TextAppearanceSpan(context, R.style.TextAppearance_SeriesGuide_Headline6)) {
                appendLine(context.getString(titleRes))
            }
            inSpans(TextAppearanceSpan(context, R.style.TextAppearance_SeriesGuide_Body1)) {
                providers.forEach {
                    appendLine(it)
                }
            }
        }
    }

    @JvmStatic
    fun getCurrentRegionOrNull(context: Context): String? {
        val regionOrNull = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_SETTING_REGION, null) ?: return null
        return if (supportedRegions.find { it == regionOrNull } != null) {
            regionOrNull
        } else {
            null // Region not supported (any longer).
        }
    }

    @JvmStatic
    fun getServiceDisplayName(service: String): String {
        return Locale("", service).displayCountry
    }

    fun setRegion(context: Context, region: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(KEY_SETTING_REGION, region)
        }
        regionLiveData.postValue(region)
        // In case changed in quick succession, do not run in parallel to avoid breaking diff
        SgApp.coroutineScope.launch(SgApp.SINGLE) {
            // Update providers for new region
            updateWatchProviders(context, SgWatchProvider.Type.SHOWS, region)
            updateWatchProviders(context, SgWatchProvider.Type.MOVIES, region)
            // Schedule shows to update mappings for new region
            SgRoomDatabase.getInstance(context).sgShow2Helper().resetLastUpdated()
            withContext(Dispatchers.Main) {
                SgSyncAdapter.requestSyncDeltaImmediate(context, true)
            }
        }
    }

    fun getCurrentRegionOrSelectString(context: Context): String {
        return when (val serviceOrEmptyOrNull = getCurrentRegionOrNull(context)) {
            null -> context.getString(R.string.action_select_region)
            else -> getServiceDisplayName(serviceOrEmptyOrNull)
        }
    }

    suspend fun updateWatchProviders(
        context: Context,
        type: SgWatchProvider.Type,
        watchRegion: String
    ): Boolean {
        val tmdb = SgApp.getServicesComponent(context).tmdb()
        val language = when (type) {
            SgWatchProvider.Type.SHOWS -> ShowsSettings.getShowsSearchLanguage(context)
            SgWatchProvider.Type.MOVIES -> MoviesSettings.getMoviesLanguage(context)
        }
        val newProviders = when (type) {
            SgWatchProvider.Type.SHOWS -> TmdbTools2()
                .getShowWatchProviders(tmdb, language, watchRegion)

            SgWatchProvider.Type.MOVIES -> TmdbTools2()
                .getMovieWatchProviders(tmdb, language, watchRegion)
        }
        if (newProviders != null) {
            val dbHelper =
                SgRoomDatabase.getInstance(context).sgWatchProviderHelper()
            val oldProviders = dbHelper.getAllWatchProviders(type.id).toMutableList()

            val diff = calculateProviderDiff(newProviders, oldProviders, type)

            dbHelper.updateWatchProviders(diff.inserts, diff.updates, diff.deletes)
            return true
        }
        return false
    }

    data class ProviderDiff(
        val inserts: List<SgWatchProvider>,
        val updates: List<SgWatchProvider>,
        val deletes: List<SgWatchProvider>
    )

    /**
     * Create inserts, updates and deletes to minimize database writes
     * at the cost of CPU and memory. Only pass providers of one type.
     */
    fun calculateProviderDiff(
        newProviders: List<WatchProviders.WatchProvider>,
        oldProviders: List<SgWatchProvider>,
        type: SgWatchProvider.Type
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