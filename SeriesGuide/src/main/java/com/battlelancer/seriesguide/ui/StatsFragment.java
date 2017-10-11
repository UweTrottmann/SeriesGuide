package com.battlelancer.seriesguide.ui;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.StatsLiveData.Stats;
import com.battlelancer.seriesguide.ui.StatsLiveData.StatsUpdateEvent;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.widgets.EmptyView;
import java.util.Locale;

/**
 * Displays some statistics about the users show database, e.g. number of shows, episodes, share of
 * watched episodes, etc.
 */
public class StatsFragment extends Fragment {

    @BindView(R.id.emptyViewStats) EmptyView errorView;

    @BindView(R.id.textViewStatsShows) TextView mShowCount;
    @BindView(R.id.textViewStatsShowsWithNext) TextView mShowsWithNextEpisode;
    @BindView(R.id.progressBarStatsShowsWithNext) ProgressBar mProgressShowsWithNextEpisode;
    @BindView(R.id.textViewStatsShowsContinuing) TextView mShowsContinuing;
    @BindView(R.id.progressBarStatsShowsContinuing) ProgressBar mProgressShowsContinuing;

    @BindView(R.id.textViewStatsEpisodes) TextView mEpisodeCount;
    @BindView(R.id.textViewStatsEpisodesWatched) TextView mEpisodesWatched;
    @BindView(R.id.progressBarStatsEpisodesWatched) ProgressBar mProgressEpisodesWatched;
    @BindView(R.id.textViewStatsEpisodesRuntime) TextView mEpisodesRuntime;
    @BindView(R.id.progressBarStatsEpisodesRuntime) ProgressBar mProgressEpisodesRuntime;

    @BindView(R.id.textViewStatsMovies) TextView mMovieCount;
    @BindView(R.id.textViewStatsMoviesWatchlist) TextView mMoviesWatchlist;
    @BindView(R.id.progressBarStatsMoviesWatchlist) ProgressBar mProgressMoviesWatchlist;
    @BindView(R.id.textViewStatsMoviesWatchlistRuntime) TextView mMoviesWatchlistRuntime;

    private Unbinder unbinder;
    private StatsViewModel model;
    private Stats currentStats;
    private boolean hasFinalValues;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_stats, container, false);
        unbinder = ButterKnife.bind(this, v);

        errorView.setVisibility(View.GONE);
        errorView.setButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadStats();
            }
        });

        // set some views invisible so they can be animated in once stats are computed
        mShowsWithNextEpisode.setVisibility(View.INVISIBLE);
        mProgressShowsWithNextEpisode.setVisibility(View.INVISIBLE);
        mShowsContinuing.setVisibility(View.INVISIBLE);
        mProgressShowsContinuing.setVisibility(View.INVISIBLE);

        mEpisodesWatched.setVisibility(View.INVISIBLE);
        mProgressEpisodesWatched.setVisibility(View.INVISIBLE);
        mEpisodesRuntime.setVisibility(View.INVISIBLE);

        mMoviesWatchlist.setVisibility(View.INVISIBLE);
        mProgressMoviesWatchlist.setVisibility(View.INVISIBLE);
        mMoviesWatchlistRuntime.setVisibility(View.INVISIBLE);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        model = ViewModelProviders.of(this).get(StatsViewModel.class);
        model.getStatsData().observe(this, new Observer<StatsUpdateEvent>() {
            @Override
            public void onChanged(@Nullable StatsUpdateEvent statsUpdateEvent) {
                handleStatsUpdate(statsUpdateEvent);
            }
        });
        loadStats();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        // guard against not attached to activity
        if (!isAdded()) {
            return;
        }

        inflater.inflate(R.menu.stats_menu, menu);

        menu.findItem(R.id.menu_action_stats_filter_specials)
                .setChecked(DisplaySettings.isHidingSpecials(getActivity()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_action_stats_share) {
            shareStats();
            return true;
        }
        if (itemId == R.id.menu_action_stats_filter_specials) {
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                    .putBoolean(DisplaySettings.KEY_HIDE_SPECIALS, !item.isChecked())
                    .apply();
            getActivity().invalidateOptionsMenu();
            loadStats();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadStats() {
        model.getStatsData().loadStats();
    }

    private void handleStatsUpdate(StatsUpdateEvent event) {
        if (!isAdded()) {
            return;
        }
        currentStats = event.stats;
        hasFinalValues = event.finalValues;
        updateStats(event.stats, event.finalValues, event.successful);
    }

    private void updateStats(@NonNull Stats stats, boolean hasFinalValues, boolean successful) {
        // display error if not all stats could be calculated
        errorView.setVisibility(successful ? View.GONE : View.VISIBLE);

        // all shows
        mShowCount.setText(String.valueOf(stats.shows()));

        // shows with next episodes
        mProgressShowsWithNextEpisode.setMax(stats.shows());
        mProgressShowsWithNextEpisode.setProgress(stats.showsWithNextEpisodes());
        mProgressShowsWithNextEpisode.setVisibility(View.VISIBLE);

        mShowsWithNextEpisode.setText(getString(R.string.shows_with_next,
                stats.showsWithNextEpisodes()).toUpperCase(Locale.getDefault()));
        mShowsWithNextEpisode.setVisibility(View.VISIBLE);

        // continuing shows
        mProgressShowsContinuing.setMax(stats.shows());
        mProgressShowsContinuing.setProgress(stats.showsContinuing());
        mProgressShowsContinuing.setVisibility(View.VISIBLE);

        mShowsContinuing.setText(getString(R.string.shows_continuing,
                stats.showsContinuing()).toUpperCase(Locale.getDefault()));
        mShowsContinuing.setVisibility(View.VISIBLE);

        // all episodes
        mEpisodeCount.setText(String.valueOf(stats.episodes()));

        // watched episodes
        mProgressEpisodesWatched.setMax(stats.episodes());
        mProgressEpisodesWatched.setProgress(stats.episodesWatched());
        mProgressEpisodesWatched.setVisibility(View.VISIBLE);

        mEpisodesWatched.setText(getString(R.string.episodes_watched,
                stats.episodesWatched()).toUpperCase(Locale.getDefault()));
        mEpisodesWatched.setVisibility(View.VISIBLE);

        // episode runtime
        String watchedDuration = getTimeDuration(stats.episodesWatchedRuntime());
        if (!hasFinalValues) {
            // showing minimum (= not the final value)
            watchedDuration = "> " + watchedDuration;
        }
        mEpisodesRuntime.setText(watchedDuration);
        mEpisodesRuntime.setVisibility(View.VISIBLE);
        mProgressEpisodesRuntime.setVisibility(successful ?
                (hasFinalValues ? View.GONE : View.VISIBLE)
                : View.GONE);

        // movies
        mMovieCount.setText(String.valueOf(stats.movies));

        // movies in watchlist
        mProgressMoviesWatchlist.setMax(stats.movies);
        mProgressMoviesWatchlist.setProgress(stats.moviesWatchlist);
        mProgressMoviesWatchlist.setVisibility(View.VISIBLE);

        mMoviesWatchlist.setText(getString(R.string.movies_on_watchlist,
                stats.moviesWatchlist).toUpperCase(Locale.getDefault()));
        mMoviesWatchlist.setVisibility(View.VISIBLE);

        // runtime of movie watchlist
        mMoviesWatchlistRuntime.setText(getTimeDuration(stats.moviesWatchlistRuntime));
        mMoviesWatchlistRuntime.setVisibility(View.VISIBLE);
    }

    private String getTimeDuration(long duration) {
        long days = duration / DateUtils.DAY_IN_MILLIS;
        duration %= DateUtils.DAY_IN_MILLIS;
        long hours = duration / DateUtils.HOUR_IN_MILLIS;
        duration %= DateUtils.HOUR_IN_MILLIS;
        long minutes = duration / DateUtils.MINUTE_IN_MILLIS;

        StringBuilder result = new StringBuilder();
        if (days != 0) {
            result.append(getResources().getQuantityString(R.plurals.days_plural, (int) days,
                    (int) days));
        }
        if (hours != 0) {
            if (days != 0) {
                result.append(" ");
            }
            result.append(getResources().getQuantityString(R.plurals.hours_plural, (int) hours,
                    (int) hours));
        }
        if (minutes != 0 || (days == 0 && hours == 0)) {
            if (days != 0 || hours != 0) {
                result.append(" ");
            }
            result.append(getResources().getQuantityString(R.plurals.minutes_plural,
                    (int) minutes,
                    (int) minutes));
        }

        return result.toString();
    }

    private void shareStats() {
        if (currentStats == null) {
            return;
        }

        StringBuilder statsString = new StringBuilder();
        statsString.append(getString(R.string.app_name))
                .append(" ")
                .append(getString(R.string.statistics));
        statsString.append("\n");
        statsString.append("\n");
        // shows
        statsString.append(currentStats.shows())
                .append(" ")
                .append(getString(R.string.statistics_shows));
        statsString.append("\n");
        statsString.append(
                getString(R.string.shows_with_next, currentStats.showsWithNextEpisodes()));
        statsString.append("\n");
        statsString.append(getString(R.string.shows_continuing, currentStats.showsContinuing()));
        statsString.append("\n");
        statsString.append("\n");
        // episodes
        statsString.append(currentStats.episodes()).append(" ").append(
                getString(R.string.statistics_episodes));
        statsString.append("\n");
        statsString.append(getString(R.string.episodes_watched, currentStats.episodesWatched()));
        statsString.append("\n");
        if (currentStats.episodesWatchedRuntime() != 0) {
            String watchedDuration = getTimeDuration(currentStats.episodesWatchedRuntime());
            if (!hasFinalValues) {
                // showing minimum (= not the final value)
                watchedDuration = "> " + watchedDuration;
            }
            statsString.append(watchedDuration)
                    .append(" ")
                    .append(getString(R.string.runtime_all_episodes));
            statsString.append("\n");
        }
        statsString.append("\n");
        // movies
        statsString.append(currentStats.movies)
                .append(" ")
                .append(getString(R.string.statistics_movies));
        statsString.append("\n");
        statsString.append(getString(R.string.movies_on_watchlist, currentStats.moviesWatchlist));
        statsString.append("\n");
        statsString.append(getTimeDuration(currentStats.moviesWatchlistRuntime))
                .append(" ")
                .append(getString(R.string.runtime_movies_watchlist));

        ShareUtils.startShareIntentChooser(getActivity(), statsString.toString(), R.string.share);
    }
}
