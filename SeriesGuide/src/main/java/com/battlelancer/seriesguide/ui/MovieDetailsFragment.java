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
import com.battlelancer.seriesguide.loaders.MovieLoader;
import com.battlelancer.seriesguide.loaders.MovieTrailersLoader;
import com.battlelancer.seriesguide.settings.TmdbSettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.ui.dialogs.MovieCheckInDialogFragment;
import com.battlelancer.seriesguide.util.ImageDownloader;
import com.battlelancer.seriesguide.util.MovieTools;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.entities.Movie;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import com.uwetrottmann.tmdb.entities.Credits;
import com.uwetrottmann.tmdb.entities.Trailers;

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

    private ImageDownloader mImageDownloader;

    private String mImageBaseUrl;

    @InjectView(R.id.textViewMovieTitle) TextView mMovieTitle;

    @InjectView(R.id.textViewMovieDate) TextView mMovieReleaseDate;

    @InjectView(R.id.textViewMovieDescription) TextView mMovieDescription;

    @InjectView(R.id.imageViewMoviePoster) ImageView mMoviePosterBackground;

    @InjectView(R.id.containerMovieButtons) View mButtonContainer;

    @InjectView(R.id.buttonMovieCheckIn) ImageButton mCheckinButton;

    @InjectView(R.id.buttonMovieWatched) ImageButton mWatchedButton;

    @InjectView(R.id.buttonMovieCollected) ImageButton mCollectedButton;

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

        mImageDownloader = ImageDownloader.getInstance(getActivity());
        mImageBaseUrl = TmdbSettings.getImageBaseUrl(getActivity())
                + TmdbSettings.POSTER_SIZE_SPEC_W342;

        Bundle args = new Bundle();
        args.putInt(InitBundle.TMDB_ID, mTmdbId);
        getLoaderManager().initLoader(MovieDetailsActivity.LOADER_ID_MOVIE, args,
                mMovieLoaderCallbacks);
        getLoaderManager().initLoader(MovieDetailsActivity.LOADER_ID_MOVIE_TRAILERS, args,
                mMovieTrailerLoaderCallbacks);

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

            boolean isEnableShare = mMovieDetails.traktOrLocalMovie() != null;
            MenuItem shareItem = menu.findItem(R.id.menu_movie_share);
            shareItem.setEnabled(isEnableShare);
            shareItem.setVisible(isEnableShare && !isDrawerOpen);

            MenuItem playStoreItem = menu.findItem(R.id.menu_open_google_play);
            playStoreItem.setEnabled(isEnableShare);
            playStoreItem.setVisible(isEnableShare);

            boolean isEnableImdb = mMovieDetails.traktOrLocalMovie() != null
                    && !TextUtils.isEmpty(mMovieDetails.traktOrLocalMovie().imdb_id);
            MenuItem imdbItem = menu.findItem(R.id.menu_open_imdb);
            imdbItem.setEnabled(isEnableImdb);
            imdbItem.setVisible(isEnableImdb);

            boolean isEnableYoutube = mMovieDetails.trailers() != null &&
                    mMovieDetails.trailers().youtube.size() > 0;
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
            ShareUtils.shareMovie(getActivity(), mTmdbId, mMovieDetails.traktOrLocalMovie().title);
            fireTrackerEvent("Share");
            return true;
        }
        if (itemId == R.id.menu_open_imdb) {
            ServiceUtils.openImdb(mMovieDetails.traktOrLocalMovie().imdb_id, TAG, getActivity());
            return true;
        }
        if (itemId == R.id.menu_open_youtube) {
            ServiceUtils.openYoutube(mMovieDetails.trailers().youtube.get(0).source, TAG,
                    getActivity());
            return true;
        }
        if (itemId == R.id.menu_open_google_play) {
            ServiceUtils.searchGooglePlay(mMovieDetails.traktOrLocalMovie().title, TAG,
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
        Movie movie = mMovieDetails.traktOrLocalMovie();
        mMovieTitle.setText(movie.title);
        mMovieDescription.setText(movie.overview);

        // release date
        if (movie.released != null && movie.released.getTime() != 0) {
            mMovieReleaseDate.setText(
                    DateUtils.formatDateTime(getActivity(), movie.released.getTime(),
                            DateUtils.FORMAT_SHOW_DATE));
        } else {
            mMovieReleaseDate.setText("");
        }

        // check-in button
        CheatSheet.setup(mCheckinButton);
        // TODO use original title for tvtag
        final String title = movie.title;
        mCheckinButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // display a check-in dialog
                MovieCheckInDialogFragment f = MovieCheckInDialogFragment
                        .newInstance(mTmdbId, title, title);
                f.show(getFragmentManager(), "checkin-dialog");
                fireTrackerEvent("Check-In");
            }
        });

        // watched button
        if (TraktCredentials.get(getActivity()).hasCredentials()) {
            mWatchedButton.setImageResource((movie.watched != null && movie.watched)
                    ? R.drawable.ic_ticked
                    : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                            R.attr.drawableWatch));
            // TODO toggle watched action, cheat sheet
        } else {
            mWatchedButton.setVisibility(View.GONE);
        }

        // collected button
        final boolean isInCollection = movie.inCollection != null && movie.inCollection;
        mCollectedButton.setImageResource(isInCollection
                ? R.drawable.ic_collected
                : Utils.resolveAttributeToResourceId(getActivity().getTheme(),
                        R.attr.drawableCollect));
        CheatSheet.setup(mCollectedButton, isInCollection ? R.string.action_collection_remove
                : R.string.action_collection_add);
        mCollectedButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO update UI state/restart loader?
                if (isInCollection) {
                    MovieTools.removeFromCollection(getActivity(), mTmdbId);
                } else {
                    MovieTools.addToCollection(getActivity(), mTmdbId);
                }
            }
        });

        // show button bar
        mButtonContainer.setVisibility(View.VISIBLE);

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
        if (movie.images != null && !TextUtils.isEmpty(movie.images.poster)) {
            mImageDownloader.download(mImageBaseUrl + movie.images.poster, mMoviePosterBackground,
                    false);
        }
    }

    private void fireTrackerEvent(String label) {
        Utils.trackAction(getActivity(), TAG, label);
    }

    private LoaderManager.LoaderCallbacks<Movie> mMovieLoaderCallbacks
            = new LoaderManager.LoaderCallbacks<Movie>() {
        @Override
        public Loader<com.jakewharton.trakt.entities.Movie> onCreateLoader(int loaderId,
                Bundle args) {
            return new MovieLoader(getActivity(), args.getInt(InitBundle.TMDB_ID));
        }

        @Override
        public void onLoadFinished(Loader<Movie> movieLoader, Movie movie) {
            mMovieDetails.traktMovie(movie);
            mProgressBar.setVisibility(View.GONE);

            if (movie != null) {
                populateMovieViews();
                getSherlockActivity().supportInvalidateOptionsMenu();
            } else {
                // TODO display error message
            }
        }

        @Override
        public void onLoaderReset(Loader<Movie> movieLoader) {
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
                mMovieDetails.trailers(trailers);
                getSherlockActivity().supportInvalidateOptionsMenu();
            }
        }

        @Override
        public void onLoaderReset(Loader<Trailers> trailersLoader) {
            // do nothing
        }
    };

    public static class MovieDetails {

        private Movie mTraktMovie;

        private com.uwetrottmann.tmdb.entities.Movie mTmdbMovie;

        private Trailers mTrailers;

        private Credits mCredits;

        public Movie traktOrLocalMovie() {
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

        public Trailers trailers() {
            return mTrailers;
        }

        public MovieDetails trailers(Trailers trailers) {
            mTrailers = trailers;
            return this;
        }

        public Credits credits() {
            return mCredits;
        }

        public MovieDetails credits(Credits credits) {
            mCredits = credits;
            return this;
        }
    }
}
