// SPDX-License-Identifier: Apache-2.0
// Copyright 2020, 2023, 2024 Uwe Trottmann

package com.battlelancer.seriesguide.util

import android.content.Context
import android.net.Uri

object Metacritic {

    /**
     * Starts VIEW Intent with Metacritic website movie search results URL.
     */
    fun searchForMovie(context: Context, title: String) {
        val url = "https://www.metacritic.com/search/${Uri.encode(title)}/?category=2"
        WebTools.openInApp(context, url)
    }

    /**
     * Starts VIEW Intent with Metacritic website TV search results URL.
     */
    fun searchForTvShow(context: Context, title: String) {
        val url = "https://www.metacritic.com/search/${Uri.encode(title)}/?category=1"
        WebTools.openInApp(context, url)
    }

}