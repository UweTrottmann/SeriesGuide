
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.google.analytics.tracking.android.EasyTracker;

import android.os.Bundle;

public class TraktShoutsActivity extends BaseActivity {

    public static Bundle createInitBundle(int showTvdbid, int seasonNumber, int episodeNumber,
            String title) {
        Bundle extras = new Bundle();
        extras.putInt(ShareItems.TVDBID, showTvdbid);
        extras.putInt(ShareItems.SEASON, seasonNumber);
        extras.putInt(ShareItems.EPISODE, episodeNumber);
        extras.putString(ShareItems.SHARESTRING, title);
        return extras;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane_empty);

        Bundle args = getIntent().getExtras();
        String title = args.getString(ShareItems.SHARESTRING);

        final ActionBar actionBar = getSupportActionBar();
        setTitle(getString(R.string.shouts_for, ""));
        actionBar.setTitle(getString(R.string.shouts_for, ""));
        actionBar.setSubtitle(title);

        // embed the shouts fragment dialog
        SherlockDialogFragment newFragment;
        int tvdbId = args.getInt(ShareItems.TVDBID);
        int episode = args.getInt(ShareItems.EPISODE);
        if (episode == 0) {
            newFragment = TraktShoutsFragment.newInstance(title, tvdbId);
        } else {
            int season = args.getInt(ShareItems.SEASON);
            newFragment = TraktShoutsFragment.newInstance(title, tvdbId, season, episode);
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, newFragment)
                .commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }
}
