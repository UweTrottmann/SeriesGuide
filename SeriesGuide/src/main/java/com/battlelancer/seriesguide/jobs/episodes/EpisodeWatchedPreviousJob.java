package com.battlelancer.seriesguide.jobs.episodes;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.appwidget.ListWidgetProvider;
import com.battlelancer.seriesguide.enums.EpisodeFlags;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;

public class EpisodeWatchedPreviousJob extends BaseEpisodesJob {

    private final long episodeFirstAired;
    private final int episodeNumber;

    public EpisodeWatchedPreviousJob(int showTvdbId, long episodeFirstAired, int episodeNumber) {
        super(showTvdbId, EpisodeFlags.WATCHED, JobAction.EPISODE_WATCHED_FLAG);
        this.episodeFirstAired = episodeFirstAired;
        this.episodeNumber = episodeNumber;
    }

    @Override
    public Uri getDatabaseUri() {
        return Episodes.buildEpisodesOfShowUri(
                String.valueOf(getShowTvdbId()));
    }

    @Override
    public String getDatabaseSelection() {
        // must
        // - be released before current episode OR at the same time, but with lower number (Netflix)
        // - have a release date,
        // - be unwatched or skipped
        return "(" + Episodes.FIRSTAIREDMS + "<" + episodeFirstAired + ")"
                + " OR (" + Episodes.FIRSTAIREDMS + "=" + episodeFirstAired
                + " AND " + Episodes.NUMBER + "<" + episodeNumber + ")"
                + " AND " + Episodes.SELECTION_HAS_RELEASE_DATE
                + " AND " + Episodes.SELECTION_UNWATCHED_OR_SKIPPED;
    }

    @Override
    protected String getDatabaseColumnToUpdate() {
        return Episodes.WATCHED;
    }

    @Override
    public boolean applyLocalChanges(Context context, boolean requiresNetworkJob) {
        if (!super.applyLocalChanges(context, requiresNetworkJob)) {
            return false;
        }

        // we don't care about the last watched episode value
        // always update last watched time, this type only marks as watched
        updateLastWatched(context, -1, true);

        ListWidgetProvider.notifyDataChanged(context);

        return true;
    }

    @NonNull
    @Override
    public String getConfirmationText(Context context) {
        return context.getString(R.string.mark_untilhere);
    }
}
