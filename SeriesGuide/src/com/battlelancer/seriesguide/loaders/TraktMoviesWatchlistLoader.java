
package com.battlelancer.seriesguide.loaders;

import android.content.Context;

import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Movie;

import java.util.List;

public class TraktMoviesWatchlistLoader extends GenericSimpleLoader<List<Movie>> {

    private static final String TAG = "TraktMoviesWatchlistLoader";

    public TraktMoviesWatchlistLoader(Context context) {
        super(context);
    }

    @Override
    public List<Movie> loadInBackground() {
        ServiceManager manager = ServiceUtils.getTraktServiceManagerWithAuth(getContext(), false);
        if (manager == null) {
            return null;
        }

        try {
            return manager.userService()
                    .watchlistMovies(ServiceUtils.getTraktUsername(getContext()))
                    .fire();
        } catch (TraktException e) {
            Utils.trackExceptionAndLog(getContext(), TAG, e);
        } catch (ApiException e) {
            Utils.trackExceptionAndLog(getContext(), TAG, e);
        }

        return null;
    }
}
