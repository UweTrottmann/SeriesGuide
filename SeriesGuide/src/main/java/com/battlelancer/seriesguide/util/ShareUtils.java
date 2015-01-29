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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import android.support.annotation.StringRes;
import android.support.v4.app.ShareCompat;
import android.support.v4.app.ShareCompat.IntentBuilder;
import android.text.format.DateUtils;
import android.widget.Toast;
import com.battlelancer.seriesguide.R;

/**
 * Contains various ways to share something about an episode (android share intent, trakt, calendar
 * event, ...).
 *
 * @author Uwe Trottmann
 */
public class ShareUtils {

    protected static final String TAG = "ShareUtils";

    public static void shareEpisode(Activity activity, int episodeTvdbId, int seasonNumber,
            int episodeNumber, String showTitle, String episodeTitle) {
        String message = activity.getString(R.string.share_checkout,
                showTitle + " " + Utils.getNextEpisodeString(activity, seasonNumber, episodeNumber,
                        episodeTitle))
                + " " + TraktTools.buildEpisodeOrShowUrl(episodeTvdbId);
        startShareIntentChooser(activity, message, R.string.share_episode);
    }

    public static void shareShow(Activity activity, int showTvdbId, String showTitle) {
        String message = activity.getString(R.string.share_checkout, showTitle) + " "
                + TraktTools.buildEpisodeOrShowUrl(showTvdbId);
        startShareIntentChooser(activity, message, R.string.share_show);
    }

    public static void shareMovie(Activity activity, int movieTmdbId, String movieTitle) {
        String message = activity.getString(R.string.share_checkout, movieTitle) + " "
                + TraktTools.buildMovieUrl(movieTmdbId);
        startShareIntentChooser(activity, message, R.string.share_movie);
    }

    /**
     * Share a text snippet. Displays a share intent chooser with the given title, share type is
     * text/plain.
     */
    public static void startShareIntentChooser(Activity activity, String message,
            @StringRes int titleResId) {
        IntentBuilder ib = ShareCompat.IntentBuilder.from(activity);
        ib.setText(message);
        ib.setChooserTitle(titleResId);
        ib.setType("text/plain");
        try {
            ib.startChooser();
        } catch (ActivityNotFoundException e) {
            // no activity available to handle the intent
            Toast.makeText(activity, R.string.app_not_available, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Launches a calendar insert intent for the given episode.
     */
    public static void suggestCalendarEvent(Context context, String showTitle, String episodeTitle,
            long episodeReleaseTime, int showRunTime) {
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, showTitle)
                .putExtra(CalendarContract.Events.DESCRIPTION, episodeTitle);

        long beginTime = TimeTools.applyUserOffset(context, episodeReleaseTime).getTime();
        long endTime = beginTime + showRunTime * DateUtils.MINUTE_IN_MILLIS;
        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime);
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime);

        if (!Utils.tryStartActivity(context, intent, false)) {
            Toast.makeText(context, context.getString(R.string.addtocalendar_failed),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
