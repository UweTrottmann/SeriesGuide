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

package com.battlelancer.seriesguide.ui.dialogs;

import com.battlelancer.seriesguide.getglueapi.GetGlueCheckin;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.settings.GetGlueSettings;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

/**
 * Allows to check into an episode on trakt, into a show on GetGlue. Launching activities should
 * subscribe to {@link com.battlelancer.seriesguide.util.TraktTask.TraktActionCompleteEvent} to
 * display status toasts.
 */
public class CheckInDialogFragment extends GenericCheckInDialogFragment {

    /**
     * Builds a new {@link CheckInDialogFragment} setting all values based on the given episode TVDb
     * id. Might return null.
     */
    public static CheckInDialogFragment newInstance(Context context, int episodeTvdbId) {
        CheckInDialogFragment f = null;

        final Cursor episode = context.getContentResolver().query(
                Episodes.buildEpisodeWithShowUri(episodeTvdbId),
                CheckInQuery.PROJECTION, null, null, null);
        if (episode != null) {
            if (episode.moveToFirst()) {
                f = new CheckInDialogFragment();
                Bundle args = new Bundle();
                args.putString(InitBundle.TITLE, episode.getString(CheckInQuery.SHOW_TITLE));
                args.putInt(InitBundle.SHOW_TVDB_ID, episode.getInt(CheckInQuery.SHOW_TVDB_ID));
                args.putInt(InitBundle.SEASON, episode.getInt(CheckInQuery.SEASON));
                args.putInt(InitBundle.EPISODE, episode.getInt(CheckInQuery.NUMBER));
                args.putString(InitBundle.SHOW_GETGLUE_ID,
                        episode.getString(CheckInQuery.SHOW_GETGLUE_ID));

                String episodeTitleWithNumbers = ShareUtils.onCreateShareString(context, episode);
                args.putString(InitBundle.ITEM_TITLE, episodeTitleWithNumbers);
                args.putString(InitBundle.DEFAULT_MESSAGE, episodeTitleWithNumbers);
                f.setArguments(args);
            }
            episode.close();
        }

        return f;
    }

    private interface CheckInQuery {

        String[] PROJECTION = new String[]{
                Episodes._ID, Episodes.SEASON, Episodes.NUMBER, Episodes.TITLE, Shows.REF_SHOW_ID,
                Shows.GETGLUEID, Shows.TITLE
        };
        int EPISODE_TVDB_ID = 0;
        int SEASON = 1;
        int NUMBER = 2;
        int EPISODE_TITLE = 3;
        int SHOW_TVDB_ID = 4;
        int SHOW_GETGLUE_ID = 5;
        int SHOW_TITLE = 6;
    }

    private String mGetGlueId;

    private int mShowTvdbId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mShowTvdbId = getArguments().getInt(InitBundle.SHOW_TVDB_ID);
        mGetGlueId = getArguments().getString(InitBundle.SHOW_GETGLUE_ID);
    }

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), "Show Check-In Dialog");
    }

    @Override
    protected void checkInGetGlue(String title, String message) {
        // check in, use task on thread pool
        AndroidUtils
                .executeAsyncTask(new GetGlueCheckin.GetGlueCheckInTask(mGetGlueId, message,
                        getActivity()));
    }

    @Override
    protected void checkInTrakt(String message) {
        final int season = getArguments().getInt(InitBundle.SEASON);
        final int episode = getArguments().getInt(InitBundle.EPISODE);

        AndroidUtils.executeAsyncTask(
                new TraktTask(getActivity())
                        .checkInEpisode(mShowTvdbId, season, episode, message));
    }

    @Override
    protected void handleGetGlueToggle(boolean isChecked) {
        if (isChecked) {
            if (!GetGlueSettings.isAuthenticated(getActivity())) {
                ensureGetGlueAuthAndConnection();
            } else if (TextUtils.isEmpty(mGetGlueId)) {
                // the user has to set a GetGlue object id
                launchFixGetGlueCheckInActivity(mCheckBoxGetGlue, mShowTvdbId);
            }
        }
    }

    @Override
    protected void setupButtonFixGetGlue(View layout) {
        mButtonFixGetGlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchFixGetGlueCheckInActivity(v, mShowTvdbId);
            }
        });
    }

    @Override
    protected boolean setupCheckInGetGlue() {
        // always get the latest GetGlue id
        final Cursor show = getActivity().getContentResolver().query(
                Shows.buildShowUri(String.valueOf(mShowTvdbId)), new String[]{
                Shows._ID, Shows.GETGLUEID
        }, null, null, null);
        if (show == null || !show.moveToFirst()) {
            return false;
        }

        mGetGlueId = show.getString(1);

        show.close();

        // check for GetGlue id
        return !TextUtils.isEmpty(mGetGlueId);
    }

}
