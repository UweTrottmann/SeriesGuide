// SPDX-License-Identifier: Apache-2.0
// Copyright 2018-2023 Uwe Trottmann

package com.battlelancer.seriesguide.streaming

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.Button
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.edit
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.util.ViewTools
import kotlinx.coroutines.Dispatchers
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
                    emit(tmdbTools.getTopWatchProvider(providers))
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
     * Set [replaceButtonText] to instead of appending on a new line replace button text with
     * watch provider info.
     */
    @SuppressLint("SetTextI18n")
    @JvmStatic
    fun configureButton(
        button: Button,
        watchInfo: TmdbTools2.WatchInfo,
        replaceButtonText: Boolean = false
    ) {
        val context = button.context
        val urlOrNull = watchInfo.url
        if (urlOrNull != null) {
            button.isEnabled = true
            ViewTools.openUriOnClick(button, watchInfo.url)
        } else {
            button.isEnabled = false
        }
        val providerOrNull = watchInfo.provider
        return if (providerOrNull != null) {
            val moreText = if (watchInfo.countMore > 0) {
                " + " + NumberFormat.getIntegerInstance().format(watchInfo.countMore)
            } else ""
            val providerText = (providerOrNull.provider_name ?: "") + moreText
            if (replaceButtonText) {
                button.text = providerText
            } else {
                button.text = context.getString(R.string.action_stream) +
                        "\n" + providerText
            }
        } else {
            button.setText(R.string.action_stream)
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
    }

    fun getCurrentRegionOrSelectString(context: Context): String {
        return when (val serviceOrEmptyOrNull = getCurrentRegionOrNull(context)) {
            null -> context.getString(R.string.action_select_region)
            else -> getServiceDisplayName(serviceOrEmptyOrNull)
        }
    }

}