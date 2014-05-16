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

package com.battlelancer.seriesguide.ui.dialogs;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.loaders.TvdbShowLoader;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Utils;
import timber.log.Timber;

/**
 * A {@link DialogFragment} allowing the user to decide whether to add a show to SeriesGuide.
 */
public class AddDialogFragment extends DialogFragment {

    public static final String TAG = "AddDialogFragment";
    private static final String KEY_SHOW_TVDBID = "show_tvdbid";
    private SearchResult mShow;

    /**
     * Display a dialog which asks if the user wants to add the given show to his show database. If
     * necessary an AsyncTask will be started which takes care of adding the show.
     */
    public static void showAddDialog(SearchResult show, FragmentManager fm) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction. We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag(TAG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = AddDialogFragment.newInstance(show);
        newFragment.show(ft, TAG);
    }

    public static AddDialogFragment newInstance(SearchResult show) {
        AddDialogFragment f = new AddDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(InitBundle.SEARCH_RESULT, show);
        f.setArguments(args);
        return f;
    }

    public interface OnAddShowListener {

        public void onAddShow(SearchResult show);
    }

    private interface InitBundle {
        String SEARCH_RESULT = "search_result";
    }

    private OnAddShowListener mListener;

    @InjectView(R.id.textViewAddTitle) TextView mTitle;
    @InjectView(R.id.textViewAddDescription) TextView mDescription;
    @InjectView(R.id.imageViewAddPoster) ImageView mPoster;

    @InjectView(R.id.buttonPositive) Button mButtonPositive;
    @InjectView(R.id.buttonNegative) Button mButtonNegative;
    @InjectView(R.id.progressBarAdd) View mProgressBar;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnAddShowListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnAddShowListener");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mShow = getArguments().getParcelable(InitBundle.SEARCH_RESULT);
        if (mShow == null || mShow.tvdbid <= 0) {
            // invalid TVDb id
            dismiss();
            return;
        }

        // hide title, use custom theme
        if (SeriesGuidePreferences.THEME == R.style.AndroidTheme) {
            setStyle(STYLE_NO_TITLE, 0);
        } else if (SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_Light) {
            setStyle(STYLE_NO_TITLE, R.style.Theme_SeriesGuide_Light_Dialog);
        } else {
            setStyle(STYLE_NO_TITLE, R.style.Theme_SeriesGuide_Dialog);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.add_dialog, container, false);
        ButterKnife.inject(this, v);

        mPoster.setVisibility(View.GONE);

        // buttons
        mButtonNegative.setText(R.string.dont_add_show);
        mButtonNegative.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        mButtonPositive.setText(R.string.action_shows_add);
        mButtonPositive.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onAddShow(mShow);
                dismiss();
            }
        });

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // ensure at least a title
        if (TextUtils.isEmpty(mShow.title)) {
            Timber.d("No title present, loading details from TVDb");
            showProgressBar(true);
            populateShowViews(null);

            // load show details
            Bundle args = new Bundle();
            args.putInt(KEY_SHOW_TVDBID, mShow.tvdbid);
            getLoaderManager().initLoader(ShowsActivity.ADD_SHOW_LOADER_ID, args,
                    mShowLoaderCallbacks);
        } else {
            // use existing show details
            showProgressBar(false);
            populateShowViews(mShow);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Utils.trackView(getActivity(), "Add Dialog");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        ButterKnife.reset(this);
    }

    private LoaderManager.LoaderCallbacks<Show> mShowLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Show>() {
        @Override
        public Loader<Show> onCreateLoader(int id, Bundle args) {
            int showTvdbId = args.getInt(KEY_SHOW_TVDBID);
            return new TvdbShowLoader(getActivity(), showTvdbId);
        }

        @Override
        public void onLoadFinished(Loader<Show> loader, Show data) {
            showProgressBar(false);
            if (data != null) {
                mShow.title = data.title;
                mShow.overview = data.overview;
                populateShowViews(mShow);
            } else {
                populateShowViews(null);
            }
        }

        @Override
        public void onLoaderReset(Loader<Show> loader) {
            // do nothing
        }
    };

    private void populateShowViews(SearchResult show) {
        if (show == null) {
            mButtonPositive.setEnabled(false);
            return;
        }

        mButtonPositive.setEnabled(true);

        // title and description
        mTitle.setText(show.title);
        mDescription.setText(show.overview);

        // poster
        if (DisplaySettings.isVeryLargeScreen(getActivity())) {
            if (show.poster != null) {
                mPoster.setVisibility(View.VISIBLE);
                ServiceUtils.getPicasso(getActivity()).load(show.poster).into(mPoster);
            }
        }
    }

    private void showProgressBar(boolean isVisible) {
        mProgressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }
}
