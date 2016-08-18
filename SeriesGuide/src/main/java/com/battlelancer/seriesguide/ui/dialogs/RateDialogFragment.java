package com.battlelancer.seriesguide.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.os.AsyncTaskCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.util.tasks.BaseRateItemTask;
import com.battlelancer.seriesguide.util.tasks.RateEpisodeTask;
import com.battlelancer.seriesguide.util.tasks.RateMovieTask;
import com.battlelancer.seriesguide.util.tasks.RateShowTask;
import com.uwetrottmann.trakt5.enums.Rating;

/**
 * Displays a 10 value rating scale. If a rating is clicked it will be stored to the database and
 * sent to trakt (if the user is connected).
 */
public class RateDialogFragment extends DialogFragment {

    private interface InitBundle {
        String ITEM_TYPE = "item-type";
        String ITEM_ID = "item-id";
    }

    private static final String ITEM_SHOW = "show";
    private static final String ITEM_EPISODE = "episode";
    private static final String ITEM_MOVIE = "movie";

    /**
     * Create {@link RateDialogFragment} to rate a show.
     */
    public static RateDialogFragment newInstanceShow(int showTvdbId) {
        RateDialogFragment f = new RateDialogFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.ITEM_TYPE, ITEM_SHOW);
        args.putInt(InitBundle.ITEM_ID, showTvdbId);
        f.setArguments(args);

        return f;
    }

    /**
     * Create {@link RateDialogFragment} to rate an episode.
     */
    public static RateDialogFragment newInstanceEpisode(int episodeTvdbId) {
        RateDialogFragment f = new RateDialogFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.ITEM_TYPE, ITEM_EPISODE);
        args.putInt(InitBundle.ITEM_ID, episodeTvdbId);
        f.setArguments(args);

        return f;
    }

    /**
     * Create {@link RateDialogFragment} to rate a movie.
     */
    public static RateDialogFragment newInstanceMovie(int movieTmdbId) {
        RateDialogFragment f = new RateDialogFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.ITEM_TYPE, ITEM_MOVIE);
        args.putInt(InitBundle.ITEM_ID, movieTmdbId);
        f.setArguments(args);

        return f;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder;

        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") View layout = inflater.inflate(R.layout.dialog_trakt_rate,
                null);

        // rating buttons from 1 (worst) to 10 (best)
        layout.findViewById(R.id.weaksauce).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.WEAKSAUCE);
            }
        });
        layout.findViewById(R.id.rating2).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.TERRIBLE);
            }
        });
        layout.findViewById(R.id.rating3).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.BAD);
            }
        });
        layout.findViewById(R.id.rating4).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.POOR);
            }
        });
        layout.findViewById(R.id.rating5).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.MEH);
            }
        });
        layout.findViewById(R.id.rating6).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.FAIR);
            }
        });
        layout.findViewById(R.id.rating7).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.GOOD);
            }
        });
        layout.findViewById(R.id.rating8).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.GREAT);
            }
        });
        layout.findViewById(R.id.rating9).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.SUPERB);
            }
        });
        layout.findViewById(R.id.totallyninja).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.TOTALLYNINJA);
            }
        });

        builder = new AlertDialog.Builder(getActivity());
        builder.setView(layout);

        return builder.create();
    }

    private void rate(Rating rating) {
        Bundle args = getArguments();

        String itemType = args.getString(InitBundle.ITEM_TYPE);
        if (itemType == null) {
            return;
        }
        int itemId = args.getInt(InitBundle.ITEM_ID);
        SgApp app = SgApp.from(getActivity());
        BaseRateItemTask task = null;
        switch (itemType) {
            case ITEM_MOVIE: {
                task = new RateMovieTask(app, rating, itemId);
                break;
            }
            case ITEM_SHOW: {
                task = new RateShowTask(app, rating, itemId);
                break;
            }
            case ITEM_EPISODE: {
                task = new RateEpisodeTask(app, rating, itemId);
                break;
            }
        }
        if (task != null) {
            AsyncTaskCompat.executeParallel(task);
        }

        dismiss();
    }
}
