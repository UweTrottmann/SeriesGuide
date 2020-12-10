package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.ArrayRes;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.DisplaySettings;

/**
 * Helper methods for language strings and codes.
 */
public class LanguageTools {

    /**
     * Returns the string representation of the given two letter ISO 639-1 language code,
     * plus optional ISO-3166-1 region tag, if it is supported by SeriesGuide
     * (see R.array.languageCodesShows).
     *
     * <p>If the given language code is {@code null} uses 'en' to ensure consistent behavior across
     * devices.
     */
    public static String getShowLanguageStringFor(Context context, @Nullable String languageCode) {
        if (TextUtils.isEmpty(languageCode)) {
            // default to 'en'
            languageCode = DisplaySettings.LANGUAGE_EN;
        }

        return getLanguageStringFor(context, languageCode, R.array.languageCodesShows);
    }

    /**
     * Returns the string representation of the given two letter ISO 639-1 language code,
     * plus optional ISO-3166-1 region tag, if it is supported by SeriesGuide
     * (see R.array.languageCodesMovies).
     *
     * <p>If the given language code is {@code null},
     * uses {@link DisplaySettings#getMoviesLanguage(Context)}.
     */
    public static String getMovieLanguageStringFor(Context context, @Nullable String languageCode) {
        if (TextUtils.isEmpty(languageCode)) {
            // fall back to default language
            languageCode = DisplaySettings.getMoviesLanguage(context);
        }

        return getLanguageStringFor(context, languageCode, R.array.languageCodesMovies);
    }

    private static String getLanguageStringFor(Context context, @Nullable String languageCode,
            @ArrayRes int languageCodesRes) {
        String[] languageCodes = context.getResources().getStringArray(languageCodesRes);
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(languageCode)) {
                return LanguageToolsK.buildLanguageDisplayName(languageCode);
            }
        }

        return context.getString(R.string.unknown);
    }

    public static class LanguageData {
        public final String languageCode;
        public final String languageString;

        public LanguageData(String languageCode, String languageString) {
            this.languageCode = languageCode;
            this.languageString = languageString;
        }
    }

    /**
     * Together with the language code, returns the string representation of the given
     * two letter ISO 639-1 language code, plus optional ISO-3166-1 region tag,
     * if it is supported by SeriesGuide (see R.array.languageCodesShows).
     *
     * <p>If the given language code is {@code null} uses 'en' to ensure consistent behavior across
     * devices.
     */
    @Nullable
    public static LanguageData getShowLanguageDataFor(Context context,
            @Nullable String languageCode) {
        if (TextUtils.isEmpty(languageCode)) {
            // default to 'en'
            languageCode = DisplaySettings.LANGUAGE_EN;
        }

        String[] languageCodes = context.getResources().getStringArray(R.array.languageCodesShows);
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(languageCode)) {
                String languageName = LanguageToolsK.buildLanguageDisplayName(languageCode);;
                return new LanguageData(languageCode, languageName);
            }
        }

        return null;
    }

    private LanguageTools() {
        // prevent instantiation
    }
}
