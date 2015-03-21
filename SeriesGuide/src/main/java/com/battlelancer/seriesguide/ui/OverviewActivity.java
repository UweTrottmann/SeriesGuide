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

import android.app.SearchManager;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.battlelancer.seriesguide.Constants;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.adapters.TabStripAdapter;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.widgets.SlidingTabLayout;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Hosts an {@link OverviewFragment}.
 */
public class OverviewActivity extends BaseNavDrawerActivity {

    public static final int SHOW_LOADER_ID = 100;
    public static final int SHOW_CREDITS_LOADER_ID = 101;
    public static final int OVERVIEW_EPISODE_LOADER_ID = 102;
    public static final int OVERVIEW_SHOW_LOADER_ID = 103;
    public static final int OVERVIEW_ACTIONS_LOADER_ID = 104;
    public static final int SEASONS_LOADER_ID = 105;

    private NfcAdapter nfcAdapter;
    private int showTvdbId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_overview);
        setupActionBar();
        setupNavDrawer();

        showTvdbId = getIntent().getIntExtra(OverviewFragment.InitBundle.SHOW_TVDBID, -1);
        if (showTvdbId < 0) {
            finish();
            return;
        }

        setupViews(savedInstanceState);

        setupAndroidBeam();

        updateShowDelayed(showTvdbId);
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    private void setupViews(Bundle savedInstanceState) {
        // look if we are on a multi-pane or single-pane layout...
        View pagerView = findViewById(R.id.pagerOverview);
        if (pagerView != null && pagerView.getVisibility() == View.VISIBLE) {
            // ...single pane layout with view pager

            // clear up left-over fragments from multi-pane layout
            findAndRemoveFragment(R.id.fragment_overview);
            findAndRemoveFragment(R.id.fragment_seasons);

            setupViewPager(pagerView);
        } else {
            // ...multi-pane overview and seasons fragment

            // clear up left-over fragments from single-pane layout
            boolean isSwitchingLayouts = getActiveFragments().size() != 0;
            for (Fragment fragment : getActiveFragments()) {
                getSupportFragmentManager().beginTransaction().remove(fragment).commit();
            }

            // attach new fragments if there are none or if we just switched
            // layouts
            if (savedInstanceState == null || isSwitchingLayouts) {
                setupPanes();
            }
        }
    }

    private void setupPanes() {
        Fragment showsFragment = ShowFragment.newInstance(showTvdbId);
        FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
        ft1.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        ft1.replace(R.id.fragment_show, showsFragment);
        ft1.commit();

        Fragment overviewFragment = OverviewFragment.newInstance(showTvdbId);
        FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
        ft2.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        ft2.replace(R.id.fragment_overview, overviewFragment);
        ft2.commit();

        Fragment seasonsFragment = SeasonsFragment.newInstance(showTvdbId);
        FragmentTransaction ft3 = getSupportFragmentManager().beginTransaction();
        ft3.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        ft3.replace(R.id.fragment_seasons, seasonsFragment);
        ft3.commit();
    }

    private void setupViewPager(View pagerView) {
        ViewPager pager = (ViewPager) pagerView;

        // setup tab strip
        TabStripAdapter tabsAdapter = new TabStripAdapter(getSupportFragmentManager(), this, pager,
                (SlidingTabLayout) findViewById(R.id.tabsOverview));
        Bundle argsShow = new Bundle();
        argsShow.putInt(ShowFragment.InitBundle.SHOW_TVDBID, showTvdbId);
        tabsAdapter.addTab(R.string.show, ShowFragment.class, argsShow);

        tabsAdapter.addTab(R.string.description_overview, OverviewFragment.class, getIntent()
                .getExtras());

        Bundle argsSeason = new Bundle();
        argsSeason.putInt(SeasonsFragment.InitBundle.SHOW_TVDBID, showTvdbId);
        tabsAdapter.addTab(R.string.seasons, SeasonsFragment.class, argsSeason);
        tabsAdapter.notifyTabsChanged();

        // select overview to be shown initially
        pager.setCurrentItem(1);
    }

    private void findAndRemoveFragment(int fragmentId) {
        Fragment overviewFragment = getSupportFragmentManager().findFragmentById(fragmentId);
        if (overviewFragment != null) {
            getSupportFragmentManager().beginTransaction().remove(overviewFragment).commit();
        }
    }

    private void setupAndroidBeam() {
        // Support beaming shows via Android Beam
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter != null) {
            nfcAdapter.setNdefPushMessageCallback(new CreateNdefMessageCallback() {
                @Override
                public NdefMessage createNdefMessage(NfcEvent event) {
                    // send show TVDB id
                    return new NdefMessage(new NdefRecord[] {
                            createMimeRecord(
                                    Constants.ANDROID_BEAM_NDEF_MIME_TYPE,
                                    String.valueOf(showTvdbId).getBytes())
                    });
                }

                /**
                 * Creates a custom MIME type encapsulated in an NDEF record
                 */
                public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
                    byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
                    return new NdefRecord(
                            NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
                }
            }, this);
        }
    }

    List<WeakReference<Fragment>> mFragments = new ArrayList<>();

    @Override
    public void onAttachFragment(Fragment fragment) {
        /*
         * View pager fragments have tags set by the pager, we can use this to
         * only add refs to those then, making them available to get removed if
         * we switch to a non-pager layout.
         */
        if (fragment.getTag() != null) {
            mFragments.add(new WeakReference<>(fragment));
        }
    }

    public ArrayList<Fragment> getActiveFragments() {
        ArrayList<Fragment> ret = new ArrayList<>();
        for (WeakReference<Fragment> ref : mFragments) {
            Fragment f = ref.get();
            if (f != null) {
                if (f.isAdded()) {
                    ret.add(f);
                }
            }
        }
        return ret;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.overview_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            Intent upIntent = new Intent(this, ShowsActivity.class);
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(upIntent);
            return true;
        } else if (itemId == R.id.menu_overview_search) {
            launchSearch();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void launchSearch() {
        // refine search with the show's title
        final Series show = DBUtils.getShow(this, showTvdbId);
        if (show != null) {
            final String showTitle = show.getTitle();

            Bundle appSearchData = new Bundle();
            appSearchData.putString(EpisodeSearchFragment.InitBundle.SHOW_TITLE, showTitle);

            Intent intent = new Intent(this, SearchActivity.class);
            intent.putExtra(SearchManager.APP_DATA, appSearchData);
            intent.setAction(Intent.ACTION_SEARCH);
            startActivity(intent);
        }
    }
}
