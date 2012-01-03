
package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.ActionBar;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.AddDialogFragment.OnAddShowListener;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.TaskManager;
import com.battlelancer.thetvdbapi.SearchResult;
import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

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
        setContentView(R.layout.episode_pager);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        mAdapter = new AddPagerAdapter(getSupportFragmentManager(), this);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);

        TitlePageIndicator indicator = (TitlePageIndicator) findViewById(R.id.indicator);
        indicator.setViewPager(mPager);
    }

    public static class AddPagerAdapter extends FragmentPagerAdapter implements TitleProvider {

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
            final boolean isValidCredentials = ShareUtils.isTraktCredentialsValid(mContext);
            if (isValidCredentials) {
                // show trakt recommended and libraried shows, too
                return 4;
            } else {
                // show search results and trakt trending shows
                return 2;
            }
        }

        @Override
        public String getTitle(int position) {
            switch (position) {
                case 1:
                    return mContext.getString(R.string.trending);
                case 2:
                    return mContext.getString(R.string.recommended);
                case 3:
                    return mContext.getString(R.string.library);
                default:
                    return mContext.getString(R.string.search_button);
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
