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

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;

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
                args.putInt(InitBundle.EPISODE_TVDB_ID, episodeTvdbId);
                String episodeTitleWithNumbers = episode.getString(CheckInQuery.SHOW_TITLE)
                        + " "
                        + Utils.getNextEpisodeString(context,
                        episode.getInt(CheckInQuery.SEASON),
                        episode.getInt(CheckInQuery.NUMBER),
                        episode.getString(CheckInQuery.EPISODE_TITLE));
                args.putString(InitBundle.ITEM_TITLE, episodeTitleWithNumbers);

                f.setArguments(args);
            }
            episode.close();
        }

        return f;
    }

    private interface CheckInQuery {

        String[] PROJECTION = new String[] {
                Episodes.SEASON,
                Episodes.NUMBER,
                Episodes.TITLE,
                Shows.TITLE
        };

        int SEASON = 0;
        int NUMBER = 1;
        int EPISODE_TITLE = 2;
        int SHOW_TITLE = 3;
    }

    @Override
    public void onStart() {
        super.onStart();

        Utils.trackView(getActivity(), "Show Check-In Dialog");
    }

    @Override
    protected void checkInTrakt(String message) {
        AndroidUtils.executeOnPool(
                new TraktTask(getActivity()).checkInEpisode(
                        getArguments().getInt(InitBundle.EPISODE_TVDB_ID),
                        getArguments().getString(InitBundle.ITEM_TITLE),
                        message)
        );
    }
}
