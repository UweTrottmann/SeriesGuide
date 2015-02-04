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

import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment.OnAddShowListener;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;
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
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_addshow);
        setupActionBar();
        setupNavDrawer();

        setupViews();
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void setupViews() {
        AddPagerAdapter adapter = new AddPagerAdapter(getSupportFragmentManager(), this);

        ViewPager pager = (ViewPager) findViewById(R.id.pagerAddShows);
        pager.setAdapter(adapter);

        SlidingTabLayout tabs = (SlidingTabLayout) findViewById(R.id.tabsAddShows);
        tabs.setCustomTabView(R.layout.tabstrip_item_allcaps, R.id.textViewTabStripItem);
        tabs.setSelectedIndicatorColors(getResources().getColor(R.color.white));
        tabs.setViewPager(pager);
        tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset,
                    int positionOffsetPixels) {
                // do nothing
            }

            @Override
            public void onPageSelected(int position) {
                String tag = null;
                switch (position) {
                    case AddPagerAdapter.SEARCH_TAB_DEFAULT_POSITION:
                        tag = "TVDb Search";
                        break;
                    case AddPagerAdapter.RECOMMENDED_TAB_POSITION:
                        tag = "Recommended";
                        break;
                    case AddPagerAdapter.LIBRARY_TAB_POSITION:
                        tag = "Library";
                        break;
                    case AddPagerAdapter.WATCHLIST_TAB_POSITION:
                        tag = "Watchlist";
                        break;
                }
                if (tag != null) {
                    Utils.trackView(AddActivity.this, tag);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                // do nothing
            }
        });

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

        // If the activity was started due to an Android Beam, handle the incoming beam
        if (getIntent() != null && NfcAdapter.ACTION_NDEF_DISCOVERED.equals(
                getIntent().getAction())) {
            processBeamIntent(getIntent());
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    /**
     * Extracts the beamed show from the NDEF Message and displays an add dialog for the show.
     */
    void processBeamIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs == null || rawMsgs.length == 0) {
            // corrupted or invalid data
            return;
        }

        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];

        int showTvdbId;
        try {
            showTvdbId = Integer.valueOf(new String(msg.getRecords()[0].getPayload()));
        } catch (NumberFormatException e) {
            return;
        }

        // display add dialog
        AddShowDialogFragment.showAddDialog(showTvdbId, getSupportFragmentManager());
    }

    public static class AddPagerAdapter extends FragmentPagerAdapter {

        private static final int DEFAULT_TABCOUNT = 1;
        public static final int SEARCH_TAB_DEFAULT_POSITION = 0;

        private static final int TRAKT_CONNECTED_TABCOUNT = 4;
        public static final int SEARCH_TAB_CONNECTED_POSITION = 0;
        public static final int RECOMMENDED_TAB_POSITION = 1;
        public static final int LIBRARY_TAB_POSITION = 2;
        public static final int WATCHLIST_TAB_POSITION = 3;

        private final boolean mIsConnectedToTrakt;

        private Context mContext;

        public AddPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
            mContext = context;
            mIsConnectedToTrakt = TraktCredentials.get(mContext).hasCredentials();
        }

        @Override
        public Fragment getItem(int position) {
            int count = getCount();
            if ((count == DEFAULT_TABCOUNT && position == SEARCH_TAB_DEFAULT_POSITION)
                    || (count == TRAKT_CONNECTED_TABCOUNT
                    && position == SEARCH_TAB_CONNECTED_POSITION)) {
                return TvdbAddFragment.newInstance();
            } else {
                return TraktAddFragment.newInstance(position);
            }
        }

        @Override
        public int getCount() {
            if (mIsConnectedToTrakt) {
                // show trakt recommendations, library and watchlist, too
                return TRAKT_CONNECTED_TABCOUNT;
            } else {
                // show only search tab
                return DEFAULT_TABCOUNT;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (getCount() == TRAKT_CONNECTED_TABCOUNT) {
                switch (position) {
                    case RECOMMENDED_TAB_POSITION:
                        return mContext.getString(R.string.recommended).toUpperCase(
                                Locale.getDefault());
                    case LIBRARY_TAB_POSITION:
                        return mContext.getString(R.string.library)
                                .toUpperCase(Locale.getDefault());
                    case WATCHLIST_TAB_POSITION:
                        return mContext.getString(R.string.watchlist).toUpperCase(
                                Locale.getDefault());
                    case SEARCH_TAB_CONNECTED_POSITION:
                        return mContext.getString(R.string.search).toUpperCase(
                                Locale.getDefault());
                }
            } else {
                switch (position) {
                    case SEARCH_TAB_DEFAULT_POSITION:
                        return mContext.getString(R.string.search).toUpperCase(
                                Locale.getDefault());
                }
            }
            return "";
        }
    }

    @Override
    public void onAddShow(SearchResult show) {
        TaskManager.getInstance(this).performAddTask(show);
    }
}
