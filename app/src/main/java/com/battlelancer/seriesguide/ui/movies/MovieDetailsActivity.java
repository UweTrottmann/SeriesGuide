package com.battlelancer.seriesguide.ui.movies;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.ActionBar;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.BaseMessageActivity;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.uwetrottmann.androidutils.AndroidUtils;

/**
 * Hosts a {@link MovieDetailsFragment} displaying details about the movie defined by the given TMDb
 * id intent extra.
 */
public class MovieDetailsActivity extends BaseMessageActivity {

    // loader ids for this activity (mostly used by fragments)
    public static int LOADER_ID_MOVIE = 100;
    public static int LOADER_ID_MOVIE_TRAILERS = 101;

    private SystemBarTintManager systemBarTintManager;

    public static Intent intentMovie(Context context, int movieTmdbId) {
        return new Intent(context, MovieDetailsActivity.class)
                .putExtra(MovieDetailsFragment.ARG_TMDB_ID, movieTmdbId);
    }

    @Override
    protected int getCustomTheme() {
        return R.style.Theme_SeriesGuide_DayNight_Immersive;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // support transparent status bar
        if (AndroidUtils.isMarshmallowOrHigher()) {
            findViewById(android.R.id.content).setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        setContentView(R.layout.activity_movie);
        setupActionBar();

        if (getIntent().getExtras() == null) {
            finish();
            return;
        }

        int tmdbId = getIntent().getExtras().getInt(MovieDetailsFragment.ARG_TMDB_ID);
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
        // fix padding with translucent (K+)/transparent (M+) status bar
        // warning: pre-M status bar not always translucent (e.g. Nexus 10)
        // (using fitsSystemWindows would not work correctly with multiple views)
        systemBarTintManager = new SystemBarTintManager(this);
        SystemBarTintManager.SystemBarConfig config = systemBarTintManager.getConfig();
        int insetTop = AndroidUtils.isMarshmallowOrHigher()
                ? config.getStatusBarHeight() // transparent status bar
                : config.getPixelInsetTop(false); // translucent status bar
        ViewGroup actionBarToolbar = findViewById(R.id.sgToolbar);
        ViewGroup.MarginLayoutParams layoutParams
                = (ViewGroup.MarginLayoutParams) actionBarToolbar.getLayoutParams();
        layoutParams.setMargins(layoutParams.leftMargin, layoutParams.topMargin + insetTop,
                layoutParams.rightMargin, layoutParams.bottomMargin);
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

    public SystemBarTintManager getSystemBarTintManager() {
        return systemBarTintManager;
    }
}
