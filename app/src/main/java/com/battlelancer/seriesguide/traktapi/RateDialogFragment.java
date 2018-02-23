package com.battlelancer.seriesguide.traktapi;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import butterknife.BindViews;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.tasks.BaseRateItemTask;
import com.battlelancer.seriesguide.util.tasks.RateEpisodeTask;
import com.battlelancer.seriesguide.util.tasks.RateMovieTask;
import com.battlelancer.seriesguide.util.tasks.RateShowTask;
import com.uwetrottmann.trakt5.enums.Rating;
import java.util.List;

/**
 * Displays a 10 value rating scale. If a rating is clicked it will be stored to the database and
 * sent to trakt (if the user is connected).
 */
public class RateDialogFragment extends AppCompatDialogFragment {

    /**
     * Display a {@link RateDialogFragment} to rate an episode.
     */
    public static void displayRateDialog(Context context, FragmentManager fragmentManager,
            int episodeTvdbId) {
        if (!TraktCredentials.ensureCredentials(context)) {
            return;
        }
        RateDialogFragment newFragment = newInstanceEpisode(episodeTvdbId);
        newFragment.show(fragmentManager, "ratedialog");
    }

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

    @BindViews({
            R.id.rating1,
            R.id.rating2,
            R.id.rating3,
            R.id.rating4,
            R.id.rating5,
            R.id.rating6,
            R.id.rating7,
            R.id.rating8,
            R.id.rating9,
            R.id.rating10
    }) List<Button> ratingButtons;
    private Unbinder unbinder;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder;

        @SuppressLint("InflateParams") View layout = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_trakt_rate, null);

        unbinder = ButterKnife.bind(this, layout);

        for (int i = 0; i < ratingButtons.size(); i++) {
            Button ratingButton = ratingButtons.get(i);
            ratingButton.setText(TraktTools.buildUserRatingString(getContext(), i + 1));
        }

        // rating buttons from 1 (worst) to 10 (best)
        ratingButtons.get(0).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.WEAKSAUCE);
            }
        });
        ratingButtons.get(1).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.TERRIBLE);
            }
        });
        ratingButtons.get(2).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.BAD);
            }
        });
        ratingButtons.get(3).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.POOR);
            }
        });
        ratingButtons.get(4).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.MEH);
            }
        });
        ratingButtons.get(5).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.FAIR);
            }
        });
        ratingButtons.get(6).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.GOOD);
            }
        });
        ratingButtons.get(7).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.GREAT);
            }
        });
        ratingButtons.get(8).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rate(Rating.SUPERB);
            }
        });
        ratingButtons.get(9).setOnClickListener(new View.OnClickListener() {
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
        BaseRateItemTask task = null;
        switch (itemType) {
            case ITEM_MOVIE: {
                task = new RateMovieTask(getContext(), rating, itemId);
                break;
            }
            case ITEM_SHOW: {
                task = new RateShowTask(getContext(), rating, itemId);
                break;
            }
            case ITEM_EPISODE: {
                task = new RateEpisodeTask(getContext(), rating, itemId);
                break;
            }
        }
        if (task != null) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        // guard against onClick being called after onSaveInstanceState by allowing state loss
        dismissAllowingStateLoss();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (unbinder != null) {
            unbinder.unbind();
        }
    }
}
