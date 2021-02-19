package com.battlelancer.seriesguide.traktapi;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import com.battlelancer.seriesguide.provider.SgEpisode2WithShow;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.util.DialogTools;
import com.battlelancer.seriesguide.util.TextTools;

/**
 * Allows to check into an episode. Launching activities should subscribe to
 * {@link TraktTask.TraktActionCompleteEvent} to display status toasts.
 */
public class CheckInDialogFragment extends GenericCheckInDialogFragment {

    /**
     * Builds and shows a new {@link CheckInDialogFragment} setting all values based on the given
     * episode row ID.
     *
     * @return {@code false} if the fragment was not shown.
     */
    public static boolean show(Context context, FragmentManager fragmentManager, long episodeId) {
        SgEpisode2WithShow episode = SgRoomDatabase.getInstance(context).sgEpisode2Helper()
                .getEpisodeWithShow(episodeId);
        if (episode == null) {
            return false;
        }

        CheckInDialogFragment f = new CheckInDialogFragment();

        Bundle args = new Bundle();
        args.putLong(InitBundle.EPISODE_ID, episodeId);
        String episodeTitleWithNumbers = episode.getSeriestitle()
                + " "
                + TextTools.getNextEpisodeString(context,
                episode.getSeason(),
                episode.getEpisodenumber(),
                episode.getEpisodetitle());
        args.putString(InitBundle.ITEM_TITLE, episodeTitleWithNumbers);

        f.setArguments(args);

        return DialogTools.safeShow(f, fragmentManager, "checkInDialog");
    }

    @Override
    protected void checkInTrakt(String message) {
        new TraktTask(getContext()).checkInEpisode(
                requireArguments().getLong(InitBundle.EPISODE_ID),
                requireArguments().getString(InitBundle.ITEM_TITLE),
                message).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
