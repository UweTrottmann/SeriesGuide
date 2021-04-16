package com.battlelancer.seriesguide.sync;

import static com.google.common.truth.Truth.assertThat;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.text.format.DateUtils;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.battlelancer.seriesguide.model.SgMovie;
import com.battlelancer.seriesguide.modules.AppModule;
import com.battlelancer.seriesguide.modules.DaggerTestServicesComponent;
import com.battlelancer.seriesguide.modules.TestHttpClientModule;
import com.battlelancer.seriesguide.modules.TestServicesComponent;
import com.battlelancer.seriesguide.modules.TestTmdbModule;
import com.battlelancer.seriesguide.modules.TestTraktModule;
import com.battlelancer.seriesguide.provider.MovieHelper;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Movies;
import com.battlelancer.seriesguide.provider.SgRoomDatabase;
import com.battlelancer.seriesguide.ui.movies.MovieDetails;
import com.battlelancer.seriesguide.ui.movies.MovieTools;
import com.google.common.truth.Truth;
import com.uwetrottmann.tmdb2.entities.Movie;
import com.uwetrottmann.tmdb2.services.ConfigurationService;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TmdbSyncTest {

    @Inject ConfigurationService tmdbConfigService;
    @Inject MovieTools movieTools;

    private ContentResolver resolver;
    private SgRoomDatabase db;
    private MovieHelper movieHelper;

    @Before
    public void setup() {
        /*
        ProviderTestRule does not work with Room, so instead blatantly replace the instance with one
         that uses an in-memory database and use the real ContentResolver.
         */
        Context context = ApplicationProvider.getApplicationContext();
        SgRoomDatabase.switchToInMemory(context);
        resolver = context.getContentResolver();
        db = SgRoomDatabase.getInstance(context);
        movieHelper = db.movieHelper();

        TestServicesComponent component = DaggerTestServicesComponent.builder()
                .appModule(new AppModule(context))
                .httpClientModule(new TestHttpClientModule())
                .traktModule(new TestTraktModule())
                .tmdbModule(new TestTmdbModule())
                .build();
        component.inject(this);
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void updatesMoviesLessFrequentIfOlder() {
        long lastUpdatedCurrent = System.currentTimeMillis();
        long lastUpdatedOutdated = System.currentTimeMillis()
                - TmdbSync.UPDATED_BEFORE_DAYS - DateUtils.DAY_IN_MILLIS;
        long lastUpdatedVeryOutdated = System.currentTimeMillis()
                - TmdbSync.UPDATED_BEFORE_90_DAYS - DateUtils.DAY_IN_MILLIS;
        long releaseDateCurrent = System.currentTimeMillis();
        long releaseDateOld = System.currentTimeMillis()
                - TmdbSync.RELEASED_AFTER_DAYS - DateUtils.DAY_IN_MILLIS;

        // released today
        insertMovie(10, releaseDateCurrent, lastUpdatedCurrent);
        insertMovie(11, releaseDateCurrent, lastUpdatedOutdated);
        // released a while ago
        insertMovie(12, releaseDateOld, lastUpdatedCurrent);
        insertMovie(13, releaseDateOld, lastUpdatedOutdated);
        insertMovie(14, releaseDateOld, lastUpdatedVeryOutdated);

        doUpdateAndAssertSuccess();

        // only the recently released outdated and the older very outdated movie should have been updated
        List<SgMovie> movies = movieHelper.getAllMovies();
        assertThat(findMovieWithId(movies, 10).lastUpdated).isEqualTo(lastUpdatedCurrent);
        assertThat(lastUpdatedOutdated < findMovieWithId(movies, 11).lastUpdated).isTrue();

        assertThat(findMovieWithId(movies, 12).lastUpdated).isEqualTo(lastUpdatedCurrent);
        assertThat(findMovieWithId(movies, 13).lastUpdated).isEqualTo(lastUpdatedOutdated);
        assertThat(lastUpdatedVeryOutdated < findMovieWithId(movies, 14).lastUpdated).isTrue();
    }

    @Test
    public void updatesMovieWithLastUpdatedIsNull() {
        // released today + last updated IS NULL
        insertMovie(12, System.currentTimeMillis(), null);

        doUpdateAndAssertSuccess();

        // the movie should have been updated
        List<SgMovie> movies = movieHelper.getAllMovies();
        SgMovie dbMovie = findMovieWithId(movies, 12);
        assertThat(dbMovie.lastUpdated).isNotNull();
        assertThat(dbMovie.lastUpdated).isNotEqualTo(0);
    }

    private SgMovie findMovieWithId(List<SgMovie> movies, int tmdbId) {
        for (SgMovie movie : movies) {
            if (movie.tmdbId == tmdbId) {
                return movie;
            }
        }
        Truth.assertWithMessage("Did not find movie with TMDB id %s", tmdbId).fail();
        throw new IllegalArgumentException();
    }

    private void insertMovie(int tmdbId, long releaseDateMs, Long lastUpdatedMs) {
        Movie movie = new Movie();
        movie.id = tmdbId;
        movie.release_date = new Date(releaseDateMs);

        MovieDetails details = new MovieDetails();
        details.setInCollection(true);
        details.setInWatchlist(true);
        details.tmdbMovie(movie);

        ContentValues values = details.toContentValuesInsert();
        if (lastUpdatedMs == null) {
            values.remove(Movies.LAST_UPDATED);
        } else {
            values.put(Movies.LAST_UPDATED, lastUpdatedMs);
        }
        resolver.insert(Movies.CONTENT_URI, values);
    }

    private void doUpdateAndAssertSuccess() {
        TmdbSync tmdbSync = new TmdbSync(ApplicationProvider.getApplicationContext(),
                tmdbConfigService, movieTools);
        boolean successful = tmdbSync.updateMovies(new SyncProgress());
        assertThat(successful).isTrue();
    }
}
