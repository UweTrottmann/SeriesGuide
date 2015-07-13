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
import butterknife.Bind;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.dataliberation.DataLiberationTools;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.items.SearchResult;
import com.battlelancer.seriesguide.loaders.TvdbShowLoader;
import com.battlelancer.seriesguide.ui.AddFragment;
import com.battlelancer.seriesguide.ui.ShowsActivity;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.TraktTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import de.greenrobot.event.EventBus;
import java.util.Date;
import java.util.List;

/**
 * A {@link DialogFragment} allowing the user to decide whether to add a show to SeriesGuide.
 * Displays show details as well.
 */
public class AddShowDialogFragment extends DialogFragment {

    public static final String TAG = "AddShowDialogFragment";
    private static final String KEY_SHOW_TVDBID = "show_tvdbid";

    private SearchResult mShow;

    /**
     * Display a {@link com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment} for the given
     * show.
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

    /**
     * Display a {@link com.battlelancer.seriesguide.ui.dialogs.AddShowDialogFragment} for the given
     * show.
     *
     * <p> Use if there is no actual search result, but just a TheTVDB id available.
     */
    public static void showAddDialog(int showTvdbId, FragmentManager fm) {
        SearchResult fakeResult = new SearchResult();
        fakeResult.tvdbid = showTvdbId;
        showAddDialog(fakeResult, fm);
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

    @Bind(R.id.textViewAddTitle) TextView title;
    @Bind(R.id.textViewAddShowMeta) TextView showmeta;
    @Bind(R.id.textViewAddDescription) TextView overview;
    @Bind(R.id.textViewAddRatingValue) TextView rating;
    @Bind(R.id.textViewAddGenres) TextView genres;
    @Bind(R.id.textViewAddReleased) TextView released;
    @Bind(R.id.imageViewAddPoster) ImageView poster;

    @Bind({
            R.id.textViewAddRatingValue,
            R.id.textViewAddRatingLabel,
            R.id.textViewAddRatingRange,
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

    @Bind(R.id.buttonPositive) Button mButtonPositive;
    @Bind(R.id.buttonNegative) Button mButtonNegative;
    @Bind(R.id.progressBarAdd) View mProgressBar;

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
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.dialog_addshow, container, false);
        ButterKnife.bind(this, v);

        // buttons
        mButtonNegative.setText(R.string.dismiss);
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
                mShow.isAdded = true;
                EventBus.getDefault().post(new AddFragment.AddShowEvent());

                mListener.onAddShow(mShow);
                dismiss();
            }
        });
        mButtonPositive.setEnabled(false);

        ButterKnife.apply(labelViews, VISIBLE, false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        showProgressBar(true);

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

        ButterKnife.unbind(this);
    }

    private LoaderManager.LoaderCallbacks<TvdbShowLoader.Result> mShowLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<TvdbShowLoader.Result>() {
        @Override
        public Loader<TvdbShowLoader.Result> onCreateLoader(int id, Bundle args) {
            int showTvdbId = args.getInt(KEY_SHOW_TVDBID);
            return new TvdbShowLoader(getActivity(), showTvdbId);
        }

        @Override
        public void onLoadFinished(Loader<TvdbShowLoader.Result> loader,
                TvdbShowLoader.Result data) {
            if (!isAdded()) {
                return;
            }
            showProgressBar(false);
            populateShowViews(data);
        }

        @Override
        public void onLoaderReset(Loader<TvdbShowLoader.Result> loader) {
            // do nothing
        }
    };

    private void populateShowViews(TvdbShowLoader.Result result) {
        Show show = result.show;
        if (show == null) {
            // failed to load, can't be added
            if (!AndroidUtils.isNetworkConnected(getActivity())) {
                overview.setText(R.string.offline);
            }
            return;
        }
        if (result.isAdded) {
            // already added, prevent adding
            mButtonPositive.setVisibility(View.GONE);
        } else {
            mButtonPositive.setEnabled(true);
        }

        // store title for add task
        mShow.title = show.title;

        // title, overview
        title.setText(show.title);
        overview.setText(show.overview);

        SpannableStringBuilder meta = new SpannableStringBuilder();

        // status
        int encodedStatus = DataLiberationTools.encodeShowStatus(show.status);
        if (encodedStatus != ShowTools.Status.UNKNOWN) {
            String statusText = ShowTools.getStatus(getActivity(), encodedStatus);
            if (statusText != null) {
                meta.append(statusText);
                // if continuing, paint status green
                meta.setSpan(new TextAppearanceSpan(getActivity(),
                                encodedStatus == ShowTools.Status.CONTINUING
                                        ? R.style.TextAppearance_Subhead_Green
                                        : R.style.TextAppearance_Subhead_Dim), 0, meta.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                meta.append("\n");
            }
        }

        // next release day and time
        if (show.release_time != -1) {
            Date release = TimeTools.getShowReleaseDateTime(getActivity(),
                    TimeTools.getShowReleaseTime(show.release_time),
                    show.release_weekday,
                    show.release_timezone,
                    show.country);
            String day = TimeTools.formatToLocalDayOrDaily(getActivity(), release,
                    show.release_weekday);
            String time = TimeTools.formatToLocalTime(getActivity(), release);
            meta.append(day).append(" ").append(time);
            meta.append("\n");
        }

        // network, runtime
        meta.append(show.network);
        meta.append("\n");
        meta.append(getString(R.string.runtime_minutes, show.runtime));

        showmeta.setText(meta);

        // rating
        rating.setText(TraktTools.buildRatingString(show.rating));

        // genres
        Utils.setValueOrPlaceholder(genres, Utils.splitAndKitTVDBStrings(show.genres));

        // original release
        Utils.setValueOrPlaceholder(released, TimeTools.getShowReleaseYear(show.firstAired));

        // poster
        Utils.loadTvdbShowPoster(getActivity(), poster, show.poster);

        // enable adding of show, display views
        mButtonPositive.setEnabled(true);
        ButterKnife.apply(labelViews, VISIBLE, true);
    }

    private void showProgressBar(boolean isVisible) {
        mProgressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }
}
