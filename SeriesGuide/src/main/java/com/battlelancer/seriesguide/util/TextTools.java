package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.TextAppearanceSpan;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import java.text.NumberFormat;
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
        NumberFormat numberFormat = NumberFormat.getIntegerInstance();
        numberFormat.setGroupingUsed(false);
        String format = DisplaySettings.getNumberFormat(context);
        String result = numberFormat.format(season);
        if (DisplaySettings.NUMBERFORMAT_DEFAULT.equals(format)) {
            // 1x01 format
            result += "x";
        } else {
            // S01E01 format
            // make season number always two chars long
            if (season < 10) {
                result = numberFormat.format(0) + result;
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
                result += numberFormat.format(0);
            }

            result += numberFormat.format(episode);
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
            return "";
        }
        String[] split = tvdbstring.split("\\|");
        StringBuilder builder = new StringBuilder();
        for (String item : split) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(item.trim());
        }
        return builder.toString();
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
     * Dot separates the two given strings. If one is empty, just returns the other string (no dot).
     */
    @NonNull
    public static String dotSeparate(@Nullable String left, @Nullable String right) {
        StringBuilder dotString = new StringBuilder(left == null ? "" : left);
        if (!TextUtils.isEmpty(right)) {
            if (dotString.length() > 0) {
                dotString.append(" · ");
            }
            dotString.append(right);
        }
        return dotString.toString();
    }

    /**
     * Builds a network + release time string for a show formatted like "Network · Tue 08:00 PM".
     */
    @NonNull
    public static String networkAndTime(Context context, @Nullable Date release, int weekDay,
            @Nullable String network) {
        if (release != null) {
            String dayString = TimeTools.formatToLocalDayOrDaily(context, release, weekDay);
            String timeString = TimeTools.formatToLocalTime(context, release);
            return TextTools.dotSeparate(network, dayString + " " + timeString);
        } else {
            return TextTools.dotSeparate(network, null);
        }
    }

    /**
     * Appends an empty new line and a new line listing the source of the text as TMDB.
     */
    public static SpannableStringBuilder textWithTmdbSource(Context context, @Nullable String text) {
        return textWithSource(context, text,
                context.getString(R.string.format_source, context.getString(R.string.tmdb)));
    }

    /**
     * Appends an empty new line and a new line listing the source of the text as TVDB and the last
     * edited date (is unknown if seconds value is less than 1).
     */
    public static SpannableStringBuilder textWithTvdbSource(Context context, @Nullable String text,
            long lastEditSeconds) {
        String lastEdited;
        if (lastEditSeconds > 0) {
            lastEdited = DateUtils.formatDateTime(context, lastEditSeconds * 1000,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME);
        } else {
            lastEdited = context.getString(R.string.unknown);
        }
        String sourceAndTime =
                context.getString(R.string.format_source, context.getString(R.string.tvdb))
                        + "\n" + context.getString(R.string.format_last_edited, lastEdited);
        return textWithSource(context, text, sourceAndTime);
    }

    private static SpannableStringBuilder textWithSource(Context context, @Nullable String text,
            @NonNull String source) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        if (text != null) {
            builder.append(text);
            builder.append("\n\n");
        }
        int sourceStartIndex = builder.length();
        builder.append(source);
        builder.setSpan(new TextAppearanceSpan(context, R.style.TextAppearance_Body_Highlight),
                sourceStartIndex, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        return builder;
    }
}
