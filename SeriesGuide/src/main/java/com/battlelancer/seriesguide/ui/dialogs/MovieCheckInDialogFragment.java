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
import com.battlelancer.seriesguide.settings.GetGlueSettings;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.battlelancer.seriesguide.R;

import android.os.Bundle;
import android.view.View;

/**
 * Allows to check into movies on trakt or GetGlue. Launching activities should subscribe to {@link
 * com.battlelancer.seriesguide.util.TraktTask.TraktActionCompleteEvent} to display status toasts.
 */
public class MovieCheckInDialogFragment extends GenericCheckInDialogFragment {

    public static MovieCheckInDialogFragment newInstance(int movieTmdbId, String movieTitle,
            String originalMovieTitle) {
        MovieCheckInDialogFragment f = new MovieCheckInDialogFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.TVTAG_ID_OR_TITLE, originalMovieTitle);
        args.putString(InitBundle.ITEM_TITLE, movieTitle);
        args.putInt(InitBundle.MOVIE_TMDB_ID, movieTmdbId);
        f.setArguments(args);

        return f;
    }

    protected final static String TAG = "Movie Check-In Dialog";

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), "Movie Check-In Dialog");
    }

    protected void checkInGetGlue(final String title, final String message) {
        // check in, use task on thread pool
        AndroidUtils.executeAsyncTask(new GetGlueCheckin.GetGlueCheckInTask(title, message,
                getActivity()));
    }

    /**
     * Start the trakt check in task.
     */
    protected void checkInTrakt(String message) {
        int movieTmdbId = getArguments().getInt(InitBundle.MOVIE_TMDB_ID);
        AndroidUtils.executeAsyncTask(
                new TraktTask(getActivity()).checkInMovie(movieTmdbId, message));
    }

    protected void handleGetGlueToggle(boolean isChecked) {
        if (isChecked) {
            if (!GetGlueSettings.isAuthenticated(getActivity())) {
                ensureGetGlueAuthAndConnection();
            }
        }
    }

    @Override
    protected void setupButtonFixGetGlue(View layout) {
        View divider = layout.findViewById(R.id.dividerHorizontalCheckIn);
        divider.setVisibility(View.GONE);
        mButtonFixGetGlue.setVisibility(View.GONE);
    }
}
