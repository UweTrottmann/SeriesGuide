
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Window;
import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.ui.dialogs.AddDialogFragment.OnAddShowListener;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.seriesguide.util.Utils;
import com.viewpagerindicator.TabPageIndicator;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.EditText;

public class AddActivity extends BaseActivity implements OnAddShowListener {

    private AddPagerAdapter mAdapter;

    private ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The TvdbAddFragment uses a progress bar
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.addactivity_pager);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        setProgressBarIndeterminateVisibility(Boolean.FALSE);
        setSupportProgressBarIndeterminateVisibility(false);

        mAdapter = new AddPagerAdapter(getSupportFragmentManager(), this);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        TabPageIndicator indicator = (TabPageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
    }

    public static class AddPagerAdapter extends FragmentPagerAdapter {

        private Context mContext;

        public AddPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
            mContext = context;
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return TvdbAddFragment.newInstance();
            } else {
                return TraktAddFragment.newInstance(position);
            }
        }

        @Override
        public int getCount() {
            final boolean isValidCredentials = Utils.isTraktCredentialsValid(mContext);
            if (isValidCredentials) {
                // show trakt recommended and libraried shows, too
                return 4;
            } else {
                // show search results and trakt trending shows
                return 2;
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 1:
                    return mContext.getString(R.string.trending).toUpperCase();
                case 2:
                    return mContext.getString(R.string.recommended).toUpperCase();
                case 3:
                    return mContext.getString(R.string.library).toUpperCase();
                default:
                    return mContext.getString(R.string.search_button).toUpperCase();
            }
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
