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
import android.support.annotation.Nullable;
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
import butterknife.Bind;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.items.MovieDetails;
import com.battlelancer.seriesguide.loaders.MovieCreditsLoader;
import com.battlelancer.seriesguide.loaders.MovieLoader;
import com.battlelancer.seriesguide.loaders.MovieTrailersLoader;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.dialogs.MovieCheckInDialogFragment;
import com.battlelancer.seriesguide.ui.dialogs.RateDialogFragment;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.PeopleListHelper;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.TmdbTools;
import com.battlelancer.seriesguide.util.TraktTools;
import com.battlelancer.seriesguide.util.Utils;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.tmdb.entities.Credits;
import com.uwetrottmann.tmdb.entities.Videos;
import com.uwetrottmann.trakt.v2.entities.Ratings;
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

    private Videos.Video mTrailer;

    private String mImageBaseUrl;

    @Bind(R.id.contentContainerMovie) ViewGroup mContentContainer;
    @Nullable @Bind(R.id.contentContainerMovieRight) ViewGroup mContentContainerRight;

    @Bind(R.id.textViewMovieTitle) TextView mMovieTitle;
    @Bind(R.id.textViewMovieDate) TextView mMovieReleaseDate;
    @Bind(R.id.textViewMovieDescription) TextView mMovieDescription;
    @Bind(R.id.imageViewMoviePoster) ImageView mMoviePosterBackground;
    @Bind(R.id.textViewMovieGenres) TextView mMovieGenres;

    @Bind(R.id.containerMovieButtons) View mButtonContainer;
    @Bind(R.id.buttonMovieCheckIn) Button mCheckinButton;
    @Bind(R.id.buttonMovieWatched) Button mWatchedButton;
    @Bind(R.id.buttonMovieCollected) Button mCollectedButton;
    @Bind(R.id.buttonMovieWatchlisted) Button mWatchlistedButton;

    @Bind(R.id.containerRatings) View mRatingsContainer;
    @Bind(R.id.textViewRatingsTmdbValue) TextView mRatingsTmdbValue;
    @Bind(R.id.textViewRatingsTmdbVotes) TextView mRatingsTmdbVotes;
    @Bind(R.id.textViewRatingsTraktValue) TextView mRatingsTraktValue;
    @Bind(R.id.textViewRatingsTraktVotes) TextView mRatingsTraktVotes;
    @Bind(R.id.textViewRatingsTraktUser) TextView mRatingsTraktUserValue;
    @Bind(R.id.textViewRatingsTraktUserLabel) View mRatingsTraktUserLabel;

    @Bind(R.id.containerMovieCast) View mCastView;
    TextView mCastLabel;
    LinearLayout mCastContainer;

    @Bind(R.id.containerMovieCrew) View mCrewView;
    TextView mCrewLabel;
    LinearLayout mCrewContainer;

    @Bind(R.id.buttonMovieComments) Button mCommentsButton;
    @Bind(R.id.progressBar) View mProgressBar;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_movie, container, false);
        ButterKnife.bind(this, v);

        mProgressBar.setVisibility(View.VISIBLE);

        // important action buttons
        mButtonContainer.setVisibility(View.GONE);
        mRatingsContainer.setVisibility(View.GONE);

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

        ButterKnife.unbind(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (mMovieDetails != null) {
            // choose theme variant
            boolean isLightTheme = SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_Light;
            inflater.inflate(
                    isLightTheme ? R.menu.movie_details_menu_light : R.menu.movie_details_menu,
                    menu);

            // enable/disable actions
            boolean isEnableShare = mMovieDetails.tmdbMovie() != null && !TextUtils.isEmpty(
                    mMovieDetails.tmdbMovie().title);
            MenuItem shareItem = menu.findItem(R.id.menu_movie_share);
            shareItem.setEnabled(isEnableShare);
            shareItem.setVisible(isEnableShare);
            MenuItem webSearchItem = menu.findItem(R.id.menu_action_movie_websearch);
            webSearchItem.setEnabled(isEnableShare);
            webSearchItem.setVisible(isEnableShare);

            MenuItem playStoreItem = menu.findItem(R.id.menu_open_google_play);
            if (Utils.isAmazonVersion()) {
                // hide Google Play button in Amazon version
                playStoreItem.setEnabled(false);
                playStoreItem.setVisible(false);
            } else {
                playStoreItem.setEnabled(isEnableShare);
                playStoreItem.setVisible(isEnableShare);
            }

            boolean isEnableImdb = mMovieDetails.tmdbMovie() != null
                    && !TextUtils.isEmpty(mMovieDetails.tmdbMovie().imdb_id);
            MenuItem imdbItem = menu.findItem(R.id.menu_open_imdb);
            imdbItem.setEnabled(isEnableImdb);
            imdbItem.setVisible(isEnableImdb);

            boolean isEnableYoutube = mTrailer != null;
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
            ServiceUtils.openImdb(mMovieDetails.tmdbMovie().imdb_id, TAG, getActivity());
            return true;
        }
        if (itemId == R.id.menu_open_youtube) {
            ServiceUtils.openYoutube(mTrailer.key, TAG, getActivity());
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
        if (itemId == R.id.menu_action_movie_websearch) {
            ServiceUtils.performWebSearch(getActivity(), mMovieDetails.tmdbMovie().title, TAG);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void populateMovieViews() {
        /**
         * Ensure to take title, overview and poster from TMDb as they are localized.
         * Get release time from trakt.
         */
        final Ratings traktRatings = mMovieDetails.traktRatings();
        final com.uwetrottmann.tmdb.entities.Movie tmdbMovie = mMovieDetails.tmdbMovie();
        final boolean inCollection = mMovieDetails.inCollection;
        final boolean inWatchlist = mMovieDetails.inWatchlist;
        final boolean isWatched = mMovieDetails.isWatched;
        final int rating = mMovieDetails.userRating;

        mMovieTitle.setText(tmdbMovie.title);
        mMovieDescription.setText(tmdbMovie.overview);

        // release date and runtime: "July 17, 2009 | 95 min"
        StringBuilder releaseAndRuntime = new StringBuilder();
        if (tmdbMovie.release_date != null) {
            releaseAndRuntime.append(DateUtils.formatDateTime(getActivity(),
                    tmdbMovie.release_date.getTime(), DateUtils.FORMAT_SHOW_DATE));
            releaseAndRuntime.append(" | ");
        }
        releaseAndRuntime.append(getString(R.string.runtime_minutes, tmdbMovie.runtime));
        mMovieReleaseDate.setText(releaseAndRuntime.toString());

        // check-in button
        final String title = tmdbMovie.title;
        mCheckinButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // display a check-in dialog
                MovieCheckInDialogFragment f = MovieCheckInDialogFragment
                        .newInstance(mTmdbId, title);
                f.show(getFragmentManager(), "checkin-dialog");
                fireTrackerEvent("Check-In");
            }
        });
        CheatSheet.setup(mCheckinButton);

        // watched button (only supported when connected to trakt)
        if (TraktCredentials.get(getActivity()).hasCredentials()) {
            mWatchedButton.setText(isWatched ? R.string.action_unwatched : R.string.action_watched);
            CheatSheet.setup(mWatchedButton,
                    isWatched ? R.string.action_unwatched : R.string.action_watched);
            Utils.setCompoundDrawablesRelativeWithIntrinsicBounds(mWatchedButton, 0, isWatched
                    ? Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                    R.attr.drawableWatched)
                    : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                            R.attr.drawableWatch), 0, 0);
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
            mWatchedButton.setVisibility(View.VISIBLE);
        } else {
            mWatchedButton.setVisibility(View.GONE);
        }

        // collected button
        Utils.setCompoundDrawablesRelativeWithIntrinsicBounds(mCollectedButton, 0,
                inCollection
                        ? R.drawable.ic_collected
                        : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                                R.attr.drawableCollect), 0, 0);
        mCollectedButton.setText(inCollection ? R.string.action_collection_remove
                : R.string.action_collection_add);
        CheatSheet.setup(mCollectedButton, inCollection ? R.string.action_collection_remove
                : R.string.action_collection_add);
        mCollectedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable button, will be re-enabled on data reload once action completes
                v.setEnabled(false);
                if (inCollection) {
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
        Utils.setCompoundDrawablesRelativeWithIntrinsicBounds(mWatchlistedButton, 0,
                inWatchlist
                        ? R.drawable.ic_listed
                        : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                                R.attr.drawableList), 0, 0);
        mWatchlistedButton.setText(
                inWatchlist ? R.string.watchlist_remove : R.string.watchlist_add);
        CheatSheet.setup(mWatchlistedButton,
                inWatchlist ? R.string.watchlist_remove : R.string.watchlist_add);
        mWatchlistedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // disable button, will be re-enabled on data reload once action completes
                v.setEnabled(false);
                if (inWatchlist) {
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
        mRatingsTmdbValue.setText(TraktTools.buildRatingString(tmdbMovie.vote_average));
        mRatingsTmdbVotes.setText(
                TraktTools.buildRatingVotesString(getActivity(), tmdbMovie.vote_count));
        if (traktRatings != null) {
            mRatingsTraktVotes.setText(
                    TraktTools.buildRatingVotesString(getActivity(), traktRatings.votes));
            mRatingsTraktValue.setText(
                    TraktTools.buildRatingString(traktRatings.rating));
        }
        // if movie is not in database, can't handle user ratings
        if (!inCollection && !inWatchlist && !isWatched) {
            mRatingsTraktUserLabel.setVisibility(View.GONE);
            mRatingsTraktUserValue.setVisibility(View.GONE);
            mRatingsContainer.setClickable(false);
            mRatingsContainer.setLongClickable(false); // cheat sheet
        } else {
            mRatingsTraktUserLabel.setVisibility(View.VISIBLE);
            mRatingsTraktUserValue.setVisibility(View.VISIBLE);
            mRatingsTraktUserValue.setText(TraktTools.buildUserRatingString(getActivity(), rating));
            mRatingsContainer.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    rateMovie();
                }
            });
            CheatSheet.setup(mRatingsContainer, R.string.action_rate);
        }
        mRatingsContainer.setVisibility(View.VISIBLE);

        // genres
        Utils.setValueOrPlaceholder(mMovieGenres, TmdbTools.buildGenresString(tmdbMovie.genres));

        // trakt comments link
        mCommentsButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), TraktCommentsActivity.class);
                i.putExtras(TraktCommentsActivity.createInitBundleMovie(title, mTmdbId));
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
            ServiceUtils.loadWithPicasso(getActivity(), mImageBaseUrl + tmdbMovie.poster_path)
                    .into(mMoviePosterBackground);
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

    private void rateMovie() {
        if (TraktCredentials.ensureCredentials(getActivity())) {
            RateDialogFragment newFragment = RateDialogFragment.newInstanceMovie(mTmdbId);
            newFragment.show(getFragmentManager(), "ratedialog");
            fireTrackerEvent("Rate (trakt)");
        }
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
            if (!isAdded()) {
                return;
            }
            mMovieDetails = movieDetails;
            mProgressBar.setVisibility(View.GONE);

            // we need at least values from database or tmdb
            if (movieDetails.tmdbMovie() != null) {
                populateMovieViews();
                getActivity().invalidateOptionsMenu();
            } else {
                // if there is no local data and loading from network failed
                mMovieDescription.setText(R.string.offline);
            }
        }

        @Override
        public void onLoaderReset(Loader<MovieDetails> movieLoader) {
            // nothing to do
        }
    };

    private LoaderManager.LoaderCallbacks<Videos.Video> mMovieTrailerLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Videos.Video>() {
        @Override
        public Loader<Videos.Video> onCreateLoader(int loaderId, Bundle args) {
            return new MovieTrailersLoader(getActivity(), args.getInt(InitBundle.TMDB_ID));
        }

        @Override
        public void onLoadFinished(Loader<Videos.Video> trailersLoader, Videos.Video trailer) {
            if (!isAdded()) {
                return;
            }
            if (trailer != null) {
                mTrailer = trailer;
                getActivity().invalidateOptionsMenu();
            }
        }

        @Override
        public void onLoaderReset(Loader<Videos.Video> trailersLoader) {
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
            if (!isAdded()) {
                return;
            }
            populateMovieCreditsViews(credits);
        }

        @Override
        public void onLoaderReset(Loader<Credits> creditsLoader) {
            // do nothing
        }
    };
}
