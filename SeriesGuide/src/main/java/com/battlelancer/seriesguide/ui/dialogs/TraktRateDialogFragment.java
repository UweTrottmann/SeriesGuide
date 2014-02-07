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

import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.enumerations.Rating;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.battlelancer.seriesguide.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;

/**
 * Displays the trakt advanced rating scale, submits the chosen rating to the given item on trakt.
 */
public class TraktRateDialogFragment extends DialogFragment {

    /**
     * Create {@link TraktRateDialogFragment} to rate a show.
     *
     * @return TraktRateDialogFragment
     */
    public static TraktRateDialogFragment newInstance(int tvdbid) {
        TraktRateDialogFragment f = new TraktRateDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ShareItems.TRAKTACTION, TraktAction.RATE_SHOW.index);
        args.putInt(ShareItems.TVDBID, tvdbid);
        f.setArguments(args);
        return f;
    }

    /**
     * Create {@link TraktRateDialogFragment} to rate an episode.
     *
     * @return TraktRateDialogFragment
     */
    public static TraktRateDialogFragment newInstance(int showTvdbid, int season, int episode) {
        TraktRateDialogFragment f = new TraktRateDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ShareItems.TRAKTACTION, TraktAction.RATE_EPISODE.index);
        args.putInt(ShareItems.TVDBID, showTvdbid);
        args.putInt(ShareItems.SEASON, season);
        args.putInt(ShareItems.EPISODE, episode);
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
        final Context context = getActivity();

        AlertDialog.Builder builder;

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.trakt_rate_dialog, null);

        layout.findViewById(R.id.totallyninja).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.TotallyNinja, context);
            }
        });
        layout.findViewById(R.id.weaksauce).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.WeakSauce, context);
            }
        });

        // advanced rating steps
        layout.findViewById(R.id.rating2).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.Terrible, context);
            }
        });
        layout.findViewById(R.id.rating3).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.Bad, context);
            }
        });
        layout.findViewById(R.id.rating4).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.Poor, context);
            }
        });
        layout.findViewById(R.id.rating5).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.Meh, context);
            }
        });
        layout.findViewById(R.id.rating6).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.Fair, context);
            }
        });
        layout.findViewById(R.id.rating7).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.Good, context);
            }
        });
        layout.findViewById(R.id.rating8).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.Great, context);
            }
        });
        layout.findViewById(R.id.rating9).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onRate(Rating.Superb, context);
            }
        });

        builder = new AlertDialog.Builder(context);
        builder.setView(layout);
        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    private void onRate(Rating rating, Context context) {
        getArguments().putString(ShareItems.RATING, rating.toString());
        AndroidUtils.executeAsyncTask(new TraktTask(context, getArguments()));
        dismiss();
    }
}
