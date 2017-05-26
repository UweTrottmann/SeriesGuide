package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.battlelancer.seriesguide.thetvdbapi.TvdbTools;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt5.entities.BaseShow;
import com.uwetrottmann.trakt5.services.Sync;
import dagger.Lazy;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Adds shows to the local database, tries to get watched and collected episodes if a trakt account
 * is connected.
 */
public class AddShowTask extends AsyncTask<Void, String, Void> {

    public static class OnShowAddedEvent {

        public final boolean successful;
        /** Is -1 if add task was aborted. */
        public final int showTvdbId;
        private final String message;

        private OnShowAddedEvent(int showTvdbId, String message, boolean successful) {
            this.showTvdbId = showTvdbId;
            this.message = message;
            this.successful = successful;
        }

        public void handle(Context context) {
            if (message != null) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show();
            }
        }

        public static OnShowAddedEvent successful(int showTvdbId) {
            return new OnShowAddedEvent(showTvdbId, null, true);
        }

        public static OnShowAddedEvent exists(Context context, int showTvdbId, String showTitle) {
            return new OnShowAddedEvent(showTvdbId,
                    context.getString(R.string.add_already_exists, showTitle), true);
        }

        public static OnShowAddedEvent failed(Context context, int showTvdbId, String showTitle) {
            return new OnShowAddedEvent(showTvdbId,
                    context.getString(R.string.add_error, showTitle),
                    false);
        }

        public static OnShowAddedEvent failedDetails(Context context, int showTvdbId,
                String showTitle, String details) {
            return new OnShowAddedEvent(showTvdbId,
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

    private final Context context;
    private final LinkedList<SearchResult> addQueue = new LinkedList<>();

    @Inject TvdbTools tvdbTools;
    @Inject Lazy<Sync> traktSync;
    private boolean isFinishedAddingShows = false;
    private boolean isSilentMode;
    private boolean isMergingShows;

    public AddShowTask(Context context, List<SearchResult> shows, boolean isSilentMode,
            boolean isMergingShows) {
        this.context = context.getApplicationContext();
        SgApp.getServicesComponent(context).inject(this);
        addQueue.addAll(shows);
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

        // don't even get started
        if (addQueue.isEmpty()) {
            Timber.d("Finished. Queue was empty.");
            return null;
        }

        if (!AndroidUtils.isNetworkConnected(context)) {
            Timber.d("Finished. No internet connection.");
            SearchResult nextShow = addQueue.peek();
            publishProgress(RESULT_OFFLINE, nextShow.tvdbid, nextShow.title);
            return null;
        }

        if (isCancelled()) {
            Timber.d("Finished. Cancelled.");
            return null;
        }

        // if not connected to Hexagon, get episodes from trakt
        HashMap<Integer, BaseShow> traktCollection = null;
        HashMap<Integer, BaseShow> traktWatched = null;
        if (!HexagonSettings.isEnabled(context) && TraktCredentials.get(context).hasCredentials()) {
            Timber.d("Getting watched and collected episodes from trakt.");
            // get collection
            HashMap<Integer, BaseShow> traktShows = getTraktShows("get collection", true);
            if (traktShows == null) {
                return null; // can not get collected state from trakt, give up.
            }
            traktCollection = traktShows;
            // get watched
            traktShows = getTraktShows("get watched", false);
            if (traktShows == null) {
                return null; // can not get watched state from trakt, give up.
            }
            traktWatched = traktShows;
        }

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
            String currentShowName = nextShow.title;
            int currentShowTvdbId = nextShow.tvdbid;

            if (!AndroidUtils.isNetworkConnected(context)) {
                Timber.d("Finished. No connection.");
                publishProgress(RESULT_OFFLINE, currentShowTvdbId, currentShowName);
                failedMergingShows = true;
                break;
            }

            try {
                boolean addedShow = tvdbTools
                        .addShow(nextShow.tvdbid, nextShow.language, traktCollection, traktWatched);
                result = addedShow ? PROGRESS_SUCCESS : PROGRESS_EXISTS;
                addedAtLeastOneShow = addedShow
                        || addedAtLeastOneShow; // do not overwrite previous success
            } catch (TvdbException e) {
                // prevent a hexagon merge from failing if a show can not be added
                // because it does not exist (any longer)
                if (!(isMergingShows && e.itemDoesNotExist())) {
                    failedMergingShows = true;
                }
                if (e.service() == TvdbException.Service.TVDB) {
                    if (e.itemDoesNotExist()) {
                        result = PROGRESS_ERROR_TVDB_NOT_EXISTS;
                    } else {
                        result = PROGRESS_ERROR_TVDB;
                    }
                } else if (e.service() == TvdbException.Service.TRAKT) {
                    result = PROGRESS_ERROR_TRAKT;
                } else if (e.service() == TvdbException.Service.HEXAGON) {
                    result = PROGRESS_ERROR_HEXAGON;
                } else if (e.service() == TvdbException.Service.DATA) {
                    result = PROGRESS_ERROR_DATA;
                } else {
                    result = PROGRESS_ERROR;
                }
                Timber.e(e, "Adding show failed");
            }

            publishProgress(result, currentShowTvdbId, currentShowName);
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
            DBUtils.rebuildFtsTable(context);
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
        int currentShowTvdbId = Integer.parseInt(values[1]);
        String currentShowName = values[2];
        switch (result) {
            case PROGRESS_SUCCESS:
                // do nothing, user will see show added to show list
                event = OnShowAddedEvent.successful(currentShowTvdbId);
                break;
            case PROGRESS_EXISTS:
                event = OnShowAddedEvent.exists(context, currentShowTvdbId, currentShowName);
                break;
            case PROGRESS_ERROR:
                event = OnShowAddedEvent.failed(context, currentShowTvdbId,
                        context.getString(R.string.add_error, currentShowName));
                break;
            case PROGRESS_ERROR_TVDB:
                event = OnShowAddedEvent.failedDetails(context, currentShowTvdbId, currentShowName,
                        context.getString(R.string.api_error_generic, context.getString(R.string.tvdb)));
                break;
            case PROGRESS_ERROR_TVDB_NOT_EXISTS:
                event = OnShowAddedEvent.failedDetails(context, currentShowTvdbId, currentShowName,
                        context.getString(R.string.tvdb_error_does_not_exist));
                break;
            case PROGRESS_ERROR_TRAKT:
                event = OnShowAddedEvent.failedDetails(context, currentShowTvdbId, currentShowName,
                        context.getString(R.string.api_error_generic, context.getString(R.string.trakt)));
                break;
            case PROGRESS_ERROR_HEXAGON:
                event = OnShowAddedEvent.failedDetails(context, currentShowTvdbId, currentShowName,
                        context.getString(R.string.api_error_generic, context.getString(R.string.hexagon)));
                break;
            case PROGRESS_ERROR_DATA:
                event = OnShowAddedEvent.failedDetails(context, currentShowTvdbId, currentShowName,
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
                event = OnShowAddedEvent.aborted(context.getString(R.string.trakt_error_credentials));
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

    private void publishProgress(int result, int showTvdbId, String showTitle) {
        publishProgress(String.valueOf(result), String.valueOf(showTvdbId), showTitle);
    }

    @Nullable
    private HashMap<Integer, BaseShow> getTraktShows(String action,
            boolean isCollectionNotWatched) {
        try {
            Response<List<BaseShow>> response;
            if (isCollectionNotWatched) {
                response = traktSync.get().collectionShows(null).execute();
            } else {
                response = traktSync.get().watchedShows(null).execute();
            }
            if (response.isSuccessful()) {
                return TraktTools.buildTraktShowsMap(response.body());
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    publishProgress(RESULT_TRAKT_AUTH_ERROR);
                    return null;
                }
                SgTrakt.trackFailedRequest(context, action, response);
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(context, action, e);
        }
        publishProgress(RESULT_TRAKT_API_ERROR);
        return null;
    }
}
