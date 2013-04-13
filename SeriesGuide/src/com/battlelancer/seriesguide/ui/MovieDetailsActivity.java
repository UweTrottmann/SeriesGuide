/*
 * Copyright 2013 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.ui;

import android.os.Bundle;

/**
 * Hosts a {@link MovieDetailsFragment} displaying details about the movie
 * defined by the given TMDb id intent extra.
 */
public class MovieDetailsActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent().getExtras() == null) {
            finish();
            return;
        }

        int tmdbId = getIntent().getExtras().getInt(MovieDetailsFragment.InitBundle.TMDB_ID);
        if (tmdbId == 0) {
            finish();
            return;
        }

        if (savedInstanceState == null) {
            MovieDetailsFragment f = MovieDetailsFragment.newInstance(tmdbId);
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, f).commit();
        }
    }
}
