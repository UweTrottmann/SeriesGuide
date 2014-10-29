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
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.InjectViews;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.JsonExportTask;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.loaders.TvdbShowLoader;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import java.util.Date;
import java.util.List;

/**
 * A {@link DialogFragment} allowing the user to decide whether to add a show to SeriesGuide.
 */
public class AddShowDialogFragment extends DialogFragment {

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
        DialogFragment newFragment = AddShowDialogFragment.newInstance(show);
        newFragment.show(ft, TAG);
    }

    public static AddShowDialogFragment newInstance(SearchResult show) {
        AddShowDialogFragment f = new AddShowDialogFragment();
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

    @InjectView(R.id.textViewAddTitle) TextView title;
    @InjectView(R.id.textViewAddShowMeta) TextView showmeta;
    @InjectView(R.id.textViewAddDescription) TextView overview;
    @InjectView(R.id.textViewAddRatingsTvdbValue) TextView tvdbRating;
    @InjectView(R.id.textViewAddGenres) TextView genres;
    @InjectView(R.id.textViewAddReleased) TextView released;
    @InjectView(R.id.imageViewAddPoster) ImageView poster;

    @InjectViews({
            R.id.textViewAddRatingsTvdbValue,
            R.id.textViewAddRatingsTvdbLabel,
            R.id.textViewAddRatingsTvdbRange,
            R.id.textViewAddGenresLabel,
            R.id.textViewAddReleasedLabel
    }) List<View> labelViews;

    static final ButterKnife.Setter<View, Boolean> VISIBLE
            = new ButterKnife.Setter<View, Boolean>() {
        @Override
        public void set(View view, Boolean value, int index) {
            view.setVisibility(value ? View.VISIBLE : View.INVISIBLE);
        }
    };

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
        if (SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_DarkBlue) {
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
        final View v = inflater.inflate(R.layout.dialog_addshow, container, false);
        ButterKnife.inject(this, v);

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

        ButterKnife.apply(labelViews, VISIBLE, false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        showProgressBar(true);
        populateShowViews(null);

        // load show details
        Bundle args = new Bundle();
        args.putInt(KEY_SHOW_TVDBID, mShow.tvdbid);
        getLoaderManager().initLoader(ShowsActivity.ADD_SHOW_LOADER_ID, args,
                mShowLoaderCallbacks);
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
            populateShowViews(data);
        }

        @Override
        public void onLoaderReset(Loader<Show> loader) {
            // do nothing
        }
    };

    private void populateShowViews(Show show) {
        if (show == null) {
            mButtonPositive.setEnabled(false);
            if (!AndroidUtils.isNetworkConnected(getActivity())) {
                overview.setText(R.string.offline);
            }
            return;
        }

        mButtonPositive.setEnabled(true);
        ButterKnife.apply(labelViews, VISIBLE, true);

        // title, overview
        title.setText(show.title);
        overview.setText(show.overview);

        SpannableStringBuilder meta = new SpannableStringBuilder();

        // status
        boolean isContinuing = JsonExportTask.ShowStatusExport.CONTINUING.equals(show.status);
        meta.append(getString(isContinuing ? R.string.show_isalive : R.string.show_isnotalive));
        // if continuing, paint status green
        meta.setSpan(
                new TextAppearanceSpan(getActivity(),
                        isContinuing ? R.style.TextAppearance_Subhead_Green
                                : R.style.TextAppearance_Subhead_Dim), 0,
                meta.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        meta.append("\n");

        // next release day and time
        long releaseTime = TimeTools.getShowReleaseTime(show.release_time, show.release_weekday,
                show.release_timezone, show.country);
        if (releaseTime != -1) {
            Date releaseDate = new Date(releaseTime);
            String time = TimeTools.formatToLocalReleaseTime(getActivity(), releaseDate);
            String day = TimeTools.formatToLocalReleaseDay(releaseDate);
            meta.append(day).append(" ").append(time);
            meta.append("\n");
        }

        // network, runtime
        meta.append(show.network);
        meta.append("\n");
        meta.append(getString(R.string.runtime_minutes, show.runtime));

        showmeta.setText(meta);

        // TheTVDB rating
        tvdbRating.setText(
                show.rating > 0 ? String.valueOf(show.rating) : getString(R.string.norating));

        // genres
        Utils.setValueOrPlaceholder(genres, Utils.splitAndKitTVDBStrings(show.genres));

        // original release
        Utils.setValueOrPlaceholder(released, TimeTools.getShowReleaseYear(show.firstAired));

        // poster
        Utils.loadPosterThumbnail(getActivity(), poster, show.poster);
    }

    private void showProgressBar(boolean isVisible) {
        mProgressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }
}
