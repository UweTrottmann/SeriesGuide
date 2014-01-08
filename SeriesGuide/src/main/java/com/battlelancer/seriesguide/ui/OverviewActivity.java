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

import com.google.analytics.tracking.android.EasyTracker;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.astuetz.PagerSlidingTabStrip;
import com.battlelancer.seriesguide.adapters.TabStripAdapter;
import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.sync.SgSyncAdapter;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.ShortcutUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.battlelancer.thetvdbapi.TheTVDB;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.View;

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Hosts an {@link OverviewFragment}.
 */
public class OverviewActivity extends BaseNavDrawerActivity {

    private static final String TAG = "Overview";

    private int mShowId;
    private NfcAdapter mNfcAdapter;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.overview);
        setupNavDrawer();

        mShowId = getIntent().getIntExtra(OverviewFragment.InitBundle.SHOW_TVDBID, -1);
        if (mShowId == -1) {
            finish();
            return;
        }

        setupActionBar();

        setupViews(savedInstanceState);

        // Support beaming shows via Android Beam
        if (AndroidUtils.isICSOrHigher()) {
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
            if (mNfcAdapter != null) {
                mNfcAdapter.setNdefPushMessageCallback(new CreateNdefMessageCallback() {
                    @Override
                    public NdefMessage createNdefMessage(NfcEvent event) {
                        final Series show = DBUtils.getShow(OverviewActivity.this, mShowId);
                        // send id, also title and overview (both can be empty)
                        NdefMessage msg = new NdefMessage(new NdefRecord[] {
                                createMimeRecord(
                                        "application/com.battlelancer.seriesguide.beam",
                                        String.valueOf(mShowId).getBytes()),
                                createMimeRecord("application/com.battlelancer.seriesguide.beam",
                                        show.getTitle().getBytes()),
                                createMimeRecord("application/com.battlelancer.seriesguide.beam",
                                        show
                                                .getOverview().getBytes())
                        });
                        return msg;
                    }

                    /**
                     * Creates a custom MIME type encapsulated in an NDEF record
                     */
                    public NdefRecord createMimeRecord(String mimeType, byte[] payload) {
                        byte[] mimeBytes = mimeType.getBytes(Charset.forName("US-ASCII"));
                        NdefRecord mimeRecord = new NdefRecord(
                                NdefRecord.TNF_MIME_MEDIA, mimeBytes, new byte[0], payload);
                        return mimeRecord;
                    }
                }, this);
            }
        }

        // try to update this show
        onUpdateShow();
    }

    private void setupActionBar() {
        final ActionBar actionBar = getSupportActionBar();
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
        Fragment showsFragment = ShowFragment.newInstance(mShowId);
        FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
        ft1.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        ft1.replace(R.id.fragment_show, showsFragment);
        ft1.commit();

        Fragment overviewFragment = OverviewFragment.newInstance(mShowId);
        FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
        ft2.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        ft2.replace(R.id.fragment_overview, overviewFragment);
        ft2.commit();

        Fragment seasonsFragment = SeasonsFragment.newInstance(mShowId);
        FragmentTransaction ft3 = getSupportFragmentManager().beginTransaction();
        ft3.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        ft3.replace(R.id.fragment_seasons, seasonsFragment);
        ft3.commit();
    }

    private void setupViewPager(View pagerView) {
        ViewPager pager = (ViewPager) pagerView;
        PagerSlidingTabStrip tabs = (PagerSlidingTabStrip) findViewById(R.id.tabsOverview);

        // setup tab strip
        TabStripAdapter tabsAdapter = new TabStripAdapter(
                getSupportFragmentManager(), this, pager, tabs);
        Bundle argsShow = new Bundle();
        argsShow.putInt(ShowFragment.InitBundle.SHOW_TVDBID, mShowId);
        tabsAdapter.addTab(R.string.show, ShowFragment.class, argsShow);

        tabsAdapter.addTab(R.string.description_overview, OverviewFragment.class, getIntent()
                .getExtras());

        Bundle argsSeason = new Bundle();
        argsSeason.putInt(SeasonsFragment.InitBundle.SHOW_TVDBID, mShowId);
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

    List<WeakReference<Fragment>> mFragments = new ArrayList<WeakReference<Fragment>>();

    @Override
    public void onAttachFragment(Fragment fragment) {
        /*
         * View pager fragments have tags set by the pager, we can use this to
         * only add refs to those then, making them available to get removed if
         * we switch to a non-pager layout.
         */
        if (fragment.getTag() != null) {
            mFragments.add(new WeakReference<Fragment>(fragment));
        }
    }

    public ArrayList<Fragment> getActiveFragments() {
        ArrayList<Fragment> ret = new ArrayList<Fragment>();
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
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.shrink_enter, R.anim.shrink_exit);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content
        // view
        menu.findItem(R.id.menu_overview_search).setVisible(!isDrawerOpen());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.overview_activity_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            Intent upIntent = new Intent(this, ShowsActivity.class);
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(upIntent);
            overridePendingTransition(R.anim.shrink_enter, R.anim.shrink_exit);
            return true;
        } else if (itemId == R.id.menu_overview_search) {
            onSearchRequested();
            return true;
        } else if (itemId == R.id.menu_overview_add_to_homescreen) {
            if (!Utils.hasAccessToX(this)) {
                Utils.advertiseSubscription(this);
                return true;
            }

            // Create the shortcut
            final Series show = DBUtils.getShow(this, mShowId);
            String title = show.getTitle();
            String poster = show.getPoster();
            ShortcutUtils.createShortcut(this, title, poster, mShowId);

            // Analytics
            fireTrackerEvent("Add to Homescreen");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Delayed request to sync the displayed show.
     */
    private void onUpdateShow() {
        final String showId = String.valueOf(mShowId);
        boolean isTime = TheTVDB.isUpdateShow(showId, System.currentTimeMillis(), this);
        if (isTime) {
            final Context context = getApplicationContext();
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    SgSyncAdapter.requestSyncIfConnected(context, mShowId);
                }
            }, 1000);
        }
    }

    @Override
    public boolean onSearchRequested() {
        // refine search with the show's title
        final Series show = DBUtils.getShow(this, mShowId);
        final String showTitle = show.getTitle();

        Bundle args = new Bundle();
        args.putString(SearchFragment.InitBundle.SHOW_TITLE, showTitle);
        startSearch(null, false, args, false);
        return true;
    }

    private void fireTrackerEvent(String label) {
        Utils.trackAction(this, TAG, label);
    }

}
