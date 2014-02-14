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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.loaders.MovieCreditsLoader;
import com.battlelancer.seriesguide.loaders.MovieLoader;
import com.battlelancer.seriesguide.loaders.MovieTrailersLoader;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.dialogs.MovieCheckInDialogFragment;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.TmdbTools;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.TraktTools;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.entities.Movie;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.squareup.picasso.Picasso;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.tmdb.entities.Credits;
import com.uwetrottmann.tmdb.entities.Trailers;
import de.greenrobot.event.EventBus;
import java.text.DecimalFormat;

/**
 * Displays details about one movie including plot, ratings, trailers and a poster.
 */
public class MovieDetailsFragment extends SherlockFragment {

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

    private Credits mCredits;

    private Trailers mTrailers;

    private String mImageBaseUrl;

    @InjectView(R.id.textViewMovieTitle) TextView mMovieTitle;

    @InjectView(R.id.textViewMovieDate) TextView mMovieReleaseDate;

    @InjectView(R.id.textViewMovieDescription) TextView mMovieDescription;

    @InjectView(R.id.imageViewMoviePoster) ImageView mMoviePosterBackground;

    @InjectView(R.id.containerMovieButtons) View mButtonContainer;

    @InjectView(R.id.buttonMovieCheckIn) ImageButton mCheckinButton;

    @InjectView(R.id.buttonMovieWatched) ImageButton mWatchedButton;

    @InjectView(R.id.buttonMovieCollected) ImageButton mCollectedButton;

    @InjectView(R.id.buttonMovieWatchlisted) ImageButton mWatchlistedButton;

    @InjectView(R.id.ratingbar) View mRatingsContainer;

    @InjectView(R.id.textViewRatingsTvdbLabel) TextView mRatingsTmdbLabel;

    @InjectView(R.id.textViewRatingsTvdbValue) TextView mRatingsTmdbValue;

    @InjectView(R.id.textViewRatingsTraktValue) TextView mRatingsTraktValue;

    @InjectView(R.id.textViewRatingsTraktVotes) TextView mRatingsTraktVotes;

    @InjectView(R.id.textViewRatingsTraktUser) TextView mRatingsTraktUserValue;

    @InjectView(R.id.textViewMovieCast) TextView mMovieCast;

    @InjectView(R.id.textViewMovieCrew) TextView mMovieCrew;

    @InjectView(R.id.buttonMovieComments) Button mCommentsButton;

    @InjectView(R.id.progressBar) View mProgressBar;

    @InjectView(R.id.dividerHorizontalMovieDetails) View mDivider;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.movie_details_fragment, container, false);
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
        mRatingsTmdbLabel.setText(R.string.tmdb);

        // comments button
        mDivider.setVisibility(View.GONE);
        mCommentsButton.setVisibility(View.GONE);

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
            getSherlockActivity().getSupportFragmentManager().popBackStack();
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
        // fix padding for translucent system bars
        if (AndroidUtils.isKitKatOrHigher()) {
            SystemBarTintManager.SystemBarConfig config
                    = ((MovieDetailsActivity) getActivity()).getSystemBarTintManager().getConfig();
            ViewGroup contentContainer = (ViewGroup) getView().findViewById(
                    R.id.contentContainerMovie);
            contentContainer.setClipToPadding(false);
            contentContainer.setPadding(0, 0, config.getPixelInsetRight(),
                    config.getPixelInsetBottom());
            ViewGroup.MarginLayoutParams layoutParams
                    = (ViewGroup.MarginLayoutParams) contentContainer.getLayoutParams();
            layoutParams.setMargins(0, config.getPixelInsetTop(true), 0, 0);
            contentContainer.setLayoutParams(layoutParams);
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
            boolean isLightTheme = SeriesGuidePreferences.THEME == R.style.SeriesGuideThemeLight;
            inflater.inflate(
                    isLightTheme ? R.menu.movie_details_menu_light : R.menu.movie_details_menu,
                    menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {

        if (mMovieDetails != null) {
            // If the nav drawer is open, hide action items related to the
            // content view
            boolean isDrawerOpen = ((BaseNavDrawerActivity) getActivity()).isDrawerOpen();

            boolean isEnableShare = mMovieDetails.tmdbMovie() != null && !TextUtils.isEmpty(
                    mMovieDetails.tmdbMovie().title);
            MenuItem shareItem = menu.findItem(R.id.menu_movie_share);
            shareItem.setEnabled(isEnableShare);
            shareItem.setVisible(isEnableShare && !isDrawerOpen);

            MenuItem playStoreItem = menu.findItem(R.id.menu_open_google_play);
            playStoreItem.setEnabled(isEnableShare);
            playStoreItem.setVisible(isEnableShare);

            boolean isEnableImdb = mMovieDetails.traktMovie() != null
                    && !TextUtils.isEmpty(mMovieDetails.traktMovie().imdb_id);
            MenuItem imdbItem = menu.findItem(R.id.menu_open_imdb);
            imdbItem.setEnabled(isEnableImdb);
            imdbItem.setVisible(isEnableImdb);

            boolean isEnableYoutube = mTrailers != null && mTrailers.youtube.size() > 0;
            MenuItem youtubeItem = menu.findItem(R.id.menu_open_youtube);
            youtubeItem.setEnabled(isEnableYoutube);
            youtubeItem.setVisible(isEnableYoutube && !isDrawerOpen);
        }

        super.onPrepareOptionsMenu(menu);
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

        // release date
        if (traktMovie.released != null && traktMovie.released.getTime() != 0) {
            mMovieReleaseDate.setText(
                    DateUtils.formatDateTime(getActivity(), traktMovie.released.getTime(),
                            DateUtils.FORMAT_SHOW_DATE));
        } else {
            mMovieReleaseDate.setText("");
        }

        // check-in button
        CheatSheet.setup(mCheckinButton);
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

        // watched button (only supported when connected to trakt)
        if (TraktCredentials.get(getActivity()).hasCredentials()) {
            final boolean isWatched = traktMovie.watched != null && traktMovie.watched;
            mWatchedButton.setImageResource(isWatched
                    ? R.drawable.ic_ticked
                    : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                            R.attr.drawableWatch));
            CheatSheet.setup(mWatchedButton,
                    isWatched ? R.string.action_unwatched : R.string.action_watched);
            mWatchedButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isWatched) {
                        MovieTools.unwatchedMovie(getActivity(), mTmdbId);
                    } else {
                        MovieTools.watchedMovie(getActivity(), mTmdbId);
                    }
                }
            });
        } else {
            mWatchedButton.setVisibility(View.GONE);
        }

        // collected button
        final boolean isInCollection = traktMovie.inCollection != null && traktMovie.inCollection;
        mCollectedButton.setImageResource(isInCollection
                ? R.drawable.ic_collected
                : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                        R.attr.drawableCollect));
        CheatSheet.setup(mCollectedButton, isInCollection ? R.string.action_collection_remove
                : R.string.action_collection_add);
        mCollectedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInCollection) {
                    MovieTools.removeFromCollection(getActivity(), mTmdbId);
                } else {
                    MovieTools.addToCollection(getActivity(), mTmdbId);
                }
            }
        });

        // watchlist button
        final boolean isInWatchlist = traktMovie.inWatchlist != null && traktMovie.inWatchlist;
        mWatchlistedButton.setImageResource(isInWatchlist
                ? R.drawable.ic_action_list_highlight
                : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                        R.attr.drawableList));
        CheatSheet.setup(mWatchlistedButton,
                isInWatchlist ? R.string.watchlist_remove : R.string.watchlist_add);
        mWatchlistedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInWatchlist) {
                    MovieTools.removeFromWatchlist(getActivity(), mTmdbId);
                } else {
                    MovieTools.addToWatchlist(getActivity(), mTmdbId);
                }
            }
        });

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

        // trakt comments link
        mDivider.setVisibility(View.VISIBLE);
        mCommentsButton.setVisibility(View.VISIBLE);
        mCommentsButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent i = new Intent(getActivity(), TraktShoutsActivity.class);
                i.putExtras(TraktShoutsActivity.createInitBundleMovie(title, mTmdbId));
                startActivity(i);
                fireTrackerEvent("Comments");
            }
        });

        // poster
        if (!TextUtils.isEmpty(tmdbMovie.poster_path)) {
            Picasso.with(getActivity()).load(mImageBaseUrl + tmdbMovie.poster_path)
                    .into(mMoviePosterBackground);
        }
    }

    private void populateMovieCreditsViews() {
        if (mCredits.cast != null) {
            StringBuilder castString = new StringBuilder();
            for (int i = 0; i < mCredits.cast.size(); i++) {
                Credits.CastMember castMember = mCredits.cast.get(i);
                castString.append(getString(R.string.movie_person_in_role, castMember.name,
                        castMember.character));
                if (i < mCredits.cast.size() - 1) {
                    castString.append("\n");
                }
            }
            mMovieCast.setText(castString.toString());
        }
        if (mCredits.crew != null) {
            StringBuilder crewString = new StringBuilder();
            for (int i = 0; i < mCredits.crew.size(); i++) {
                Credits.CrewMember crewMember = mCredits.crew.get(i);
                crewString.append(getString(R.string.movie_person_in_role, crewMember.name,
                        crewMember.job));
                if (i < mCredits.crew.size() - 1) {
                    crewString.append("\n");
                }
            }
            mMovieCrew.setText(crewString.toString());
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
                getSherlockActivity().supportInvalidateOptionsMenu();
            } else {
                // TODO display error message
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
                getSherlockActivity().supportInvalidateOptionsMenu();
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
            if (credits != null) {
                mCredits = credits;
                populateMovieCreditsViews();
            }
        }

        @Override
        public void onLoaderReset(Loader<Credits> creditsLoader) {
            // do nothing
        }
    };

    public static class MovieDetails {

        private Movie mTraktMovie;

        private com.uwetrottmann.tmdb.entities.Movie mTmdbMovie;

        public Movie traktMovie() {
            return mTraktMovie;
        }

        public MovieDetails traktMovie(Movie traktMovie) {
            mTraktMovie = traktMovie;
            return this;
        }

        public com.uwetrottmann.tmdb.entities.Movie tmdbMovie() {
            return mTmdbMovie;
        }

        public MovieDetails tmdbMovie(com.uwetrottmann.tmdb.entities.Movie movie) {
            mTmdbMovie = movie;
            return this;
        }
    }
}
