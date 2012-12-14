
package com.battlelancer.seriesguide.ui;

import android.os.Bundle;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.adapters.ListsPagerAdapter;
import com.battlelancer.seriesguide.ui.dialogs.AddListDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.ListManageDialogFragment;
import com.google.analytics.tracking.android.EasyTracker;
import com.slidingmenu.lib.SlidingMenu;
import com.uwetrottmann.seriesguide.R;
import com.viewpagerindicator.TabPageIndicator;
import com.viewpagerindicator.TabPageIndicator.OnTabReselectedListener;

/**
 * Hosts a view pager to display and manage lists of shows, seasons and
 * episodes.
 */
public class ListsActivity extends BaseActivity implements OnListsChangedListener {

    public static final String TAG = "ListsActivity";
    private ListsPagerAdapter mListsAdapter;
    private ViewPager mPager;
    private TabPageIndicator mIndicator;

    /**
     * Google Analytics helper method for easy event tracking.
     */
    public void fireTrackerEvent(String label) {
        EasyTracker.getTracker().trackEvent(TAG, "Click", label, (long) 0);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lists);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setIcon(R.drawable.ic_action_list);
        actionBar.setTitle(R.string.lists);

        getSlidingMenu().setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);

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
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.lists_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_list_add) {
            AddListDialogFragment.showAddListDialog(getSupportFragmentManager());
            fireTrackerEvent("Add list");
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
}
