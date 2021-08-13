package com.battlelancer.seriesguide.sync;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.text.format.DateUtils;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.TraktSettings;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.Errors;
import com.battlelancer.seriesguide.util.TimeTools;
import com.uwetrottmann.trakt5.entities.RatedEpisode;
import com.uwetrottmann.trakt5.entities.RatedMovie;
import com.uwetrottmann.trakt5.entities.RatedShow;
import com.uwetrottmann.trakt5.enums.Rating;
import com.uwetrottmann.trakt5.enums.RatingsFilter;
import com.uwetrottmann.trakt5.services.Sync;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.threeten.bp.OffsetDateTime;
import retrofit2.Response;
import timber.log.Timber;

public class TraktRatingsSync {

    private Context context;
    private Sync traktSync;

    TraktRatingsSync(Context context, Sync traktSync) {
        this.context = context;
        this.traktSync = traktSync;
    }

    /**
     * Downloads trakt show ratings and applies the latest ones to the database.
     *
     * <p> To apply all ratings, set {@link TraktSettings#KEY_LAST_SHOWS_RATED_AT} to 0.
     */
    public boolean downloadForShows(@Nullable OffsetDateTime ratedAt) {
        if (ratedAt == null) {
            Timber.e("downloadForShows: null rated_at");
            return false;
        }

        long lastRatedAt = TraktSettings.getLastShowsRatedAt(context);
        if (!TimeTools.isAfterMillis(ratedAt, lastRatedAt)) {
            // not initial sync, no ratings have changed
            Timber.d("downloadForShows: no changes since %tF %tT", lastRatedAt, lastRatedAt);
            return true;
        }

        if (!TraktCredentials.get(context).hasCredentials()) {
            return false;
        }

        // download rated shows
        List<RatedShow> ratedShows;
        try {
            Response<List<RatedShow>> response = traktSync
                    .ratingsShows(RatingsFilter.ALL, null, null, null)
                    .execute();
            if (response.isSuccessful()) {
                ratedShows = response.body();
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return false;
                }
                Errors.logAndReport("get show ratings", response);
                return false;
            }
        } catch (Exception e) {
            Errors.logAndReport("get show ratings", e);
            return false;
        }
        if (ratedShows == null) {
            Timber.e("downloadForShows: null response");
            return false;
        }
        if (ratedShows.isEmpty()) {
            Timber.d("downloadForShows: no ratings on trakt");
            return true;
        }

        // trakt last activity rated_at timestamp is set after the rating timestamp
        // so include ratings that are a little older
        long ratedAtThreshold = lastRatedAt - 5 * DateUtils.MINUTE_IN_MILLIS;

        // go through ratings, latest first (trakt sends in that order)
        Map<Integer, Integer> tmdbIdsToRatings = new HashMap<>();
        for (RatedShow show : ratedShows) {
            Rating rating = show.rating;
            if (rating == null || show.show == null || show.show.ids == null) {
                continue;
            }
            Integer showTmdbId = show.show.ids.tmdb;
            if (showTmdbId == null) {
                continue;
            }
            if (show.rated_at != null
                    && TimeTools.isBeforeMillis(show.rated_at, ratedAtThreshold)) {
                // no need to apply older ratings again
                break;
            }
            // if a show does not exist, this update will do nothing
            tmdbIdsToRatings.put(showTmdbId, rating.value);
        }

        // apply database updates
        SgRoomDatabase.getInstance(context).sgShow2Helper().updateUserRatings(tmdbIdsToRatings);

        // save last rated instant
        long ratedAtTime = ratedAt.toInstant().toEpochMilli();
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(TraktSettings.KEY_LAST_SHOWS_RATED_AT, ratedAtTime)
                .apply();

        Timber.d("downloadForShows: success, last rated_at %tF %tT", ratedAtTime,
                ratedAtTime);
        return true;
    }

    /**
     * Downloads trakt episode ratings and applies the latest ones to the database.
     *
     * <p> To apply all ratings, set {@link TraktSettings#KEY_LAST_EPISODES_RATED_AT} to 0.
     */
    public boolean downloadForEpisodes(@Nullable OffsetDateTime ratedAt) {
        if (ratedAt == null) {
            Timber.e("downloadForEpisodes: null rated_at");
            return false;
        }

        long lastRatedAt = TraktSettings.getLastEpisodesRatedAt(context);
        if (!TimeTools.isAfterMillis(ratedAt, lastRatedAt)) {
            // not initial sync, no ratings have changed
            Timber.d("downloadForEpisodes: no changes since %tF %tT", lastRatedAt, lastRatedAt);
            return true;
        }

        if (!TraktCredentials.get(context).hasCredentials()) {
            return false;
        }

        // download rated episodes
        List<RatedEpisode> ratedEpisodes;
        try {
            Response<List<RatedEpisode>> response = traktSync
                    .ratingsEpisodes(RatingsFilter.ALL, null, null, null)
                    .execute();
            if (response.isSuccessful()) {
                ratedEpisodes = response.body();
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return false;
                }
                Errors.logAndReport("get episode ratings", response);
                return false;
            }
        } catch (Exception e) {
            Errors.logAndReport("get episode ratings", e);
            return false;
        }
        if (ratedEpisodes == null) {
            Timber.e("downloadForEpisodes: null response");
            return false;
        }
        if (ratedEpisodes.isEmpty()) {
            Timber.d("downloadForEpisodes: no ratings on trakt");
            return true;
        }

        // trakt last activity rated_at timestamp is set after the rating timestamp
        // so include ratings that are a little older
        long ratedAtThreshold = lastRatedAt - 5 * DateUtils.MINUTE_IN_MILLIS;

        Map<Integer, Integer> tmdbIdsToRatings = new HashMap<>();
        for (RatedEpisode episode : ratedEpisodes) {
            if (episode.rating == null || episode.episode == null || episode.episode.ids == null
                    || episode.episode.ids.tmdb == null) {
                // skip, can't handle
                continue;
            }
            if (episode.rated_at != null
                    && TimeTools.isBeforeMillis(episode.rated_at, ratedAtThreshold)) {
                // no need to apply older ratings again
                break;
            }
            // if an episode does not exist, this update will do nothing
            tmdbIdsToRatings.put(episode.episode.ids.tmdb, episode.rating.value);
        }

        // apply database updates
        SgRoomDatabase.getInstance(context).sgEpisode2Helper().updateUserRatings(tmdbIdsToRatings);

        // save last rated instant
        long ratedAtTime = ratedAt.toInstant().toEpochMilli();
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(TraktSettings.KEY_LAST_EPISODES_RATED_AT, ratedAtTime)
                .apply();

        Timber.d("downloadForEpisodes: success, last rated_at %tF %tT", ratedAtTime,
                ratedAtTime);
        return true;
    }

    /**
     * Downloads trakt movie ratings and applies the latest ones to the database.
     *
     * <p> To apply all ratings, set {@link TraktSettings#KEY_LAST_MOVIES_RATED_AT} to 0.
     */
    public boolean downloadForMovies(OffsetDateTime ratedAt) {
        if (ratedAt == null) {
            Timber.e("downloadForMovies: null rated_at");
            return false;
        }

        long lastRatedAt = TraktSettings.getLastMoviesRatedAt(context);
        if (!TimeTools.isAfterMillis(ratedAt, lastRatedAt)) {
            // not initial sync, no ratings have changed
            Timber.d("downloadForMovies: no changes since %tF %tT", lastRatedAt, lastRatedAt);
            return true;
        }

        if (!TraktCredentials.get(context).hasCredentials()) {
            return false;
        }

        // download rated shows
        List<RatedMovie> ratedMovies;
        try {
            Response<List<RatedMovie>> response = traktSync
                    .ratingsMovies(RatingsFilter.ALL, null, null, null)
                    .execute();
            if (response.isSuccessful()) {
                ratedMovies = response.body();
            } else {
                if (SgTrakt.isUnauthorized(context, response)) {
                    return false;
                }
                Errors.logAndReport("get movie ratings", response);
                return false;
            }
        } catch (Exception e) {
            Errors.logAndReport("get movie ratings", e);
            return false;
        }
        if (ratedMovies == null) {
            Timber.e("downloadForMovies: null response");
            return false;
        }
        if (ratedMovies.isEmpty()) {
            Timber.d("downloadForMovies: no ratings on trakt");
            return true;
        }

        // trakt last activity rated_at timestamp is set after the rating timestamp
        // so include ratings that are a little older
        long ratedAtThreshold = lastRatedAt - 5 * DateUtils.MINUTE_IN_MILLIS;

        // go through ratings, latest first (trakt sends in that order)
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (RatedMovie movie : ratedMovies) {
            if (movie.rating == null || movie.movie == null || movie.movie.ids == null
                    || movie.movie.ids.tmdb == null) {
                // skip, can't handle
                continue;
            }
            if (movie.rated_at != null &&
                    TimeTools.isBeforeMillis(movie.rated_at, ratedAtThreshold)) {
                // no need to apply older ratings again
                break;
            }

            // if a movie does not exist, this update will do nothing
            ContentProviderOperation op = ContentProviderOperation.newUpdate(
                    SeriesGuideContract.Movies.buildMovieUri(movie.movie.ids.tmdb))
                    .withValue(SeriesGuideContract.Movies.RATING_USER, movie.rating.value)
                    .build();
            batch.add(op);
        }

        // apply database updates
        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "downloadForMovies: database update failed");
            return false;
        }

        // save last rated instant
        long ratedAtTime = ratedAt.toInstant().toEpochMilli();
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(TraktSettings.KEY_LAST_MOVIES_RATED_AT, ratedAtTime)
                .apply();

        Timber.d("downloadForMovies: success, last rated_at %tF %tT", ratedAtTime, ratedAtTime);
        return true;
    }
}
