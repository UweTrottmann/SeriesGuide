package com.battlelancer.seriesguide.ui.shows;

import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.collection.SparseArrayCompat;
import com.battlelancer.seriesguide.modules.ApplicationContext;
import com.battlelancer.seriesguide.util.shows.ShowTools2;
import com.battlelancer.seriesguide.util.tasks.AddShowToWatchlistTask;
import com.battlelancer.seriesguide.util.tasks.RemoveShowFromWatchlistTask;
import java.util.Map;
import javax.inject.Inject;

/**
 * Common activities and tools useful when interacting with shows.
 */
public class ShowTools {

    private final Context context;
    private final ShowTools2 showTools2;

    @Inject
    public ShowTools(@ApplicationContext Context context) {
        this.context = context;
        this.showTools2 = new ShowTools2(context);
    }

    public void removeShow(long showId) {
        showTools2.removeShow(showId);
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
    public static void addToWatchlist(Context context, int showTmdbId) {
        new AddShowToWatchlistTask(context, showTmdbId).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Remove a show from the users trakt watchlist.
     */
    public static void removeFromWatchlist(Context context, int showTmdbId) {
        new RemoveShowFromWatchlistTask(context, showTmdbId).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @NonNull
    public Map<Integer, Long> getTmdbIdsToShowIds() {
        return showTools2.getTmdbIdsToShowIds(context);
    }

    @NonNull
    public SparseArrayCompat<String> getTmdbIdsToPoster() {
        return showTools2.getTmdbIdsToPoster(context);
    }

}
