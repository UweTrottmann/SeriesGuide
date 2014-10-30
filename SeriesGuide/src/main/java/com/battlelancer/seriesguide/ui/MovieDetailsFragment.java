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

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
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
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.Optional;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.items.MovieDetails;
import com.battlelancer.seriesguide.loaders.MovieCreditsLoader;
import com.battlelancer.seriesguide.loaders.MovieLoader;
import com.battlelancer.seriesguide.loaders.MovieTrailersLoader;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.dialogs.MovieCheckInDialogFragment;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.PeopleListHelper;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.TmdbTools;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.TraktTools;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.entities.Movie;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.tmdb.entities.Credits;
import com.uwetrottmann.tmdb.entities.Trailers;
import de.greenrobot.event.EventBus;

/**
 * Displays details about one movie including plot, ratings, trailers and a poster.
 */
public class MovieDetailsFragment extends Fragment {

    public static MovieDetailsFragment newInstance(int tmdbId) {
        MovieDetailsFragment f = new MovieDetailsFragment();

        Bundle args = new Bundle();
        args.putInt(InitBundle.TMDB_ID, tmdbId);
        f.setArguments(args);

        return f;
    }

    public interface InitBundle {

        String TMDB_ID = "tmdbid";
    }

    private static final String TAG = "Movie Details";

    private int mTmdbId;

    private MovieDetails mMovieDetails = new MovieDetails();

    private Trailers mTrailers;

    private String mImageBaseUrl;

    @InjectView(R.id.contentContainerMovie) ViewGroup mContentContainer;
    @Optional @InjectView(R.id.contentContainerMovieRight) ViewGroup mContentContainerRight;

    @InjectView(R.id.textViewMovieTitle) TextView mMovieTitle;
    @InjectView(R.id.textViewMovieDate) TextView mMovieReleaseDate;
    @InjectView(R.id.textViewMovieDescription) TextView mMovieDescription;
    @InjectView(R.id.imageViewMoviePoster) ImageView mMoviePosterBackground;
    @InjectView(R.id.textViewMovieGenres) TextView mMovieGenres;

    @InjectView(R.id.containerMovieButtons) View mButtonContainer;
    @InjectView(R.id.buttonMovieCheckIn) Button mCheckinButton;
    @InjectView(R.id.buttonMovieWatched) Button mWatchedButton;
    @InjectView(R.id.buttonMovieCollected) Button mCollectedButton;
    @InjectView(R.id.buttonMovieWatchlisted) Button mWatchlistedButton;

    @InjectView(R.id.ratingbar) View mRatingsContainer;
    @InjectView(R.id.textViewRatingsTvdbLabel) TextView mRatingsTmdbLabel;
    @InjectView(R.id.textViewRatingsTvdbValue) TextView mRatingsTmdbValue;
    @InjectView(R.id.textViewRatingsTraktValue) TextView mRatingsTraktValue;
    @InjectView(R.id.textViewRatingsTraktVotes) TextView mRatingsTraktVotes;
    @InjectView(R.id.textViewRatingsTraktUser) TextView mRatingsTraktUserValue;

    @InjectView(R.id.containerMovieCast) View mCastView;
    TextView mCastLabel;
    LinearLayout mCastContainer;

    @InjectView(R.id.containerMovieCrew) View mCrewView;
    TextView mCrewLabel;
    LinearLayout mCrewContainer;

    @InjectView(R.id.buttonMovieComments) Button mCommentsButton;
    @InjectView(R.id.progressBar) View mProgressBar;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_movie, container, false);
        ButterKnife.inject(this, v);

        mProgressBar.setVisibility(View.VISIBLE);

        // important action buttons
        mButtonContainer.setVisibility(View.GONE);
        mRatingsContainer.setVisibility(View.GONE);
        mRatingsContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                rateOnTrakt();
            }
        });
        CheatSheet.setup(mRatingsContainer, R.string.action_rate);
        mRatingsTmdbLabel.setText(R.string.tmdb);

        // cast and crew labels
        mCastLabel = ButterKnife.findById(mCastView, R.id.textViewPeopleHeader);
        mCastLabel.setText(R.string.movie_cast);
        mCastContainer = ButterKnife.findById(mCastView, R.id.containerPeople);
        mCastView.setVisibility(View.GONE);
        mCrewLabel = ButterKnife.findById(mCrewView, R.id.textViewPeopleHeader);
        mCrewLabel.setText(R.string.movie_crew);
        mCrewContainer = ButterKnife.findById(mCrewView, R.id.containerPeople);
        mCrewView.setVisibility(View.GONE);

        // poster background transparency
        if (AndroidUtils.isJellyBeanOrHigher()) {
            mMoviePosterBackground.setImageAlpha(30);
        } else {
            mMoviePosterBackground.setAlpha(30);
        }

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mTmdbId = getArguments().getInt(InitBundle.TMDB_ID);
        if (mTmdbId <= 0) {
            getFragmentManager().popBackStack();
            return;
        }

        setupViews();

        mImageBaseUrl = TmdbSettings.getImageBaseUrl(getActivity())
                + TmdbSettings.POSTER_SIZE_SPEC_W342;

        Bundle args = new Bundle();
        args.putInt(InitBundle.TMDB_ID, mTmdbId);
        getLoaderManager().initLoader(MovieDetailsActivity.LOADER_ID_MOVIE, args,
                mMovieLoaderCallbacks);
        getLoaderManager().initLoader(MovieDetailsActivity.LOADER_ID_MOVIE_TRAILERS, args,
                mMovieTrailerLoaderCallbacks);
        getLoaderManager().initLoader(MovieDetailsActivity.LOADER_ID_MOVIE_CREDITS, args,
                mMovieCreditsLoaderCallbacks);

        setHasOptionsMenu(true);
    }

    private void setupViews() {
        if (AndroidUtils.isKitKatOrHigher()) {
            // avoid overlap with status + action bar (adjust top margin)
            // warning: status bar not always translucent (e.g. Nexus 10)
            // (using fitsSystemWindows would not work correctly with multiple views)
            SystemBarTintManager.SystemBarConfig config
                    = ((MovieDetailsActivity) getActivity()).getSystemBarTintManager().getConfig();
            int pixelInsetTop = config.getPixelInsetTop(false);

            // action bar height is pre-set as top margin, add to it
            ViewGroup.MarginLayoutParams layoutParams
                    = (ViewGroup.MarginLayoutParams) mContentContainer.getLayoutParams();
            layoutParams.setMargins(0, pixelInsetTop + layoutParams.topMargin, 0, 0);
            mContentContainer.setLayoutParams(layoutParams);

            // dual pane layout?
            if (mContentContainerRight != null) {
                ViewGroup.MarginLayoutParams layoutParamsRight
                        = (ViewGroup.MarginLayoutParams) mContentContainerRight.getLayoutParams();
                layoutParamsRight.setMargins(layoutParamsRight.leftMargin,
                        pixelInsetTop + layoutParams.topMargin, 0, 0);
            }
        }
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

        ButterKnife.reset(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (mMovieDetails != null) {
            boolean isLightTheme = SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_Light;
            inflater.inflate(
                    isLightTheme ? R.menu.movie_details_menu_light : R.menu.movie_details_menu,
                    menu);

            // hide Google Play button in Amazon version
            if (Utils.isAmazonVersion()) {
                MenuItem playStoreItem = menu.findItem(R.id.menu_open_google_play);
                playStoreItem.setEnabled(false);
                playStoreItem.setVisible(false);
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        if (mMovieDetails != null) {
            boolean isEnableShare = mMovieDetails.tmdbMovie() != null && !TextUtils.isEmpty(
                    mMovieDetails.tmdbMovie().title);
            MenuItem shareItem = menu.findItem(R.id.menu_movie_share);
            shareItem.setEnabled(isEnableShare);
            shareItem.setVisible(isEnableShare);

            if (!Utils.isAmazonVersion()) {
                MenuItem playStoreItem = menu.findItem(R.id.menu_open_google_play);
                playStoreItem.setEnabled(isEnableShare);
                playStoreItem.setVisible(isEnableShare);
            }

            boolean isEnableImdb = mMovieDetails.traktMovie() != null
                    && !TextUtils.isEmpty(mMovieDetails.traktMovie().imdb_id);
            MenuItem imdbItem = menu.findItem(R.id.menu_open_imdb);
            imdbItem.setEnabled(isEnableImdb);
            imdbItem.setVisible(isEnableImdb);

            boolean isEnableYoutube = mTrailers != null && mTrailers.youtube.size() > 0;
            MenuItem youtubeItem = menu.findItem(R.id.menu_open_youtube);
            youtubeItem.setEnabled(isEnableYoutube);
            youtubeItem.setVisible(isEnableYoutube);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_movie_share) {
            ShareUtils.shareMovie(getActivity(), mTmdbId, mMovieDetails.tmdbMovie().title);
            fireTrackerEvent("Share");
            return true;
        }
        if (itemId == R.id.menu_open_imdb) {
            ServiceUtils.openImdb(mMovieDetails.traktMovie().imdb_id, TAG, getActivity());
            return true;
        }
        if (itemId == R.id.menu_open_youtube) {
            ServiceUtils.openYoutube(mTrailers.youtube.get(0).source, TAG,
                    getActivity());
            return true;
        }
        if (itemId == R.id.menu_open_google_play) {
            ServiceUtils.searchGooglePlay(mMovieDetails.tmdbMovie().title, TAG,
                    getActivity());
            return true;
        }
        if (itemId == R.id.menu_open_tmdb) {
            TmdbTools.openTmdbMovie(getActivity(), mTmdbId, TAG);
        }
        if (itemId == R.id.menu_open_trakt) {
            ServiceUtils.openTraktMovie(getActivity(), mTmdbId, TAG);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void populateMovieViews() {
        /**
         * Take title, overview and poster from TMDb as they are localized.
         * Everything else from trakt.
         */
        Movie traktMovie = mMovieDetails.traktMovie();
        com.uwetrottmann.tmdb.entities.Movie tmdbMovie = mMovieDetails.tmdbMovie();

        mMovieTitle.setText(tmdbMovie.title);
        mMovieDescription.setText(tmdbMovie.overview);

        // release date and runtime: "July 17, 2009 | 95 min"
        StringBuilder releaseAndRuntime = new StringBuilder();
        if (traktMovie.released != null && traktMovie.released.getTime() != 0) {
            releaseAndRuntime.append(DateUtils.formatDateTime(getActivity(),
                    traktMovie.released.getTime(), DateUtils.FORMAT_SHOW_DATE));
            releaseAndRuntime.append(" | ");
        }
        releaseAndRuntime.append(getString(R.string.runtime_minutes, tmdbMovie.runtime));
        mMovieReleaseDate.setText(releaseAndRuntime.toString());

        // check-in button
        final String title = tmdbMovie.title;
        // fall back to local title for tvtag check-in if we currently don't have the original one
        final String originalTitle = TextUtils.isEmpty(tmdbMovie.original_title)
                ? title : tmdbMovie.original_title;
        mCheckinButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // display a check-in dialog
                MovieCheckInDialogFragment f = MovieCheckInDialogFragment
                        .newInstance(mTmdbId, title, originalTitle);
                f.show(getFragmentManager(), "checkin-dialog");
                fireTrackerEvent("Check-In");
            }
        });
        CheatSheet.setup(mCheckinButton);

        // watched button (only supported when connected to trakt)
        if (TraktCredentials.get(getActivity()).hasCredentials()) {
            final boolean isWatched = traktMovie.watched != null && traktMovie.watched;
            Utils.setCompoundDrawablesRelativeWithIntrinsicBounds(mWatchedButton, 0,
                    isWatched ? Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                            R.attr.drawableWatched)
                            : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                                    R.attr.drawableWatch), 0, 0);
            mWatchedButton.setText(isWatched ? R.string.action_unwatched : R.string.action_watched);
            CheatSheet.setup(mWatchedButton,
                    isWatched ? R.string.action_unwatched : R.string.action_watched);
            mWatchedButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    // disable button, will be re-enabled on data reload once action completes
                    v.setEnabled(false);
                    if (isWatched) {
                        MovieTools.unwatchedMovie(getActivity(), mTmdbId);
                        fireTrackerEvent("Unwatched movie");
                    } else {
                        MovieTools.watchedMovie(getActivity(), mTmdbId);
                        fireTrackerEvent("Watched movie");
                    }
                }
            });
            mWatchedButton.setEnabled(true);
        } else {
            mWatchedButton.setVisibility(View.GONE);
        }

        // collected button
        final boolean isInCollection = traktMovie.inCollection != null && traktMovie.inCollection;
        Utils.setCompoundDrawablesRelativeWithIntrinsicBounds(mCollectedButton, 0,
                isInCollection
                        ? R.drawable.ic_collected
                        : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                                R.attr.drawableCollect), 0, 0);
        mCollectedButton.setText(isInCollection ? R.string.action_collection_remove
                : R.string.action_collection_add);
        CheatSheet.setup(mCollectedButton, isInCollection ? R.string.action_collection_remove
                : R.string.action_collection_add);
        mCollectedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable button, will be re-enabled on data reload once action completes
                v.setEnabled(false);
                if (isInCollection) {
                    MovieTools.removeFromCollection(getActivity(), mTmdbId);
                    fireTrackerEvent("Uncollected movie");
                } else {
                    MovieTools.addToCollection(getActivity(), mTmdbId);
                    fireTrackerEvent("Collected movie");
                }
            }
        });
        mCollectedButton.setEnabled(true);

        // watchlist button
        final boolean isInWatchlist = traktMovie.inWatchlist != null && traktMovie.inWatchlist;
        Utils.setCompoundDrawablesRelativeWithIntrinsicBounds(mWatchlistedButton, 0,
                isInWatchlist
                        ? R.drawable.ic_listed
                        : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                                R.attr.drawableList), 0, 0);
        mWatchlistedButton.setText(
                isInWatchlist ? R.string.watchlist_remove : R.string.watchlist_add);
        CheatSheet.setup(mWatchlistedButton,
                isInWatchlist ? R.string.watchlist_remove : R.string.watchlist_add);
        mWatchlistedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable button, will be re-enabled on data reload once action completes
                v.setEnabled(false);
                if (isInWatchlist) {
                    MovieTools.removeFromWatchlist(getActivity(), mTmdbId);
                    fireTrackerEvent("Unwatchlist movie");
                } else {
                    MovieTools.addToWatchlist(getActivity(), mTmdbId);
                    fireTrackerEvent("Watchlist movie");
                }
            }
        });
        mWatchlistedButton.setEnabled(true);

        // show button bar
        mButtonContainer.setVisibility(View.VISIBLE);

        // ratings
        mRatingsTmdbValue.setText(TmdbTools.buildRatingValue(tmdbMovie.vote_average));
        mRatingsTraktUserValue.setText(
                TraktTools.buildUserRatingString(getActivity(), traktMovie.rating_advanced));
        if (traktMovie.ratings != null) {
            mRatingsTraktVotes.setText(
                    TraktTools.buildRatingVotesString(getActivity(), traktMovie.ratings.votes));
            mRatingsTraktValue.setText(
                    TraktTools.buildRatingPercentageString(traktMovie.ratings.percentage));
        }
        mRatingsContainer.setVisibility(View.VISIBLE);

        // genres
        Utils.setValueOrPlaceholder(mMovieGenres, TmdbTools.buildGenresString(tmdbMovie.genres));

        // trakt comments link
        mCommentsButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), TraktShoutsActivity.class);
                i.putExtras(TraktShoutsActivity.createInitBundleMovie(title, mTmdbId));
                ActivityCompat.startActivity(getActivity(), i,
                        ActivityOptionsCompat
                                .makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight())
                                .toBundle()
                );
                fireTrackerEvent("Comments");
            }
        });

        // load poster, cache on external storage
        if (!TextUtils.isEmpty(tmdbMovie.poster_path)) {
            ServiceUtils.getPicasso(getActivity())
                    .load(mImageBaseUrl + tmdbMovie.poster_path).into(mMoviePosterBackground);
        }
    }

    private void populateMovieCreditsViews(final Credits credits) {
        if (credits == null) {
            mCastView.setVisibility(View.GONE);
            mCrewView.setVisibility(View.GONE);
            return;
        }

        // cast members
        if (credits.cast == null || credits.cast.size() == 0) {
            mCastView.setVisibility(View.GONE);
        } else {
            mCastView.setVisibility(View.VISIBLE);
            PeopleListHelper.populateMovieCast(getActivity(), getActivity().getLayoutInflater(),
                    mCastContainer, credits);
        }

        // crew members
        if (credits.crew == null || credits.crew.size() == 0) {
            mCrewView.setVisibility(View.GONE);
        } else {
            mCrewView.setVisibility(View.VISIBLE);
            PeopleListHelper.populateMovieCrew(getActivity(), getActivity().getLayoutInflater(),
                    mCrewContainer, credits);
        }
    }

    public void onEvent(MovieTools.MovieChangedEvent event) {
        if (event.movieTmdbId != mTmdbId) {
            return;
        }
        // re-query some movie details to update button states
        restartMovieLoader();
    }

    public void onEvent(TraktTask.TraktActionCompleteEvent event) {
        if (event.mWasSuccessful &&
                (event.mTraktAction == TraktAction.WATCHED_MOVIE
                        || event.mTraktAction == TraktAction.UNWATCHED_MOVIE
                        || event.mTraktAction == TraktAction.RATE_MOVIE)) {
            restartMovieLoader();
        }
    }

    private void rateOnTrakt() {
        TraktTools.rateMovie(getActivity(), getFragmentManager(), mTmdbId);
        fireTrackerEvent("Rate (trakt)");
    }

    private void restartMovieLoader() {
        Bundle args = new Bundle();
        args.putInt(InitBundle.TMDB_ID, mTmdbId);
        getLoaderManager().restartLoader(MovieDetailsActivity.LOADER_ID_MOVIE, args,
                mMovieLoaderCallbacks);
    }

    private void fireTrackerEvent(String label) {
        Utils.trackAction(getActivity(), TAG, label);
    }

    private LoaderManager.LoaderCallbacks<MovieDetails> mMovieLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<MovieDetails>() {
        @Override
        public Loader<MovieDetails> onCreateLoader(int loaderId, Bundle args) {
            return new MovieLoader(getActivity(), args.getInt(InitBundle.TMDB_ID));
        }

        @Override
        public void onLoadFinished(Loader<MovieDetails> movieLoader, MovieDetails movieDetails) {
            mMovieDetails = movieDetails;
            mProgressBar.setVisibility(View.GONE);

            if (movieDetails.traktMovie() != null && movieDetails.tmdbMovie() != null) {
                populateMovieViews();
                getActivity().invalidateOptionsMenu();
            } else {
                // display offline message
                mMovieDescription.setText(R.string.offline);
            }
        }

        @Override
        public void onLoaderReset(Loader<MovieDetails> movieLoader) {
            // nothing to do
        }
    };

    private LoaderManager.LoaderCallbacks<Trailers> mMovieTrailerLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Trailers>() {
        @Override
        public Loader<Trailers> onCreateLoader(int loaderId, Bundle args) {
            return new MovieTrailersLoader(getActivity(), args.getInt(InitBundle.TMDB_ID));
        }

        @Override
        public void onLoadFinished(Loader<Trailers> trailersLoader, Trailers trailers) {
            if (trailers != null) {
                mTrailers = trailers;
                getActivity().invalidateOptionsMenu();
            }
        }

        @Override
        public void onLoaderReset(Loader<Trailers> trailersLoader) {
            // do nothing
        }
    };

    private LoaderManager.LoaderCallbacks<Credits> mMovieCreditsLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Credits>() {
        @Override
        public Loader<Credits> onCreateLoader(int loaderId, Bundle args) {
            return new MovieCreditsLoader(getActivity(), args.getInt(InitBundle.TMDB_ID));
        }

        @Override
        public void onLoadFinished(Loader<Credits> creditsLoader, Credits credits) {
            if (isAdded()) {
                populateMovieCreditsViews(credits);
            }
        }

        @Override
        public void onLoaderReset(Loader<Credits> creditsLoader) {
            // do nothing
        }
    };
}
