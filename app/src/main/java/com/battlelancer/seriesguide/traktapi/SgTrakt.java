package com.battlelancer.seriesguide.traktapi;

import android.content.Context;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.util.Errors;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.TraktError;
import com.uwetrottmann.trakt5.entities.TraktOAuthError;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Extends {@link TraktV2} to use our own caching OkHttp client and {@link
 * TraktCredentials} to store user credentials.
 */
public class SgTrakt extends TraktV2 {

    private final Context context;
    private final OkHttpClient okHttpClient;

    public SgTrakt(Context context, OkHttpClient okHttpClient) {
        super(BuildConfig.TRAKT_CLIENT_ID, BuildConfig.TRAKT_CLIENT_SECRET,
                BaseOAuthActivity.OAUTH_CALLBACK_URL_CUSTOM);
        this.context = context;
        this.okHttpClient = okHttpClient;
    }

    @Override
    public String accessToken() {
        return TraktCredentials.get(context).getAccessToken();
    }

    @Override
    public String refreshToken() {
        return TraktOAuthSettings.getRefreshToken(context);
    }

    @Override
    protected synchronized OkHttpClient okHttpClient() {
        return okHttpClient;
    }

    /**
     * Check if the request was unauthorized.
     *
     * @see #isUnauthorized(Context, Response)
     */
    public static boolean isUnauthorized(retrofit2.Response<?> response) {
        return response.code() == 401;
    }

    /**
     * Check if the associated Trakt account is locked.
     *
     * https://trakt.docs.apiary.io/#introduction/locked-user-account
     */
    public static boolean isAccountLocked(retrofit2.Response<?> response) {
        return response.code() == 423;
    }

    /**
     * Returns if the request was not authorized. If it was, also calls {@link
     * TraktCredentials#setCredentialsInvalid()} to notify the user.
     */
    public static boolean isUnauthorized(Context context, retrofit2.Response<?> response) {
        if (response.code() == 401) {
            // current access token is invalid, remove it and notify user to re-connect
            TraktCredentials.get(context).setCredentialsInvalid();
            return true;
        } else {
            return false;
        }
    }

    @Nullable
    public static String checkForTraktError(TraktV2 trakt, Response<?> response) {
        TraktError error = trakt.checkForTraktError(response);
        if (error != null && error.message != null) {
            return error.message;
        } else {
            return null;
        }
    }

    @Nullable
    public static String checkForTraktOAuthError(TraktV2 trakt, Response<?> response) {
        TraktOAuthError error = trakt.checkForTraktOAuthError(response);
        if (error != null && error.error != null && error.error_description != null) {
            return error.error + " " + error.error_description;
        } else {
            return null;
        }
    }

    /**
     * Executes the given call. Will return null if the call fails for any reason, including auth
     * failures.
     */
    @Nullable
    public static <T> T executeCall(Call<T> call, String action) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                Errors.logAndReport(action, response);
            }
        } catch (Exception e) {
            Errors.logAndReport(action, e);
        }
        return null;
    }

    /**
     * Executes the given call. If the call fails because auth is invalid, removes the current
     * access token and displays a warning notification to the user.
     */
    public static <T> T executeAuthenticatedCall(Context context, Call<T> call, String action) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                if (!isUnauthorized(context, response)) {
                    Errors.logAndReport(action, response);
                }
            }
        } catch (Exception e) {
            Errors.logAndReport(action, e);
        }
        return null;
    }
}
