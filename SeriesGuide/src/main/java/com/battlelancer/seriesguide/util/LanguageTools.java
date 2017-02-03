package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.support.annotation.ArrayRes;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import java.util.Locale;

/**
 * Helper methods for language strings and codes.
 */
public class LanguageTools {

    /**
     * Returns the string representation of the given two letter ISO 639-1 language code if it is
     * supported by SeriesGuide ({@link SeriesGuideContract.Shows#LANGUAGE}).
     *
     * <p>If the given language code is {@code null}, uses {@link DisplaySettings#getContentLanguage(Context)}.
     */
    public static String getShowLanguageStringFor(Context context, @Nullable String languageCode) {
        if (TextUtils.isEmpty(languageCode)) {
            // fall back to default language
            languageCode = DisplaySettings.getContentLanguage(context);
        }

        return getLanguageStringFor(context, languageCode, R.array.languageCodesShows, R.array.languagesShows);
    }

    /**
     * Returns the string representation of the given two letter ISO 639-1 language code if it is
     * supported by SeriesGuide ({@link SeriesGuideContract.Shows#LANGUAGE}).
     *
     * <p>If the given language code is {@code null}, uses {@link DisplaySettings#getContentLanguage(Context)}.
     */
    public static String getMovieLanguageStringFor(Context context, @Nullable String languageCode) {
        if (TextUtils.isEmpty(languageCode)) {
            // fall back to default language
            languageCode = DisplaySettings.getMoviesLanguage(context);
        }

        return getLanguageStringFor(context, languageCode, R.array.languageCodesMovies,
                R.array.languagesMovies);
    }

    private static String getLanguageStringFor(Context context, @Nullable String languageCode,
            @ArrayRes int languageCodesRes, @ArrayRes int languagesRes) {
        String[] languageCodes = context.getResources().getStringArray(languageCodesRes);
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(languageCode)) {
                String[] languages = context.getResources().getStringArray(languagesRes);
                return languages[i];
            }
        }

        return context.getString(R.string.unknown);
    }

    public static class LanguageData {
        public final int languageIndex;
        public final String languageCode;
        public final String languageString;

        public LanguageData(int languageIndex, String languageCode, String languageString) {
            this.languageIndex = languageIndex;
            this.languageCode = languageCode;
            this.languageString = languageString;
        }
    }

    /**
     * Returns the string representation and index of the given two letter ISO 639-1 language code
     * if it is supported by SeriesGuide ({@link SeriesGuideContract.Shows#LANGUAGE}).
     *
     * <p>If the given language code is {@code null}, uses {@link DisplaySettings#getContentLanguage(Context)}.
     */
    @Nullable
    public static LanguageData getShowLanguageDataFor(Context context,
            @Nullable String languageCode) {
        if (TextUtils.isEmpty(languageCode)) {
            // fall back to default language
            languageCode = DisplaySettings.getContentLanguage(context);
        }

        return getLanguageDataFor(context, languageCode, R.array.languageCodesShows, R.array.languagesShows);
    }

    /**
     * Returns the string representation and index of the given two letter ISO 639-1 language code
     * plus an extra ISO-3166-1 region tag used by TMDB currently set by {@link
     * DisplaySettings#getMoviesLanguage(Context)}.
     */
    @Nullable
    public static LanguageData getMovieLanguageData(Context context) {
        String languageCodeCurrent = DisplaySettings.getMoviesLanguage(context);
        String[] languageCodes = context.getResources().getStringArray(R.array.languageCodesMovies);
        for (int i = 0; i < languageCodes.length; i++) {
            String languageCode = languageCodes[i];
            if (languageCode.equals(languageCodeCurrent)) {
                String languageDisplayName = new Locale(languageCode.substring(0, 2),
                        languageCode.substring(3)).getDisplayName();
                return new LanguageData(i, languageCode, languageDisplayName);
            }
        }

        return null;
    }

    @Nullable
    private static LanguageData getLanguageDataFor(Context context, @Nullable String languageCode,
            @ArrayRes int languageCodesRes, @ArrayRes int languagesRes) {
        String[] languageCodes = context.getResources().getStringArray(languageCodesRes);
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(languageCode)) {
                String[] languages = context.getResources().getStringArray(languagesRes);
                return new LanguageData(i, languageCode, languages[i]);
            }
        }

        return null;
    }

    private LanguageTools() {
        // prevent instantiation
    }
}
