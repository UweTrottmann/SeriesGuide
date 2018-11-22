package com.battlelancer.seriesguide.ui.streams;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import com.battlelancer.seriesguide.BuildConfig;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.BaseActivity;
import com.battlelancer.seriesguide.ui.search.AddShowDialogFragment;
import com.battlelancer.seriesguide.ui.search.SearchResult;
import com.battlelancer.seriesguide.util.TaskManager;
import timber.log.Timber;

/**
 * Displays history of watched episodes or movies.
 */
public class HistoryActivity extends BaseActivity implements
        AddShowDialogFragment.OnAddShowListener {

    public static final int EPISODES_LOADER_ID = 100;
    public static final int MOVIES_LOADER_ID = 101;

    public static final int DISPLAY_EPISODE_HISTORY = 0;
    public static final int DISPLAY_MOVIE_HISTORY = 1;

    public interface InitBundle {
        String HISTORY_TYPE = BuildConfig.APPLICATION_ID + ".historytype";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane);
        setupActionBar();

        if (savedInstanceState == null) {
            int historyType = getIntent().getIntExtra(InitBundle.HISTORY_TYPE, -1);
            Fragment f;
            if (historyType == DISPLAY_EPISODE_HISTORY) {
                f = new UserEpisodeStreamFragment();
            } else if (historyType == DISPLAY_MOVIE_HISTORY) {
                f = new UserMovieStreamFragment();
            } else {
                // default to episode history
                Timber.w("onCreate: did not specify a valid HistoryType in the launch intent.");
                f = new UserEpisodeStreamFragment();
            }
            f.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_frame, f)
                    .commit();
        }
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Called if the user adds a show from a trakt stream fragment.
     */
    @Override
    public void onAddShow(SearchResult show) {
        TaskManager.getInstance().performAddTask(this, show);
    }
}
