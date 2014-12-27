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
import com.battlelancer.seriesguide.util.ShareUtils;
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
                String showTitle = episode.getString(CheckInQuery.SHOW_TITLE);
                args.putInt(InitBundle.SHOW_TVDB_ID, episode.getInt(CheckInQuery.SHOW_TVDB_ID));
                int seasonNumber = episode.getInt(CheckInQuery.SEASON);
                args.putInt(InitBundle.SEASON, seasonNumber);
                int episodeNumber = episode.getInt(CheckInQuery.NUMBER);
                args.putInt(InitBundle.EPISODE, episode.getInt(CheckInQuery.NUMBER));

                String episodeTitleWithNumbers = showTitle + " "
                        + Utils.getNextEpisodeString(context,
                        seasonNumber,
                        episodeNumber,
                        episode.getString(CheckInQuery.TITLE));
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
                Episodes.SEASON,
                Episodes.NUMBER,
                Episodes.TITLE,
                Shows.REF_SHOW_ID,
                Shows.TITLE
        };

        int SEASON = 0;
        int NUMBER = 1;
        int TITLE = 2;
        int SHOW_TVDB_ID = 3;
        int SHOW_TITLE = 4;
    }

    private int mShowTvdbId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mShowTvdbId = getArguments().getInt(InitBundle.SHOW_TVDB_ID);
    }

    @Override
    public void onStart() {
        super.onStart();

        Utils.trackView(getActivity(), "Show Check-In Dialog");
    }

    @Override
    protected void checkInTrakt(String message) {
        final int season = getArguments().getInt(InitBundle.SEASON);
        final int episode = getArguments().getInt(InitBundle.EPISODE);

        AndroidUtils.executeOnPool(
                new TraktTask(getActivity())
                        .checkInEpisode(mShowTvdbId, season, episode, message)
        );
    }
}
