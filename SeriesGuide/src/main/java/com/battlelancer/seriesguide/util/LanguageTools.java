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

    private LanguageTools() {
        // prevent instantiation
    }
}
