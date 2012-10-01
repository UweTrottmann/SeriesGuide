/*
 * Copyright 2011 Uwe Trottmann
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

package com.battlelancer.seriesguide.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.app.ShareCompat.IntentBuilder;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.ui.dialogs.TraktRateDialogFragment;
import com.battlelancer.seriesguide.util.TraktTask.OnTraktActionCompleteListener;
import com.google.analytics.tracking.android.EasyTracker;

import java.util.Calendar;

/**
 * Contains various ways to share something about an episode (android share
 * intent, trakt, calendar event, ...).
 * 
 * @author Uwe Trottmann
 */
public class ShareUtils {

    public static final String KEY_GETGLUE_COMMENT = "com.battlelancer.seriesguide.getglue.comment";

    public static final String KEY_GETGLUE_IMDBID = "com.battlelancer.seriesguide.getglue.imdbid";

    protected static final String TAG = "ShareUtils";

    public enum ShareMethod {
        RATE_TRAKT(0, R.string.menu_rate_trakt, R.drawable.trakt_love_large),

        OTHER_SERVICES(1, R.string.menu_share_others, R.drawable.ic_action_share);

        ShareMethod(int index, int titleRes, int drawableRes) {
            this.index = index;
            this.titleRes = titleRes;
            this.drawableRes = drawableRes;
        }

        public int index;

        public int titleRes;

        public int drawableRes;
    }

    public interface ShareItems {
        String SEASON = "season";

        String IMDBID = "imdbId";

        String SHARESTRING = "sharestring";

        String EPISODESTRING = "episodestring";

        String EPISODE = "episode";

        String TVDBID = "tvdbid";

        String RATING = "rating";

        String TRAKTACTION = "traktaction";

        String ISSPOILER = "isspoiler";
    }

    /**
     * Share an episode via the given {@link ShareMethod}.
     * 
     * @param activity
     * @param args - a {@link Bundle} including all
     *            {@link ShareUtils.ShareItems}
     * @param shareMethod the {@link ShareMethod} to use
     * @param listener listener will be notified once trakt task is finished
     *            (only first time)
     */
    public static void onShareEpisode(FragmentActivity activity, Bundle args,
            ShareMethod shareMethod, OnTraktActionCompleteListener listener) {
        final FragmentManager fm = activity.getSupportFragmentManager();

        switch (shareMethod) {
            case RATE_TRAKT: {
                // trakt rate
                TraktRateDialogFragment newFragment = TraktRateDialogFragment.newInstance(
                        args.getInt(ShareItems.TVDBID), args.getInt(ShareItems.SEASON),
                        args.getInt(ShareItems.EPISODE));
                newFragment.show(fm, "traktratedialog");
                break;
            }
            case OTHER_SERVICES: {
                // Android apps
                IntentBuilder ib = ShareCompat.IntentBuilder.from(activity);

                String text = args.getString(ShareUtils.ShareItems.SHARESTRING);
                final String imdbId = args.getString(ShareUtils.ShareItems.IMDBID);
                if (imdbId.length() != 0) {
                    text += " " + Utils.IMDB_TITLE_URL + imdbId;
                }

                ib.setText(text);
                ib.setChooserTitle(R.string.share_episode);
                ib.setType("text/plain");
                ib.startChooser();
                break;
            }
        }
    }

    public static String onCreateShareString(Context context, final Cursor episode) {
        int season = episode.getInt(episode.getColumnIndexOrThrow(Episodes.SEASON));
        int number = episode.getInt(episode.getColumnIndexOrThrow(Episodes.NUMBER));
        String title = episode.getString(episode.getColumnIndexOrThrow(Episodes.TITLE));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return Utils.getNextEpisodeString(prefs, season, number, title);
    }

    public static void onAddCalendarEvent(Context context, String title, String description,
            long airtime, int runtime) {
        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType("vnd.android.cursor.item/event");
        intent.putExtra("title", title);
        intent.putExtra("description", description);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context
                .getApplicationContext());

        Calendar cal = Utils.getAirtimeCalendar(airtime, prefs);

        long startTime = cal.getTimeInMillis();
        long endTime = startTime + runtime * DateUtils.MINUTE_IN_MILLIS;
        intent.putExtra("beginTime", startTime);
        intent.putExtra("endTime", endTime);

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            EasyTracker.getTracker().trackEvent(TAG, "Calendar", "Failed", (long) 0);
            Toast.makeText(context, context.getString(R.string.addtocalendar_failed),
                    Toast.LENGTH_SHORT).show();
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
            View v = inflater.inflate(R.layout.progress_dialog, container, false);
            return v;
        }
    }
}
