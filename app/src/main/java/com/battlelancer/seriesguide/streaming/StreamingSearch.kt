package com.battlelancer.seriesguide.streaming

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.Button
import androidx.core.content.edit
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.battlelancer.seriesguide.util.Utils
import com.uwetrottmann.androidutils.CheatSheet
import kotlinx.coroutines.Dispatchers
import java.util.Locale
import kotlin.coroutines.CoroutineContext

object StreamingSearch {

    val regionLiveData = MutableLiveData<String>()

    data class WatchInfo(val showTmdbId: Int?, val region: String?)

    /** if null = not configured, also used in settings_basic.xml */
    const val KEY_SETTING_REGION = "com.uwetrottmann.seriesguide.watch.region"

    // TODO Add all newly supported countries.
    val serviceToUrl = mapOf(
        "US" to "search",
        "CA" to "search",
        "MX" to "buscar",
        "BR" to "busca",
        "DE" to "Suche",
        "AT" to "Suche",
        "CH" to "Suche",
        "UK" to "search",
        "IE" to "search",
        "RU" to "поиск",
        "IT" to "cerca",
        "FR" to "recherche",
        "ES" to "buscar",
        "NL" to "search",
        "NO" to "search",
        "SE" to "search",
        "DK" to "search",
        "FI" to "search",
        "LT" to "search",
        "LV" to "search",
        "EE" to "search",
        "PT" to "search",
        "PL" to "search",
        "ZA" to "search",
        "AU" to "search",
        "NZ" to "search",
        "IN" to "search",
        "JP" to "検索",
        "KR" to "검색",
        "TH" to "search",
        "TR" to "arama",
        "MY" to "search",
        "PH" to "search",
        "SG" to "search",
        "ID" to "search"
    )

    fun initRegionLiveData(context: Context) {
        regionLiveData.value = getCurrentRegionOrNull(context)
    }

    fun getWatchInfoMediator(showTmdbId: LiveData<Int>): MediatorLiveData<WatchInfo> {
        return MediatorLiveData<WatchInfo>().apply {
            addSource(showTmdbId) { value = WatchInfo(it, value?.region) }
            addSource(regionLiveData) { value = WatchInfo(value?.showTmdbId, it) }
        }
    }

    fun getWatchProviderLiveData(
        watchInfoMediator: MediatorLiveData<WatchInfo>,
        viewModelContext: CoroutineContext,
        context: Context
    ): LiveData<TmdbTools2.WatchInfo> {
        return Transformations.switchMap(watchInfoMediator) {
            liveData(context = viewModelContext + Dispatchers.IO) {
                if (it.showTmdbId != null && it.region != null) {
                    val tmdbTools = TmdbTools2()
                    val providers = tmdbTools.getWatchProvidersForShow(
                        it.showTmdbId,
                        it.region,
                        context
                    )
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
        CheatSheet.setup(configureButton)
        configureButton.setOnClickListener {
            StreamingSearchInfoDialog.show(fragmentManager)
        }
    }

    @SuppressLint("SetTextI18n")
    @JvmStatic
    fun configureButton(button: Button, watchInfo: TmdbTools2.WatchInfo) {
        val context = button.context
        val urlOrNull = watchInfo.url
        if (urlOrNull != null) {
            button.isEnabled = true
            button.setOnClickListener {
                Utils.launchWebsite(context, watchInfo.url)
            }
        } else {
            button.isEnabled = false
        }
        val providerOrNull = watchInfo.provider
        if (providerOrNull != null) {
            val moreText = if (watchInfo.countMore > 0) {
                " + " + context.getString(R.string.more, watchInfo.countMore)
            } else ""
            button.text = context.getString(R.string.action_stream) +
                    "\n" + providerOrNull.provider_name + moreText
        } else {
            button.setText(R.string.action_stream)
        }
    }

    @JvmStatic
    fun isNotConfigured(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_SETTING_REGION, null) == null
    }

    @JvmStatic
    fun getCurrentRegionOrNull(context: Context): String? {
        val regionOrNull = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_SETTING_REGION, null) ?: return null
        return if (serviceToUrl.keys.find { it == regionOrNull } != null) {
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

    private fun getServiceSearchUrl(context: Context, type: String): String {
        val service = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_SETTING_REGION, null) ?: ""
        return if (service == "reelgood-us") {
            "https://reelgood.com/search?q="
        } else {
            val searchPath = serviceToUrl[service] ?: "search"
            "https://www.justwatch.com/$service/$searchPath?content_type=$type&q="
        }
    }

    private fun buildAndLaunch(
        context: Context,
        title: String,
        justWatchType: String
    ) {
        val titleEncoded = Uri.encode(title)
        val searchUrl = getServiceSearchUrl(context, justWatchType)
        val url = "$searchUrl$titleEncoded"
        Utils.launchWebsite(context, url)
    }

    @JvmStatic
    fun searchForShow(context: Context, showTitle: String) {
        buildAndLaunch(context, showTitle, "show")
    }

    @JvmStatic
    fun searchForMovie(context: Context, movieTitle: String) {
        buildAndLaunch(context, movieTitle, "movie")
    }

}