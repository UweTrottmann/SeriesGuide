package com.battlelancer.seriesguide.util

import android.content.Context
import android.view.View
import com.battlelancer.seriesguide.SgApp
import com.battlelancer.seriesguide.model.SgEpisode2
import com.battlelancer.seriesguide.model.SgShow2
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.tmdbapi.TmdbTools2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ViewToolsK {

    /**
     * Configures button to open IMDB, if needed fetches ID from network while disabling button.
     */
    fun configureImdbButton(
        button: View,
        coroutineScope: CoroutineScope,
        context: Context,
        show: SgShow2?,
        episode: SgEpisode2
    ) {
        button.apply {
            isEnabled = true
            setOnClickListener { button ->
                // Disable button to prevent multiple presses.
                button.isEnabled = false
                coroutineScope.launch {
                    if (show?.tmdbId == null) {
                        button.isEnabled = true
                        return@launch
                    }
                    val episodeImdbId = if (!episode.imdbId.isNullOrEmpty()) {
                        episode.imdbId
                    } else {
                        withContext(Dispatchers.IO) {
                            TmdbTools2().getImdbIdForEpisode(
                                SgApp.getServicesComponent(context).tmdb().tvEpisodesService(),
                                show.tmdbId, episode.season, episode.number
                            )?.also {
                                SgRoomDatabase.getInstance(context).sgEpisode2Helper()
                                    .updateImdbId(episode.id, it)
                            }
                        }
                    }
                    val imdbId = if (episodeImdbId.isNullOrEmpty()) {
                        show.imdbId // Fall back to show IMDb id.
                    } else {
                        episodeImdbId
                    }
                    // Leave button disabled if no id found.
                    if (!imdbId.isNullOrEmpty()) {
                        button.isEnabled = true
                        ServiceUtils.openImdb(imdbId, context)
                    }
                }
            }
        }
    }

}