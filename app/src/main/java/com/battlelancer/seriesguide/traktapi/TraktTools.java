package com.battlelancer.seriesguide.traktapi;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.battlelancer.seriesguide.R;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.trakt5.TraktLink;
import com.uwetrottmann.trakt5.entities.BaseEpisode;
import com.uwetrottmann.trakt5.entities.BaseSeason;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class TraktTools {

    private TraktTools() {
    }

    @NonNull
    public static HashMap<Integer, BaseSeason> mapSeasonsByNumber(List<BaseSeason> seasons) {
        @SuppressLint("UseSparseArrays")
        HashMap<Integer, BaseSeason> traktSeasonsMap = new HashMap<>(seasons.size());
        for (BaseSeason season : seasons) {
            if (season.number == null
                    || season.episodes == null
                    || season.episodes.isEmpty()) {
                continue; // trakt season misses required data, skip.
            }
            traktSeasonsMap.put(season.number, season);
        }
        return traktSeasonsMap;
    }

    @NonNull
    public static HashMap<Integer, BaseEpisode> buildTraktEpisodesMap(List<BaseEpisode> episodes) {
        HashMap<Integer, BaseEpisode> traktEpisodesMap = new HashMap<>(episodes.size());
        for (BaseEpisode episode : episodes) {
            if (episode.number == null) {
                continue; // trakt episode misses required data, skip.
            }
            traktEpisodesMap.put(episode.number, episode);
        }
        return traktEpisodesMap;
    }

    public static String buildShowUrl(int showTmdbId) {
        return TraktLink.tmdb(showTmdbId) + "?id_type=show";
    }

    public static String buildEpisodeUrl(int episodeTmdbId) {
        return TraktLink.tmdb(episodeTmdbId) + "?id_type=episode";
    }

    public static String buildMovieUrl(int movieTmdbId) {
        return TraktLink.tmdb(movieTmdbId) + "?id_type=movie";
    }

    /**
     * Returns the given double as number string with one decimal digit, like "1.5". Formatted using
     * the default locale.
     */
    public static String buildRatingString(@Nullable Double rating) {
        return buildRatingString(rating, Locale.getDefault());
    }

    public static String buildRatingString(@Nullable Double rating, @NonNull Locale locale) {
        if (rating == null || rating == 0) {
            return "--";
        }
        if (!AndroidUtils.isNougatOrHigher()) {
            // before Android 7.0 string format seems to round half down, despite docs saying half up
            // it likely used DecimalFormat, which defaults to half even
            BigDecimal bigDecimal = new BigDecimal(rating);
            bigDecimal = bigDecimal.setScale(1, BigDecimal.ROUND_HALF_UP);
            rating = bigDecimal.doubleValue();
        }
        return String.format(locale, "%.1f", rating);
    }

    /**
     * Builds a localized string like "x votes".
     */
    public static String buildRatingVotesString(Context context, Integer votes) {
        if (votes == null || votes < 0) {
            votes = 0;
        }
        return context.getResources().getQuantityString(R.plurals.votes, votes, votes);
    }

    /**
     * Converts a rating index from 1 to 10 into the localized string representation. Any other
     * value will return the rate action string.
     */
    public static String buildUserRatingString(Context context, @Nullable Integer rating) {
        int resId = getRatingStringRes(rating);
        if (resId == 0) {
            return context.getString(R.string.action_rate);
        } else {
            return context.getString(R.string.rating_number_text_format, rating,
                    context.getString(resId));
        }
    }

    @StringRes
    private static int getRatingStringRes(@Nullable Integer rating) {
        if (rating == null) {
            return 0;
        }
        switch (rating) {
            case 1:
                return R.string.hate;
            case 2:
                return R.string.rating2;
            case 3:
                return R.string.rating3;
            case 4:
                return R.string.rating4;
            case 5:
                return R.string.rating5;
            case 6:
                return R.string.rating6;
            case 7:
                return R.string.rating7;
            case 8:
                return R.string.rating8;
            case 9:
                return R.string.rating9;
            case 10:
                return R.string.love;
            default:
                return 0;
        }
    }
}
