package com.battlelancer.seriesguide.settings;

import android.content.Context;

/**
 * Settings for {@link com.battlelancer.seriesguide.extensions.AmazonExtension}.
 */
public class AmazonSettings {

    public static final String SETTINGS_FILE = "extension_amazon";

    public static final String KEY_COUNTRY = "extension_amazon";

    public static final String DEFAULT_DOMAIN = "amazon.com";

    /**
     * Return the Amazon country domain selected by the user.
     */
    public static String getAmazonCountryDomain(Context context) {
        return context.getSharedPreferences(SETTINGS_FILE, 0)
                .getString(KEY_COUNTRY, DEFAULT_DOMAIN);
    }
}
