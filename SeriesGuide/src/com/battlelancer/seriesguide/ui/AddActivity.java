
package com.battlelancer.seriesguide.ui;

import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.AddDialogFragment.OnAddShowListener;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.thetvdbapi.SearchResult;
import com.battlelancer.thetvdbapi.TheTVDB;
import com.viewpagerindicator.TitlePageIndicator;
import com.viewpagerindicator.TitleProvider;

import org.xml.sax.SAXException;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.widget.EditText;
import android.widget.Toast;

import java.util.LinkedList;

public class AddActivity extends BaseActivity implements OnAddShowListener {

    private AddPagerAdapter mAdapter;

    private ViewPager mPager;

    private AddShowTask mAddTask;

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

    /**
     * Clears the search field.
     */
    private void clearSearchField() {
        EditText searchbox = (EditText) findViewById(R.id.searchbox);
        if (searchbox != null) {
            searchbox.setText("");
        }
    }

    public static class AddShowTask extends AsyncTask<Void, Integer, Void> {

        private static final int ADD_ALREADYEXISTS = 0;

        private static final int ADD_SUCCESS = 1;

        private static final int ADD_SAXERROR = 2;

        final private Context mContext;

        final private LinkedList<SearchResult> mAddQueue = new LinkedList<SearchResult>();

        private boolean mIsFinishedAddingShows = false;

        private String mCurrentShowName;

        public AddShowTask(Context activity, SearchResult show) {
            mContext = activity;
            mAddQueue.add(show);
        }

        public AddShowTask(Context activity, LinkedList<SearchResult> shows) {
            mContext = activity;
            mAddQueue.addAll(shows);
        }

        public LinkedList<SearchResult> getRemainingShows() {
            return mAddQueue;
        }

        /**
         * Add a show to the add queue. If this returns false, the show was not
         * added because the task is finishing up. Create a new one instead.
         * 
         * @param show
         * @return
         */
        public boolean addShow(SearchResult show) {
            if (mIsFinishedAddingShows) {
                return false;
            } else {
                mAddQueue.add(show);
                return true;
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            int result;
            boolean modifiedDB = false;

            while (!mAddQueue.isEmpty()) {
                if (isCancelled()) {
                    // only cancelled on config change, so don't rebuild fts
                    // table yet
                    return null;
                }

                SearchResult nextShow = mAddQueue.removeFirst();

                try {
                    if (TheTVDB.addShow(nextShow.getId(), mContext.getApplicationContext())) {
                        // success
                        result = ADD_SUCCESS;
                    } else {
                        // already exists
                        result = ADD_ALREADYEXISTS;
                    }
                    modifiedDB = true;
                } catch (SAXException e) {
                    result = ADD_SAXERROR;
                }

                mCurrentShowName = nextShow.getSeriesName();
                publishProgress(result);
            }

            mIsFinishedAddingShows = true;
            // renew FTS3 table
            if (modifiedDB) {
                TheTVDB.onRenewFTSTable(mContext.getApplicationContext());
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            switch (values[0]) {
                case ADD_SUCCESS:
                    Toast.makeText(
                            mContext.getApplicationContext(),
                            "\"" + mCurrentShowName + "\" "
                                    + mContext.getString(R.string.add_success), Toast.LENGTH_SHORT)
                            .show();
                    break;
                case ADD_ALREADYEXISTS:
                    Toast.makeText(
                            mContext.getApplicationContext(),
                            "\"" + mCurrentShowName + "\" "
                                    + mContext.getString(R.string.add_already_exists),
                            Toast.LENGTH_LONG).show();
                    break;
                case ADD_SAXERROR:
                    Toast.makeText(
                            mContext.getApplicationContext(),
                            mContext.getString(R.string.add_error_begin) + mCurrentShowName
                                    + mContext.getString(R.string.add_error_end) + " ("
                                    + mContext.getString(R.string.saxerror_title) + ")",
                            Toast.LENGTH_LONG).show();
                    break;
            }
        }
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
            // TODO: put into strings.xml
                case 1:
                    return "Trending";
                case 2:
                    return "Recommended";
                case 3:
                    return "Library";
                default:
                    return "Search";
            }
        }

    }

    @Override
    public void onAddShow(SearchResult show) {
        clearSearchField();

        // notify user here already
        Toast.makeText(this, "\"" + show.getSeriesName() + "\" " + getString(R.string.add_started),
                Toast.LENGTH_SHORT).show();

        // add the show to a running add task or create a new one
        if (mAddTask == null || mAddTask.getStatus() == AsyncTask.Status.FINISHED) {
            mAddTask = (AddShowTask) new AddShowTask(this, show).execute();
        } else {
            // addTask is still running, try to add another show to its queue
            boolean hasAddedShow = mAddTask.addShow(show);
            if (!hasAddedShow) {
                mAddTask = (AddShowTask) new AddShowTask(this, show).execute();
            }
        }
    }
}
