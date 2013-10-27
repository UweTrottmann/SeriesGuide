
package com.battlelancer.seriesguide.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.getglueapi.GetGlueCheckin.CheckInTask;
import com.battlelancer.seriesguide.provider.SeriesContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.settings.GetGlueSettings;
import com.battlelancer.seriesguide.ui.dialogs.TraktCancelCheckinDialogFragment;
import com.battlelancer.seriesguide.util.ShareUtils.ProgressDialog;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.TraktTask.OnTraktActionCompleteListener;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

/**
 * Blank activity, just used to quickly check into a show/episode on
 * GetGlue/trakt.
 */
public class QuickCheckInActivity extends SherlockFragmentActivity implements
        OnTraktActionCompleteListener {

    public interface InitBundle {
        String EPISODE_TVDBID = "episode_tvdbid";
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onCreate(Bundle arg0) {
        // make the activity show the wallpaper, nothing else
        if (AndroidUtils.isHoneycombOrHigher()) {
            setTheme(android.R.style.Theme_Holo_Wallpaper_NoTitleBar);
        } else {
            setTheme(android.R.style.Theme_Translucent_NoTitleBar);
        }
        super.onCreate(arg0);

        int episodeId = getIntent().getIntExtra(InitBundle.EPISODE_TVDBID, 0);
        if (episodeId == 0) {
            finish();
            return;
        }

        final Cursor episodeWithShow = getContentResolver()
                .query(Episodes.buildEpisodeWithShowUri(String.valueOf(episodeId)),
                        new String[] {
                                Episodes._ID, Episodes.TITLE, Episodes.SEASON, Episodes.NUMBER,
                                Shows.REF_SHOW_ID, Shows.TITLE
                        },
                        null,
                        null,
                        null);
        if (episodeWithShow == null) {
            finish();
            return;
        }

        if (!episodeWithShow.moveToFirst()) {
            episodeWithShow.close();
            finish();
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // get values
        String title = episodeWithShow.getString(1);
        int season = episodeWithShow.getInt(2);
        int episode = episodeWithShow.getInt(3);
        int showTvdbId = episodeWithShow.getInt(4);
        String showTitle = episodeWithShow.getString(5);
        episodeWithShow.close();
        String defaultMessage = Utils.getNextEpisodeString(prefs, season, episode, title);

        // get share service enabled settings
        boolean isShareWithGetGlue = prefs.getBoolean(SeriesGuidePreferences.KEY_SHAREWITHGETGLUE,
                false);
        boolean isShareWithTrakt = prefs.getBoolean(SeriesGuidePreferences.KEY_SHAREWITHTRAKT,
                false);

        if (isShareWithTrakt) {
            // We want to remove any currently showing
            // dialog, so make our own transaction and
            // take care of that here.
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentByTag("progress-dialog");
            if (prev != null) {
                ft.remove(prev);
            }
            ProgressDialog newFragment = ProgressDialog.newInstance();
            newFragment.show(ft, "progress-dialog");

            // check in with trakt
            AndroidUtils
                    .executeAsyncTask(
                            new TraktTask(this, this)
                                    .checkInEpisode(showTvdbId, season, episode, defaultMessage),
                            new Void[] {});
        }

        if (isShareWithGetGlue) {
            boolean isAbortingCheckIn = false;
            String objectId = null;

            // require GetGlue authentication
            if (!GetGlueSettings.isAuthenticated(this)) {
                isAbortingCheckIn = true;
            } else {
                Cursor show = getContentResolver().query(
                        Shows.buildShowUri(String.valueOf(showTvdbId)), new String[] {
                                Shows._ID, Shows.GETGLUEID
                        }, null, null, null);
                if (show != null) {
                    show.moveToFirst();
                    objectId = show.getString(1);
                    show.close();
                }

                // fall back to IMDb id
                if (TextUtils.isEmpty(objectId)) {
                    if (TextUtils.isEmpty(showTitle)) {
                        // cancel if we don't know what to check into
                        isAbortingCheckIn = true;
                    } else {
                        objectId = showTitle;
                    }
                }
            }

            if (!isAbortingCheckIn) {
                // check in, use task on thread pool
                AndroidUtils.executeAsyncTask(new CheckInTask(objectId, defaultMessage,
                        this), new Void[] {});
            }
        }

        if (!isShareWithTrakt) {
            if (!isShareWithGetGlue) {
                // No service is setup, direct user to within SeriesGuide
                Toast.makeText(this, R.string.checkin_noaction, Toast.LENGTH_LONG).show();
                Intent intent = new Intent(this, EpisodesActivity.class);
                intent.putExtra(EpisodesActivity.InitBundle.EPISODE_TVDBID, episodeId);
                startActivity(intent);
            }

            // GetGlue check-ins do not need an activity to stay active
            finish();
        }
    }

    @Override
    public void onTraktActionComplete(Bundle traktTaskArgs, boolean wasSuccessfull) {
        dismissProgressDialog(traktTaskArgs);
        finish();
    }

    @SuppressLint("CommitTransaction")
    @Override
    public void onCheckinBlocked(Bundle traktTaskArgs, int wait) {
        dismissProgressDialog(traktTaskArgs);
        TraktCancelCheckinDialogFragment newFragment = TraktCancelCheckinDialogFragment
                .newInstance(traktTaskArgs, wait);
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        newFragment.show(ft, "cancel-checkin-dialog");
    }

    private void dismissProgressDialog(Bundle traktTaskArgs) {
        TraktAction action = TraktAction.values()[traktTaskArgs
                .getInt(TraktTask.InitBundle.TRAKTACTION)];
        // dismiss a potential progress dialog
        if (action == TraktAction.CHECKIN_EPISODE || action ==
                TraktAction.CHECKIN_MOVIE) {
            Fragment prev = getSupportFragmentManager().findFragmentByTag("progress-dialog");
            if (prev != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.remove(prev);
                ft.commit();
            }
        }
    }

}
