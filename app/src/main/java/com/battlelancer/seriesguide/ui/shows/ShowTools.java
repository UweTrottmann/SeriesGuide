package com.battlelancer.seriesguide.ui.shows;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SparseArrayCompat;
import androidx.core.content.ContextCompat;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.model.SgShow2;
import com.battlelancer.seriesguide.modules.ApplicationContext;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.tasks.AddShowToWatchlistTask;
import com.battlelancer.seriesguide.util.tasks.RemoveShowFromWatchlistTask;
import com.uwetrottmann.seriesguide.backend.shows.model.Show;
import java.util.ArrayList;
import java.util.HashSet;
import javax.inject.Inject;
import kotlin.Pair;
import timber.log.Timber;

/**
 * Common activities and tools useful when interacting with shows.
 */
public class ShowTools {

    public static class ShowChangedEvent {
        public int showTvdbId;

        public ShowChangedEvent(int showTvdbId) {
            this.showTvdbId = showTvdbId;
        }
    }

    /**
     * Show status valued as stored in the database in {@link com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows#STATUS}.
     */
    public interface Status {
        int IN_PRODUCTION = 5;
        int PILOT = 4;
        int CANCELED = 3;
        int UPCOMING = 2;
        int CONTINUING = 1;
        int ENDED = 0;
        int UNKNOWN = -1;
    }

    private final Context context;
    private final ShowTools2 showTools2;

    @Inject
    public ShowTools(@ApplicationContext Context context) {
        this.context = context;
        this.showTools2 = new ShowTools2(this, context);
    }

    @NonNull
    public Pair<SgShow2, Boolean> getShowDetails(int showTmdbId, String desiredLanguage) {
        return showTools2.getShowDetails(showTmdbId, desiredLanguage);
    }

    @Nullable
    public Long getShowId(int showTmdbId, Integer showTvdbId) {
        return showTools2.getShowId(showTmdbId, showTvdbId);
    }

    public void removeShow(long showId) {
        showTools2.removeShow(showId);
    }

    /**
     * Adds the show on Hexagon. Or if it does already exist, clears the isRemoved flag and updates
     * the language, so the show will be auto-added on other connected devices.
     */
    public void sendIsAdded(int showTvdbId, @NonNull String language) {
        Show show = new Show();
        show.setTvdbId(showTvdbId);
        show.setLanguage(language);
        show.setIsRemoved(false);
        uploadShowAsync(show);
    }

    /**
     * Saves new favorite flag to the local database and, if signed in, up into the cloud as well.
     */
    public void storeIsFavorite(long showId, boolean isFavorite) {
        showTools2.storeIsFavorite(showId, isFavorite);
    }

    /**
     * Saves new hidden flag to the local database and, if signed in, up into the cloud as well.
     */
    public void storeIsHidden(long showId, boolean isHidden) {
        showTools2.storeIsHidden(showId, isHidden);
    }

    /**
     * Removes hidden flag from all hidden shows in the local database and, if signed in, sends to
     * the cloud as well.
     */
    public void storeAllHiddenVisible() {
        showTools2.storeAllHiddenVisible();
    }

    public void storeLanguage(final long showId, @NonNull final String languageCode) {
        showTools2.storeLanguage(showId, languageCode);
    }

    /**
     * Saves new notify flag to the local database and, if signed in, up into the cloud as well.
     */
    public void storeNotify(long showId, boolean notify) {
        showTools2.storeNotify(showId, notify);
    }

    /**
     * Add a show to the users trakt watchlist.
     */
    public static void addToWatchlist(Context context, int showTvdbId) {
        new AddShowToWatchlistTask(context, showTvdbId).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Remove a show from the users trakt watchlist.
     */
    public static void removeFromWatchlist(Context context, int showTvdbId) {
        new RemoveShowFromWatchlistTask(context, showTvdbId).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void uploadShowAsync(Show show) {
        showTools2.uploadShowToCloud(show);
    }

    public static boolean addLastWatchedUpdateOpIfNewer(Context context,
            ArrayList<ContentProviderOperation> batch, int showTvdbId, long lastWatchedMsNew) {
        Uri uri = SeriesGuideContract.Shows.buildShowUri(showTvdbId);
        Cursor query = context.getContentResolver().query(uri, new String[]{
                SeriesGuideContract.Shows.LASTWATCHED_MS}, null, null, null);
        if (query == null) {
            Timber.e("addLastWatchedTimeUpdateOpIfNewer: query was null.");
            return false;
        }
        if (!query.moveToFirst()) {
            Timber.e("addLastWatchedTimeUpdateOpIfNewer: query has no results.");
            query.close();
            return false;
        }
        long lastWatchedMs = query.getLong(0);
        query.close();

        if (lastWatchedMs < lastWatchedMsNew) {
            batch.add(ContentProviderOperation.newUpdate(uri)
                    .withValue(SeriesGuideContract.Shows.LASTWATCHED_MS, lastWatchedMsNew)
                    .build());
        }
        return true;
    }

    /**
     * Returns the trakt id of a show. Returns {@code null} if the query failed, there is no trakt
     * id or if it is invalid.
     *
     * @deprecated Use {@link #getShowTraktId(Context, long)} and show row ID instead.
     */
    @Nullable
    public static Integer getShowTraktId(@NonNull Context context, int showTvdbId) {
        Cursor traktIdQuery = context.getContentResolver()
                .query(SeriesGuideContract.Shows.buildShowUri(showTvdbId),
                        new String[]{SeriesGuideContract.Shows.TRAKT_ID}, null, null, null);
        if (traktIdQuery == null) {
            return null;
        }

        Integer traktId = null;
        if (traktIdQuery.moveToFirst()) {
            traktId = traktIdQuery.getInt(0);
            if (traktId <= 0) {
                traktId = null;
            }
        }

        traktIdQuery.close();

        return traktId;
    }

    /**
     * Returns the Trakt id of a show, or {@code null} if it is invalid or there is none.
     */
    @Nullable
    public static Integer getShowTraktId(@NonNull Context context, long showId) {
        int traktIdOrZero = SgRoomDatabase.getInstance(context).sgShow2Helper()
                .getShowTraktId(showId);
        if (traktIdOrZero <= 0) {
            return null;
        } else {
            return traktIdOrZero;
        }
    }

    /**
     * Returns a set of the TVDb ids of all shows in the local database.
     *
     * @return null if there was an error, empty list if there are no shows.
     */
    @Nullable
    public static HashSet<Integer> getShowTvdbIdsAsSet(Context context) {
        HashSet<Integer> existingShows = new HashSet<>();

        Cursor shows = context.getContentResolver().query(SeriesGuideContract.Shows.CONTENT_URI,
                new String[]{SeriesGuideContract.Shows._ID}, null, null, null);
        if (shows == null) {
            return null;
        }

        while (shows.moveToNext()) {
            existingShows.add(shows.getInt(0));
        }

        shows.close();

        return existingShows;
    }

    /**
     * Returns a set of the TVDb ids of all shows in the local database mapped
     * to their small poster path (null if there is no poster).
     *
     * @return null if there was an error, empty list if there are no shows.
     */
    @Nullable
    public static SparseArrayCompat<String> getSmallPostersByTvdbId(Context context) {
        SparseArrayCompat<String> existingShows = new SparseArrayCompat<>();

        Cursor shows = context.getContentResolver().query(SeriesGuideContract.Shows.CONTENT_URI,
                new String[]{SeriesGuideContract.Shows._ID, SeriesGuideContract.Shows.POSTER_SMALL},
                null, null, null);
        if (shows == null) {
            return null;
        }

        while (shows.moveToNext()) {
            existingShows.put(shows.getInt(0), shows.getString(1));
        }

        shows.close();

        return existingShows;
    }

    @NonNull
    public SparseArrayCompat<String> getTmdbIdsToPoster() {
        return showTools2.getTmdbIdsToPoster(context);
    }

    /**
     * Decodes the show status and returns the localized text representation. May be {@code null} if
     * status is unknown.
     *
     * @param encodedStatus Detection based on {@link ShowTools.Status}.
     */
    @Nullable
    public String getStatus(int encodedStatus) {
        return showTools2.getStatus(encodedStatus);
    }

    /**
     * Gets the show status from {@link #getStatus} and sets a status dependant text color on the
     * given view.
     *
     * @param encodedStatus Detection based on {@link ShowTools.Status}.
     */
    public void setStatusAndColor(@NonNull TextView view, int encodedStatus) {
        view.setText(getStatus(encodedStatus));
        if (encodedStatus == Status.CONTINUING) {
            view.setTextColor(
                    ContextCompat.getColor(view.getContext(), Utils.resolveAttributeToResourceId(
                            view.getContext().getTheme(), R.attr.sgTextColorGreen)));
        } else {
            view.setTextColor(
                    ContextCompat.getColor(view.getContext(), Utils.resolveAttributeToResourceId(
                            view.getContext().getTheme(), android.R.attr.textColorSecondary)));
        }
    }
}
