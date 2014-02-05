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

import com.battlelancer.seriesguide.enums.NetworkResult;
import com.battlelancer.seriesguide.enums.Result;
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

import de.greenrobot.event.EventBus;
import retrofit.RetrofitError;

public class GetGlueCheckin {

    private static final String TAG = "GetGlue";

    public static final String OAUTH_CALLBACK_URL = "http://seriesgui.de";

    public static class GetGlueCheckInTask extends AsyncTask<Void, Void, Integer> {

        public static class GetGlueCheckInCompleteEvent {

            /**
             * One of {@link com.battlelancer.seriesguide.enums.NetworkResult}.
             */
            public int statusCode;

            /**
             * The title of the show or movie that was checked in.
             */
            public String objectTitle;

            public GetGlueCheckInCompleteEvent(int statusCode, String objectTitle) {
                this.statusCode = statusCode;
                this.objectTitle = objectTitle;
            }

            public void handle(Context context) {
                switch (statusCode) {
                    case Result.SUCCESS:
                        Toast.makeText(context,
                                context.getString(R.string.checkinsuccess, objectTitle),
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Result.ERROR:
                        Toast.makeText(context, context.getString(R.string.checkinfailed),
                                Toast.LENGTH_LONG).show();
                        break;
                    case NetworkResult.OFFLINE:
                        Toast.makeText(context, R.string.offline, Toast.LENGTH_LONG).show();
                }
            }
        }

        private String mObjectId;

        private String mComment;

        private Context mContext;

        public GetGlueCheckInTask(String objectId, String comment, Context context) {
            mObjectId = objectId;
            mComment = comment;
            mContext = context;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            if (!AndroidUtils.isNetworkConnected(mContext)) {
                return NetworkResult.OFFLINE;
            }

            // ensure there is a valid access token, get a new one if it is expired
            if (GetGlueSettings.isAuthTokenExpired(mContext)) {
                boolean gotNewTokens = GetGlueAuthActivity.fetchAndStoreTokens(mContext,
                        GetGlueSettings.getRefreshToken(mContext));
                if (!gotNewTokens) {
                    // abort, user needs to re-authenticate
                    return NetworkResult.ERROR;
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

                return Result.SUCCESS;
            } catch (RetrofitError e) {
                Utils.trackExceptionAndLog(mContext, TAG, e);
            }

            return Result.ERROR;
        }

        @Override
        protected void onPostExecute(Integer result) {
            EventBus.getDefault().post(new GetGlueCheckInCompleteEvent(result, mComment));

            switch (result) {
                case Result.SUCCESS:
                    Utils.trackCustomEvent(mContext, TAG, "Check-In", "Success");
                    break;
                case Result.ERROR:
                    Utils.trackCustomEvent(mContext, TAG, "Check-In", "Failure");
                    break;
            }
        }

    }

}
