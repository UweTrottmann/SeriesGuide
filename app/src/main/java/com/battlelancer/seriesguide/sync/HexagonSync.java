package com.battlelancer.seriesguide.sync;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.provider.SgShow2Helper;
import com.battlelancer.seriesguide.provider.SgShow2Ids;
import com.battlelancer.seriesguide.ui.movies.MovieTools;
import com.battlelancer.seriesguide.ui.search.SearchResult;
import com.battlelancer.seriesguide.util.TaskManager;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HexagonSync {

    private Context context;
    private HexagonTools hexagonTools;
    private MovieTools movieTools;
    private SyncProgress progress;

    HexagonSync(Context context, HexagonTools hexagonTools,
            MovieTools movieTools, SyncProgress progress) {
        this.context = context;
        this.hexagonTools = hexagonTools;
        this.movieTools = movieTools;
        this.progress = progress;
    }

    public static class HexagonResult {
        public final boolean hasAddedShows;
        public final boolean success;
        public HexagonResult(boolean hasAddedShows, boolean success) {
            this.hasAddedShows = hasAddedShows;
            this.success = success;
        }
    }

    /**
     * Syncs episodes, shows and movies with Hexagon.
     *
     * <p> Merges shows, episodes and movies after a sign-in. Consecutive syncs will only download
     * changes to shows, episodes and movies.
     */
    public HexagonResult sync() {
        Map<Integer, Long> tmdbIdsToShowIds = SgApp.getServicesComponent(context).showTools()
                .getTmdbIdsToShowIds();

        //// EPISODES
        progress.publish(SyncProgress.Step.HEXAGON_EPISODES);
        boolean syncEpisodesSuccessful = syncEpisodes(tmdbIdsToShowIds);
        if (!syncEpisodesSuccessful) {
            progress.recordError();
        }

        //// SHOWS
        progress.publish(SyncProgress.Step.HEXAGON_SHOWS);
        HexagonResult syncShowsResult = syncShows(tmdbIdsToShowIds);
        if (!syncShowsResult.success) {
            progress.recordError();
        }

        //// MOVIES
        progress.publish(SyncProgress.Step.HEXAGON_MOVIES);
        boolean syncMoviesSuccessful = syncMovies();
        if (!syncMoviesSuccessful) {
            progress.recordError();
        }

        //// LISTS
        progress.publish(SyncProgress.Step.HEXAGON_LISTS);
        boolean syncListsSuccessful = syncLists();
        if (!syncListsSuccessful) {
            progress.recordError();
        }

        boolean success = syncEpisodesSuccessful
                && syncShowsResult.success
                && syncMoviesSuccessful
                && syncListsSuccessful;

        return new HexagonResult(syncShowsResult.hasAddedShows, success);
    }

    private boolean syncEpisodes(@NonNull Map<Integer, Long> tmdbIdsToShowIds) {
        // get shows that need episode merging
        SgShow2Helper helper = SgRoomDatabase.getInstance(context).sgShow2Helper();
        List<SgShow2Ids> showsToMerge = helper.getHexagonMergeNotCompleted();

        // try merging episodes for them
        boolean mergeSuccessful = true;
        HexagonEpisodeSync episodeSync = new HexagonEpisodeSync(context, hexagonTools);
        for (SgShow2Ids show : showsToMerge) {
            // abort if connection is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                return false;
            }

            // TMDB ID is required, legacy shows with TVDB only data will no longer be synced.
            Integer showTmdbId = show.getTmdbId();
            if (showTmdbId == null || showTmdbId == 0) continue;

            boolean success = episodeSync.downloadFlags(show.getId(), showTmdbId, show.getTvdbId());
            if (!success) {
                // try again next time
                mergeSuccessful = false;
                continue;
            }

            success = episodeSync.uploadFlags(show.getId(), showTmdbId);
            if (success) {
                // set merge as completed
                helper.setHexagonMergeCompleted(show.getId());
            } else {
                mergeSuccessful = false;
            }
        }

        // download changed episodes and update properties on existing episodes
        boolean changedDownloadSuccessful = episodeSync.downloadChangedFlags(tmdbIdsToShowIds);

        return mergeSuccessful && changedDownloadSuccessful;
    }

    private HexagonResult syncShows(Map<Integer, Long> tmdbIdsToShowIds) {
        boolean hasMergedShows = HexagonSettings.hasMergedShows(context);

        // download shows and apply property changes (if merging only overwrite some properties)
        HexagonShowSync showSync = new HexagonShowSync(context, hexagonTools);
        HashMap<Integer, SearchResult> newShows = new HashMap<>();
        boolean downloadSuccessful = showSync.download(tmdbIdsToShowIds, newShows, hasMergedShows);
        if (!downloadSuccessful) {
            return new HexagonResult(false, false);
        }

        // if merge required, upload all shows to Hexagon
        if (!hasMergedShows) {
            boolean uploadSuccessful = showSync.uploadAll();
            if (!uploadSuccessful) {
                return new HexagonResult(false, false);
            }
        }

        // add new shows
        boolean addNewShows = !newShows.isEmpty();
        if (addNewShows) {
            List<SearchResult> newShowsList = new LinkedList<>(newShows.values());
            TaskManager.getInstance().performAddTask(context, newShowsList, true, !hasMergedShows);
        } else if (!hasMergedShows) {
            // set shows as merged
            HexagonSettings.setHasMergedShows(context, true);
        }

        return new HexagonResult(addNewShows, true);
    }

    private boolean syncMovies() {
        boolean hasMergedMovies = HexagonSettings.hasMergedMovies(context);

        // download movies and apply property changes, build list of new movies
        Set<Integer> newCollectionMovies = new HashSet<>();
        Set<Integer> newWatchlistMovies = new HashSet<>();
        Map<Integer, Integer> newWatchedMoviesToPlays = new HashMap<>();
        HexagonMovieSync movieSync = new HexagonMovieSync(context, hexagonTools);
        boolean downloadSuccessful = movieSync.download(
                newCollectionMovies,
                newWatchlistMovies,
                newWatchedMoviesToPlays,
                hasMergedMovies
        );
        if (!downloadSuccessful) {
            return false;
        }

        if (!hasMergedMovies) {
            boolean uploadSuccessful = movieSync.uploadAll();
            if (!uploadSuccessful) {
                return false;
            }
        }

        // add new movies with the just downloaded properties
        boolean addingSuccessful = movieTools
                .addMovies(newCollectionMovies, newWatchlistMovies, newWatchedMoviesToPlays);
        if (!hasMergedMovies) {
            // ensure all missing movies from Hexagon are added before merge is complete
            if (!addingSuccessful) {
                return false;
            }
            // set movies as merged
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putBoolean(HexagonSettings.KEY_MERGED_MOVIES, true)
                    .apply();
        }

        return addingSuccessful;
    }

    private boolean syncLists() {
        boolean hasMergedLists = HexagonSettings.hasMergedLists(context);

        HexagonListsSync listsSync = new HexagonListsSync(context, hexagonTools);
        if (!listsSync.download(hasMergedLists)) {
            return false;
        }

        if (hasMergedLists) {
            // on regular syncs, remove lists gone from hexagon
            if (!listsSync.pruneRemovedLists()) {
                return false;
            }
        } else {
            // upload all lists on initial data merge
            if (!listsSync.uploadAll()) {
                return false;
            }
        }

        if (!hasMergedLists) {
            // set lists as merged
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putBoolean(HexagonSettings.KEY_MERGED_LISTS, true)
                    .apply();
        }

        return true;
    }
}
