package com.battlelancer.seriesguide.ui.streams

import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.battlelancer.seriesguide.provider.SgRoomDatabase
import com.battlelancer.seriesguide.ui.episodes.EpisodesActivity
import com.battlelancer.seriesguide.ui.search.AddShowDialogFragment
import com.uwetrottmann.trakt5.entities.HistoryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Displays the latest Trakt episode watch history of the user.
 */
class UserEpisodeStreamFragment : StreamFragment() {

    private var adapter: EpisodeHistoryAdapter? = null

    override val listAdapter: BaseHistoryAdapter
        get() {
            if (adapter == null) {
                adapter = EpisodeHistoryAdapter(requireContext(), itemClickListener)
            }
            return adapter!!
        }

    override fun initializeStream() {
        LoaderManager.getInstance(this)
            .initLoader(HistoryActivity.EPISODES_LOADER_ID, null, activityLoaderCallbacks)
    }

    override fun refreshStream() {
        LoaderManager.getInstance(this).restartLoader(
            HistoryActivity.EPISODES_LOADER_ID, null,
            activityLoaderCallbacks
        )
    }

    private val itemClickListener: BaseHistoryAdapter.OnItemClickListener =
        object : BaseHistoryAdapter.OnItemClickListener {
            override fun onItemClick(view: View, item: HistoryEntry) {
                val season = item.episode?.season
                val number = item.episode?.number
                val showTmdbId = item.show?.ids?.tmdb
                if (season == null || number == null || showTmdbId == null) {
                    // no episode or show? give up
                    return
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val episodeIdOrNull = withContext(Dispatchers.IO) {
                        val database = SgRoomDatabase.getInstance(requireContext())
                        val showId = database.sgShow2Helper().getShowIdByTmdbId(showTmdbId)
                        if (showId != 0L) {
                            val episodeId = database.sgEpisode2Helper()
                                .getEpisodeIdByNumber(showId, season, number)
                            if (episodeId != 0L) {
                                return@withContext episodeId
                            }
                        }
                        null
                    }
                    if (episodeIdOrNull != null) {
                        // Is in database, show details.
                        val intent =
                            EpisodesActivity.intentEpisode(episodeIdOrNull, requireContext())
                        ActivityCompat.startActivity(
                            requireActivity(), intent,
                            ActivityOptionsCompat
                                .makeScaleUpAnimation(view, 0, 0, view.width, view.height)
                                .toBundle()
                        )
                    } else {
                        // Offer to add the show if not in database.
                        AddShowDialogFragment.show(parentFragmentManager, showTmdbId)
                    }
                }
            }
        }

    private val activityLoaderCallbacks: LoaderManager.LoaderCallbacks<TraktEpisodeHistoryLoader.Result> =
        object : LoaderManager.LoaderCallbacks<TraktEpisodeHistoryLoader.Result> {
            override fun onCreateLoader(
                id: Int,
                args: Bundle?
            ): Loader<TraktEpisodeHistoryLoader.Result> {
                showProgressBar(true)
                return TraktEpisodeHistoryLoader(requireContext())
            }

            override fun onLoadFinished(
                loader: Loader<TraktEpisodeHistoryLoader.Result>,
                data: TraktEpisodeHistoryLoader.Result
            ) {
                if (!isAdded) {
                    return
                }
                setListData(data.results, data.emptyText)
            }

            override fun onLoaderReset(
                loader: Loader<TraktEpisodeHistoryLoader.Result>
            ) {
                // keep current data
            }
        }
}