// Copyright 2023 Uwe Trottmann
// SPDX-License-Identifier: Apache-2.0

package com.battlelancer.seriesguide.movies.details

import android.content.Context
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import com.uwetrottmann.androidutils.GenericSimpleLoader

/**
 * Loads a YouTube movie trailer from TMDb. Tries to get a local trailer, if not falls back to
 * English.
 */
class MovieTrailersLoader(context: Context, private val tmdbId: Int) :
    GenericSimpleLoader<String?>(context) {

    override fun loadInBackground(): String? {
        return TmdbTools2().getMovieTrailerYoutubeId(context, tmdbId)
    }

}