package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import java.util.Date;
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
     * Returns the title or if its empty a string like "Episode 2".
     */
    @NonNull
    public static String getEpisodeTitle(Context context, @Nullable String title, int episode) {
        return TextUtils.isEmpty(title)
                ? context.getString(R.string.episode_number, episode)
                : title;
    }

    /**
     * Returns a string like "1x01 Title". The number format may change based on user preference.
     */
    public static String getNextEpisodeString(Context context, int season, int episode,
            String title) {
        return getEpisodeNumber(context, season, episode) + " "
                + getEpisodeTitle(context, title, episode);
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
        // pre-size builder based on reasonable average length of a string
        StringBuilder result = new StringBuilder(strings.size() * 10);
        for (String string : strings) {
            if (result.length() > 0) {
                result.append("|");
            }
            result.append(string);
        }
        return result.toString();
    }

    /**
     * Builds a network + release time string for a show formatted like "Network · Tue 08:00 PM".
     */
    @NonNull
    public static String networkAndTime(@Nullable String network, @Nullable String time) {
        StringBuilder networkAndTime = new StringBuilder();
        networkAndTime.append(network);
        if (!TextUtils.isEmpty(time)) {
            if (networkAndTime.length() > 0) {
                networkAndTime.append(" · ");
            }
            networkAndTime.append(time);
        }
        return networkAndTime.toString();
    }

    @NonNull
    public static String networkAndTime(Context context, Date release, int weekDay,
            @Nullable String network) {
        if (release != null) {
            String dayString = TimeTools.formatToLocalDayOrDaily(context, release, weekDay);
            String timeString = TimeTools.formatToLocalTime(context, release);
            return TextTools.networkAndTime(network, dayString + " " + timeString);
        } else {
            return TextTools.networkAndTime(network, null);
        }
    }
}
