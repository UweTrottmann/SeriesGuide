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

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.view.ViewGroup;
import com.battlelancer.seriesguide.R;
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
        if (SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_Light) {
            setTheme(R.style.ImmersiveTheme_Light);
        } else if (SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide) {
            setTheme(R.style.ImmersiveTheme);
        } else {
            setTheme(R.style.ImmersiveTheme_Stock);
        }
    }

    @Override
    protected void setupActionBar() {
        super.setupActionBar();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
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
