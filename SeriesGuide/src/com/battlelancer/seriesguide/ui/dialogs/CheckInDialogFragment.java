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

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.battlelancer.seriesguide.getglueapi.GetGlue;
import com.battlelancer.seriesguide.getglueapi.GetGlue.CheckInTask;
import com.battlelancer.seriesguide.getglueapi.GetGlueAuthActivity;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.ui.FixGetGlueCheckInActivity;
import com.battlelancer.seriesguide.util.TraktTask;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

public class CheckInDialogFragment extends GenericCheckInDialogFragment {

    public static CheckInDialogFragment newInstance(String imdbid, int tvdbid, int season,
            int episode, String defaultMessage) {
        CheckInDialogFragment f = new CheckInDialogFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.IMDB_ID, imdbid);
        args.putInt(InitBundle.TVDB_ID, tvdbid);
        args.putInt(InitBundle.SEASON, season);
        args.putInt(InitBundle.EPISODE, episode);
        args.putString(InitBundle.ITEM_TITLE, defaultMessage);
        args.putString(InitBundle.DEFAULT_MESSAGE, defaultMessage);
        f.setArguments(args);

        return f;
    }

    private String mGetGlueId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View layout = super.onCreateView(inflater, container, savedInstanceState);

        final int tvdbId = getArguments().getInt(InitBundle.TVDB_ID);
        setupFixGetGlueButton(layout, true, tvdbId);

        return layout;
    }
    
    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getTracker().sendView("Show Check-In Dialog");
    }

    @Override
    protected void onGetGlueCheckin(SharedPreferences prefs, String imdbid, String message) {
        final int tvdbid = getArguments().getInt(InitBundle.TVDB_ID);
        boolean isAbortingCheckIn = false;
        String objectId = null;

        // require GetGlue authentication
        if (!GetGlue.isAuthenticated(prefs)) {
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
                if (TextUtils.isEmpty(imdbid)) {
                    // cancel if we don't know what to check into
                    isAbortingCheckIn = true;
                } else {
                    objectId = imdbid;
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
        final int tvdbid = getArguments().getInt(InitBundle.TVDB_ID);
        final int season = getArguments().getInt(InitBundle.SEASON);
        final int episode = getArguments().getInt(InitBundle.EPISODE);

        AndroidUtils.executeAsyncTask(new TraktTask(getActivity(),
                getFragmentManager(), null).checkInEpisode(tvdbid, season, episode,
                message), new Void[] {
                null
        });
    }

    @Override
    protected void handleGetGlueToggle(SharedPreferences prefs, String imdbid, boolean isChecked) {
        if (isChecked) {
            if (!GetGlue.isAuthenticated(prefs)) {
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
            } else if (TextUtils.isEmpty(imdbid) && TextUtils.isEmpty(mGetGlueId)) {
                // the user has to set a GetGlue object id
                Intent i = new Intent(getSherlockActivity(),
                        FixGetGlueCheckInActivity.class);
                startActivity(i);
            }
        }
    }

}
