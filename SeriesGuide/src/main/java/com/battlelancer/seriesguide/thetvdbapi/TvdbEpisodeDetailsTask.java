package com.battlelancer.seriesguide.thetvdbapi;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.os.AsyncTaskCompat;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.util.TextTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.thetvdb.entities.Episode;
import com.uwetrottmann.thetvdb.entities.EpisodeResponse;
import com.uwetrottmann.thetvdb.services.TheTvdbEpisodes;
import java.io.IOException;
import retrofit2.Response;
import timber.log.Timber;

/**
 * AsyncTask that fetches full episode information from TVDb and updates the database with it.
 */
public class TvdbEpisodeDetailsTask extends AsyncTask<Void, Void, Void> {

    @Nullable
    public static TvdbEpisodeDetailsTask runIfOutdated(Context context, int showTvdbId,
            int episodeTvdbId,
            long lastEdited, long lastUpdated) {
        if ((lastUpdated == 0 || lastEdited > lastUpdated)
                && Utils.isAllowedLargeDataConnection(context)) {
            TvdbEpisodeDetailsTask detailsTask = new TvdbEpisodeDetailsTask(context, showTvdbId,
                    episodeTvdbId, lastEdited);
            AsyncTaskCompat.executeParallel(detailsTask);
            return detailsTask;
        } else {
            return null;
        }
    }

    private final Context context;
    private final int showTvdbId;
    private final int episodeTvdbId;
    private final long lastEdited;

    public TvdbEpisodeDetailsTask(Context context, int showTvdbId, int episodeTvdbId,
            long lastEdited) {
        this.context = context.getApplicationContext();
        this.showTvdbId = showTvdbId;
        this.episodeTvdbId = episodeTvdbId;
        this.lastEdited = lastEdited;
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (isCancelled()) {
            return null;
        }

        String language = TvdbTools.getShowLanguage(context, showTvdbId);
        if (language == null) {
            return null; // failed to get language
        }

        if (isCancelled()) {
            return null;
        }

        Episode.FullEpisode episode = getEpisode(language);
        if (episode == null) {
            return null; // failed to get episode from TVDB
        }

        ContentValues values = new ContentValues();
        values.put(Episodes.IMAGE, episode.filename);
        values.put(Episodes.IMDBID, episode.imdbId);
        values.put(Episodes.DIRECTORS, TextTools.mendTvdbStrings(episode.directors));
        values.put(Episodes.GUESTSTARS, TextTools.mendTvdbStrings(episode.guestStars));
        values.put(Episodes.WRITERS, TextTools.mendTvdbStrings(episode.writers));
        // set last updated to last edited field to acknowledge we got details of this change
        // do not use current last updated value as this task does not update all episode properties
        values.put(Episodes.LAST_UPDATED, lastEdited);

        ContentResolver resolver = context.getContentResolver();
        resolver.update(Episodes.buildEpisodeUri(episodeTvdbId), values, null, null);

        // make sure other loaders (activity, overview, details) are notified
        resolver.notifyChange(Episodes.buildEpisodeWithShowUri(episodeTvdbId), null);

        return null;
    }

    @Nullable
    private Episode.FullEpisode getEpisode(String language) {
        TheTvdbEpisodes tvdbEpisodes = SgApp.getServicesComponent(context).tvdbEpisodes();
        try {
            Response<EpisodeResponse> response = tvdbEpisodes.get(episodeTvdbId, language)
                    .execute();
            if (response.isSuccessful()) {
                return response.body().data;
            }
            Timber.e("%s %s Getting full episode details failed. (id=%s,lang=%s)",
                    response.code(), response.message(), episodeTvdbId, language);
        } catch (IOException ignored) {
            Timber.e(ignored, "Getting full episode details failed (id=%s,lang=%s)",
                    episodeTvdbId, language);
        }

        return null;
    }
}
