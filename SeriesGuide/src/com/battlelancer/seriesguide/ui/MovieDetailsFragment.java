/*
 * Copyright 2013 Uwe Trottmann
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
 * 
 */

package com.battlelancer.seriesguide.ui;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.loaders.TmdbMovieDetailsLoader;
import com.battlelancer.seriesguide.loaders.TmdbMovieDetailsLoader.MovieDetails;
import com.battlelancer.seriesguide.ui.dialogs.MovieCheckInDialogFragment;
import com.battlelancer.seriesguide.util.ImageDownloader;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;
import com.uwetrottmann.tmdb.entities.Movie;
import com.uwetrottmann.tmdb.entities.Trailers;

/**
 * Displays details about one movie including plot, ratings, trailers and a
 * poster.
 */
public class MovieDetailsFragment extends SherlockFragment implements
        LoaderManager.LoaderCallbacks<MovieDetails> {

    public interface InitBundle {
        String TMDB_ID = "tmdbid";
    }

    private static final int LOADER_ID = R.layout.movie_details_fragment;
    private ImageDownloader mImageDownloader;
    private String mBaseUrl;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.movie_details_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        int tmdbId = getArguments().getInt(InitBundle.TMDB_ID);
        if (tmdbId == 0) {
            getSherlockActivity().getSupportFragmentManager().popBackStack();
            return;
        }

        mImageDownloader = ImageDownloader.getInstance(getActivity());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mBaseUrl = prefs.getString(SeriesGuidePreferences.KEY_TMDB_BASE_URL,
                "http://cf2.imgobject.com/t/p/") + "w342";

        Bundle args = new Bundle();
        args.putInt(InitBundle.TMDB_ID, tmdbId);
        getLoaderManager().initLoader(LOADER_ID, args, this);
    }

    @Override
    public Loader<MovieDetails> onCreateLoader(int loaderId, Bundle args) {
        if (args != null) {
            int tmdbId = args.getInt(InitBundle.TMDB_ID);
            if (tmdbId != 0) {
                return new TmdbMovieDetailsLoader(getActivity(), tmdbId);
            }
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<MovieDetails> loader, MovieDetails details) {
        if (details != null) {
            onPopulateMovieDetails(details);
        }
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void onPopulateMovieDetails(MovieDetails details) {
        final Movie movie = details.movie();
        if (movie != null) {
            ((TextView) getView().findViewById(R.id.textViewMovieTitle)).setText(movie.title);

            TextView textViewDate = (TextView) getView().findViewById(R.id.textViewMovieDate);
            if (movie.release_date != null) {
                textViewDate.setText(DateUtils.formatDateTime(getActivity(),
                        movie.release_date.getTime(),
                        DateUtils.FORMAT_SHOW_DATE));
            } else {
                textViewDate.setText("");
            }

            ((TextView) getView().findViewById(R.id.textViewMovieDescription))
                    .setText(movie.overview);

            if (!TextUtils.isEmpty(movie.poster_path)) {
                ImageView background = (ImageView) getView()
                        .findViewById(R.id.imageViewMoviePoster);
                if (AndroidUtils.isJellyBeanOrHigher()) {
                    background.setImageAlpha(50);
                } else {
                    background.setAlpha(50);
                }

                String posterPath = mBaseUrl + movie.poster_path;
                mImageDownloader.download(posterPath, background, false);
            }

            View checkinButton = getView().findViewById(R.id.buttonMovieCheckIn);
            checkinButton.setVisibility(View.VISIBLE);
            checkinButton.setOnClickListener(
                    new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            // show check-in dialog
                            if (!TextUtils.isEmpty(movie.imdb_id)) {
                                // display a check-in dialog
                                MovieCheckInDialogFragment f = MovieCheckInDialogFragment
                                        .newInstance(movie.imdb_id, movie.title);
                                f.show(getFragmentManager(), "checkin-dialog");
                            }
                        }
                    });
        }

        // Trailer button
        // TODO use new YouTube API to display inline
        final Trailers trailers = details.trailers();
        View buttonTrailer = getView().findViewById(R.id.buttonMovieTrailer);
        View divider = getView().findViewById(R.id.divider);
        if (trailers != null && trailers.youtube.size() > 0) {
            buttonTrailer.setVisibility(View.VISIBLE);
            divider.setVisibility(View.VISIBLE);
            buttonTrailer.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://www.youtube.com/watch?v="
                                    + trailers.youtube.get(0).source));
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    startActivity(intent);
                }
            });
        } else {
            buttonTrailer.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
        }

    }

    @Override
    public void onLoaderReset(Loader<MovieDetails> loader) {
    }

}
