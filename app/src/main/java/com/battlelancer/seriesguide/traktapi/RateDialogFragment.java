// SPDX-License-Identifier: Apache-2.0
// Copyright 2012-2018, 2020-2023 Uwe Trottmann

package com.battlelancer.seriesguide.traktapi;

import android.app.Dialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.FragmentManager;
import com.battlelancer.seriesguide.databinding.DialogTraktRateBinding;
import com.battlelancer.seriesguide.util.DialogTools;
import com.battlelancer.seriesguide.util.tasks.BaseRateItemTask;
import com.battlelancer.seriesguide.util.tasks.RateEpisodeTask;
import com.battlelancer.seriesguide.util.tasks.RateMovieTask;
import com.battlelancer.seriesguide.util.tasks.RateShowTask;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.uwetrottmann.trakt5.enums.Rating;
import java.util.ArrayList;
import java.util.List;

/**
 * If connected to Trakt, allows rating a show, episode or movie on a 10 value rating scale.
 * If not connected, asks the user to connect Trakt.
 * <p>
 * Use via {@link #safeShow(Context, FragmentManager)}.
 */
public class RateDialogFragment extends AppCompatDialogFragment {

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
    public static RateDialogFragment newInstanceShow(long showId) {
        RateDialogFragment f = new RateDialogFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.ITEM_TYPE, ITEM_SHOW);
        args.putLong(InitBundle.ITEM_ID, showId);
        f.setArguments(args);

        return f;
    }

    /**
     * Create {@link RateDialogFragment} to rate an episode.
     */
    public static RateDialogFragment newInstanceEpisode(long episodeId) {
        RateDialogFragment f = new RateDialogFragment();

        Bundle args = new Bundle();
        args.putString(InitBundle.ITEM_TYPE, ITEM_EPISODE);
        args.putLong(InitBundle.ITEM_ID, episodeId);
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
        args.putLong(InitBundle.ITEM_ID, movieTmdbId);
        f.setArguments(args);

        return f;
    }

    private DialogTraktRateBinding binding;

    /**
     * Checks and asks for missing trakt credentials. Otherwise if they are valid shows the dialog.
     */
    public boolean safeShow(Context context, FragmentManager fragmentManager) {
        if (!TraktCredentials.ensureCredentials(context)) {
            return false;
        }
        return DialogTools.safeShow(this, fragmentManager, "rateDialog");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder;

        binding = DialogTraktRateBinding.inflate(LayoutInflater.from(getContext()));

        List<Button> ratingButtons = new ArrayList<>();
        ratingButtons.add(binding.rating1);
        ratingButtons.add(binding.rating2);
        ratingButtons.add(binding.rating3);
        ratingButtons.add(binding.rating4);
        ratingButtons.add(binding.rating5);
        ratingButtons.add(binding.rating6);
        ratingButtons.add(binding.rating7);
        ratingButtons.add(binding.rating8);
        ratingButtons.add(binding.rating9);
        ratingButtons.add(binding.rating10);

        for (int i = 0; i < ratingButtons.size(); i++) {
            Button ratingButton = ratingButtons.get(i);
            ratingButton.setText(TraktTools.buildUserRatingString(getContext(), i + 1));
        }

        // rating buttons from 1 (worst) to 10 (best)
        ratingButtons.get(0).setOnClickListener(v -> rate(Rating.WEAKSAUCE));
        ratingButtons.get(1).setOnClickListener(v -> rate(Rating.TERRIBLE));
        ratingButtons.get(2).setOnClickListener(v -> rate(Rating.BAD));
        ratingButtons.get(3).setOnClickListener(v -> rate(Rating.POOR));
        ratingButtons.get(4).setOnClickListener(v -> rate(Rating.MEH));
        ratingButtons.get(5).setOnClickListener(v -> rate(Rating.FAIR));
        ratingButtons.get(6).setOnClickListener(v -> rate(Rating.GOOD));
        ratingButtons.get(7).setOnClickListener(v -> rate(Rating.GREAT));
        ratingButtons.get(8).setOnClickListener(v -> rate(Rating.SUPERB));
        ratingButtons.get(9).setOnClickListener(v -> rate(Rating.TOTALLYNINJA));

        builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setView(binding.getRoot());

        return builder.create();
    }

    private void rate(Rating rating) {
        Bundle args = requireArguments();

        String itemType = args.getString(InitBundle.ITEM_TYPE);
        if (itemType == null) {
            return;
        }
        long itemId = args.getLong(InitBundle.ITEM_ID);
        BaseRateItemTask task = null;
        switch (itemType) {
            case ITEM_MOVIE: {
                task = new RateMovieTask(getContext(), rating, (int) itemId);
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
        binding = null;
    }
}
