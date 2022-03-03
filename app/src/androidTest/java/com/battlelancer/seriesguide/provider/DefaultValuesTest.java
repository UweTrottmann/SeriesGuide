package com.battlelancer.seriesguide.provider;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.dataliberation.ImportTools;
import com.battlelancer.seriesguide.dataliberation.model.Episode;
import com.battlelancer.seriesguide.dataliberation.model.List;
import com.battlelancer.seriesguide.dataliberation.model.Season;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.model.SgEpisode2;
import com.battlelancer.seriesguide.model.SgSeason2;
import com.battlelancer.seriesguide.model.SgShow2;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Lists;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;
import com.battlelancer.seriesguide.ui.episodes.EpisodeFlags;
import com.battlelancer.seriesguide.ui.movies.MovieDetails;
import com.battlelancer.seriesguide.util.tasks.AddListTask;
import com.uwetrottmann.tmdb2.entities.Movie;
import java.util.ArrayList;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DefaultValuesTest {

    private static final Show SHOW;
    private static final Season SEASON;
    private static final Episode EPISODE;
    private static final MovieDetails MOVIE;
    private static final com.battlelancer.seriesguide.dataliberation.model.Movie MOVIE_I;

    private static final List LIST;

    static {
        SHOW = new Show();
        SHOW.tmdb_id = 12;
        SHOW.tvdb_id = 12;

        SEASON = new Season();
        SEASON.tmdb_id = "1234";
        SEASON.tvdbId = 1234;
        SEASON.season = 42;

        EPISODE = new Episode();
        EPISODE.tmdb_id = 123456;
        EPISODE.tvdbId = 123456;

        LIST = new List();
        LIST.name = "Test List";
        LIST.listId = SeriesGuideContract.Lists.generateListId(LIST.name);

        MOVIE = new MovieDetails();
        Movie tmdbMovie = new Movie();
        tmdbMovie.id = 12;
        MOVIE.tmdbMovie(tmdbMovie);

        MOVIE_I = new com.battlelancer.seriesguide.dataliberation.model.Movie();
    }

    private ContentResolver resolver;

    @Before
    public void switchToInMemoryDb() {
        // ProviderTestRule does not work with Room
        // so instead blatantly replace the instance with one that uses an in-memory database
        // and use the real ContentResolver
        Context context = ApplicationProvider.getApplicationContext();
        SgRoomDatabase.switchToInMemory(context);
        resolver = context.getContentResolver();
    }

    @After
    public void closeDb() {
        SgRoomDatabase.getInstance(ApplicationProvider.getApplicationContext()).close();
    }

    @Test
    public void showDefaultValuesImport() {
        Context context = ApplicationProvider.getApplicationContext();
        SgShow2Helper showHelper = SgRoomDatabase.getInstance(context).sgShow2Helper();

        SgShow2 sgShow = ImportTools.toSgShowForImport(SHOW);
        long showId = showHelper.insertShow(sgShow);

        SgShow2 show = showHelper.getShow(showId);
        assertThat(show).isNotNull();

        // Note: compare with SgShow and ShowTools.
        assertThat(show.getTvdbId()).isEqualTo(SHOW.tvdb_id);
        assertThat(show.getTitle()).isNotNull();
        assertThat(show.getOverview()).isNotNull();
        assertThat(show.getGenres()).isNotNull();
        assertThat(show.getNetwork()).isNotNull();
        assertThat(show.getRuntime()).isNotNull();
        assertThat(show.getStatus()).isNotNull();
        assertThat(show.getContentRating()).isNotNull();
        assertThat(show.getNextEpisode()).isNotNull();
        assertThat(show.getPoster()).isNotNull();
        assertThat(show.getPosterSmall()).isNotNull();
        assertThat(show.getNextText()).isNotNull();
        assertThat(show.getImdbId()).isNotNull();
        assertThat(show.getTraktId()).isEqualTo(0);
        assertThat(show.getFavorite()).isFalse();
        assertThat(show.getHexagonMergeComplete()).isTrue();
        assertThat(show.getHidden()).isFalse();
        assertThat(show.getLastUpdatedMs()).isEqualTo(0);
        assertThat(show.getLastEditedSec()).isEqualTo(0);
        assertThat(show.getLastWatchedEpisodeId()).isEqualTo(0);
        assertThat(show.getLastWatchedMs()).isEqualTo(0);
        assertThat(show.getLanguage()).isNotNull();
        assertThat(show.getUnwatchedCount()).isEqualTo(SgShow2.UNKNOWN_UNWATCHED_COUNT);
        assertThat(show.getNotify()).isTrue();
    }

    @Test
    public void seasonDefaultValuesImport() {
        // with Room insert actually checks constraints, so add a matching show first
        Context context = ApplicationProvider.getApplicationContext();
        SgRoomDatabase database = SgRoomDatabase.getInstance(context);

        SgShow2 sgShow = ImportTools.toSgShowForImport(SHOW);
        long showId = database.sgShow2Helper().insertShow(sgShow);

        SgSeason2 sgSeason = ImportTools.toSgSeasonForImport(SEASON, showId);
        long seasonId = database.sgSeason2Helper().insertSeason(sgSeason);

        SgSeason2 season = database.sgSeason2Helper().getSeason(seasonId);
        assertThat(season).isNotNull();

        assertThat(season.getTmdbId()).isEqualTo(SEASON.tmdb_id);
        assertThat(season.getTvdbId()).isEqualTo(SEASON.tvdbId);
        assertThat(season.getShowId()).isEqualTo(showId);
        assertThat(season.getNumberOrNull()).isEqualTo(SEASON.season);
        // getInt returns 0 if NULL, so check explicitly
        assertThat(season.getNotWatchedReleasedOrNull()).isEqualTo(0);
        assertThat(season.getNotWatchedToBeReleasedOrNull()).isEqualTo(0);
        assertThat(season.getNotWatchedNoReleaseOrNull()).isEqualTo(0);
        assertThat(season.getTotalOrNull()).isEqualTo(0);
    }

    @Test
    public void episodeDefaultValuesImport() {
        // with Room insert actually checks constraints, so add a matching show and season first
        Context context = ApplicationProvider.getApplicationContext();
        SgRoomDatabase database = SgRoomDatabase.getInstance(context);

        SgShow2 sgShow = ImportTools.toSgShowForImport(SHOW);
        long showId = database.sgShow2Helper().insertShow(sgShow);

        SgSeason2 sgSeason = ImportTools.toSgSeasonForImport(SEASON, showId);
        long seasonId = database.sgSeason2Helper().insertSeason(sgSeason);

        SgEpisode2 sgEpisode = ImportTools
                .toSgEpisodeForImport(EPISODE, showId, seasonId, sgSeason.getNumber());
        long episodeId = database.sgEpisode2Helper().insertEpisode(sgEpisode);

        SgEpisode2 episode = database.sgEpisode2Helper().getEpisode(episodeId);
        assertThat(episode).isNotNull();

        assertThat(episode.getTitle()).isNotNull();
        assertThat(episode.getNumber()).isEqualTo(0);
        assertThat(episode.getWatched()).isEqualTo(EpisodeFlags.UNWATCHED);
        assertThat(episode.getPlays()).isEqualTo(0);
        assertThat(episode.getDirectors()).isNotNull();
        assertThat(episode.getGuestStars()).isNotNull();
        assertThat(episode.getWriters()).isNotNull();
        assertThat(episode.getImage()).isNotNull();
        assertThat(episode.getCollected()).isFalse();
        assertThat(episode.getImdbId()).isNotNull();
        assertThat(episode.getLastEditedSec()).isEqualTo(0);
        assertThat(episode.getLastUpdatedSec()).isEqualTo(0);
    }

    @Test
    public void listDefaultValues() {
        AddListTask addListTask = new AddListTask(ApplicationProvider.getApplicationContext(),
                LIST.name);
        addListTask.doDatabaseUpdate(resolver, addListTask.getListId());

        Cursor query = resolver.query(Lists.CONTENT_URI, null,
                null, null, null);
        assertThat(query).isNotNull();
        assertThat(query.getCount()).isEqualTo(1);
        assertThat(query.moveToFirst()).isTrue();

        assertDefaultValue(query, Lists.ORDER, 0);

        query.close();
    }

    @Test
    public void listDefaultValuesImport() throws Exception {
        ContentValues values = LIST.toContentValues();

        ContentProviderOperation op = ContentProviderOperation.newInsert(Lists.CONTENT_URI)
                .withValues(values).build();

        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        batch.add(op);
        resolver.applyBatch(SgApp.CONTENT_AUTHORITY, batch);

        Cursor query = resolver.query(Lists.CONTENT_URI, null,
                null, null, null);
        assertThat(query).isNotNull();
        assertThat(query.getCount()).isEqualTo(1);
        assertThat(query.moveToFirst()).isTrue();

        assertDefaultValue(query, Lists.ORDER, 0);

        query.close();
    }

    @Test
    public void movieDefaultValues() {
        ContentValues values = MOVIE.toContentValuesInsert();
        resolver.insert(Movies.CONTENT_URI, values);

        assertMovie();
    }

    @Test
    public void movieDefaultValuesImport() {
        resolver.insert(Movies.CONTENT_URI, MOVIE_I.toContentValues());

        assertMovie();
    }

    private void assertMovie() {
        Cursor query = resolver.query(Movies.CONTENT_URI, null,
                null, null, null);
        assertThat(query).isNotNull();
        assertThat(query.getCount()).isEqualTo(1);
        assertThat(query.moveToFirst()).isTrue();

        assertDefaultValue(query, Movies.RUNTIME_MIN, 0);
        assertDefaultValue(query, Movies.IN_COLLECTION, 0);
        assertDefaultValue(query, Movies.IN_WATCHLIST, 0);
        assertDefaultValue(query, Movies.PLAYS, 0);
        assertDefaultValue(query, Movies.WATCHED, 0);
        assertDefaultValue(query, Movies.RATING_TMDB, 0);
        assertDefaultValue(query, Movies.RATING_VOTES_TMDB, 0);
        assertDefaultValue(query, Movies.RATING_TRAKT, 0);
        assertDefaultValue(query, Movies.RATING_VOTES_TRAKT, 0);

        query.close();
    }

    private void assertNotNullValue(Cursor query, String column) {
        assertThat(query.isNull(query.getColumnIndexOrThrow(column))).isFalse();
    }

    private void assertDefaultValue(Cursor query, String column, int defaultValue) {
        assertNotNullValue(query, column);
        assertThat(query.getInt(query.getColumnIndexOrThrow(column))).isEqualTo(defaultValue);
    }
}
