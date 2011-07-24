
package com.battlelancer.seriesguide;

import com.battlelancer.seriesguide.util.ActivityHelper;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

public class UpcomingRecent extends TabActivity {

    final ActivityHelper mActivityHelper = ActivityHelper.createInstance(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.upcomingrecent);
        mActivityHelper.setupActionBar(getString(R.string.upcoming));

        TabHost tabHost = getTabHost(); // The activity TabHost
        TabHost.TabSpec spec; // Resusable TabSpec for each tab
        Intent intent; // Reusable Intent for each tab
        View tab;
        TextView title;

        // Upcoming episodes
        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, UpcomingEpisodes.class);

        // Initialize a TabSpec for each tab and add it to the TabHost
        tab = getLayoutInflater().inflate(R.layout.tab_indicator_holo, null);
        title = (TextView) tab.findViewById(R.id.title);
        title.setText(R.string.upcoming);
        spec = tabHost.newTabSpec("upcoming").setIndicator(tab).setContent(intent);
        tabHost.addTab(spec);

        // Recent episodes
        intent = new Intent().setClass(this, RecentEpisodes.class);
        tab = getLayoutInflater().inflate(R.layout.tab_indicator_holo, null);
        title = (TextView) tab.findViewById(R.id.title);
        title.setText(R.string.recent);
        spec = tabHost.newTabSpec("recent").setIndicator(tab).setContent(intent);
        tabHost.addTab(spec);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mActivityHelper.onOptionsItemSelected(item)) {
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

}
