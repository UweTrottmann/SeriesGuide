/*
 * Copyright 2015 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.uwetrottmann.androidutils.AndroidUtils;

public abstract class BaseActionTask extends AsyncTask<Void, Void, Integer> {

    public static final int SUCCESS = 0;
    public static final int ERROR_NETWORK = -1;
    public static final int ERROR_DATABASE = -2;
    public static final int ERROR_TRAKT_AUTH = -3;
    public static final int ERROR_TRAKT_API = -4;
    public static final int ERROR_HEXAGON_API = -5;

    private final Context context;
    private boolean isSendingToHexagon;
    private boolean isSendingToTrakt;

    public BaseActionTask(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    protected void onPreExecute() {
        isSendingToHexagon = HexagonTools.isSignedIn(context);
        isSendingToTrakt = TraktCredentials.get(context).hasCredentials();

        // if sending to services and there is no network, cancel right away
        if (isSendingToHexagon() || isSendingToTrakt()) {
            if (!AndroidUtils.isNetworkConnected(getContext())) {
                cancel(true);
                Toast.makeText(context, R.string.offline, Toast.LENGTH_LONG).show();
                return;
            }
        }

        // show toast to which service we send
        if (isSendingToHexagon()) {
            Toast.makeText(context, R.string.hexagon_api_queued, Toast.LENGTH_SHORT).show();
        }
        if (isSendingToTrakt()) {
            Toast.makeText(context, R.string.trakt_submitqueued, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPostExecute(Integer result) {
        if (result == SUCCESS) {
            // success!
            Toast.makeText(context, getSuccessTextResId(), Toast.LENGTH_SHORT).show();
            return;
        }

        // handle errors
        Integer errorResId = null;
        switch (result) {
            case ERROR_NETWORK:
                errorResId = R.string.offline;
                break;
            case ERROR_DATABASE:
                errorResId = R.string.database_error;
                break;
            case ERROR_TRAKT_AUTH:
                errorResId = R.string.trakt_error_credentials;
                break;
            case ERROR_TRAKT_API:
                errorResId = R.string.trakt_error_general;
                break;
            case ERROR_HEXAGON_API:
                errorResId = R.string.hexagon_api_error;
                break;
        }
        if (errorResId != null) {
            Toast.makeText(context, errorResId, Toast.LENGTH_LONG).show();
        }
    }

    protected Context getContext() {
        return context;
    }

    protected abstract int getSuccessTextResId();

    /**
     * Will be true if signed in with hexagon. Override and return {@code false} to not send to
     * hexagon.
     */
    protected boolean isSendingToHexagon() {
        return isSendingToHexagon;
    }

    /**
     * Will be true if signed in with trakt.
     */
    protected boolean isSendingToTrakt() {
        return isSendingToTrakt;
    }
}
