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
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.Utils;

/**
 * Displays information about the app, the developer and licence information about content and
 * libraries.
 *
 * <p>Note: this is a platform, not a support library fragment so it can be used right within {@link
 * SeriesGuidePreferences}.
 */
public class AboutSettingsFragment extends Fragment {

    @BindView(R.id.textViewAboutVersion) TextView textVersion;
    @BindView(R.id.buttonAboutWebsite) Button buttonWebsite;
    @BindView(R.id.buttonAboutTvdbTerms) Button buttonTvdbTerms;
    @BindView(R.id.buttonAboutCreativeCommons) Button buttonCreativeCommons;
    @BindView(R.id.buttonAboutTmdbTerms) Button buttonTmdbTerms;
    @BindView(R.id.buttonAboutTmdbApiTerms) Button buttonTmdbApiTerms;
    @BindView(R.id.buttonAboutTraktTerms) Button buttonTraktTerms;
    @BindView(R.id.buttonAboutCredits) Button buttonCredits;

    private Unbinder unbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_about, container, false);
        unbinder = ButterKnife.bind(this, v);

        // display version number and database version
        textVersion.setText(Utils.getVersionString(getActivity()));

        buttonWebsite.setOnClickListener(urlButtonClickListener);
        buttonTvdbTerms.setOnClickListener(urlButtonClickListener);
        buttonCreativeCommons.setOnClickListener(urlButtonClickListener);
        buttonTmdbTerms.setOnClickListener(urlButtonClickListener);
        buttonTmdbApiTerms.setOnClickListener(urlButtonClickListener);
        buttonTraktTerms.setOnClickListener(urlButtonClickListener);
        buttonCredits.setOnClickListener(urlButtonClickListener);

        return v;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
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
        Utils.launchWebsite(getActivity(), getString(urlResId));
    }
}
