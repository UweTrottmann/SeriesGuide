package com.battlelancer.seriesguide.ui.streams

import android.os.Bundle
import android.view.View
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.battlelancer.seriesguide.provider.SeriesGuideContract
import com.battlelancer.seriesguide.ui.search.AddShowDialogFragment.Companion.show
import com.battlelancer.seriesguide.ui.streams.UserEpisodeStreamFragment
import com.uwetrottmann.trakt5.entities.HistoryEntry

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
                if (item.episode == null || item.episode?.season == null || item.episode?.number == null
                    || item.show == null || item.show?.ids == null || item.show?.ids?.tmdb == null) {
                    // no episode or show? give up
                    return
                }

                // FIXME Look up using TMDB id.
                val episodeQuery = requireContext()
                    .contentResolver.query(
                        SeriesGuideContract.Episodes.buildEpisodesOfShowUri(item.show.ids.tvdb),
                        arrayOf(SeriesGuideContract.Episodes._ID),
                        "${SeriesGuideContract.Episodes.NUMBER}=${item.episode.number} AND " +
                                "${SeriesGuideContract.Episodes.SEASON}=${item.episode.season}",
                        null,
                        null
                    ) ?: return
                if (episodeQuery.count != 0) {
                    // display the episode details if we have a match
                    episodeQuery.moveToFirst()
                    showDetails(view, episodeQuery.getInt(0))
                } else {
                    // offer to add the show if it's not in the show database yet
                    show(
                        requireContext(),
                        this@UserEpisodeStreamFragment.parentFragmentManager,
                        item.show.ids.tvdb
                    )
                }
                episodeQuery.close()
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