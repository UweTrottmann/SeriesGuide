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

package com.battlelancer.seriesguide.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.ShareCompat;
import android.support.v4.app.ShareCompat.IntentBuilder;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;

/**
 * Contains various ways to share something about an episode (android share intent, trakt, calendar
 * event, ...).
 *
 * @author Uwe Trottmann
 */
public class ShareUtils {

    public static final String KEY_GETGLUE_COMMENT = "com.battlelancer.seriesguide.getglue.comment";

    public static final String KEY_GETGLUE_IMDBID = "com.battlelancer.seriesguide.getglue.imdbid";

    protected static final String TAG = "ShareUtils";

    public enum ShareMethod {

        RATE_TRAKT,

        OTHER_SERVICES

    }

    public interface ShareItems {

        String SEASON = "season";

        String IMDBID = "imdbId";

        String SHARESTRING = "sharestring";

        String EPISODESTRING = "episodestring";

        String EPISODE = "episode";

        String TVDBID = "tvdbid";

        String TMDBID = "tmdbid";

        String RATING = "rating";

        String TRAKTACTION = "traktaction";

        String ISSPOILER = "isspoiler";
    }

    public static void shareEpisode(Activity activity, int showTvdbId, int seasonNumber,
            int episodeNumber, String showTitle, String episodeTitle) {
        String message = activity.getString(R.string.share_checkout,
                showTitle + " " + Utils.getNextEpisodeString(activity, seasonNumber, episodeNumber, episodeTitle))
                + " " + TraktTools.buildEpisodeOrShowUrl(showTvdbId, seasonNumber, episodeNumber);
        startShareIntentChooser(activity, message, R.string.share_episode);
    }

    public static void shareShow(Activity activity, int showTvdbId, String showTitle) {
        String message = activity.getString(R.string.share_checkout, showTitle) + " "
                + TraktTools.buildEpisodeOrShowUrl(showTvdbId, -1, -1);
        startShareIntentChooser(activity, message, R.string.share_show);
    }

    public static void shareMovie(Activity activity, int movieTmdbId, String movieTitle) {
        String message = activity.getString(R.string.share_checkout, movieTitle) + " "
                + TraktTools.buildMovieUrl(movieTmdbId);
        startShareIntentChooser(activity, message, R.string.share_movie);
    }

    private static void startShareIntentChooser(Activity activity, String message, int titleResId) {
        IntentBuilder ib = ShareCompat.IntentBuilder.from(activity);
        ib.setText(message);
        ib.setChooserTitle(titleResId);
        ib.setType("text/plain");
        ib.startChooser();
    }

    public static String onCreateShareString(Context context, final Cursor episode) {
        int season = episode.getInt(episode.getColumnIndexOrThrow(Episodes.SEASON));
        int number = episode.getInt(episode.getColumnIndexOrThrow(Episodes.NUMBER));
        String title = episode.getString(episode.getColumnIndexOrThrow(Episodes.TITLE));
        return Utils.getNextEpisodeString(context, season, number, title);
    }

    public static void onAddCalendarEvent(Context context, String showTitle, String episodeTitle,
            long episodeReleaseTime, int showRunTime) {
        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType("vnd.android.cursor.item/event");
        intent.putExtra("title", showTitle);
        intent.putExtra("description", episodeTitle);

        long startTime = TimeTools.getEpisodeReleaseTime(context, episodeReleaseTime).getTime();
        long endTime = startTime + showRunTime * DateUtils.MINUTE_IN_MILLIS;
        intent.putExtra("beginTime", startTime);
        intent.putExtra("endTime", endTime);

        if (!Utils.tryStartActivity(context, intent, false)) {
            Toast.makeText(context, context.getString(R.string.addtocalendar_failed),
                    Toast.LENGTH_SHORT).show();
            Utils.trackCustomEvent(context, TAG, "Calendar", "Failed");
        }
    }

    public static class ProgressDialog extends DialogFragment {

        public static ProgressDialog newInstance() {
            ProgressDialog f = new ProgressDialog();
            f.setCancelable(false);
            return f;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            setStyle(STYLE_NO_TITLE, 0);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.progress_dialog, container, false);
        }
    }
}
