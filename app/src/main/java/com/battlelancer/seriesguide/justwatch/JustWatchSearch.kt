package com.battlelancer.seriesguide.justwatch

import android.content.Context
import android.net.Uri
import android.preference.PreferenceManager
import com.battlelancer.seriesguide.util.Utils
import java.util.Locale

object JustWatchSearch {

    /** if null = not configured, if has country = enabled, if is empty = disabled */
    const val KEY_SETTING_COUNTRY = "com.battlelancer.seriesguide.justwatch"

    val countryToUrl = mapOf(
            "us" to "search",
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
            "za" to "search",
            "au" to "search",
            "nz" to "search",
            "in" to "search",
            "jp" to "検索",
            "kr" to "검색",
            "th" to "search",
            "my" to "search",
            "ph" to "search",
            "sg" to "search",
            "id" to "search"
    )

    @JvmStatic
    fun isTurnedOff(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_SETTING_COUNTRY, null)?.isEmpty() == true
    }

    @JvmStatic
    fun isNotConfigured(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_SETTING_COUNTRY, null) == null
    }

    @JvmStatic
    fun getCountryOrEmptyOrNull(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_SETTING_COUNTRY, null)
    }

    @JvmStatic
    fun getCountryDisplayName(country: String): String {
        return Locale("", country).displayCountry
    }

    fun setCountryOrEmpty(context: Context, countryOrEmpty: String) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(KEY_SETTING_COUNTRY, countryOrEmpty)
                .apply()
    }

    @JvmStatic
    fun searchForShow(context: Context, showTitle: String, logTag: String) {
        buildAndLaunch(context, showTitle, "show", logTag)
    }

    @JvmStatic
    fun searchForMovie(context: Context, movieTitle: String, logTag: String) {
        buildAndLaunch(context, movieTitle, "movie", logTag)
    }

    private fun getCountrySearchUrl(context: Context): String {
        val country = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_SETTING_COUNTRY, null)
        val searchPath = countryToUrl[country] ?: "search"
        return "https://www.justwatch.com/$country/$searchPath?q="
    }

    private fun buildAndLaunch(context: Context, title: String, type: String, logTag: String) {
        val titleEncoded = Uri.encode(title)
        val searchUrl = getCountrySearchUrl(context)
        val url = "$searchUrl$titleEncoded&content_type=$type"
        Utils.launchWebsite(context, url, logTag, "JustWatch")
    }

}