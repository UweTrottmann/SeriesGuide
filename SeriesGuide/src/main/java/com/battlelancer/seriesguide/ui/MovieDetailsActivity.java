package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.ViewGroup;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ThemeUtils;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.uwetrottmann.androidutils.AndroidUtils;

/**
 * Hosts a {@link MovieDetailsFragment} displaying details about the movie defined by the given TMDb
 * id intent extra.
 */
public class MovieDetailsActivity extends BaseNavDrawerActivity {

    // loader ids for this activity (mostly used by fragments)
    public static int LOADER_ID_MOVIE = 100;
    public static int LOADER_ID_MOVIE_TRAILERS = 101;
    public static int LOADER_ID_MOVIE_CREDITS = 102;

    private SystemBarTintManager mSystemBarTintManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie);
        setupActionBar();
        setupNavDrawer();

        if (getIntent().getExtras() == null) {
            finish();
            return;
        }

        int tmdbId = getIntent().getExtras().getInt(MovieDetailsFragment.InitBundle.TMDB_ID);
        if (tmdbId == 0) {
            finish();
            return;
        }

        setupViews();

        if (savedInstanceState == null) {
            MovieDetailsFragment f = MovieDetailsFragment.newInstance(tmdbId);
            getSupportFragmentManager().beginTransaction().add(R.id.content_frame, f).commit();
        }
    }

    private void setupViews() {
        if (AndroidUtils.isKitKatOrHigher()) {
            // fix padding with translucent status bar
            // warning: status bar not always translucent (e.g. Nexus 10)
            // (using fitsSystemWindows would not work correctly with multiple views)
            mSystemBarTintManager = new SystemBarTintManager(this);
            int insetTop = mSystemBarTintManager.getConfig().getPixelInsetTop(false);
            ViewGroup actionBarToolbar = (ViewGroup) findViewById(R.id.sgToolbar);
            ViewGroup.MarginLayoutParams layoutParams
                    = (ViewGroup.MarginLayoutParams) actionBarToolbar.getLayoutParams();
            layoutParams.setMargins(layoutParams.leftMargin, layoutParams.topMargin + insetTop,
                    layoutParams.rightMargin, layoutParams.bottomMargin);
        }
    }

    @Override
    protected void setCustomTheme() {
        // use a special immersive theme
        ThemeUtils.setImmersiveTheme(this);
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            Intent parentActivityIntent = new Intent(this, MoviesActivity.class);
            parentActivityIntent.addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                            Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(parentActivityIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public SystemBarTintManager getSystemBarTintManager() {
        return mSystemBarTintManager;
    }
}
