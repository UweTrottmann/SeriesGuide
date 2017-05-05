package com.battlelancer.seriesguide.ui;

import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.loaders.ShowCreditsLoader;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.thetvdbapi.TvdbImageTools;
import com.battlelancer.seriesguide.ui.dialogs.LanguageChoiceDialogFragment;
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
import com.battlelancer.seriesguide.util.ViewTools;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.tmdb2.entities.Credits;
import java.util.Date;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
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
    @BindView(R.id.textViewShowReleaseCountry) TextView mTextViewReleaseCountry;
    @BindView(R.id.textViewShowFirstAirdate) TextView mTextViewFirstRelease;
    @BindView(R.id.textViewShowContentRating) TextView mTextViewContentRating;
    @BindView(R.id.textViewShowGenres) TextView mTextViewGenres;
    @BindView(R.id.textViewRatingsValue) TextView mTextViewRatingGlobal;
    @BindView(R.id.textViewRatingsVotes) TextView mTextViewRatingVotes;
    @BindView(R.id.textViewRatingsUser) TextView mTextViewRatingUser;
    @BindView(R.id.textViewShowLastEdit) TextView mTextViewLastEdit;

    @BindView(R.id.buttonShowFavorite) Button buttonFavorite;
    @BindView(R.id.buttonShowNotify) Button buttonNotify;
    @BindView(R.id.buttonShowHidden) Button buttonHidden;
    @BindView(R.id.buttonShowShortcut) Button buttonShortcut;
    @BindView(R.id.buttonShowLanguage) Button buttonLanguage;
    @BindView(R.id.containerRatings) View mButtonRate;
    @BindView(R.id.buttonShowImdb) View mButtonImdb;
    @BindView(R.id.buttonShowTvdb) View mButtonTvdb;
    @BindView(R.id.buttonShowTrakt) View mButtonTrakt;
    @BindView(R.id.buttonShowWebSearch) Button mButtonWebSearch;
    @BindView(R.id.buttonShowComments) Button mButtonComments;
    @BindView(R.id.buttonShowShare) Button buttonShare;

    @BindView(R.id.labelCast) TextView castLabel;
    @BindView(R.id.containerCast) LinearLayout castContainer;
    @BindView(R.id.labelCrew) TextView crewLabel;
    @BindView(R.id.containerCrew) LinearLayout crewContainer;

    private Unbinder unbinder;
    private Cursor showCursor;
    private TraktRatingsTask traktTask;
    private String showTitle;
    private String posterPath;
    @Nullable private String languageCode;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_show, container, false);
        unbinder = ButterKnife.bind(this, v);

        // favorite + notifications + visibility button
        CheatSheet.setup(buttonFavorite);
        CheatSheet.setup(buttonNotify);
        CheatSheet.setup(buttonHidden);

        // language button
        Resources.Theme theme = getActivity().getTheme();
        ViewTools.setVectorDrawableLeft(theme, buttonLanguage, R.drawable.ic_language_white_24dp);
        buttonLanguage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                displayLanguageSettings();
            }
        });
        CheatSheet.setup(buttonLanguage, R.string.pref_language);

        // rate button
        mButtonRate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rateShow();
            }
        });
        CheatSheet.setup(mButtonRate, R.string.action_rate);

        // search and comments button
        ViewTools.setVectorDrawableLeft(theme, mButtonWebSearch, R.drawable.ic_search_white_24dp);
        ViewTools.setVectorDrawableLeft(theme, mButtonComments, R.drawable.ic_forum_black_24dp);

        // share button
        ViewTools.setVectorCompoundDrawableLeft(theme, buttonShare, R.attr.drawableShare);
        buttonShare.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                shareShow();
            }
        });

        // shortcut button
        buttonShortcut.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                createShortcut();
            }
        });

        setCastVisibility(false);
        setCrewVisibility(false);

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
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
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
                Shows.LANGUAGE,
                Shows.NOTIFY,
                Shows.HIDDEN
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
        int NOTIFY = 21;
        int HIDDEN = 22;
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
        posterPath = showCursor.getString(ShowQuery.POSTER);

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
        mTextViewRuntime.setText(getString(R.string.runtime_minutes,
                String.valueOf(showCursor.getInt(ShowQuery.RUNTIME))));

        // network
        mTextViewNetwork.setText(network);

        // favorite button
        final boolean isFavorite = showCursor.getInt(ShowQuery.IS_FAVORITE) == 1;
        ViewTools.setVectorDrawableTop(getActivity().getTheme(), buttonFavorite, isFavorite
                ? R.drawable.ic_star_black_24dp
                : R.drawable.ic_star_border_black_24dp);
        String labelFavorite = getString(
                isFavorite ? R.string.context_unfavorite : R.string.context_favorite);
        buttonFavorite.setText(labelFavorite);
        buttonFavorite.setContentDescription(labelFavorite);
        buttonFavorite.setEnabled(true);
        buttonFavorite.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable until action is complete
                v.setEnabled(false);
                SgApp.from(getActivity())
                        .getShowTools()
                        .storeIsFavorite(getShowTvdbId(), !isFavorite);
            }
        });

        // notifications button
        final boolean notify = showCursor.getInt(ShowQuery.NOTIFY) == 1;
        buttonNotify.setContentDescription(getString(notify
                ? R.string.action_episode_notifications_off
                : R.string.action_episode_notifications_on));
        ViewTools.setVectorDrawableTop(getActivity().getTheme(), buttonNotify, notify
                ? R.drawable.ic_notifications_active_black_24dp
                : R.drawable.ic_notifications_off_black_24dp);
        buttonNotify.setEnabled(true);
        buttonNotify.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Utils.hasAccessToX(getActivity())) {
                    Utils.advertiseSubscription(getActivity());
                    return;
                }
                // disable until action is complete
                v.setEnabled(false);
                SgApp.from(getActivity())
                        .getShowTools()
                        .storeNotify(getShowTvdbId(), !notify);
            }
        });

        // hidden button
        final boolean isHidden = showCursor.getInt(ShowQuery.HIDDEN) == 1;
        String label = getString(isHidden ? R.string.context_unhide : R.string.context_hide);
        buttonHidden.setContentDescription(label);
        buttonHidden.setText(label);
        ViewTools.setVectorDrawableTop(getActivity().getTheme(), buttonHidden,
                isHidden
                        ? R.drawable.ic_visibility_off_black_24dp
                        : R.drawable.ic_visibility_black_24dp);
        buttonHidden.setEnabled(true);
        buttonHidden.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable until action is complete
                v.setEnabled(false);
                SgApp.from(getActivity()).getShowTools().storeIsHidden(getShowTvdbId(), !isHidden);
            }
        });

        // overview
        String overview = showCursor.getString(ShowQuery.OVERVIEW);
        if (TextUtils.isEmpty(overview) && showCursor != null) {
            // no description available, show no translation available message
            mTextViewOverview.setText(getString(R.string.no_translation,
                    LanguageTools.getShowLanguageStringFor(getContext(),
                            showCursor.getString(ShowQuery.LANGUAGE)),
                    getString(R.string.tvdb)));
        } else {
            mTextViewOverview.setText(overview);
        }

        // language preferred for content
        LanguageTools.LanguageData languageData = LanguageTools.getShowLanguageDataFor(
                getContext(), showCursor.getString(ShowQuery.LANGUAGE));
        if (languageData != null) {
            languageCode = languageData.languageCode;
            buttonLanguage.setText(languageData.languageString);
        }

        // country for release time calculation
        // show "unknown" if country is not supported
        mTextViewReleaseCountry.setText(TimeTools.getCountry(getActivity(), releaseCountry));

        // original release
        String firstRelease = showCursor.getString(ShowQuery.FIRST_RELEASE);
        ViewTools.setValueOrPlaceholder(mTextViewFirstRelease,
                TimeTools.getShowReleaseYear(firstRelease));

        // content rating
        ViewTools.setValueOrPlaceholder(mTextViewContentRating,
                showCursor.getString(ShowQuery.CONTENT_RATING));
        // genres
        ViewTools.setValueOrPlaceholder(mTextViewGenres,
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
        if (TextUtils.isEmpty(posterPath)) {
            // have no poster
            posterContainer.setClickable(false);
            posterContainer.setFocusable(false);
        } else {
            // poster and fullscreen button
            TvdbImageTools.loadShowPoster(getActivity(), posterView, posterPath);
            posterContainer.setFocusable(true);
            posterContainer.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(getActivity(), FullscreenImageActivity.class);
                    intent.putExtra(FullscreenImageActivity.EXTRA_PREVIEW_IMAGE,
                            TvdbImageTools.smallSizeUrl(posterPath));
                    intent.putExtra(FullscreenImageActivity.EXTRA_IMAGE,
                            TvdbImageTools.fullSizeUrl(posterPath));
                    Utils.startActivityWithAnimation(getActivity(), intent, v);
                }
            });

            // poster background
            TvdbImageTools.loadShowPosterAlpha(getActivity(), posterBackgroundView, posterPath);
        }

        loadTraktRatings();
    }

    private LoaderCallbacks<Credits> mCreditsLoaderCallbacks = new LoaderCallbacks<Credits>() {
        @Override
        public Loader<Credits> onCreateLoader(int id, Bundle args) {
            return new ShowCreditsLoader((SgApp) getActivity().getApplication(), getShowTvdbId(),
                    true);
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
            setCastVisibility(false);
            setCrewVisibility(false);
            return;
        }

        if (credits.cast == null || credits.cast.size() == 0) {
            setCastVisibility(false);
        } else {
            setCastVisibility(true);
            PeopleListHelper.populateShowCast(getActivity(), castContainer, credits, TAG);
        }

        if (credits.crew == null || credits.crew.size() == 0) {
            setCrewVisibility(false);
        } else {
            setCrewVisibility(true);
            PeopleListHelper.populateShowCrew(getActivity(), crewContainer, credits, TAG);
        }
    }

    private void setCastVisibility(boolean visible) {
        castLabel.setVisibility(visible ? View.VISIBLE : View.GONE);
        castContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setCrewVisibility(boolean visible) {
        crewLabel.setVisibility(visible ? View.VISIBLE : View.GONE);
        crewContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
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
            traktTask = new TraktRatingsTask(SgApp.from(getActivity()), getShowTvdbId());
            AsyncTaskCompat.executeParallel(traktTask);
        }
    }

    private void displayLanguageSettings() {
        // guard against onClick called after fragment is up navigated (multi-touch)
        // onSaveInstanceState might already be called
        if (isResumed()) {
            DialogFragment dialog = LanguageChoiceDialogFragment.newInstance(
                    R.array.languageCodesShows, languageCode);
            dialog.show(getFragmentManager(), "dialog-language");
        }
    }

    private void changeShowLanguage(@NonNull String languageCode) {
        this.languageCode = languageCode;

        Timber.d("Changing show language to %s", languageCode);
        SgApp.from(getActivity()).getShowTools().storeLanguage(getShowTvdbId(), languageCode);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(LanguageChoiceDialogFragment.LanguageChangedEvent event) {
        changeShowLanguage(event.selectedLanguageCode);
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
        ShortcutUtils.createShortcut(getContext(), showTitle, posterPath, getShowTvdbId());

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
