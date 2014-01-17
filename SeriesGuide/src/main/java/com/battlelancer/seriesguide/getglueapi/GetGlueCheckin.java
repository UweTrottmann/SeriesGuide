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
 */

package com.battlelancer.seriesguide.getglueapi;

import com.battlelancer.seriesguide.settings.GetGlueSettings;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.getglue.GetGlue;
import com.uwetrottmann.getglue.entities.GetGlueInteraction;
import com.uwetrottmann.getglue.entities.GetGlueInteractionResource;
import com.uwetrottmann.getglue.entities.GetGlueObject;
import com.uwetrottmann.getglue.entities.GetGlueObjects;
import com.uwetrottmann.seriesguide.R;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.widget.Toast;

import retrofit.RetrofitError;

public class GetGlueCheckin {

    private static final String TAG = "GetGlue";

    public static final String OAUTH_CALLBACK_URL = "http://seriesgui.de";

    public static class CheckInTask extends AsyncTask<Void, Void, Integer> {

        private static final int CHECKIN_SUCCESSFUL = 0;

        private static final int CHECKIN_FAILED = 1;

        private static final int CHECKIN_OFFLINE = 2;

        private String mObjectId;

        private String mComment;

        private Context mContext;

        public CheckInTask(String objectId, String comment, Context context) {
            mObjectId = objectId;
            mComment = comment;
            mContext = context;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (!AndroidUtils.isNetworkConnected(mContext)) {
                return CHECKIN_OFFLINE;
            }

            // ensure there is a valid access token, get a new one if it is expired
            if (GetGlueSettings.isAuthTokenExpired(mContext)) {
                boolean gotNewTokens = GetGlueAuthActivity.fetchAndStoreTokens(mContext,
                        GetGlueSettings.getRefreshToken(mContext));
                if (!gotNewTokens) {
                    // abort, user needs to re-authenticate
                    return CHECKIN_FAILED;
                }
            }

            GetGlue getglue = new GetGlue();
            getglue.setAccessToken(GetGlueSettings.getAuthToken(mContext));

            try {
                // search for an id if only a title was given
                if (!(mObjectId.startsWith("tv_shows") || mObjectId.startsWith("movies"))) {
                    GetGlueObjects response = getglue.searchService().searchAnyObject(mObjectId);
                    if (response != null
                            && response.objects != null
                            && response.objects.size() > 0) {
                        GetGlueObject glueObject = response.objects.get(0);
                        mObjectId = glueObject.id;
                    }
                }

                // do the checkin
                GetGlueInteraction checkin;
                if (TextUtils.isEmpty(mComment)) {
                    checkin = getglue.objectService().checkin(mObjectId);
                } else {
                    checkin = getglue.objectService().checkin(mObjectId, mComment);
                }

                // get title of checked in item
                GetGlueInteractionResource interaction = getglue.interactionService()
                        .get(checkin.id);
                if (interaction != null
                        && interaction.interaction != null
                        && interaction.interaction._object != null
                        && interaction.interaction._object.title != null) {
                    mComment = interaction.interaction._object.title;
                } else {
                    mComment = "";
                }

                return CHECKIN_SUCCESSFUL;
            } catch (RetrofitError e) {
                Utils.trackExceptionAndLog(mContext, TAG, e);
            }

            return CHECKIN_FAILED;
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                case CHECKIN_SUCCESSFUL:
                    Toast.makeText(mContext, mContext.getString(R.string.checkinsuccess, mComment),
                            Toast.LENGTH_SHORT).show();
                    Utils.trackCustomEvent(mContext, TAG, "Check-In", "Success");
                    break;
                case CHECKIN_FAILED:
                    Toast.makeText(mContext, mContext.getString(R.string.checkinfailed),
                            Toast.LENGTH_LONG).show();
                    Utils.trackCustomEvent(mContext, TAG, "Check-In", "Failure");
                    break;
                case CHECKIN_OFFLINE:
                    Toast.makeText(mContext, R.string.offline, Toast.LENGTH_LONG).show();
            }
        }

    }

}
