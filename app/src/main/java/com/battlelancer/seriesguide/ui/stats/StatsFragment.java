package com.battlelancer.seriesguide.ui.stats;

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
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.widgets.EmptyView;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Displays some statistics about the users show database, e.g. number of shows, episodes, share of
 * watched episodes, etc.
 */
public class StatsFragment extends Fragment {

    @BindView(R.id.emptyViewStats) EmptyView errorView;

    @BindView(R.id.textViewStatsShows) TextView textViewShows;
    @BindView(R.id.textViewStatsShowsWithNext) TextView textViewShowsWithNextEpisode;
    @BindView(R.id.progressBarStatsShowsWithNext) ProgressBar progressBarShowsWithNextEpisode;
    @BindView(R.id.textViewStatsShowsContinuing) TextView textViewShowsContinuing;
    @BindView(R.id.progressBarStatsShowsContinuing) ProgressBar progressBarShowsContinuing;

    @BindView(R.id.textViewStatsEpisodes) TextView textViewEpisodes;
    @BindView(R.id.textViewStatsEpisodesWatched) TextView textViewEpisodesWatched;
    @BindView(R.id.progressBarStatsEpisodesWatched) ProgressBar progressBarEpisodesWatched;
    @BindView(R.id.textViewStatsEpisodesRuntime) TextView textViewEpisodesRuntime;
    @BindView(R.id.progressBarStatsEpisodesRuntime) ProgressBar progressBarEpisodesRuntime;

    @BindView(R.id.textViewStatsMovies) TextView textViewMovies;
    @BindView(R.id.textViewStatsMoviesWatchlist) TextView textViewMoviesWatchlist;
    @BindView(R.id.progressBarStatsMoviesWatchlist) ProgressBar progressBarMoviesWatchlist;
    @BindView(R.id.textViewStatsMoviesWatchlistRuntime) TextView textViewMoviesWatchlistRuntime;

    private Unbinder unbinder;
    private StatsViewModel model;
    private StatsLiveData.Stats currentStats;
    private boolean hasFinalValues;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
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
        textViewShowsWithNextEpisode.setVisibility(View.INVISIBLE);
        progressBarShowsWithNextEpisode.setVisibility(View.INVISIBLE);
        textViewShowsContinuing.setVisibility(View.INVISIBLE);
        progressBarShowsContinuing.setVisibility(View.INVISIBLE);

        textViewEpisodesWatched.setVisibility(View.INVISIBLE);
        progressBarEpisodesWatched.setVisibility(View.INVISIBLE);
        textViewEpisodesRuntime.setVisibility(View.INVISIBLE);

        textViewMoviesWatchlist.setVisibility(View.INVISIBLE);
        progressBarMoviesWatchlist.setVisibility(View.INVISIBLE);
        textViewMoviesWatchlistRuntime.setVisibility(View.INVISIBLE);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        model = ViewModelProviders.of(this).get(StatsViewModel.class);
        model.getStatsData().observe(this, new Observer<StatsLiveData.StatsUpdateEvent>() {
            @Override
            public void onChanged(@Nullable StatsLiveData.StatsUpdateEvent statsUpdateEvent) {
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
            //noinspection ConstantConditions always attached to activity
            getActivity().invalidateOptionsMenu();
            loadStats();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadStats() {
        model.getStatsData().loadStats();
    }

    private void handleStatsUpdate(StatsLiveData.StatsUpdateEvent event) {
        if (!isAdded()) {
            return;
        }
        currentStats = event.stats;
        hasFinalValues = event.finalValues;
        updateStats(event.stats, event.finalValues, event.successful);
    }

    private void updateStats(@NonNull StatsLiveData.Stats stats, boolean hasFinalValues,
            boolean successful) {
        // display error if not all stats could be calculated
        errorView.setVisibility(successful ? View.GONE : View.VISIBLE);

        NumberFormat format = NumberFormat.getIntegerInstance();

        // all shows
        textViewShows.setText(format.format(stats.shows));

        // shows with next episodes
        progressBarShowsWithNextEpisode.setMax(stats.shows);
        progressBarShowsWithNextEpisode.setProgress(stats.showsWithNextEpisodes);
        progressBarShowsWithNextEpisode.setVisibility(View.VISIBLE);

        textViewShowsWithNextEpisode.setText(getString(R.string.shows_with_next,
                format.format(stats.showsWithNextEpisodes)).toUpperCase(Locale.getDefault()));
        textViewShowsWithNextEpisode.setVisibility(View.VISIBLE);

        // continuing shows
        progressBarShowsContinuing.setMax(stats.shows);
        progressBarShowsContinuing.setProgress(stats.showsContinuing);
        progressBarShowsContinuing.setVisibility(View.VISIBLE);

        textViewShowsContinuing.setText(getString(R.string.shows_continuing,
                format.format(stats.showsContinuing)).toUpperCase(Locale.getDefault()));
        textViewShowsContinuing.setVisibility(View.VISIBLE);

        // all episodes
        textViewEpisodes.setText(format.format(stats.episodes));

        // watched episodes
        progressBarEpisodesWatched.setMax(stats.episodes);
        progressBarEpisodesWatched.setProgress(stats.episodesWatched);
        progressBarEpisodesWatched.setVisibility(View.VISIBLE);

        textViewEpisodesWatched.setText(getString(R.string.episodes_watched,
                format.format(stats.episodesWatched)).toUpperCase(Locale.getDefault()));
        textViewEpisodesWatched.setVisibility(View.VISIBLE);

        // episode runtime
        String watchedDuration = getTimeDuration(stats.episodesWatchedRuntime);
        if (!hasFinalValues) {
            // showing minimum (= not the final value)
            watchedDuration = "> " + watchedDuration;
        }
        textViewEpisodesRuntime.setText(watchedDuration);
        textViewEpisodesRuntime.setVisibility(View.VISIBLE);
        progressBarEpisodesRuntime.setVisibility(successful ?
                (hasFinalValues ? View.GONE : View.VISIBLE)
                : View.GONE);

        // movies
        textViewMovies.setText(format.format(stats.movies));

        // movies in watchlist
        progressBarMoviesWatchlist.setMax(stats.movies);
        progressBarMoviesWatchlist.setProgress(stats.moviesWatchlist);
        progressBarMoviesWatchlist.setVisibility(View.VISIBLE);

        textViewMoviesWatchlist.setText(getString(R.string.movies_on_watchlist,
                format.format(stats.moviesWatchlist)).toUpperCase(Locale.getDefault()));
        textViewMoviesWatchlist.setVisibility(View.VISIBLE);

        // runtime of movie watchlist
        textViewMoviesWatchlistRuntime.setText(getTimeDuration(stats.moviesWatchlistRuntime));
        textViewMoviesWatchlistRuntime.setVisibility(View.VISIBLE);
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

        NumberFormat format = NumberFormat.getIntegerInstance();

        StringBuilder statsString = new StringBuilder();
        statsString.append(getString(R.string.app_name))
                .append(" ")
                .append(getString(R.string.statistics));
        statsString.append("\n");
        statsString.append("\n");
        // shows
        statsString.append(format.format(currentStats.shows))
                .append(" ")
                .append(getString(R.string.statistics_shows));
        statsString.append("\n");
        statsString.append(getString(R.string.shows_with_next,
                format.format(currentStats.showsWithNextEpisodes)));
        statsString.append("\n");
        statsString.append(getString(R.string.shows_continuing,
                format.format(currentStats.showsContinuing)));
        statsString.append("\n");
        statsString.append("\n");
        // episodes
        statsString.append(format.format(currentStats.episodes)).append(" ").append(
                getString(R.string.statistics_episodes));
        statsString.append("\n");
        statsString.append(getString(R.string.episodes_watched,
                format.format(currentStats.episodesWatched)));
        statsString.append("\n");
        if (currentStats.episodesWatchedRuntime != 0) {
            String watchedDuration = getTimeDuration(currentStats.episodesWatchedRuntime);
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
        statsString.append(format.format(currentStats.movies))
                .append(" ")
                .append(getString(R.string.statistics_movies));
        statsString.append("\n");
        statsString.append(getString(R.string.movies_on_watchlist,
                format.format(currentStats.moviesWatchlist)));
        statsString.append("\n");
        statsString.append(getTimeDuration(currentStats.moviesWatchlistRuntime))
                .append(" ")
                .append(getString(R.string.runtime_movies_watchlist));

        ShareUtils.startShareIntentChooser(getActivity(), statsString.toString(), R.string.share);
    }
}
