
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;

import android.os.Bundle;

public class TraktShoutsActivity extends SherlockFragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane_empty);

        // FIXME I don't like that I had to wrap everything here in a Bundle
        // again
        Bundle args = getIntent().getExtras();
        String title = args.getString(ShareItems.SHARESTRING);
        int tvdbId = args.getInt(ShareItems.TVDBID);
        int episode = args.getInt(ShareItems.EPISODE);

        final ActionBar actionBar = getSupportActionBar();
        setTitle(getString(R.string.shouts_for, title));
        actionBar.setTitle(getString(R.string.shouts_for, title));

        // embed the shouts fragment dialog
        SherlockDialogFragment newFragment;
        if (episode == 0) {
            newFragment = TraktShoutsFragment.newInstance(title, tvdbId);
        } else {
            int season = args.getInt(ShareItems.SEASON);
            newFragment = TraktShoutsFragment.newInstance(title, tvdbId, season, episode);
        }
        getSupportFragmentManager().beginTransaction().replace(R.id.root_container, newFragment)
                .commit();
    }
}
