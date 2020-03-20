package com.battlelancer.seriesguide.ui.movies;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;

import android.content.Context;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.settings.DisplaySettings;

/**
 * Provides settings used to sort displayed movies in
 * {@link MoviesBaseFragment} subclasses.
 */
public class MoviesDistillationSettings {

    public static class MoviesSortOrderChangedEvent {
    }

    static final String KEY_SORT_ORDER = "com.battlelancer.seriesguide.movies.sort.order";

    /**
     * Builds an appropriate SQL sort statement for sorting movies.
     */
    static String getSortQuery(Context context) {
        int sortOrderId = getSortOrderId(context);

        if (sortOrderId == MoviesSortOrder.TITLE_REVERSE_ALHPABETICAL_ID) {
            if (DisplaySettings.isSortOrderIgnoringArticles(context)) {
                return Movies.SORT_TITLE_REVERSE_ALPHACETICAL_NO_ARTICLE;
            } else {
                return Movies.SORT_TITLE_REVERSE_ALPHACETICAL;
            }
        } else if (sortOrderId == MoviesSortOrder.RELEASE_DATE_NEWEST_FIRST_ID) {
            return Movies.SORT_RELEASE_DATE_NEWEST_FIRST;
        } else if (sortOrderId == MoviesSortOrder.RELEASE_DATE_OLDEST_FIRST_ID) {
            return Movies.SORT_RELEASE_DATE_OLDEST_FIRST;
        }

        if (DisplaySettings.isSortOrderIgnoringArticles(context)) {
            return Movies.SORT_TITLE_ALPHABETICAL_NO_ARTICLE;
        } else {
            return Movies.SORT_TITLE_ALPHABETICAL;
        }
    }

    /**
     * Returns the id as of
     * {@link MoviesDistillationSettings.MoviesSortOrder}
     * of the current movie sort order.
     */
    static int getSortOrderId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getInt(KEY_SORT_ORDER, MoviesSortOrder.TITLE_ALPHABETICAL_ID);
    }

    interface MoviesSortOrder {
        int TITLE_ALPHABETICAL_ID = 0;
        int TITLE_REVERSE_ALHPABETICAL_ID = 1;
        int RELEASE_DATE_NEWEST_FIRST_ID = 2;
        int RELEASE_DATE_OLDEST_FIRST_ID = 3;
    }
}
