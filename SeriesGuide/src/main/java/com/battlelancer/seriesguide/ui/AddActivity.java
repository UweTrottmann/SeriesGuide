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

package com.battlelancer.seriesguide.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.EditText;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Window;
import com.astuetz.PagerSlidingTabStrip;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.dialogs.AddDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.AddDialogFragment.OnAddShowListener;
import com.battlelancer.seriesguide.util.TaskManager;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.util.Locale;

/**
 * Hosts various fragments in a {@link ViewPager} which allow adding shows to the database.
 */
public class AddActivity extends BaseNavDrawerActivity implements OnAddShowListener {

    public interface InitBundle {

        /**
         * Which tab to select upon launch.
         */
        String DEFAULT_TAB = "default_tab";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // The TvdbAddFragment uses a progress bar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addactivity_pager);
        setupNavDrawer();

        setupActionBar();

        setupViews();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setIcon(R.drawable.ic_action_new_show);
        actionBar.setDisplayHomeAsUpEnabled(true);
        setProgressBarIndeterminateVisibility(Boolean.FALSE);
        setSupportProgressBarIndeterminateVisibility(false);
    }

    private void setupViews() {
        AddPagerAdapter adapter = new AddPagerAdapter(getSupportFragmentManager(), this);

        ViewPager pager = (ViewPager) findViewById(R.id.pagerAddShows);
        pager.setAdapter(adapter);

        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabsAddShows);
        tabs.setViewPager(pager);

        // set default tab
        if (getIntent() != null && getIntent().getExtras() != null) {
            int defaultTab = getIntent().getExtras().getInt(InitBundle.DEFAULT_TAB);
            if (defaultTab < adapter.getCount()) {
                pager.setCurrentItem(defaultTab);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (AndroidUtils.isICSOrHigher()) {
            // Check to see that the Activity started due to an Android Beam
            if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
                processIntent(getIntent());
            }
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    /**
     * Parses the NDEF Message from the intent and prints to the TextView
     */
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    void processIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(
                NfcAdapter.EXTRA_NDEF_MESSAGES);

        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        SearchResult show = new SearchResult();
        show.tvdbid = Integer.valueOf(new String(msg.getRecords()[0].getPayload()));
        show.title = new String(msg.getRecords()[1].getPayload());
        show.overview = new String(msg.getRecords()[2].getPayload());

        // display add dialog
        AddDialogFragment.showAddDialog(show, getSupportFragmentManager());
    }

    public static class AddPagerAdapter extends FragmentPagerAdapter {

        public static final int SEARCH_TAB_DEFAULT_POSITION = 0;

        private Context mContext;

        public AddPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
            mContext = context;
        }

        @Override
        public Fragment getItem(int position) {
            return TvdbAddFragment.newInstance();
        }

        @Override
        public int getCount() {
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (position == SEARCH_TAB_DEFAULT_POSITION) {
                return mContext.getString(R.string.search).toUpperCase(Locale.getDefault());
            }
            return "";
        }
    }

    @Override
    public void onAddShow(SearchResult show) {
        // clear the search field (if it is shown)
        EditText searchbox = (EditText) findViewById(R.id.searchbox);
        if (searchbox != null) {
            searchbox.setText("");
        }

        TaskManager.getInstance(this).performAddTask(show);
    }
}
