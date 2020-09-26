package com.battlelancer.seriesguide.ui.stats

import android.os.Bundle
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.databinding.FragmentStatsBinding
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.ShareUtils
import com.battlelancer.seriesguide.util.copyTextToClipboardOnLongClick
import java.text.NumberFormat

/**
 * Displays some statistics about the users show database, e.g. number of shows, episodes, share of
 * watched episodes, etc.
 */
class StatsFragment : Fragment() {

    private var binding: FragmentStatsBinding? = null
    private lateinit var model: StatsViewModel
    private var currentStats: StatsLiveData.Stats? = null
    private var hasFinalValues: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = binding!!
        binding.errorView.visibility = View.GONE
        binding.errorView.setButtonClickListener { loadStats() }

        // set some views invisible so they can be animated in once stats are computed
        binding.textViewShowsWithNextEpisode.visibility = View.INVISIBLE
        binding.progressBarShowsWithNextEpisode.visibility = View.INVISIBLE
        binding.textViewShowsContinuing.visibility = View.INVISIBLE
        binding.progressBarShowsContinuing.visibility = View.INVISIBLE

        binding.textViewEpisodesWatched.visibility = View.INVISIBLE
        binding.progressBarEpisodesWatched.visibility = View.INVISIBLE
        binding.textViewEpisodesRuntime.visibility = View.INVISIBLE

        binding.textViewMoviesWatchlist.visibility = View.INVISIBLE
        binding.progressBarMoviesWatchlist.visibility = View.INVISIBLE
        binding.textViewMoviesWatched.visibility = View.INVISIBLE
        binding.progressBarMoviesWatched.visibility = View.INVISIBLE
        binding.textViewMoviesWatchlistRuntime.visibility = View.INVISIBLE
        binding.textViewMoviesWatchedRuntime.visibility = View.INVISIBLE

        // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
        binding.textViewShows.copyTextToClipboardOnLongClick()
        binding.textViewShowsWithNextEpisode.copyTextToClipboardOnLongClick()
        binding.textViewShowsContinuing.copyTextToClipboardOnLongClick()
        binding.textViewEpisodes.copyTextToClipboardOnLongClick()
        binding.textViewEpisodesWatched.copyTextToClipboardOnLongClick()
        binding.textViewEpisodesRuntime.copyTextToClipboardOnLongClick()
        binding.textViewMovies.copyTextToClipboardOnLongClick()
        binding.textViewMoviesWatchlist.copyTextToClipboardOnLongClick()
        binding.textViewMoviesWatched.copyTextToClipboardOnLongClick()
        binding.textViewMoviesWatchlistRuntime.copyTextToClipboardOnLongClick()
        binding.textViewMoviesWatchedRuntime.copyTextToClipboardOnLongClick()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)

        model = ViewModelProvider(this).get(StatsViewModel::class.java)
        model.statsData.observe(viewLifecycleOwner, Observer { this.handleStatsUpdate(it) })
        loadStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        // guard against not attached to activity
        if (!isAdded) {
            return
        }

        inflater.inflate(R.menu.stats_menu, menu)

        menu.findItem(R.id.menu_action_stats_filter_specials).isChecked =
            DisplaySettings.isHidingSpecials(activity)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == R.id.menu_action_stats_share) {
            shareStats()
            return true
        }
        if (itemId == R.id.menu_action_stats_filter_specials) {
            PreferenceManager.getDefaultSharedPreferences(activity).edit()
                .putBoolean(DisplaySettings.KEY_HIDE_SPECIALS, !item.isChecked)
                .apply()

            requireActivity().invalidateOptionsMenu()
            loadStats()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun loadStats() {
        model.statsData.loadStats()
    }

    private fun handleStatsUpdate(event: StatsLiveData.StatsUpdateEvent) {
        if (!isAdded) {
            return
        }
        currentStats = event.stats
        hasFinalValues = event.finalValues
        updateStats(event.stats, event.finalValues, event.successful)
    }

    private fun updateStats(
        stats: StatsLiveData.Stats, hasFinalValues: Boolean,
        successful: Boolean
    ) {
        val binding = binding ?: return

        // display error if not all stats could be calculated
        binding.errorView.isGone = successful

        val format = NumberFormat.getIntegerInstance()

        // all shows
        binding.textViewShows.text = format.format(stats.shows.toLong())

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
        var watchedDuration = getTimeDuration(stats.episodesWatchedRuntime)
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

        // movies in watchlist
        binding.progressBarMoviesWatchlist.max = stats.movies
        binding.progressBarMoviesWatchlist.progress = stats.moviesWatchlist
        binding.progressBarMoviesWatchlist.visibility = View.VISIBLE

        binding.textViewMoviesWatchlist.text = getString(
            R.string.movies_on_watchlist,
            format.format(stats.moviesWatchlist.toLong())
        )
        binding.textViewMoviesWatchlist.visibility = View.VISIBLE

        // watched movies
        binding.progressBarMoviesWatched.max = stats.movies
        binding.progressBarMoviesWatched.progress = stats.moviesWatched
        binding.progressBarMoviesWatched.visibility = View.VISIBLE

        binding.textViewMoviesWatched.text = getString(
            R.string.movies_watched_format,
            format.format(stats.moviesWatched.toLong())
        )
        binding.textViewMoviesWatched.visibility = View.VISIBLE

        // runtime of movie watchlist
        binding.textViewMoviesWatchlistRuntime.text = getTimeDuration(stats.moviesWatchlistRuntime)
        binding.textViewMoviesWatchlistRuntime.visibility = View.VISIBLE

        // runtime of watched movies
        binding.textViewMoviesWatchedRuntime.text = getTimeDuration(stats.moviesWatchedRuntime)
        binding.textViewMoviesWatchedRuntime.visibility = View.VISIBLE
    }

    private fun getTimeDuration(duration: Long): String {
        var durationCalc = duration
        val days = durationCalc / DateUtils.DAY_IN_MILLIS
        durationCalc %= DateUtils.DAY_IN_MILLIS
        val hours = durationCalc / DateUtils.HOUR_IN_MILLIS
        durationCalc %= DateUtils.HOUR_IN_MILLIS
        val minutes = durationCalc / DateUtils.MINUTE_IN_MILLIS

        val result = StringBuilder()
        if (days != 0L) {
            result.append(
                resources.getQuantityString(
                    R.plurals.days_plural, days.toInt(),
                    days.toInt()
                )
            )
        }
        if (hours != 0L) {
            if (days != 0L) {
                result.append(" ")
            }
            result.append(
                resources.getQuantityString(
                    R.plurals.hours_plural, hours.toInt(),
                    hours.toInt()
                )
            )
        }
        if (minutes != 0L || days == 0L && hours == 0L) {
            if (days != 0L || hours != 0L) {
                result.append(" ")
            }
            result.append(
                resources.getQuantityString(
                    R.plurals.minutes_plural,
                    minutes.toInt(),
                    minutes.toInt()
                )
            )
        }

        return result.toString()
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

        val episodes = format.format(currentStats.episodes.toLong())
        val episodesWatched = getString(
            R.string.episodes_watched,
            format.format(currentStats.episodesWatched.toLong())
        )

        val showStats =
            "${getString(R.string.app_name)} ${getString(R.string.statistics)}\n\n" +
                    "$shows ${getString(R.string.statistics_shows)}\n" +
                    "$showsWithNext\n" +
                    "$showsContinuing\n\n" +
                    "$episodes ${getString(R.string.statistics_episodes)}\n" +
                    "$episodesWatched\n"

        val statsString = StringBuilder(showStats)
        if (currentStats.episodesWatchedRuntime != 0L) {
            var watchedDuration = getTimeDuration(currentStats.episodesWatchedRuntime)
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
        val moviesWatchlistRuntime = getTimeDuration(currentStats.moviesWatchlistRuntime)
        val moviesWatchedRuntime = getTimeDuration(currentStats.moviesWatchedRuntime)

        val movieStats = "$movies ${getString(R.string.statistics_movies)}\n" +
                "$moviesWatched\n" +
                "$moviesWatchedRuntime\n" +
                "$moviesWatchlist\n" +
                "$moviesWatchlistRuntime\n"

        statsString.append(movieStats)

        ShareUtils.startShareIntentChooser(activity, statsString.toString(), R.string.share)
    }
}
