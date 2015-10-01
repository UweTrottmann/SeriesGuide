/*
 * Copyright 2015 Uwe Trottmann
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

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideDatabase;
import com.battlelancer.seriesguide.util.Utils;

/**
 * Displays information about the app, the developer and licence information about content and
 * libraries.
 */
public class AboutSettingsFragment extends Fragment {

    @Bind(R.id.textViewAboutVersion) TextView textVersion;
    @Bind(R.id.buttonAboutWebsite) Button buttonWebsite;
    @Bind(R.id.buttonAboutTvdbTerms) Button buttonTvdbTerms;
    @Bind(R.id.buttonAboutCreativeCommons) Button buttonCreativeCommons;
    @Bind(R.id.buttonAboutTmdbTerms) Button buttonTmdbTerms;
    @Bind(R.id.buttonAboutTmdbApiTerms) Button buttonTmdbApiTerms;
    @Bind(R.id.buttonAboutTraktTerms) Button buttonTraktTerms;
    @Bind(R.id.buttonAboutCredits) Button buttonCredits;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_about, container, false);
        ButterKnife.bind(this, v);

        // display version number and database version
        final String versionFinal = Utils.getVersion(getActivity());
        textVersion.setText(
                "v" + versionFinal + " (Database v" + SeriesGuideDatabase.DATABASE_VERSION + ")");

        buttonWebsite.setOnClickListener(urlButtonClickListener);
        buttonTvdbTerms.setOnClickListener(urlButtonClickListener);
        buttonCreativeCommons.setOnClickListener(urlButtonClickListener);
        buttonTmdbTerms.setOnClickListener(urlButtonClickListener);
        buttonTmdbApiTerms.setOnClickListener(urlButtonClickListener);
        buttonTraktTerms.setOnClickListener(urlButtonClickListener);
        buttonCredits.setOnClickListener(urlButtonClickListener);

        return v;
    }

    private View.OnClickListener urlButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onWebsiteButtonClick(v.getId());
        }
    };

    private void onWebsiteButtonClick(@IdRes int viewId) {
        if (viewId == R.id.buttonAboutWebsite) {
            viewUrl(R.string.url_website);
        } else if (viewId == R.id.buttonAboutTvdbTerms) {
            viewUrl(R.string.url_terms_tvdb);
        } else if (viewId == R.id.buttonAboutCreativeCommons) {
            viewUrl(R.string.url_creative_commons);
        } else if (viewId == R.id.buttonAboutTmdbTerms) {
            viewUrl(R.string.url_terms_tmdb);
        } else if (viewId == R.id.buttonAboutTmdbApiTerms) {
            viewUrl(R.string.url_terms_tmdb_api);
        } else if (viewId == R.id.buttonAboutTraktTerms) {
            viewUrl(R.string.url_terms_trakt);
        } else if (viewId == R.id.buttonAboutCredits) {
            viewUrl(R.string.url_credits);
        }
    }

    private void viewUrl(@StringRes int urlResId) {
        Utils.launchWebsite(getActivity(), getString(urlResId), "About", "Terms site");
    }
}
