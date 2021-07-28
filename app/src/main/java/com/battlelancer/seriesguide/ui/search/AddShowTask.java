package com.battlelancer.seriesguide.ui.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.sync.HexagonEpisodeSync;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.TraktSettings;
import com.battlelancer.seriesguide.traktapi.TraktTools2;
import com.battlelancer.seriesguide.ui.shows.ShowTools2.ShowResult;
import com.battlelancer.seriesguide.util.Errors;
import com.battlelancer.seriesguide.util.TaskManager;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt5.entities.BaseShow;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import kotlin.Pair;
import org.greenrobot.eventbus.EventBus;
import timber.log.Timber;

/**
 * Adds shows to the local database, tries to get watched and collected episodes if a trakt account
 * is connected.
 */
public class AddShowTask extends AsyncTask<Void, String, Void> {

    public static class OnShowAddedEvent {

        public final boolean successful;
        /**
         * Is -1 if add task was aborted.
         */
        public final int showTmdbId;
        private final String message;

        private OnShowAddedEvent(int showTmdbId, String message, boolean successful) {
            this.showTmdbId = showTmdbId;
            this.message = message;
            this.successful = successful;
        }

        public void handle(Context context) {
            if (message != null) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        }

        public static OnShowAddedEvent successful(int showTmdbId) {
            return new OnShowAddedEvent(showTmdbId, null, true);
        }

        public static OnShowAddedEvent exists(Context context, int showTmdbId, String showTitle) {
            return new OnShowAddedEvent(showTmdbId,
                    context.getString(R.string.add_already_exists, showTitle), true);
        }

        public static OnShowAddedEvent failed(Context context, int showTmdbId, String showTitle) {
            return new OnShowAddedEvent(showTmdbId,
                    context.getString(R.string.add_error, showTitle),
                    false);
        }

        public static OnShowAddedEvent failedDetails(Context context, int showTmdbId,
                String showTitle, String details) {
            return new OnShowAddedEvent(showTmdbId,
                    String.format("%s %s", context.getString(R.string.add_error, showTitle),
                            details),
                    false);
        }

        public static OnShowAddedEvent aborted(String message) {
            return new OnShowAddedEvent(-1, message, false);
        }
    }

    private static final int PROGRESS_EXISTS = 0;
    private static final int PROGRESS_SUCCESS = 1;
    private static final int PROGRESS_ERROR = 2;
    private static final int PROGRESS_ERROR_TVDB = 3;
    private static final int PROGRESS_ERROR_TVDB_NOT_EXISTS = 4;
    private static final int PROGRESS_ERROR_TRAKT = 5;
    private static final int PROGRESS_ERROR_HEXAGON = 6;
    private static final int PROGRESS_ERROR_DATA = 7;
    private static final int RESULT_OFFLINE = 8;
    private static final int RESULT_TRAKT_API_ERROR = 9;
    private static final int RESULT_TRAKT_AUTH_ERROR = 10;

    @SuppressLint("StaticFieldLeak") private final Context context;
    private final LinkedList<SearchResult> addQueue = new LinkedList<>();

    private boolean isFinishedAddingShows = false;
    private boolean isSilentMode;
    private boolean isMergingShows;

    public AddShowTask(Context context, List<SearchResult> shows, boolean isSilentMode,
            boolean isMergingShows) {
        this.context = context.getApplicationContext();
        this.addQueue.addAll(shows);
        this.isSilentMode = isSilentMode;
        this.isMergingShows = isMergingShows;
    }

    /**
     * Adds shows to the add queue. If this returns false, the shows were not added because the task
     * is finishing up. Create a new one instead.
     */
    public boolean addShows(List<SearchResult> show, boolean isSilentMode, boolean isMergingShows) {
        if (isFinishedAddingShows) {
            Timber.d("addShows: failed, already finishing up.");
            return false;
        } else {
            this.isSilentMode = isSilentMode;
            // never reset isMergingShows once true, so merged flag is correctly set on completion
            this.isMergingShows = this.isMergingShows || isMergingShows;
            addQueue.addAll(show);
            Timber.d("addShows: added shows to queue.");
            return true;
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        Timber.d("Starting to add shows...");

        SearchResult firstShow = addQueue.peek();
        if (firstShow == null) {
            Timber.d("Finished. Queue was empty.");
            return null;
        }

        if (!AndroidUtils.isNetworkConnected(context)) {
            Timber.d("Finished. No internet connection.");
            publishProgress(RESULT_OFFLINE, firstShow.getTmdbId(), firstShow.getTitle());
            return null;
        }

        if (isCancelled()) {
            Timber.d("Finished. Cancelled.");
            return null;
        }

        // if not connected to Hexagon, get episodes from trakt
        Map<Integer, BaseShow> traktCollection = null;
        Map<Integer, BaseShow> traktWatched = null;
        if (!HexagonSettings.isEnabled(context) && TraktCredentials.get(context).hasCredentials()) {
            Timber.d("Getting watched and collected episodes from trakt.");
            // get collection
            Map<Integer, BaseShow> traktShows = getTraktShows(true);
            if (traktShows == null) {
                return null; // can not get collected state from trakt, give up.
            }
            traktCollection = traktShows;
            // get watched
            traktShows = getTraktShows(false);
            if (traktShows == null) {
                return null; // can not get watched state from trakt, give up.
            }
            traktWatched = traktShows;
        }

        HexagonEpisodeSync hexagonEpisodeSync = new HexagonEpisodeSync(context,
                SgApp.getServicesComponent(context).hexagonTools());

        int result;
        boolean addedAtLeastOneShow = false;
        boolean failedMergingShows = false;
        while (!addQueue.isEmpty()) {
            Timber.d("Starting to add next show...");
            if (isCancelled()) {
                Timber.d("Finished. Cancelled.");
                // only cancelled on config change, so don't rebuild fts
                // table yet
                return null;
            }

            SearchResult nextShow = addQueue.removeFirst();
            // set values required for progress update
            String currentShowName = nextShow.getTitle();
            int currentShowTmdbId = nextShow.getTmdbId();

            if (currentShowTmdbId <= 0) {
                // Invalid ID, should never have been passed, report.
                // Background: Hexagon gets requests with ID 0.
                IllegalStateException invalidIdException = new IllegalStateException(
                        "Show id invalid: " + currentShowTmdbId
                                + ", silentMode=" + isSilentMode
                                + ", merging=" + isMergingShows
                );
                Errors.logAndReport("Add show", invalidIdException);
                continue;
            }

            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.d("Finished. No connection.");
                publishProgress(RESULT_OFFLINE, currentShowTmdbId, currentShowName);
                failedMergingShows = true;
                break;
            }

            ShowResult addResult = SgApp.getServicesComponent(context).showTools()
                    .addShow(nextShow.getTmdbId(), nextShow.getLanguage(),
                            traktCollection, traktWatched, hexagonEpisodeSync);
            if (addResult == ShowResult.SUCCESS) {
                result = PROGRESS_SUCCESS;
                addedAtLeastOneShow = true;
            } else if (addResult == ShowResult.IN_DATABASE) {
                result = PROGRESS_EXISTS;
            } else {
                Timber.e("Adding show failed: %s", addResult);

                // Only fail a hexagon merge if show can not be added due to network error,
                // not because it does not (longer) exist.
                if (isMergingShows && addResult != ShowResult.DOES_NOT_EXIST) {
                    failedMergingShows = true;
                }

                switch (addResult) {
                    case DOES_NOT_EXIST:
                        result = PROGRESS_ERROR_TVDB_NOT_EXISTS;
                        break;
                    case TMDB_ERROR:
                        result = PROGRESS_ERROR_TVDB;
                        break;
                    case TRAKT_ERROR:
                        result = PROGRESS_ERROR_TRAKT;
                        break;
                    case HEXAGON_ERROR:
                        result = PROGRESS_ERROR_HEXAGON;
                        break;
                    case DATABASE_ERROR:
                        result = PROGRESS_ERROR_DATA;
                        break;
                    default:
                        result = PROGRESS_ERROR;
                        break;
                }
            }
            publishProgress(result, currentShowTmdbId, currentShowName);
            Timber.d("Finished adding show. (Result code: %s)", result);
        }

        isFinishedAddingShows = true;

        // when merging shows down from Hexagon, set success flag
        if (isMergingShows && !failedMergingShows) {
            HexagonSettings.setHasMergedShows(context, true);
        }

        if (addedAtLeastOneShow) {
            // make sure the next sync will download all ratings
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putLong(TraktSettings.KEY_LAST_SHOWS_RATED_AT, 0)
                    .putLong(TraktSettings.KEY_LAST_EPISODES_RATED_AT, 0)
                    .apply();

            // renew FTS3 table
            Timber.d("Renewing search table.");
            SeriesGuideDatabase.rebuildFtsTable(context);
        }

        Timber.d("Finished adding shows.");
        return null;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        if (isSilentMode) {
            Timber.d("SILENT MODE: do not show progress toast");
            return;
        }

        // passing tvdb id and show name through values as fields may already have been
        // overwritten on processing thread

        OnShowAddedEvent event = null;
        // not catching format/null exceptions, if they happen we made a mistake passing values
        int result = Integer.parseInt(values[0]);
        int showTmdbId = Integer.parseInt(values[1]);
        String showTitle = values[2];
        switch (result) {
            case PROGRESS_SUCCESS:
                // do nothing, user will see show added to show list
                event = OnShowAddedEvent.successful(showTmdbId);
                break;
            case PROGRESS_EXISTS:
                event = OnShowAddedEvent.exists(context, showTmdbId, showTitle);
                break;
            case PROGRESS_ERROR:
                event = OnShowAddedEvent.failed(context, showTmdbId, showTitle);
                break;
            case PROGRESS_ERROR_TVDB:
                event = OnShowAddedEvent.failedDetails(context, showTmdbId, showTitle,
                        context.getString(R.string.api_error_generic,
                                context.getString(R.string.tvdb)));
                break;
            case PROGRESS_ERROR_TVDB_NOT_EXISTS:
                event = OnShowAddedEvent.failedDetails(context, showTmdbId, showTitle,
                        context.getString(R.string.tvdb_error_does_not_exist));
                break;
            case PROGRESS_ERROR_TRAKT:
                event = OnShowAddedEvent.failedDetails(context, showTmdbId, showTitle,
                        context.getString(R.string.api_error_generic,
                                context.getString(R.string.trakt)));
                break;
            case PROGRESS_ERROR_HEXAGON:
                event = OnShowAddedEvent.failedDetails(context, showTmdbId, showTitle,
                        context.getString(R.string.api_error_generic,
                                context.getString(R.string.hexagon)));
                break;
            case PROGRESS_ERROR_DATA:
                event = OnShowAddedEvent.failedDetails(context, showTmdbId, showTitle,
                        context.getString(R.string.database_error));
                break;
            case RESULT_OFFLINE:
                event = OnShowAddedEvent.aborted(context.getString(R.string.offline));
                break;
            case RESULT_TRAKT_API_ERROR:
                event = OnShowAddedEvent.aborted(context.getString(R.string.api_error_generic,
                        context.getString(R.string.trakt)));
                break;
            case RESULT_TRAKT_AUTH_ERROR:
                event = OnShowAddedEvent
                        .aborted(context.getString(R.string.trakt_error_credentials));
                break;
        }

        if (event != null) {
            EventBus.getDefault().post(event);
        }
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        TaskManager.getInstance().releaseAddTaskRef();
    }

    private void publishProgress(int result) {
        publishProgress(String.valueOf(result), "0", "");
    }

    private void publishProgress(int result, int showTmdbId, String showTitle) {
        publishProgress(String.valueOf(result), String.valueOf(showTmdbId), showTitle);
    }

    @Nullable
    private Map<Integer, BaseShow> getTraktShows(boolean isCollectionNotWatched) {
        Pair<Map<Integer, BaseShow>, TraktTools2.ServiceResult> result = TraktTools2
                .getCollectedOrWatchedShows(isCollectionNotWatched, context);
        if (result.getSecond() == TraktTools2.ServiceResult.AUTH_ERROR) {
            publishProgress(RESULT_TRAKT_AUTH_ERROR);
        } else if (result.getSecond() == TraktTools2.ServiceResult.API_ERROR) {
            publishProgress(RESULT_TRAKT_API_ERROR);
        }
        return result.getFirst();
    }
}
