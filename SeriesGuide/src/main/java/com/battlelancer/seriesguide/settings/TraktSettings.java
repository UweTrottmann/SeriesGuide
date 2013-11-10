package com.battlelancer.seriesguide.settings;

import com.battlelancer.seriesguide.util.SimpleCrypto;

import android.content.Context;
import android.preference.PreferenceManager;
import android.text.TextUtils;

/**
 * Settings related to trakt.tv integration.
 */
public class TraktSettings {

    public static final String KEY_PASSWORD_SHA1_ENCR = "com.battlelancer.seriesguide.traktpwd";

    public static final String KEY_USERNAME = "com.battlelancer.seriesguide.traktuser";

    public static final String KEY_LAST_UPDATE = "com.battlelancer.seriesguide.lasttraktupdate";

    public static final String KEY_SHARE_WITH_TRAKT = "com.battlelancer.seriesguide.sharewithtrakt";

    public static final String KEY_AUTO_ADD_TRAKT_SHOWS
            = "com.battlelancer.seriesguide.autoaddtraktshows";

    public static final String KEY_SYNC_UNWATCHED_EPISODES
            = "com.battlelancer.seriesguide.syncunseenepisodes";

    /**
     * Checks if there are a non-empty trakt username and password. Returns false if either one is
     * empty.
     */
    public static boolean hasTraktCredentials(Context context) {
        String username = getUsername(context);
        String password = getPasswordSha1(context);

        return (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password));
    }

    public static String getUsername(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(
                KEY_USERNAME, "");
    }

    /**
     * Returns the SHA hash of the users trakt password.<br> <b>Never</b> store this yourself,
     * always call this method.
     */
    public static String getPasswordSha1(Context context) {
        String hash = PreferenceManager.getDefaultSharedPreferences(context).getString(
                KEY_PASSWORD_SHA1_ENCR, "");

        // try decrypting the hash
        if (!TextUtils.isEmpty(hash)) {
            hash = SimpleCrypto.decrypt(hash, context);
        }

        return hash;
    }

    public static boolean isSharingWithTrakt(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_SHARE_WITH_TRAKT, false);
    }

    public static long getLastUpdateTime(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_LAST_UPDATE, System.currentTimeMillis());
    }

    public static boolean isAutoAddingShows(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_AUTO_ADD_TRAKT_SHOWS, true);
    }

    public static boolean isSyncingUnwatchedEpisodes(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_SYNC_UNWATCHED_EPISODES, false);
    }

}
