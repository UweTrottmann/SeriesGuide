package com.battlelancer.seriesguide.ui.stats

import android.os.Bundle
import android.preference.PreferenceManager
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import com.battlelancer.seriesguide.R
import com.battlelancer.seriesguide.settings.DisplaySettings
import com.battlelancer.seriesguide.util.ShareUtils
import com.battlelancer.seriesguide.util.copyTextToClipboardOnLongClick
import com.battlelancer.seriesguide.widgets.EmptyView
import java.text.NumberFormat
import java.util.Locale

/**
 * Displays some statistics about the users show database, e.g. number of shows, episodes, share of
 * watched episodes, etc.
 */
class StatsFragment : Fragment() {

    @BindView(R.id.emptyViewStats)
    lateinit var errorView: EmptyView

    @BindView(R.id.textViewStatsShows)
    lateinit var textViewShows: TextView
    @BindView(R.id.textViewStatsShowsWithNext)
    lateinit var textViewShowsWithNextEpisode: TextView
    @BindView(R.id.progressBarStatsShowsWithNext)
    lateinit var progressBarShowsWithNextEpisode: ProgressBar
    @BindView(R.id.textViewStatsShowsContinuing)
    lateinit var textViewShowsContinuing: TextView
    @BindView(R.id.progressBarStatsShowsContinuing)
    lateinit var progressBarShowsContinuing: ProgressBar

    @BindView(R.id.textViewStatsEpisodes)
    lateinit var textViewEpisodes: TextView
    @BindView(R.id.textViewStatsEpisodesWatched)
    lateinit var textViewEpisodesWatched: TextView
    @BindView(R.id.progressBarStatsEpisodesWatched)
    lateinit var progressBarEpisodesWatched: ProgressBar
    @BindView(R.id.textViewStatsEpisodesRuntime)
    lateinit var textViewEpisodesRuntime: TextView
    @BindView(R.id.progressBarStatsEpisodesRuntime)
    lateinit var progressBarEpisodesRuntime: ProgressBar

    @BindView(R.id.textViewStatsMovies)
    lateinit var textViewMovies: TextView
    @BindView(R.id.textViewStatsMoviesWatchlist)
    lateinit var textViewMoviesWatchlist: TextView
    @BindView(R.id.progressBarStatsMoviesWatchlist)
    lateinit var progressBarMoviesWatchlist: ProgressBar
    @BindView(R.id.textViewStatsMoviesWatched)
    lateinit var textViewMoviesWatched: TextView
    @BindView(R.id.progressBarStatsMoviesWatched)
    lateinit var progressBarMoviesWatched: ProgressBar
    @BindView(R.id.textViewStatsMoviesWatchlistRuntime)
    lateinit var textViewMoviesWatchlistRuntime: TextView
    @BindView(R.id.textViewStatsMoviesWatchedRuntime)
    lateinit var textViewMoviesWatchedRuntime: TextView

    private lateinit var unbinder: Unbinder
    private lateinit var model: StatsViewModel
    private var currentStats: StatsLiveData.Stats? = null
    private var hasFinalValues: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_stats, container, false)
        unbinder = ButterKnife.bind(this, view)

        errorView.visibility = View.GONE
        errorView.setButtonClickListener { loadStats() }

        // set some views invisible so they can be animated in once stats are computed
        textViewShowsWithNextEpisode.visibility = View.INVISIBLE
        progressBarShowsWithNextEpisode.visibility = View.INVISIBLE
        textViewShowsContinuing.visibility = View.INVISIBLE
        progressBarShowsContinuing.visibility = View.INVISIBLE

        textViewEpisodesWatched.visibility = View.INVISIBLE
        progressBarEpisodesWatched.visibility = View.INVISIBLE
        textViewEpisodesRuntime.visibility = View.INVISIBLE

        textViewMoviesWatchlist.visibility = View.INVISIBLE
        progressBarMoviesWatchlist.visibility = View.INVISIBLE
        textViewMoviesWatched.visibility = View.INVISIBLE
        progressBarMoviesWatched.visibility = View.INVISIBLE
        textViewMoviesWatchlistRuntime.visibility = View.INVISIBLE
        textViewMoviesWatchedRuntime.visibility = View.INVISIBLE

        // set up long-press to copy text to clipboard (d-pad friendly vs text selection)
        textViewShows.copyTextToClipboardOnLongClick()
        textViewShowsWithNextEpisode.copyTextToClipboardOnLongClick()
        textViewShowsContinuing.copyTextToClipboardOnLongClick()
        textViewEpisodes.copyTextToClipboardOnLongClick()
        textViewEpisodesWatched.copyTextToClipboardOnLongClick()
        textViewEpisodesRuntime.copyTextToClipboardOnLongClick()
        textViewMovies.copyTextToClipboardOnLongClick()
        textViewMoviesWatchlist.copyTextToClipboardOnLongClick()
        textViewMoviesWatched.copyTextToClipboardOnLongClick()
        textViewMoviesWatchlistRuntime.copyTextToClipboardOnLongClick()
        textViewMoviesWatchedRuntime.copyTextToClipboardOnLongClick()

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setHasOptionsMenu(true)

        model = ViewModelProviders.of(this).get(StatsViewModel::class.java)
        model.statsData.observe(this, Observer { this.handleStatsUpdate(it) })
        loadStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        unbinder.unbind()
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

            activity!!.invalidateOptionsMenu()
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
        // display error if not all stats could be calculated
        errorView.isGone = successful

        val format = NumberFormat.getIntegerInstance()

        // all shows
        textViewShows.text = format.format(stats.shows.toLong())

        // shows with next episodes
        progressBarShowsWithNextEpisode.apply {
            max = stats.shows
            progress = stats.showsWithNextEpisodes
            visibility = View.VISIBLE
        }

        textViewShowsWithNextEpisode.apply {
            text = getString(
                R.string.shows_with_next,
                format.format(stats.showsWithNextEpisodes.toLong())
            ).toUpperCase(Locale.getDefault())
            visibility = View.VISIBLE
        }

        // continuing shows
        progressBarShowsContinuing.apply {
            max = stats.shows
            progress = stats.showsContinuing
            visibility = View.VISIBLE
        }

        textViewShowsContinuing.text = getString(
            R.string.shows_continuing,
            format.format(stats.showsContinuing.toLong())
        ).toUpperCase(Locale.getDefault())
        textViewShowsContinuing.visibility = View.VISIBLE

        // all episodes
        textViewEpisodes.text = format.format(stats.episodes.toLong())

        // watched episodes
        progressBarEpisodesWatched.max = stats.episodes
        progressBarEpisodesWatched.progress = stats.episodesWatched
        progressBarEpisodesWatched.visibility = View.VISIBLE

        textViewEpisodesWatched.text = getString(
            R.string.episodes_watched,
            format.format(stats.episodesWatched.toLong())
        ).toUpperCase(Locale.getDefault())
        textViewEpisodesWatched.visibility = View.VISIBLE

        // episode runtime
        var watchedDuration = getTimeDuration(stats.episodesWatchedRuntime)
        if (!hasFinalValues) {
            // showing minimum (= not the final value)
            watchedDuration = "> $watchedDuration"
        }
        textViewEpisodesRuntime.text = watchedDuration
        textViewEpisodesRuntime.visibility = View.VISIBLE
        progressBarEpisodesRuntime.visibility = if (successful)
            if (hasFinalValues) View.GONE else View.VISIBLE
        else
            View.GONE

        // movies
        textViewMovies.text = format.format(stats.movies.toLong())

        // movies in watchlist
        progressBarMoviesWatchlist.max = stats.movies
        progressBarMoviesWatchlist.progress = stats.moviesWatchlist
        progressBarMoviesWatchlist.visibility = View.VISIBLE

        textViewMoviesWatchlist.text = getString(
            R.string.movies_on_watchlist,
            format.format(stats.moviesWatchlist.toLong())
        ).toUpperCase(Locale.getDefault())
        textViewMoviesWatchlist.visibility = View.VISIBLE

        // watched movies
        progressBarMoviesWatched.max = stats.movies
        progressBarMoviesWatched.progress = stats.moviesWatched
        progressBarMoviesWatched.visibility = View.VISIBLE

        textViewMoviesWatched.text = getString(
            R.string.movies_watched_format,
            format.format(stats.moviesWatched.toLong())
        ).toUpperCase(Locale.getDefault())
        textViewMoviesWatched.visibility = View.VISIBLE

        // runtime of movie watchlist
        textViewMoviesWatchlistRuntime.text = getTimeDuration(stats.moviesWatchlistRuntime)
        textViewMoviesWatchlistRuntime.visibility = View.VISIBLE

        // runtime of watched movies
        textViewMoviesWatchedRuntime.text = getTimeDuration(stats.moviesWatchedRuntime)
        textViewMoviesWatchedRuntime.visibility = View.VISIBLE
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
            statsString.append("$watchedDuration ${getString(R.string.runtime_all_episodes)}\n")
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
                "$moviesWatchlist\n" +
                "$moviesWatched\n" +
                "$moviesWatchlistRuntime ${getString(R.string.runtime_movies_watchlist)}\n" +
                "$moviesWatchedRuntime ${getString(R.string.movies_watched)}"

        statsString.append(movieStats)

        ShareUtils.startShareIntentChooser(activity, statsString.toString(), R.string.share)
    }
}
