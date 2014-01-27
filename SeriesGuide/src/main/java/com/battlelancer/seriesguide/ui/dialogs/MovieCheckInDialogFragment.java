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

import com.battlelancer.seriesguide.getglueapi.GetGlueCheckin.CheckInTask;
import com.battlelancer.seriesguide.settings.GetGlueSettings;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Allows to check into movies on trakt or GetGlue. Launching activities should subscribe to {@link
 * com.battlelancer.seriesguide.util.TraktTask.TraktActionCompleteEvent} to display status toasts.
 */
public class MovieCheckInDialogFragment extends GenericCheckInDialogFragment {

    public static MovieCheckInDialogFragment newInstance(String imdbId, String movieTitle) {
        MovieCheckInDialogFragment f = new MovieCheckInDialogFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.TITLE, movieTitle);
        args.putString(InitBundle.ITEM_TITLE, movieTitle);
        args.putString(InitBundle.MOVIE_IMDB_ID, imdbId);
        f.setArguments(args);

        return f;
    }

    protected final static String TAG = "Movie Check-In Dialog";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View layout = super.onCreateView(inflater, container, savedInstanceState);

        setupFixGetGlueButton(layout, false, 0);

        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), "Movie Check-In Dialog");
    }

    protected boolean checkInGetGlue(final String title, final String message) {
        boolean isAbortingCheckIn = false;

        // require GetGlue authentication
        if (!GetGlueSettings.isAuthenticated(getActivity())) {
            isAbortingCheckIn = true;
        }

        if (isAbortingCheckIn) {
            mCheckBoxGetGlue.setChecked(false);
            mGetGlueChecked = false;
            updateCheckInButtonState();
        } else {
            // check in, use task on thread pool
            AndroidUtils.executeAsyncTask(new CheckInTask(title, message,
                    getActivity()));
        }

        return isAbortingCheckIn;
    }

    /**
     * Start the trakt check in task.
     */
    protected void checkInTrakt(String message) {
        final String imdbId = getArguments().getString(InitBundle.MOVIE_IMDB_ID);
        AndroidUtils.executeAsyncTask(
                new TraktTask(getActivity()).checkInMovie(imdbId, message));
    }

    protected void handleGetGlueToggle(boolean isChecked) {
        if (isChecked) {
            if (!GetGlueSettings.isAuthenticated(getActivity())) {
                ensureGetGlueAuthAndConnection();
            }
        }
    }

}
