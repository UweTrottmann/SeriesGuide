package com.battlelancer.seriesguide.traktapi;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.util.DialogTools;
import com.battlelancer.seriesguide.util.TextTools;

/**
 * Allows to check into an episode. Launching activities should subscribe to
 * {@link TraktTask.TraktActionCompleteEvent} to display status toasts.
 */
public class CheckInDialogFragment extends GenericCheckInDialogFragment {

    /**
     * Builds and shows a new {@link CheckInDialogFragment} setting all values based on the given
     * episode TVDb id.
     *
     * @return {@code false} if the fragment was not shown.
     */
    public static boolean show(Context context, FragmentManager fragmentManager,
            int episodeTvdbId) {
        final Cursor episode = context.getContentResolver().query(
                Episodes.buildEpisodeWithShowUri(episodeTvdbId),
                CheckInQuery.PROJECTION, null, null, null);
        if (episode == null) {
            return false;
        }
        if (!episode.moveToFirst()) {
            episode.close();
            return false;
        }

        CheckInDialogFragment f = new CheckInDialogFragment();

        Bundle args = new Bundle();
        args.putInt(InitBundle.EPISODE_TVDB_ID, episodeTvdbId);
        String episodeTitleWithNumbers = episode.getString(CheckInQuery.SHOW_TITLE)
                + " "
                + TextTools.getNextEpisodeString(context,
                episode.getInt(CheckInQuery.SEASON),
                episode.getInt(CheckInQuery.NUMBER),
                episode.getString(CheckInQuery.EPISODE_TITLE));
        args.putString(InitBundle.ITEM_TITLE, episodeTitleWithNumbers);

        f.setArguments(args);
        episode.close();

        return DialogTools.safeShow(f, fragmentManager, "checkInDialog");
    }

    private interface CheckInQuery {

        String[] PROJECTION = new String[]{
                Episodes.SEASON,
                Episodes.NUMBER,
                Episodes.TITLE,
                Shows.TITLE
        };

        int SEASON = 0;
        int NUMBER = 1;
        int EPISODE_TITLE = 2;
        int SHOW_TITLE = 3;
    }

    @Override
    protected void checkInTrakt(String message) {
        new TraktTask(getContext()).checkInEpisode(
                getArguments().getInt(InitBundle.EPISODE_TVDB_ID),
                getArguments().getString(InitBundle.ITEM_TITLE),
                message).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
}
