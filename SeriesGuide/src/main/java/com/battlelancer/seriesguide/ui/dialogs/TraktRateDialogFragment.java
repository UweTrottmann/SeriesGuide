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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.enumerations.Rating;
import com.uwetrottmann.androidutils.AndroidUtils;

/**
 * Displays the trakt advanced rating scale. If a rating is chosen, launches an appropriate {@link
 * com.battlelancer.seriesguide.util.TraktTask} to submit the rating to trakt.
 */
public class TraktRateDialogFragment extends DialogFragment {

    /**
     * Create {@link TraktRateDialogFragment} to rate a show.
     *
     * @return TraktRateDialogFragment
     */
    public static TraktRateDialogFragment newInstanceShow(int showTvdbId) {
        TraktRateDialogFragment f = new TraktRateDialogFragment();
        Bundle args = new Bundle();
        args.putString(TraktTask.InitBundle.TRAKTACTION, TraktAction.RATE_SHOW.name());
        args.putInt(TraktTask.InitBundle.SHOW_TVDBID, showTvdbId);
        f.setArguments(args);
        return f;
    }

    /**
     * Create {@link TraktRateDialogFragment} to rate an episode.
     *
     * @return TraktRateDialogFragment
     */
    public static TraktRateDialogFragment newInstanceEpisode(int showTvdbid, int seasonNumber,
            int episodeNumber) {
        TraktRateDialogFragment f = new TraktRateDialogFragment();
        Bundle args = new Bundle();
        args.putString(TraktTask.InitBundle.TRAKTACTION, TraktAction.RATE_EPISODE.name());
        args.putInt(TraktTask.InitBundle.SHOW_TVDBID, showTvdbid);
        args.putInt(TraktTask.InitBundle.SEASON, seasonNumber);
        args.putInt(TraktTask.InitBundle.EPISODE, episodeNumber);
        f.setArguments(args);
        return f;
    }

    /**
     * Create {@link TraktRateDialogFragment} to rate a show.
     *
     * @return TraktRateDialogFragment
     */
    public static TraktRateDialogFragment newInstanceMovie(int movieTmdbId) {
        TraktRateDialogFragment f = new TraktRateDialogFragment();
        Bundle args = new Bundle();
        args.putString(TraktTask.InitBundle.TRAKTACTION, TraktAction.RATE_MOVIE.name());
        args.putInt(TraktTask.InitBundle.MOVIE_TMDB_ID, movieTmdbId);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), "Rate Dialog");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder;

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.trakt_rate_dialog, null);

        layout.findViewById(R.id.totallyninja).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.TotallyNinja);
            }
        });
        layout.findViewById(R.id.weaksauce).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.WeakSauce);
            }
        });

        // advanced rating steps
        layout.findViewById(R.id.rating2).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.Terrible);
            }
        });
        layout.findViewById(R.id.rating3).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.Bad);
            }
        });
        layout.findViewById(R.id.rating4).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.Poor);
            }
        });
        layout.findViewById(R.id.rating5).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.Meh);
            }
        });
        layout.findViewById(R.id.rating6).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.Fair);
            }
        });
        layout.findViewById(R.id.rating7).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.Good);
            }
        });
        layout.findViewById(R.id.rating8).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.Great);
            }
        });
        layout.findViewById(R.id.rating9).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.Superb);
            }
        });

        builder = new AlertDialog.Builder(getActivity());
        builder.setView(layout);
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    private void onRate(Rating rating) {
        getArguments().putString(TraktTask.InitBundle.RATING, rating.toString());
        AndroidUtils.executeAsyncTask(new TraktTask(getActivity(), getArguments()));
        dismiss();
    }
}
