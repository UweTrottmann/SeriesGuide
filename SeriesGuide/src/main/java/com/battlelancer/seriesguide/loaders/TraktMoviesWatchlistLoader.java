
package com.battlelancer.seriesguide.loaders;

import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Movie;
import com.uwetrottmann.androidutils.GenericSimpleLoader;

import android.content.Context;

import java.util.List;

import retrofit.RetrofitError;

public class TraktMoviesWatchlistLoader extends GenericSimpleLoader<List<Movie>> {

    private static final String TAG = "TraktMoviesWatchlistLoader";

    public TraktMoviesWatchlistLoader(Context context) {
        super(context);
    }

    @Override
    public List<Movie> loadInBackground() {
        Trakt manager = ServiceUtils.getTraktServiceManagerWithAuth(getContext(), false);
        if (manager == null) {
            return null;
        }

        try {
            return manager.userService()
                    .watchlistMovies(TraktSettings.getUsername(getContext()));
        } catch (RetrofitError e) {
            Utils.trackExceptionAndLog(getContext(), TAG, e);
        }

        return null;
    }
}
