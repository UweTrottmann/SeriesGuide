/*
 * Copyright 2012 Uwe Trottmann
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

package com.battlelancer.seriesguide.ui.dialogs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.battlelancer.seriesguide.getglueapi.GetGlueCheckin;
import com.battlelancer.seriesguide.getglueapi.GetGlueCheckin.CheckInTask;
import com.battlelancer.seriesguide.getglueapi.GetGlueAuthActivity;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.settings.GetGlueSettings;
import com.battlelancer.seriesguide.ui.FixGetGlueCheckInActivity;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.TraktTask.OnTraktActionCompleteListener;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

/**
 * Allows to check into an episode on trakt, into a show on GetGlue. Launching
 * activities must implement {@link OnTraktActionCompleteListener}.
 */
public class CheckInDialogFragment extends GenericCheckInDialogFragment {

    /**
     * Builds a new {@link CheckInDialogFragment} setting all values based on
     * the given episode TVDb id. Might return null.
     */
    public static CheckInDialogFragment newInstance(Context context, int episodeTvdbId) {
        final Cursor episode = context.getContentResolver().query(
                Episodes.buildEpisodeWithShowUri(episodeTvdbId),
                CheckInQuery.PROJECTION, null, null, null);

        CheckInDialogFragment f = null;

        if (episode != null) {
            if (episode.moveToFirst()) {
                f = new CheckInDialogFragment();
                Bundle args = new Bundle();
                args.putString(InitBundle.TITLE, episode.getString(CheckInQuery.SHOW_TITLE));
                args.putInt(InitBundle.SHOW_TVDB_ID, episode.getInt(CheckInQuery.SHOW_TVDB_ID));
                args.putInt(InitBundle.SEASON, episode.getInt(CheckInQuery.SEASON));
                args.putInt(InitBundle.EPISODE, episode.getInt(CheckInQuery.NUMBER));

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
        String[] PROJECTION = new String[] {
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = super.onCreateView(inflater, container, savedInstanceState);

        final int tvdbId = getArguments().getInt(InitBundle.SHOW_TVDB_ID);
        setupFixGetGlueButton(layout, true, tvdbId);

        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getTracker().sendView("Show Check-In Dialog");
    }

    @Override
    protected void onGetGlueCheckin(String title, String message) {
        final int tvdbid = getArguments().getInt(InitBundle.SHOW_TVDB_ID);
        boolean isAbortingCheckIn = false;
        String objectId = null;

        // require GetGlue authentication
        if (!GetGlueSettings.isAuthenticated(getActivity())) {
            isAbortingCheckIn = true;
        } else {
            Cursor show = getActivity().getContentResolver().query(
                    Shows.buildShowUri(String.valueOf(tvdbid)), new String[] {
                            Shows._ID, Shows.GETGLUEID
                    }, null, null, null);
            if (show != null) {
                show.moveToFirst();
                mGetGlueId = show.getString(1);
                show.close();
            }

            // fall back to IMDb id
            if (TextUtils.isEmpty(mGetGlueId)) {
                if (TextUtils.isEmpty(title)) {
                    // cancel if we don't know what to check into
                    isAbortingCheckIn = true;
                } else {
                    objectId = title;
                }
            } else {
                objectId = mGetGlueId;
            }
        }

        if (isAbortingCheckIn) {
            mToggleGetGlueButton.setChecked(false);
            mGetGlueChecked = false;
            updateCheckInButtonState();
            return;
        } else {
            // check in, use task on thread pool
            AndroidUtils.executeAsyncTask(new CheckInTask(objectId, message,
                    getActivity()), new Void[] {});
        }
    }

    @Override
    protected void onTraktCheckIn(String message) {
        final int tvdbid = getArguments().getInt(InitBundle.SHOW_TVDB_ID);
        final int season = getArguments().getInt(InitBundle.SEASON);
        final int episode = getArguments().getInt(InitBundle.EPISODE);

        AndroidUtils.executeAsyncTask(
                new TraktTask(getActivity(), mListener)
                        .checkInEpisode(tvdbid, season, episode, message),
                new Void[] {
                    null
                });
    }

    @Override
    protected void handleGetGlueToggle(boolean isChecked) {
        if (isChecked) {
            if (!GetGlueSettings.isAuthenticated(getActivity())) {
                if (!AndroidUtils.isNetworkConnected(getActivity())) {
                    Toast.makeText(getActivity(), R.string.offline, Toast.LENGTH_LONG)
                            .show();
                    mToggleGetGlueButton.setChecked(false);
                    return;
                } else {
                    // authenticate already here
                    Intent i = new Intent(getSherlockActivity(),
                            GetGlueAuthActivity.class);
                    startActivity(i);
                }
            } else if (TextUtils.isEmpty(mGetGlueId)) {
                // the user has to set a GetGlue object id
                Intent i = new Intent(getSherlockActivity(),
                        FixGetGlueCheckInActivity.class);
                startActivity(i);
            }
        }
    }

}
