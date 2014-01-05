/*
 * Copyright 2014 Uwe Trottmann
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
 *
 */

package com.battlelancer.seriesguide.util;

import com.battlelancer.seriesguide.enums.NetworkResult;
import com.battlelancer.seriesguide.enums.Result;
import com.battlelancer.seriesguide.enums.TraktStatus;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Response;
import com.jakewharton.trakt.services.AccountService;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import retrofit.RetrofitError;

/**
 * Expects a trakt username, password and email (can be null) as parameters. Checks the validity
 * with trakt servers or creates a new account if an email adress is given. If successful, the
 * credentials are stored.
 */
public class ConnectTraktTask extends AsyncTask<String, Void, Integer> {

    public interface OnTaskFinishedListener {

        /**
         * Returns one of {@link com.battlelancer.seriesguide.enums.NetworkResult}.
         */
        public void onTaskFinished(int resultCode);

    }

    private final Context mContext;

    private OnTaskFinishedListener mListener;

    public ConnectTraktTask(Context context, OnTaskFinishedListener listener) {
        mContext = context;
        mListener = listener;
    }

    @Override
    protected Integer doInBackground(String... params) {
        // check for connectivity
        if (!AndroidUtils.isNetworkConnected(mContext)) {
            return NetworkResult.OFFLINE;
        }

        // get account data
        String username = params[0];
        String password = params[1];
        String email = params[2];

        // check if we have any usable data
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            return Result.ERROR;
        }

        // create SHA1 of password
        password = Utils.toSHA1(mContext, password);

        // check validity
        // use a new Trakt instance for testing
        final Trakt manager = new Trakt();
        manager.setApiKey(mContext.getResources().getString(R.string.trakt_apikey));
        manager.setAuthentication(username, password);

        Response response = null;
        try {
            if (TextUtils.isEmpty(email)) {
                // validate existing account
                response = manager.accountService().test();
            } else {
                // create new account
                response = manager.accountService().create(
                        new AccountService.NewAccount(username, password, email));
            }
        } catch (RetrofitError e) {
            response = null;
        }

        // did anything go wrong?
        if (response == null || response.status.equals(TraktStatus.FAILURE)) {
            return Result.ERROR;
        }

        // store the new credentials
        TraktCredentials.get(mContext).setCredentials(username, password);

        // set new auth data for service manager
        Trakt trakt = ServiceUtils.getTraktWithAuth(mContext);
        trakt.setAuthentication(username, password);

        return Result.SUCCESS;
    }

    @Override
    protected void onPostExecute(Integer resultCode) {
        if (mListener != null) {
            mListener.onTaskFinished(resultCode);
        }
    }
}
