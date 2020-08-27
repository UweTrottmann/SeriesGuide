
package com.battlelancer.seriesguide.dataliberation.model;

import android.content.ContentValues;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Episodes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Seasons;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.google.gson.annotations.SerializedName;

public class Episode {

    @SerializedName("tvdb_id")
    public int tvdbId;

    public int episode;

    @Nullable
    @SerializedName("episode_absolute")
    public Integer episodeAbsolute;

    @Nullable
    public String title;

    @SerializedName("first_aired")
    public long firstAired;

    public boolean watched;

    public int plays;

    public boolean skipped;

    public boolean collected;

    @Nullable
    @SerializedName("imdb_id")
    public String imdbId;

    /*
     * Full dump only follows.
     */

    @Nullable
    @SerializedName("episode_dvd")
    public Double episodeDvd;

    @Nullable
    public String overview;

    @Nullable
    public String image;

    @Nullable
    public String writers;

    @Nullable
    public String gueststars;

    @Nullable
    public String directors;

    @Nullable
    public Double rating;
    @Nullable
    public Integer rating_votes;
    @Nullable
    public Integer rating_user;

    @SerializedName("last_edited")
    public long lastEdited;

    public ContentValues toContentValues(int showTvdbId, int seasonTvdbId, int seasonNumber) {
        ContentValues values = new ContentValues();
        values.put(Episodes._ID, tvdbId);

        values.put(Episodes.TITLE, title != null ? title : "");
        values.put(Episodes.OVERVIEW, overview);
        values.put(Episodes.NUMBER, Math.max(episode, 0));
        values.put(Episodes.SEASON, seasonNumber);
        if (episodeDvd != null) {
            values.put(Episodes.DVDNUMBER, episodeDvd >= 0 ? episodeDvd : 0);
        }

        values.put(Shows.REF_SHOW_ID, showTvdbId);
        values.put(Seasons.REF_SEASON_ID, seasonTvdbId);

        // watched/skipped represented internally in watched flag
        values.put(Episodes.WATCHED, skipped
                ? EpisodeFlags.SKIPPED : watched
                ? EpisodeFlags.WATCHED : EpisodeFlags.UNWATCHED);
        int playsValue;
        if (watched && plays >= 1) {
            playsValue = plays;
        } else {
            playsValue = watched ? 1 : 0;
        }
        values.put(Episodes.PLAYS, playsValue);

        values.put(Episodes.DIRECTORS, directors != null ? directors : "");
        values.put(Episodes.GUESTSTARS, gueststars != null ? gueststars : "");
        values.put(Episodes.WRITERS, writers != null ? writers : "");
        values.put(Episodes.IMAGE, image != null ? image : "");

        values.put(Episodes.FIRSTAIREDMS, firstAired);
        values.put(Episodes.COLLECTED, collected ? 1 : 0);

        if (rating != null) {
            values.put(Episodes.RATING_GLOBAL, (rating >= 0 && rating <= 10) ? rating : 0);
        }
        if (rating_votes != null) {
            values.put(Episodes.RATING_VOTES, rating_votes >= 0 ? rating_votes : 0);
        }
        if (rating_user != null) {
            values.put(Episodes.RATING_USER, (rating_user >= 0 && rating_user <= 10)
                    ? rating_user : 0);
        }

        values.put(Episodes.IMDBID, imdbId != null ? imdbId : "");
        values.put(Episodes.LAST_EDITED, lastEdited);
        if (episodeAbsolute != null) {
            values.put(Episodes.ABSOLUTE_NUMBER, episodeAbsolute >= 0 ? episodeAbsolute : 0);
        }

        // set default values
        values.put(Episodes.LAST_UPDATED, 0);

        return values;
    }
}
