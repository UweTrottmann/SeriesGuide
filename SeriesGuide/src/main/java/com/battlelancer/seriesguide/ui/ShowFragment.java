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

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.os.AsyncTaskCompat;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.loaders.ShowCreditsLoader;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.ui.dialogs.ManageListsDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.RateDialogFragment;
import com.battlelancer.seriesguide.util.LanguageTools;
import com.battlelancer.seriesguide.util.PeopleListHelper;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.ShortcutUtils;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.TraktRatingsTask;
import com.battlelancer.seriesguide.util.TraktTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.tmdb2.entities.Credits;
import java.util.Date;
import timber.log.Timber;

import static com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;

/**
 * Displays extended information (poster, release info, description, ...) and actions (favoriting,
 * shortcut) for a particular show.
 */
public class ShowFragment extends Fragment {

    public interface InitBundle {

        String SHOW_TVDBID = "tvdbid";
    }

    private static final String TAG = "Show Info";

    public static ShowFragment newInstance(int showTvdbId) {
        ShowFragment f = new ShowFragment();

        Bundle args = new Bundle();
        args.putInt(InitBundle.SHOW_TVDBID, showTvdbId);
        f.setArguments(args);

        return f;
    }

    @BindView(R.id.imageViewShowPosterBackground) ImageView posterBackgroundView;

    @BindView(R.id.containerShowPoster) View posterContainer;
    @BindView(R.id.imageViewShowPoster) ImageView posterView;
    @BindView(R.id.textViewShowStatus) TextView mTextViewStatus;
    @BindView(R.id.textViewShowReleaseTime) TextView mTextViewReleaseTime;
    @BindView(R.id.textViewShowRuntime) TextView mTextViewRuntime;
    @BindView(R.id.textViewShowNetwork) TextView mTextViewNetwork;
    @BindView(R.id.textViewShowOverview) TextView mTextViewOverview;
    @BindView(R.id.spinnerShowLanguage) Spinner mSpinnerLanguage;
    @BindView(R.id.textViewShowReleaseCountry) TextView mTextViewReleaseCountry;
    @BindView(R.id.textViewShowFirstAirdate) TextView mTextViewFirstRelease;
    @BindView(R.id.textViewShowContentRating) TextView mTextViewContentRating;
    @BindView(R.id.textViewShowGenres) TextView mTextViewGenres;
    @BindView(R.id.textViewRatingsValue) TextView mTextViewRatingGlobal;
    @BindView(R.id.textViewRatingsVotes) TextView mTextViewRatingVotes;
    @BindView(R.id.textViewRatingsUser) TextView mTextViewRatingUser;
    @BindView(R.id.textViewShowLastEdit) TextView mTextViewLastEdit;

    @BindView(R.id.buttonShowInfoIMDB) View mButtonImdb;
    @BindView(R.id.buttonShowFavorite) Button mButtonFavorite;
    @BindView(R.id.buttonShowShare) Button mButtonShare;
    @BindView(R.id.buttonShowShortcut) Button mButtonShortcut;
    @BindView(R.id.containerRatings) View mButtonRate;
    @BindView(R.id.buttonTVDB) View mButtonTvdb;
    @BindView(R.id.buttonTrakt) View mButtonTrakt;
    @BindView(R.id.buttonWebSearch) View mButtonWebSearch;
    @BindView(R.id.buttonShouts) View mButtonComments;

    @BindView(R.id.containerShowCast) View mCastView;
    private LinearLayout mCastContainer;
    @BindView(R.id.containerShowCrew) View mCrewView;
    private LinearLayout mCrewContainer;

    private Unbinder unbinder;
    private Cursor showCursor;
    private TraktRatingsTask traktTask;
    private String showTitle;
    private String showPoster;
    private int spinnerLastPosition;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_show, container, false);
        unbinder = ButterKnife.bind(this, v);

        // share button
        mButtonShare.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                shareShow();
            }
        });
        CheatSheet.setup(mButtonShare);

        // shortcut button
        mButtonShortcut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                createShortcut();
            }
        });
        CheatSheet.setup(mButtonShortcut);

        // rate button
        mButtonRate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rateShow();
            }
        });
        CheatSheet.setup(mButtonRate, R.string.action_rate);

        TextView castHeader = ButterKnife.findById(mCastView, R.id.textViewPeopleHeader);
        castHeader.setText(R.string.movie_cast);
        mCastContainer = ButterKnife.findById(mCastView, R.id.containerPeople);

        TextView crewHeader = ButterKnife.findById(mCrewView, R.id.textViewPeopleHeader);
        crewHeader.setText(R.string.movie_crew);
        mCrewContainer = ButterKnife.findById(mCrewView, R.id.containerPeople);

        // language chooser
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                R.array.languages, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerLanguage.setAdapter(adapter);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getLoaderManager().initLoader(OverviewActivity.SHOW_LOADER_ID, null, mShowLoaderCallbacks);
        getLoaderManager().initLoader(OverviewActivity.SHOW_CREDITS_LOADER_ID, null,
                mCreditsLoaderCallbacks);

        setHasOptionsMenu(true);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.show_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_show_manage_lists) {
            ManageListsDialogFragment.showListsDialog(getShowTvdbId(), ListItemTypes.SHOW,
                    getFragmentManager());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    interface ShowQuery {

        String[] PROJECTION = new String[] {
                Shows._ID,
                Shows.TITLE,
                Shows.STATUS,
                Shows.RELEASE_TIME,
                Shows.RELEASE_WEEKDAY,
                Shows.RELEASE_TIMEZONE,
                Shows.RELEASE_COUNTRY,
                Shows.NETWORK,
                Shows.POSTER,
                Shows.IMDBID,
                Shows.RUNTIME,
                Shows.FAVORITE,
                Shows.OVERVIEW,
                Shows.FIRST_RELEASE,
                Shows.CONTENTRATING,
                Shows.GENRES,
                Shows.RATING_GLOBAL,
                Shows.RATING_VOTES,
                Shows.RATING_USER,
                Shows.LASTEDIT,
                Shows.LANGUAGE
        };

        int TITLE = 1;
        int STATUS = 2;
        int RELEASE_TIME = 3;
        int RELEASE_WEEKDAY = 4;
        int RELEASE_TIMEZONE = 5;
        int RELEASE_COUNTRY = 6;
        int NETWORK = 7;
        int POSTER = 8;
        int IMDBID = 9;
        int RUNTIME = 10;
        int IS_FAVORITE = 11;
        int OVERVIEW = 12;
        int FIRST_RELEASE = 13;
        int CONTENT_RATING = 14;
        int GENRES = 15;
        int RATING_GLOBAL = 16;
        int RATING_VOTES = 17;
        int RATING_USER = 18;
        int LAST_EDIT_MS = 19;
        int LANGUAGE = 20;
    }

    private LoaderCallbacks<Cursor> mShowLoaderCallbacks = new LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            return new CursorLoader(getActivity(), Shows.buildShowUri(getShowTvdbId()),
                    ShowQuery.PROJECTION, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (!isAdded()) {
                return;
            }
            if (data != null && data.moveToFirst()) {
                showCursor = data;
                populateShow();
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            // do nothing, prefer stale data
        }
    };

    private void populateShow() {
        if (showCursor == null) {
            return;
        }

        // title
        showTitle = showCursor.getString(ShowQuery.TITLE);
        showPoster = showCursor.getString(ShowQuery.POSTER);

        // status
        ShowTools.setStatusAndColor(mTextViewStatus, showCursor.getInt(ShowQuery.STATUS));

        // next release day and time
        String releaseCountry = showCursor.getString(ShowQuery.RELEASE_COUNTRY);
        int releaseTime = showCursor.getInt(ShowQuery.RELEASE_TIME);
        String network = showCursor.getString(ShowQuery.NETWORK);
        if (releaseTime != -1) {
            int weekDay = showCursor.getInt(ShowQuery.RELEASE_WEEKDAY);
            Date release = TimeTools.getShowReleaseDateTime(getActivity(),
                    TimeTools.getShowReleaseTime(releaseTime),
                    weekDay,
                    showCursor.getString(ShowQuery.RELEASE_TIMEZONE),
                    releaseCountry, network);
            String dayString = TimeTools.formatToLocalDayOrDaily(getActivity(), release, weekDay);
            String timeString = TimeTools.formatToLocalTime(getActivity(), release);
            mTextViewReleaseTime.setText(String.format("%s %s", dayString, timeString));
        } else {
            mTextViewReleaseTime.setText(null);
        }

        // runtime
        mTextViewRuntime.setText(
                getString(R.string.runtime_minutes, showCursor.getInt(ShowQuery.RUNTIME)));

        // network
        mTextViewNetwork.setText(network);

        // favorite button
        final boolean isFavorite = showCursor.getInt(ShowQuery.IS_FAVORITE) == 1;
        mButtonFavorite.setEnabled(true);
        Utils.setCompoundDrawablesRelativeWithIntrinsicBounds(mButtonFavorite, 0,
                Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                        isFavorite ? R.attr.drawableStar : R.attr.drawableStar0),
                0, 0);
        mButtonFavorite.setText(
                isFavorite ? R.string.context_unfavorite : R.string.context_favorite);
        CheatSheet.setup(mButtonFavorite,
                isFavorite ? R.string.context_unfavorite : R.string.context_favorite);
        mButtonFavorite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable until action is complete
                v.setEnabled(false);
                ShowTools.get(v.getContext()).storeIsFavorite(getShowTvdbId(), !isFavorite);
            }
        });

        // overview
        String overview = showCursor.getString(ShowQuery.OVERVIEW);
        if (TextUtils.isEmpty(overview) && showCursor != null) {
            // no description available, show no translation available message
            mTextViewOverview.setText(getString(R.string.no_translation,
                    LanguageTools.getLanguageStringForCode(getContext(),
                            showCursor.getString(ShowQuery.LANGUAGE)),
                    getString(R.string.tvdb)));
        } else {
            mTextViewOverview.setText(overview);
        }

        // language preferred for content
        String languageCode = showCursor.getString(ShowQuery.LANGUAGE);
        if (TextUtils.isEmpty(languageCode)) {
            languageCode = DisplaySettings.getContentLanguage(getContext());
        }
        final String[] languageCodes = getResources().getStringArray(R.array.languageData);
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(languageCode)) {
                spinnerLastPosition = i;
                mSpinnerLanguage.setSelection(i, false);
                break;
            }
        }
        mSpinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == spinnerLastPosition) {
                    // guard against firing after layout completes
                    // still happening on custom ROMs despite workaround described at
                    // http://stackoverflow.com/a/17336944/1000543
                    return;
                }
                spinnerLastPosition = position;
                changeShowLanguage(parent.getContext(), getShowTvdbId(), languageCodes[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });

        // country for release time calculation
        // show "unknown" if country is not supported
        mTextViewReleaseCountry.setText(TimeTools.getCountry(getActivity(), releaseCountry));

        // original release
        String firstRelease = showCursor.getString(ShowQuery.FIRST_RELEASE);
        Utils.setValueOrPlaceholder(mTextViewFirstRelease,
                TimeTools.getShowReleaseYear(firstRelease));

        // content rating
        Utils.setValueOrPlaceholder(mTextViewContentRating,
                showCursor.getString(ShowQuery.CONTENT_RATING));
        // genres
        Utils.setValueOrPlaceholder(mTextViewGenres,
                TextTools.splitAndKitTVDBStrings(showCursor.getString(ShowQuery.GENRES)));

        // trakt rating
        mTextViewRatingGlobal.setText(TraktTools.buildRatingString(
                showCursor.getDouble(ShowQuery.RATING_GLOBAL)));
        mTextViewRatingVotes.setText(TraktTools.buildRatingVotesString(getActivity(),
                showCursor.getInt(ShowQuery.RATING_VOTES)));

        // user rating
        mTextViewRatingUser.setText(TraktTools.buildUserRatingString(getActivity(),
                showCursor.getInt(ShowQuery.RATING_USER)));

        // last edit
        long lastEditRaw = showCursor.getLong(ShowQuery.LAST_EDIT_MS);
        if (lastEditRaw > 0) {
            mTextViewLastEdit.setText(DateUtils.formatDateTime(getActivity(), lastEditRaw * 1000,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME));
        } else {
            mTextViewLastEdit.setText(R.string.unknown);
        }

        // IMDb button
        String imdbId = showCursor.getString(ShowQuery.IMDBID);
        ServiceUtils.setUpImdbButton(imdbId, mButtonImdb, TAG);

        // TVDb button
        ServiceUtils.setUpTvdbButton(getShowTvdbId(), mButtonTvdb, TAG);

        // trakt button
        ServiceUtils.setUpTraktShowButton(mButtonTrakt, getShowTvdbId(), TAG);

        // web search button
        ServiceUtils.setUpWebSearchButton(showTitle, mButtonWebSearch, TAG);

        // shout button
        mButtonComments.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), TraktCommentsActivity.class);
                i.putExtras(TraktCommentsActivity.createInitBundleShow(showTitle,
                        getShowTvdbId()));
                Utils.startActivityWithAnimation(getActivity(), i, v);
                Utils.trackAction(v.getContext(), TAG, "Comments");
            }
        });

        // poster, full screen poster button
        Utils.loadPoster(getActivity(), posterView, showPoster);
        posterContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), FullscreenImageActivity.class);
                intent.putExtra(FullscreenImageActivity.EXTRA_PREVIEW_IMAGE,
                        TheTVDB.buildPosterUrl(showPoster));
                intent.putExtra(FullscreenImageActivity.EXTRA_IMAGE,
                        TheTVDB.buildScreenshotUrl(showPoster));
                Utils.startActivityWithAnimation(getActivity(), intent, v);
            }
        });

        // background
        Utils.loadPosterBackground(getActivity(), posterBackgroundView, showPoster);

        loadTraktRatings();
    }

    private LoaderCallbacks<Credits> mCreditsLoaderCallbacks = new LoaderCallbacks<Credits>() {
        @Override
        public Loader<Credits> onCreateLoader(int id, Bundle args) {
            return new ShowCreditsLoader(getActivity(), getShowTvdbId(), true);
        }

        @Override
        public void onLoadFinished(Loader<Credits> loader, Credits data) {
            if (isAdded()) {
                populateCredits(data);
            }
        }

        @Override
        public void onLoaderReset(Loader<Credits> loader) {

        }
    };

    private void populateCredits(final Credits credits) {
        if (credits == null) {
            mCastView.setVisibility(View.GONE);
            mCrewView.setVisibility(View.GONE);
            return;
        }

        if (credits.cast == null || credits.cast.size() == 0) {
            mCastView.setVisibility(View.GONE);
        } else {
            mCastView.setVisibility(View.VISIBLE);
            PeopleListHelper.populateShowCast(getActivity(), mCastContainer, credits, TAG);
        }

        if (credits.crew == null || credits.crew.size() == 0) {
            mCrewView.setVisibility(View.GONE);
        } else {
            mCrewView.setVisibility(View.VISIBLE);
            PeopleListHelper.populateShowCrew(getActivity(), mCrewContainer, credits, TAG);
        }
    }

    private int getShowTvdbId() {
        return getArguments().getInt(InitBundle.SHOW_TVDBID);
    }

    private void rateShow() {
        if (TraktCredentials.ensureCredentials(getActivity())) {
            RateDialogFragment rateDialog = RateDialogFragment.newInstanceShow(getShowTvdbId());
            rateDialog.show(getFragmentManager(), "ratedialog");
            Utils.trackAction(getActivity(), TAG, "Rate (trakt)");
        }
    }

    private void loadTraktRatings() {
        if (traktTask == null || traktTask.getStatus() == AsyncTask.Status.FINISHED) {
            traktTask = new TraktRatingsTask(getActivity(), getShowTvdbId());
            AsyncTaskCompat.executeParallel(traktTask);
        }
    }

    private static void changeShowLanguage(Context context, int showTvdbId, String languageCode) {
        Timber.d("Changing show language to %s", languageCode);
        ShowTools.get(context).storeLanguage(showTvdbId, languageCode);
    }

    private void createShortcut() {
        if (!Utils.hasAccessToX(getActivity())) {
            Utils.advertiseSubscription(getActivity());
            return;
        }

        if (showCursor == null) {
            return;
        }

        // create the shortcut
        ShortcutUtils.createShortcut(getActivity(), showTitle, showPoster, getShowTvdbId());

        // drop to home screen
        startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK));

        // Analytics
        Utils.trackAction(getActivity(), TAG, "Add to Homescreen");
    }

    private void shareShow() {
        if (showCursor != null) {
            ShareUtils.shareShow(getActivity(), getShowTvdbId(), showTitle);
            Utils.trackAction(getActivity(), TAG, "Share");
        }
    }
}
