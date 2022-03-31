package com.battlelancer.seriesguide.ui.shows;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SparseArrayCompat;
import androidx.core.content.ContextCompat;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.modules.ApplicationContext;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.sync.HexagonEpisodeSync;
import com.battlelancer.seriesguide.ui.shows.ShowTools2.ShowDetails;
import com.battlelancer.seriesguide.ui.shows.ShowTools2.ShowResult;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.util.tasks.AddShowToWatchlistTask;
import com.battlelancer.seriesguide.util.tasks.RemoveShowFromWatchlistTask;
import com.github.michaelbull.result.Result;
import com.uwetrottmann.trakt5.entities.BaseShow;
import java.util.Map;
import javax.inject.Inject;

/**
 * Common activities and tools useful when interacting with shows.
 */
public class ShowTools {

    /**
     * Show status valued as stored in the database in {@link com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows#STATUS}.
     */
    // Compare with https://www.themoviedb.org/bible/tv#59f7403f9251416e7100002b
    // Note: used to order shows by status, so ensure similar are next to each other.
    public interface Status {
        int IN_PRODUCTION = 5;
        int PILOT = 4;
        int PLANNED = 2;
        /**
         * Episodes are to be released.
         */
        int RETURNING = 1;
        /**
         * Typically all episodes released, with a planned ending.
         */
        int ENDED = 0;
        int UNKNOWN = -1;
        /**
         * Typically all episodes released, but abruptly ended.
         */
        int CANCELED = -2;
    }

    private final Context context;
    private final ShowTools2 showTools2;

    @Inject
    public ShowTools(@ApplicationContext Context context) {
        this.context = context;
        this.showTools2 = new ShowTools2(this, context);
    }

    @NonNull
    public Result<ShowDetails, ShowTools2.GetShowError> getShowDetails(int showTmdbId, String desiredLanguage) {
        return showTools2.getShowDetails(showTmdbId, desiredLanguage, false);
    }

    @Nullable
    public Long getShowId(int showTmdbId, Integer showTvdbId) {
        return showTools2.getShowId(showTmdbId, showTvdbId);
    }

    public ShowResult addShow(int showTmdbId,
            @Nullable String desiredLanguage,
            @Nullable Map<Integer, BaseShow> traktCollection,
            @Nullable Map<Integer, BaseShow> traktWatched,
            HexagonEpisodeSync hexagonEpisodeSync) {
        return showTools2.addShow(showTmdbId, desiredLanguage, traktCollection, traktWatched,
                hexagonEpisodeSync);
    }

    public ShowTools2.UpdateResult updateShow(long showId) {
        return showTools2.updateShow(showId);
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

    @NonNull
    public Map<Integer, Long> getTmdbIdsToShowIds() {
        return showTools2.getTmdbIdsToShowIds(context);
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
        if (encodedStatus == Status.RETURNING) {
            view.setTextColor(
                    ContextCompat.getColor(view.getContext(), Utils.resolveAttributeToResourceId(
                            view.getContext().getTheme(), R.attr.colorSecondary)));
        } else {
            view.setTextColor(
                    ContextCompat.getColor(view.getContext(), Utils.resolveAttributeToResourceId(
                            view.getContext().getTheme(), android.R.attr.textColorSecondary)));
        }
    }

    public boolean shouldUpdateShow(long showId) {
        return showTools2.shouldUpdateShow(showId);
    }
}
