package com.battlelancer.seriesguide.traktapi;

import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.trakt5.TraktV2;
import com.uwetrottmann.trakt5.entities.TraktError;
import java.io.IOException;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Extends {@link TraktV2} to use our own caching OkHttp client and {@link
 * TraktCredentials} to store user credentials.
 */
public class SgTrakt extends TraktV2 {

    private static String TAG_TRAKT_ERROR = "trakt Error";

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
    public static boolean isUnauthorized(retrofit2.Response response) {
        return response.code() == 401;
    }

    /**
     * Returns if the request was not authorized. If it was, also calls {@link
     * TraktCredentials#setCredentialsInvalid()} to notify the user.
     */
    public static boolean isUnauthorized(Context context, retrofit2.Response response) {
        if (response.code() == 401) {
            // current access token is invalid, remove it and notify user to re-connect
            TraktCredentials.get(context).setCredentialsInvalid();
            return true;
        } else {
            return false;
        }
    }

    public static void trackFailedRequest(Context context, TraktV2 trakt, String action,
            retrofit2.Response response) {
        String message = response.message();
        TraktError error = trakt.checkForTraktError(response);
        if (error != null && error.message != null) {
            message += ", " + error.message;
        }
        Utils.trackFailedRequest(context, TAG_TRAKT_ERROR, action, response.code(), message);
    }

    public static void trackFailedRequest(Context context, String action,
            retrofit2.Response response) {
        Utils.trackFailedRequest(context, TAG_TRAKT_ERROR, action, response.code(),
                response.message());
    }

    public static void trackFailedRequest(Context context, String action,
            @NonNull Throwable throwable) {
        Utils.trackFailedRequest(context, TAG_TRAKT_ERROR, action, throwable);
    }

    /**
     * Executes the given call. Will return null if the call fails for any reason, including auth
     * failures.
     */
    public static <T> T executeCall(Context context, Call<T> call, String action) {
        try {
            Response<T> response = call.execute();
            if (response.isSuccessful()) {
                return response.body();
            } else {
                trackFailedRequest(context, action, response);
            }
        } catch (IOException e) {
            trackFailedRequest(context, action, e);
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
                    trackFailedRequest(context, action, response);
                }
            }
        } catch (IOException e) {
            trackFailedRequest(context, action, e);
        }
        return null;
    }
}
