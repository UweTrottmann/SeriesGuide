
package com.battlelancer.seriesguide.ui;

import android.os.Bundle;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.adapters.ListsPagerAdapter;
import com.battlelancer.seriesguide.interfaces.OnListsChangedListener;
import com.battlelancer.seriesguide.ui.dialogs.AddListDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ListManageDialogFragment;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.seriesguide.R;
import com.viewpagerindicator.TabPageIndicator;
import com.viewpagerindicator.TabPageIndicator.OnTabReselectedListener;

/**
 * Hosts a view pager to display and manage lists of shows, seasons and
 * episodes.
 */
public class ListsActivity extends BaseTopShowsActivity implements OnListsChangedListener {

    public static final String TAG = "Lists";
    private ListsPagerAdapter mListsAdapter;
    private ViewPager mPager;
    private TabPageIndicator mIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getMenu().setContentView(R.layout.lists);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setIcon(R.drawable.ic_action_list);
        actionBar.setTitle(R.string.lists);

        mListsAdapter = new ListsPagerAdapter(getSupportFragmentManager(), this);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mListsAdapter);

        mIndicator = (TabPageIndicator) findViewById(R.id.indicator);
        mIndicator.setViewPager(mPager);
        mIndicator.setOnTabReselectedListener(new OnTabReselectedListener() {
            @Override
            public void onTabReselected(int position) {
                String listId = mListsAdapter.getListId(position);
                ListManageDialogFragment.showListManageDialog(listId, getSupportFragmentManager());
            }
        });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mListsAdapter.onCleanUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.lists_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content
        // view
        boolean isDrawerOpen = isMenuDrawerOpen();
        menu.findItem(R.id.menu_list_add).setVisible(!isDrawerOpen);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_list_add) {
            fireTrackerEvent("Add list");
            AddListDialogFragment.showAddListDialog(getSupportFragmentManager());
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onListsChanged() {
        // refresh list adapter
        mListsAdapter.onListsChanged();
        // update indicator and view pager
        mIndicator.notifyDataSetChanged();
    }

    protected void fireTrackerEvent(String label) {
        EasyTracker.getTracker().sendEvent(TAG, "Action Item", label, (long) 0);
    }
}
