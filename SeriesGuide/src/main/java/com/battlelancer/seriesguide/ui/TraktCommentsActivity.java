package com.battlelancer.seriesguide.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import com.battlelancer.seriesguide.R;

public class TraktCommentsActivity extends BaseActivity {

    public static final int LOADER_ID_COMMENTS = 100;

    /**
     * Display comments of an episode.
     */
    public static Bundle createInitBundleEpisode(String title, int episodeTvdbId) {
        Bundle extras = new Bundle();
        extras.putInt(TraktCommentsFragment.InitBundle.EPISODE_TVDB_ID, episodeTvdbId);
        extras.putString(InitBundle.TITLE, title);
        return extras;
    }

    /**
     * Display comments of a show.
     */
    public static Bundle createInitBundleShow(String title, int showTvdbId) {
        Bundle extras = new Bundle();
        extras.putInt(TraktCommentsFragment.InitBundle.SHOW_TVDB_ID, showTvdbId);
        extras.putString(InitBundle.TITLE, title);
        return extras;
    }

    /**
     * Display comments of a movie.
     */
    public static Bundle createInitBundleMovie(String title, int movieTmdbId) {
        Bundle extras = new Bundle();
        extras.putInt(TraktCommentsFragment.InitBundle.MOVIE_TMDB_ID, movieTmdbId);
        extras.putString(InitBundle.TITLE, title);
        return extras;
    }

    private interface InitBundle {
        String TITLE = "title";
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_singlepane);
        setupActionBar();

        if (savedInstanceState == null) {
            Fragment f = new TraktCommentsFragment();
            f.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_frame, f)
                    .commit();
        }
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        String commentsTitle = getIntent().getExtras().getString(InitBundle.TITLE);
        setTitle(getString(R.string.comments) + " " + commentsTitle);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.comments);
            actionBar.setSubtitle(commentsTitle);
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
}
