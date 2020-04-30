package com.battlelancer.seriesguide.streaming

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.util.Utils
import java.util.Locale

object StreamingSearch {

    /** if null = not configured, if has value = enabled, if is empty = disabled */
    const val KEY_SETTING_SERVICE = "com.battlelancer.seriesguide.justwatch"

    val serviceToUrl = mapOf(
            "us" to "search",
            "reelgood-us" to "",
            "ca" to "search",
            "mx" to "buscar",
            "br" to "busca",
            "de" to "Suche",
            "at" to "Suche",
            "ch" to "Suche",
            "uk" to "search",
            "ie" to "search",
            "ru" to "поиск",
            "it" to "cerca",
            "fr" to "recherche",
            "es" to "buscar",
            "nl" to "search",
            "no" to "search",
            "se" to "search",
            "dk" to "search",
            "fi" to "search",
            "lt" to "search",
            "lv" to "search",
            "ee" to "search",
            "pt" to "search",
            "pl" to "search",
            "za" to "search",
            "au" to "search",
            "nz" to "search",
            "in" to "search",
            "jp" to "検索",
            "kr" to "검색",
            "th" to "search",
            "tr" to "arama",
            "my" to "search",
            "ph" to "search",
            "sg" to "search",
            "id" to "search"
    )

    @JvmStatic
    fun isTurnedOff(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_SETTING_SERVICE, null)?.isEmpty() == true
    }

    @JvmStatic
    fun isNotConfigured(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_SETTING_SERVICE, null) == null
    }

    @JvmStatic
    fun isNotConfiguredOrTurnedOff(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_SETTING_SERVICE, null).isNullOrEmpty()
    }

    @JvmStatic
    fun getServiceOrEmptyOrNull(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_SETTING_SERVICE, null)
    }

    @JvmStatic
    fun getServiceDisplayName(service: String): String {
        return if (service == "reelgood-us") {
            "Reelgood ${Locale("", "us").displayCountry}"
        } else {
            "JustWatch ${Locale("", service).displayCountry}"
        }
    }

    fun setServiceOrEmpty(context: Context, countryOrEmpty: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(KEY_SETTING_SERVICE, countryOrEmpty)
        }
    }

    private fun getServiceSearchUrl(context: Context, type: String): String {
        val service = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_SETTING_SERVICE, null) ?: ""
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
        justWatchType: String) {
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