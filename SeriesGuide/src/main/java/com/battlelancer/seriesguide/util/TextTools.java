package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import java.util.List;

/**
 * Tools to help build text fragments to be used throughout the user interface.
 */
public class TextTools {

    private TextTools() {
        // prevent instantiation
    }

    /**
     * Returns the episode number formatted according to the users preference (e.g. '1x01',
     * 'S01E01', ...).
     */
    public static String getEpisodeNumber(Context context, int season, int episode) {
        String format = DisplaySettings.getNumberFormat(context);
        String result = String.valueOf(season);
        if (DisplaySettings.NUMBERFORMAT_DEFAULT.equals(format)) {
            // 1x01 format
            result += "x";
        } else {
            // S01E01 format
            // make season number always two chars long
            if (season < 10) {
                result = "0" + result;
            }
            if (DisplaySettings.NUMBERFORMAT_ENGLISHLOWER.equals(format)) {
                result = "s" + result + "e";
            } else {
                result = "S" + result + "E";
            }
        }

        if (episode != -1) {
            // make episode number always two chars long
            if (episode < 10) {
                result += "0";
            }

            result += episode;
        }
        return result;
    }

    /**
     * Returns a string like "1x01 Title". The number format may change based on user preference.
     */
    public static String getNextEpisodeString(Context context, int season, int episode,
            String title) {
        String result = getEpisodeNumber(context, season, episode);
        result += " " + title;
        return result;
    }

    /**
     * Returns a string like "Title 1x01". The number format may change based on user preference.
     */
    public static String getShowWithEpisodeNumber(Context context, String title, int season,
            int episode) {
        String number = getEpisodeNumber(context, season, episode);
        title += " " + number;
        return title;
    }

    /**
     * Splits the string on the pipe character {@code "|"} and reassembles it, separating the items
     * with commas. The given object is returned with the new string.
     *
     * @see #mendTvdbStrings(List)
     */
    @NonNull
    public static String splitAndKitTVDBStrings(@Nullable String tvdbstring) {
        if (tvdbstring == null) {
            tvdbstring = "";
        }
        String[] splitted = tvdbstring.split("\\|");
        tvdbstring = "";
        for (String item : splitted) {
            if (tvdbstring.length() != 0) {
                tvdbstring += ", ";
            }
            tvdbstring += item.trim();
        }
        return tvdbstring;
    }

    /**
     * Combines the strings into a single string, separated by the pipe character {@code "|"}.
     *
     * @see #splitAndKitTVDBStrings(String)
     */
    @NonNull
    public static String mendTvdbStrings(@Nullable List<String> strings) {
        if (strings == null || strings.size() == 0) {
            return "";
        }
        // pre-size builder based on average length of genre string, determined by science (tm) :)
        StringBuilder result = new StringBuilder(strings.size() * 9);
        for (String string : strings) {
            if (result.length() > 0) {
                result.append("|");
            }
            result.append(string);
        }
        return result.toString();
    }
}
