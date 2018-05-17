package com.battlelancer.seriesguide.sync;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.preference.PreferenceManager;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.ui.search.SearchResult;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.ListsActivity;
import com.battlelancer.seriesguide.ui.movies.MovieTools;
import com.battlelancer.seriesguide.util.TaskManager;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.greenrobot.eventbus.EventBus;

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

    /**
     * Syncs episodes, shows and movies with Hexagon.
     *
     * <p> Merges shows, episodes and movies after a sign-in. Consecutive syncs will only download
     * changes to shows, episodes and movies.
     */
    public SgSyncAdapter.UpdateResult sync(HashSet<Integer> existingShows,
            HashMap<Integer, SearchResult> newShows) {
        //// EPISODES
        progress.publish(SyncProgress.Step.HEXAGON_EPISODES);
        boolean syncEpisodesSuccessful = syncEpisodes();
        if (!syncEpisodesSuccessful) {
            progress.recordError();
        }

        //// SHOWS
        progress.publish(SyncProgress.Step.HEXAGON_SHOWS);
        boolean syncShowsSuccessful = syncShows(existingShows, newShows);
        if (!syncShowsSuccessful) {
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
                && syncShowsSuccessful
                && syncMoviesSuccessful
                && syncListsSuccessful;

        return success ? SgSyncAdapter.UpdateResult.SUCCESS : SgSyncAdapter.UpdateResult.INCOMPLETE;
    }

    private boolean syncEpisodes() {
        // get shows that need episode merging
        Cursor query = context.getContentResolver().query(SeriesGuideContract.Shows.CONTENT_URI,
                new String[] { SeriesGuideContract.Shows._ID },
                SeriesGuideContract.Shows.HEXAGON_MERGE_COMPLETE + "=0",
                null, null);
        if (query == null) {
            return false;
        }

        // try merging episodes for them
        boolean mergeSuccessful = true;
        HexagonEpisodeSync episodeSync = new HexagonEpisodeSync(context, hexagonTools);
        while (query.moveToNext()) {
            // abort if connection is lost
            if (!AndroidUtils.isNetworkConnected(context)) {
                return false;
            }

            int showTvdbId = query.getInt(0);

            boolean success = episodeSync.downloadFlags(showTvdbId);
            if (!success) {
                // try again next time
                mergeSuccessful = false;
                continue;
            }

            success = episodeSync.uploadFlags(showTvdbId);
            if (success) {
                // set merge as completed
                ContentValues values = new ContentValues();
                values.put(SeriesGuideContract.Shows.HEXAGON_MERGE_COMPLETE, 1);
                context.getContentResolver()
                        .update(SeriesGuideContract.Shows.buildShowUri(showTvdbId), values,
                                null, null);
            } else {
                mergeSuccessful = false;
            }
        }
        query.close();

        // download changed episodes and update properties on existing episodes
        boolean changedDownloadSuccessful = episodeSync.downloadChangedFlags();

        return mergeSuccessful && changedDownloadSuccessful;
    }

    private boolean syncShows(HashSet<Integer> existingShows,
            HashMap<Integer, SearchResult> newShows) {
        boolean hasMergedShows = HexagonSettings.hasMergedShows(context);

        // download shows and apply property changes (if merging only overwrite some properties)
        HexagonShowSync showSync = new HexagonShowSync(context, hexagonTools);
        boolean downloadSuccessful = showSync.download(existingShows, newShows, hasMergedShows);
        if (!downloadSuccessful) {
            return false;
        }

        // if merge required, upload all shows to Hexagon
        if (!hasMergedShows) {
            boolean uploadSuccessful = showSync.uploadAll();
            if (!uploadSuccessful) {
                return false;
            }
        }

        // add new shows
        if (newShows.size() > 0) {
            List<SearchResult> newShowsList = new LinkedList<>(newShows.values());
            TaskManager.getInstance().performAddTask(context, newShowsList, true, !hasMergedShows);
        } else if (!hasMergedShows) {
            // set shows as merged
            HexagonSettings.setHasMergedShows(context, true);
        }

        return true;
    }

    private boolean syncMovies() {
        boolean hasMergedMovies = HexagonSettings.hasMergedMovies(context);

        // download movies and apply property changes, build list of new movies
        Set<Integer> newCollectionMovies = new HashSet<>();
        Set<Integer> newWatchlistMovies = new HashSet<>();
        HexagonMovieSync movieSync = new HexagonMovieSync(context, hexagonTools);
        boolean downloadSuccessful = movieSync.download(newCollectionMovies, newWatchlistMovies,
                hasMergedMovies);
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
        boolean addingSuccessful = movieTools.addMovies(newCollectionMovies, newWatchlistMovies);
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

        // notify lists activity
        EventBus.getDefault().post(new ListsActivity.ListsChangedEvent());

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
