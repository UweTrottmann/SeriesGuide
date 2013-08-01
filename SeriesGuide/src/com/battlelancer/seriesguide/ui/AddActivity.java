/*
 * Copyright 2012 Uwe Trottmann
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
import com.astuetz.viewpager.extensions.PagerSlidingTabStrip;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.ui.dialogs.AddDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.AddDialogFragment.OnAddShowListener;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TaskManager;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import java.util.Locale;

/**
 * Hosts various fragments in a {@link ViewPager} which allow adding shows to
 * the database.
 */
public class AddActivity extends BaseNavDrawerActivity implements OnAddShowListener {

    private AddPagerAdapter mAdapter;

    private ViewPager mPager;

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

        getMenu().setContentView(R.layout.addactivity_pager);

        setupActionBar();

        setupViews();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setIcon(R.drawable.ic_action_add);
        actionBar.setDisplayHomeAsUpEnabled(true);
        setProgressBarIndeterminateVisibility(Boolean.FALSE);
        setSupportProgressBarIndeterminateVisibility(false);
    }

    private void setupViews() {
        mAdapter = new AddPagerAdapter(getSupportFragmentManager(), this);

        mPager = (ViewPager) findViewById(R.id.pagerAddShows);
        mPager.setAdapter(mAdapter);

        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabsAddShows);
        tabs.setViewPager(mPager);

        // set default tab
        if (getIntent() != null && getIntent().getExtras() != null) {
            int defaultTab = getIntent().getExtras().getInt(InitBundle.DEFAULT_TAB);
            if (defaultTab < mAdapter.getCount()) {
                mPager.setCurrentItem(defaultTab);
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
        show.tvdbid = new String(msg.getRecords()[0].getPayload());
        show.title = new String(msg.getRecords()[1].getPayload());
        show.overview = new String(msg.getRecords()[2].getPayload());

        // display add dialog
        AddDialogFragment.showAddDialog(show, getSupportFragmentManager());
    }

    public static class AddPagerAdapter extends FragmentPagerAdapter {

        private static final int DEFAULT_TABCOUNT = 2;
        private static final int TRAKT_CONNECTED_TABCOUNT = 5;

        public static final int TRENDING_TAB_POSITION = 0;
        public static final int RECOMMENDED_TAB_POSITION = 2;
        public static final int LIBRARY_TAB_POSITION = 3;
        public static final int WATCHLIST_TAB_POSITION = 4;

        public static final int SEARCH_TAB_DEFAULT_POSITION = 1;
        public static final int SEARCH_TAB_CONNECTED_POSITION = 1;

        private Context mContext;

        public AddPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
            mContext = context;
        }

        @Override
        public Fragment getItem(int position) {
            int count = getCount();
            if ((count == DEFAULT_TABCOUNT && position == SEARCH_TAB_DEFAULT_POSITION)
                    || (count == TRAKT_CONNECTED_TABCOUNT && position == SEARCH_TAB_CONNECTED_POSITION)) {
                return TvdbAddFragment.newInstance();
            } else {
                return TraktAddFragment.newInstance(position);
            }
        }

        @Override
        public int getCount() {
            final boolean isValidCredentials = ServiceUtils.hasTraktCredentials(mContext);
            if (isValidCredentials) {
                // show trakt recommended and libraried shows, too
                return TRAKT_CONNECTED_TABCOUNT;
            } else {
                // show search results and trakt trending shows
                return DEFAULT_TABCOUNT;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (getCount() == TRAKT_CONNECTED_TABCOUNT) {
                switch (position) {
                    case TRENDING_TAB_POSITION:
                        return mContext.getString(R.string.trending).toUpperCase(
                                Locale.getDefault());
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
                        return mContext.getString(R.string.search_button).toUpperCase(
                                Locale.getDefault());
                }
            } else {
                switch (position) {
                    case TRENDING_TAB_POSITION:
                        return mContext.getString(R.string.trending).toUpperCase(
                                Locale.getDefault());
                    case SEARCH_TAB_DEFAULT_POSITION:
                        return mContext.getString(R.string.search_button).toUpperCase(
                                Locale.getDefault());
                }
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
