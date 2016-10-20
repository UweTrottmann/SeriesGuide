package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.settings.DisplaySettings;

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
    public static String getLanguageStringForCode(Context context, @Nullable String languageCode) {
        if (TextUtils.isEmpty(languageCode)) {
            // fall back to default language
            languageCode = DisplaySettings.getContentLanguage(context);
        }

        String[] languageCodes = context.getResources().getStringArray(R.array.languageData);
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(languageCode)) {
                String[] languages = context.getResources().getStringArray(R.array.languages);
                return languages[i];
            }
        }

        return "";
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
    public static LanguageData getLanguageDataForCode(Context context,
            @Nullable String languageCode) {
        if (TextUtils.isEmpty(languageCode)) {
            // fall back to default language
            languageCode = DisplaySettings.getContentLanguage(context);
        }

        String[] languageCodes = context.getResources().getStringArray(R.array.languageData);
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(languageCode)) {
                String[] languages = context.getResources().getStringArray(R.array.languages);
                return new LanguageData(i, languageCode, languages[i]);
            }
        }

        return null;
    }

    private LanguageTools() {
        // prevent instantiation
    }
}
