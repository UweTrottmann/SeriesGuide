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

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
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
import com.battlelancer.seriesguide.backend.HexagonTools;
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
import com.battlelancer.seriesguide.util.TimeTools;
import com.battlelancer.seriesguide.util.TmdbTools;
import com.battlelancer.seriesguide.util.TraktTools;
import com.battlelancer.seriesguide.util.Utils;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.tmdb2.entities.Credits;
import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.tmdb2.entities.Videos;
import com.uwetrottmann.trakt5.entities.Ratings;
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

    @BindView(R.id.contentContainerMovie) ViewGroup mContentContainer;
    @Nullable @BindView(R.id.contentContainerMovieRight) ViewGroup mContentContainerRight;

    @BindView(R.id.textViewMovieTitle) TextView mMovieTitle;
    @BindView(R.id.textViewMovieDate) TextView mMovieReleaseDate;
    @BindView(R.id.textViewMovieDescription) TextView mMovieDescription;
    @BindView(R.id.imageViewMoviePoster) ImageView mMoviePosterBackground;
    @BindView(R.id.textViewMovieGenres) TextView mMovieGenres;

    @BindView(R.id.containerMovieButtons) View mButtonContainer;
    @BindView(R.id.buttonMovieCheckIn) Button mCheckinButton;
    @BindView(R.id.buttonMovieWatched) Button mWatchedButton;
    @BindView(R.id.buttonMovieCollected) Button mCollectedButton;
    @BindView(R.id.buttonMovieWatchlisted) Button mWatchlistedButton;

    @BindView(R.id.containerRatings) View mRatingsContainer;
    @BindView(R.id.textViewRatingsTmdbValue) TextView mRatingsTmdbValue;
    @BindView(R.id.textViewRatingsTmdbVotes) TextView mRatingsTmdbVotes;
    @BindView(R.id.textViewRatingsTraktValue) TextView mRatingsTraktValue;
    @BindView(R.id.textViewRatingsTraktVotes) TextView mRatingsTraktVotes;
    @BindView(R.id.textViewRatingsTraktUser) TextView mRatingsTraktUserValue;
    @BindView(R.id.textViewRatingsTraktUserLabel) View mRatingsTraktUserLabel;

    @BindView(R.id.containerMovieCast) View mCastView;
    TextView mCastLabel;
    LinearLayout mCastContainer;

    @BindView(R.id.containerMovieCrew) View mCrewView;
    TextView mCrewLabel;
    LinearLayout mCrewContainer;

    @BindView(R.id.buttonMovieComments) Button mCommentsButton;
    @BindView(R.id.progressBar) View mProgressBar;

    private int tmdbId;
    private MovieDetails movieDetails = new MovieDetails();
    private Videos.Video trailer;
    private String imageBaseUrl;
    private Unbinder unbinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_movie, container, false);
        unbinder = ButterKnife.bind(this, v);

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mMoviePosterBackground.setImageAlpha(30);
        } else {
            //noinspection deprecation
            mMoviePosterBackground.setAlpha(30);
        }

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        tmdbId = getArguments().getInt(InitBundle.TMDB_ID);
        if (tmdbId <= 0) {
            getFragmentManager().popBackStack();
            return;
        }

        setupViews();

        imageBaseUrl = TmdbSettings.getImageBaseUrl(getActivity())
                + TmdbSettings.POSTER_SIZE_SPEC_W342;

        Bundle args = new Bundle();
        args.putInt(InitBundle.TMDB_ID, tmdbId);
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

        unbinder.unbind();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        if (movieDetails != null) {
            // choose theme variant
            boolean isLightTheme = SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_Light;
            inflater.inflate(
                    isLightTheme ? R.menu.movie_details_menu_light : R.menu.movie_details_menu,
                    menu);

            // enable/disable actions
            boolean isEnableShare = movieDetails.tmdbMovie() != null && !TextUtils.isEmpty(
                    movieDetails.tmdbMovie().title);
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

            boolean isEnableImdb = movieDetails.tmdbMovie() != null
                    && !TextUtils.isEmpty(movieDetails.tmdbMovie().imdb_id);
            MenuItem imdbItem = menu.findItem(R.id.menu_open_imdb);
            imdbItem.setEnabled(isEnableImdb);
            imdbItem.setVisible(isEnableImdb);

            boolean isEnableYoutube = trailer != null;
            MenuItem youtubeItem = menu.findItem(R.id.menu_open_youtube);
            youtubeItem.setEnabled(isEnableYoutube);
            youtubeItem.setVisible(isEnableYoutube);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_movie_share) {
            ShareUtils.shareMovie(getActivity(), tmdbId, movieDetails.tmdbMovie().title);
            Utils.trackAction(getActivity(), TAG, "Share");
            return true;
        }
        if (itemId == R.id.menu_open_imdb) {
            ServiceUtils.openImdb(movieDetails.tmdbMovie().imdb_id, TAG, getActivity());
            return true;
        }
        if (itemId == R.id.menu_open_youtube) {
            ServiceUtils.openYoutube(trailer.key, TAG, getActivity());
            return true;
        }
        if (itemId == R.id.menu_open_google_play) {
            ServiceUtils.searchGooglePlay(movieDetails.tmdbMovie().title, TAG,
                    getActivity());
            return true;
        }
        if (itemId == R.id.menu_open_tmdb) {
            TmdbTools.openTmdbMovie(getActivity(), tmdbId, TAG);
        }
        if (itemId == R.id.menu_open_trakt) {
            Utils.launchWebsite(getActivity(), TraktTools.buildMovieUrl(tmdbId), TAG, "trakt");
            return true;
        }
        if (itemId == R.id.menu_action_movie_websearch) {
            ServiceUtils.performWebSearch(getActivity(), movieDetails.tmdbMovie().title, TAG);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void populateMovieViews() {
        /**
         * Get everything from TMDb. Also get additional rating from trakt.
         */
        final Ratings traktRatings = movieDetails.traktRatings();
        final Movie tmdbMovie = movieDetails.tmdbMovie();
        final boolean inCollection = movieDetails.inCollection;
        final boolean inWatchlist = movieDetails.inWatchlist;
        final boolean isWatched = movieDetails.isWatched;
        final int rating = movieDetails.userRating;

        mMovieTitle.setText(tmdbMovie.title);
        getActivity().setTitle(tmdbMovie.title);
        mMovieDescription.setText(tmdbMovie.overview);

        // release date and runtime: "July 17, 2009 | 95 min"
        StringBuilder releaseAndRuntime = new StringBuilder();
        if (tmdbMovie.release_date != null) {
            releaseAndRuntime.append(
                    TimeTools.formatToLocalDate(getContext(), tmdbMovie.release_date));
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
                        .newInstance(tmdbId, title);
                f.show(getFragmentManager(), "checkin-dialog");
                Utils.trackAction(getActivity(), TAG, "Check-In");
            }
        });
        CheatSheet.setup(mCheckinButton);

        // prevent checking in if hexagon is enabled
        mCheckinButton.setVisibility(
                HexagonTools.isSignedIn(getActivity()) ? View.GONE : View.VISIBLE);

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
                        MovieTools.unwatchedMovie(getActivity(), tmdbId);
                        Utils.trackAction(getActivity(), TAG, "Unwatched movie");
                    } else {
                        MovieTools.watchedMovie(getActivity(), tmdbId);
                        Utils.trackAction(getActivity(), TAG, "Watched movie");
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
                    MovieTools.removeFromCollection(getActivity(), tmdbId);
                    Utils.trackAction(getActivity(), TAG, "Uncollected movie");
                } else {
                    MovieTools.addToCollection(getActivity(), tmdbId);
                    Utils.trackAction(getActivity(), TAG, "Collected movie");
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
                    MovieTools.removeFromWatchlist(getActivity(), tmdbId);
                    Utils.trackAction(getActivity(), TAG, "Unwatchlist movie");
                } else {
                    MovieTools.addToWatchlist(getActivity(), tmdbId);
                    Utils.trackAction(getActivity(), TAG, "Watchlist movie");
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
                i.putExtras(TraktCommentsActivity.createInitBundleMovie(title, tmdbId));
                Utils.startActivityWithAnimation(getActivity(), i, v);
                Utils.trackAction(v.getContext(), TAG, "Comments");
            }
        });

        // load poster, cache on external storage
        if (!TextUtils.isEmpty(tmdbMovie.poster_path)) {
            ServiceUtils.loadWithPicasso(getActivity(), imageBaseUrl + tmdbMovie.poster_path)
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
            PeopleListHelper.populateMovieCast(getActivity(), mCastContainer, credits, TAG);
        }

        // crew members
        if (credits.crew == null || credits.crew.size() == 0) {
            mCrewView.setVisibility(View.GONE);
        } else {
            mCrewView.setVisibility(View.VISIBLE);
            PeopleListHelper.populateMovieCrew(getActivity(), mCrewContainer, credits, TAG);
        }
    }

    public void onEvent(MovieTools.MovieChangedEvent event) {
        if (event.movieTmdbId != tmdbId) {
            return;
        }
        // re-query some movie details to update button states
        restartMovieLoader();
    }

    private void rateMovie() {
        if (TraktCredentials.ensureCredentials(getActivity())) {
            RateDialogFragment newFragment = RateDialogFragment.newInstanceMovie(tmdbId);
            newFragment.show(getFragmentManager(), "ratedialog");
            Utils.trackAction(getActivity(), TAG, "Rate (trakt)");
        }
    }

    private void restartMovieLoader() {
        Bundle args = new Bundle();
        args.putInt(InitBundle.TMDB_ID, tmdbId);
        getLoaderManager().restartLoader(MovieDetailsActivity.LOADER_ID_MOVIE, args,
                mMovieLoaderCallbacks);
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
            MovieDetailsFragment.this.movieDetails = movieDetails;
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
                MovieDetailsFragment.this.trailer = trailer;
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
