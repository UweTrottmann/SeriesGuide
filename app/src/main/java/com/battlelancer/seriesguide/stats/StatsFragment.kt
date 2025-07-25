// SPDX-License-Identifier: Apache-2.0
// Copyright 2019-2024 Uwe Trottmann
// Copyright 2021 Andre Ippisch

package com.battlelancer.seriesguide.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentStatsBinding
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.ShareUtils
import com.battlelancer.seriesguide.util.TimeTools
import com.battlelancer.seriesguide.util.copyTextToClipboardOnLongClick
import java.text.NumberFormat

/**
 * Displays some statistics about the users show database, e.g. number of shows, episodes, share of
 * watched episodes, etc.
 */
class StatsFragment : Fragment() {

    private var binding: FragmentStatsBinding? = null
    private val model by viewModels<StatsViewModel>()
    private var currentStats: Stats? = null
    private var hasFinalValues: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding!!
        binding.errorView.visibility = View.GONE
        binding.errorView.setButtonClickListener { loadStats() }

        // set some views invisible so they can be animated in once stats are computed
        binding.textViewShowsFinished.visibility = View.INVISIBLE
        binding.progressBarShowsFinished.visibility = View.INVISIBLE
        binding.textViewShowsWithNextEpisode.visibility = View.INVISIBLE
        binding.progressBarShowsWithNextEpisode.visibility = View.INVISIBLE
        binding.textViewShowsContinuing.visibility = View.INVISIBLE
        binding.progressBarShowsContinuing.visibility = View.INVISIBLE

        binding.textViewEpisodesWatched.visibility = View.INVISIBLE
        binding.progressBarEpisodesWatched.visibility = View.INVISIBLE
        binding.textViewEpisodesRuntime.visibility = View.INVISIBLE

        binding.textViewMoviesWatchlist.visibility = View.INVISIBLE
        binding.textViewMoviesWatchlistRuntime.visibility = View.INVISIBLE

        binding.progressBarMoviesWatched.visibility = View.INVISIBLE
        binding.textViewMoviesWatched.visibility = View.INVISIBLE
        binding.textViewMoviesWatchedRuntime.visibility = View.INVISIBLE

        binding.progressBarMoviesCollection.visibility = View.INVISIBLE
        binding.textViewMoviesCollection.visibility = View.INVISIBLE
        binding.textViewMoviesCollectionRuntime.visibility = View.INVISIBLE

        // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
        binding.textViewShows.copyTextToClipboardOnLongClick()
        binding.textViewShowsFinished.copyTextToClipboardOnLongClick()
        binding.textViewShowsWithNextEpisode.copyTextToClipboardOnLongClick()
        binding.textViewShowsContinuing.copyTextToClipboardOnLongClick()
        binding.textViewEpisodes.copyTextToClipboardOnLongClick()
        binding.textViewEpisodesWatched.copyTextToClipboardOnLongClick()
        binding.textViewEpisodesRuntime.copyTextToClipboardOnLongClick()
        binding.textViewMovies.copyTextToClipboardOnLongClick()
        binding.textViewMoviesWatchlist.copyTextToClipboardOnLongClick()
        binding.textViewMoviesWatchlistRuntime.copyTextToClipboardOnLongClick()
        binding.textViewMoviesWatched.copyTextToClipboardOnLongClick()
        binding.textViewMoviesWatchedRuntime.copyTextToClipboardOnLongClick()
        binding.textViewMoviesCollection.copyTextToClipboardOnLongClick()
        binding.textViewMoviesCollectionRuntime.copyTextToClipboardOnLongClick()

        model.statsData.observe(viewLifecycleOwner) { this.handleStatsUpdate(it) }

        requireActivity().addMenuProvider(
            optionsMenuProvider,
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
    }

    private val optionsMenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.stats_menu, menu)

            menu.findItem(R.id.menu_action_stats_filter_specials).isChecked =
                DisplaySettings.isHidingSpecials(requireContext())
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            when (menuItem.itemId) {
                R.id.menu_action_stats_share -> {
                    shareStats()
                    return true
                }

                R.id.menu_action_stats_filter_specials -> {
                    PreferenceManager.getDefaultSharedPreferences(requireContext()).edit()
                        .putBoolean(DisplaySettings.KEY_HIDE_SPECIALS, !menuItem.isChecked)
                        .apply()

                    requireActivity().invalidateOptionsMenu()
                    loadStats()
                    return true
                }

                else -> return false
            }
        }
    }

    private fun loadStats() {
        model.hideSpecials.value = DisplaySettings.isHidingSpecials(requireContext())
    }

    private fun handleStatsUpdate(event: StatsUpdateEvent) {
        if (!isAdded) {
            return
        }
        currentStats = event.stats
        hasFinalValues = event.finalValues
        updateStats(event.stats, event.finalValues, event.successful)
    }

    private fun updateStats(
        stats: Stats, hasFinalValues: Boolean,
        successful: Boolean
    ) {
        val binding = binding ?: return

        // display error if not all stats could be calculated
        binding.errorView.isGone = successful

        val format = NumberFormat.getIntegerInstance()

        // all shows
        binding.textViewShows.text = format.format(stats.shows.toLong())

        // shows finished
        binding.progressBarShowsFinished.apply {
            max = stats.shows
            progress = stats.showsFinished
            visibility = View.VISIBLE
        }

        binding.textViewShowsFinished.apply {
            text = getString(
                R.string.shows_finished,
                format.format(stats.showsFinished.toLong())
            )
            visibility = View.VISIBLE
        }

        // shows with next episodes
        binding.progressBarShowsWithNextEpisode.apply {
            max = stats.shows
            progress = stats.showsWithNextEpisodes
            visibility = View.VISIBLE
        }

        binding.textViewShowsWithNextEpisode.apply {
            text = getString(
                R.string.shows_with_next,
                format.format(stats.showsWithNextEpisodes.toLong())
            )
            visibility = View.VISIBLE
        }

        // continuing shows
        binding.progressBarShowsContinuing.apply {
            max = stats.shows
            progress = stats.showsContinuing
            visibility = View.VISIBLE
        }

        binding.textViewShowsContinuing.text = getString(
            R.string.shows_continuing,
            format.format(stats.showsContinuing.toLong())
        )
        binding.textViewShowsContinuing.visibility = View.VISIBLE

        // all episodes
        binding.textViewEpisodes.text = format.format(stats.episodes.toLong())

        // watched episodes
        binding.progressBarEpisodesWatched.max = stats.episodes
        binding.progressBarEpisodesWatched.progress = stats.episodesWatched
        binding.progressBarEpisodesWatched.visibility = View.VISIBLE

        binding.textViewEpisodesWatched.text = getString(
            R.string.episodes_watched,
            format.format(stats.episodesWatched.toLong())
        )
        binding.textViewEpisodesWatched.visibility = View.VISIBLE

        // episode runtime
        var watchedDuration =
            TimeTools.formatToDaysHoursAndMinutes(resources, stats.episodesWatchedRuntime)
        if (!hasFinalValues) {
            // showing minimum (= not the final value)
            watchedDuration = "> $watchedDuration"
        }
        binding.textViewEpisodesRuntime.text = watchedDuration
        binding.textViewEpisodesRuntime.visibility = View.VISIBLE
        binding.progressBarEpisodesRuntime.visibility = if (successful)
            if (hasFinalValues) View.GONE else View.VISIBLE
        else
            View.GONE

        // movies
        binding.textViewMovies.text = format.format(stats.movies.toLong())

        // watched movies
        binding.progressBarMoviesWatched.apply {
            max = stats.movies
            progress = stats.moviesWatched
            visibility = View.VISIBLE
        }
        binding.textViewMoviesWatched.apply {
            text = getString(
                R.string.movies_watched_format,
                format.format(stats.moviesWatched.toLong())
            )
            visibility = View.VISIBLE
        }
        binding.textViewMoviesWatchedRuntime.apply {
            text = TimeTools.formatToDaysHoursAndMinutes(resources, stats.moviesWatchedRuntime)
            visibility = View.VISIBLE
        }

        // movies in watchlist
        binding.textViewMoviesWatchlist.apply {
            text = getString(
                R.string.movies_on_watchlist,
                format.format(stats.moviesWatchlist.toLong())
            )
            visibility = View.VISIBLE
        }
        binding.textViewMoviesWatchlistRuntime.apply {
            text = TimeTools.formatToDaysHoursAndMinutes(resources, stats.moviesWatchlistRuntime)
            visibility = View.VISIBLE
        }

        // movies in collection
        binding.progressBarMoviesCollection.apply {
            max = stats.movies
            progress = stats.moviesCollection
            visibility = View.VISIBLE
        }
        binding.textViewMoviesCollection.apply {
            text = getString(R.string.stats_in_collection_format, stats.moviesCollection)
            visibility = View.VISIBLE
        }
        binding.textViewMoviesCollectionRuntime.apply {
            text = TimeTools.formatToDaysHoursAndMinutes(resources, stats.moviesCollectionRuntime)
            visibility = View.VISIBLE
        }
    }

    private fun shareStats() {
        val currentStats = this.currentStats ?: return

        val format = NumberFormat.getIntegerInstance()

        val shows = format.format(currentStats.shows.toLong())
        val showsWithNext = getString(
            R.string.shows_with_next,
            format.format(currentStats.showsWithNextEpisodes.toLong())
        )
        val showsContinuing = getString(
            R.string.shows_continuing,
            format.format(currentStats.showsContinuing.toLong())
        )
        val showsFinished = getString(
            R.string.shows_finished,
            format.format(currentStats.showsFinished.toLong())
        )

        val episodes = format.format(currentStats.episodes.toLong())
        val episodesWatched = getString(
            R.string.episodes_watched,
            format.format(currentStats.episodesWatched.toLong())
        )

        val showStats =
            "${getString(R.string.app_name)} ${getString(R.string.statistics)}\n\n" +
                    "${getString(R.string.shows)}\n" +
                    "$shows\n" +
                    "$showsWithNext\n" +
                    "$showsContinuing\n" +
                    "$showsFinished\n\n" +
                    "${getString(R.string.episodes)}\n" +
                    "$episodes\n" +
                    "$episodesWatched\n"

        val statsString = StringBuilder(showStats)
        if (currentStats.episodesWatchedRuntime != 0L) {
            var watchedDuration = TimeTools.formatToDaysHoursAndMinutes(
                resources,
                currentStats.episodesWatchedRuntime
            )
            if (!hasFinalValues) {
                // showing minimum (= not the final value)
                watchedDuration = "> $watchedDuration"
            }
            statsString.append("$watchedDuration\n")
        }

        statsString.append("\n")

        // movies
        val movies = format.format(currentStats.movies.toLong())
        val moviesWatchlist = getString(
            R.string.movies_on_watchlist,
            format.format(currentStats.moviesWatchlist.toLong())
        )
        val moviesWatched = getString(
            R.string.movies_watched_format,
            format.format(currentStats.moviesWatched.toLong())
        )
        val moviesCollection = getString(
            R.string.stats_in_collection_format,
            currentStats.moviesCollection
        )
        val moviesWatchlistRuntime =
            TimeTools.formatToDaysHoursAndMinutes(resources, currentStats.moviesWatchlistRuntime)
        val moviesWatchedRuntime =
            TimeTools.formatToDaysHoursAndMinutes(resources, currentStats.moviesWatchedRuntime)
        val moviesCollectionRuntime =
            TimeTools.formatToDaysHoursAndMinutes(resources, currentStats.moviesCollectionRuntime)

        val movieStats = "${getString(R.string.movies)}\n" +
                "$movies\n" +
                "$moviesWatched\n" +
                "$moviesWatchedRuntime\n" +
                "$moviesWatchlist\n" +
                "$moviesWatchlistRuntime\n" +
                "$moviesCollection\n" +
                "$moviesCollectionRuntime\n"

        statsString.append(movieStats)

        ShareUtils.startShareIntentChooser(
            requireActivity(),
            statsString.toString(),
            R.string.share
        )
    }
}
