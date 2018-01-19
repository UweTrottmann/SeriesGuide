package com.battlelancer.seriesguide.ui.streams;

import android.app.Activity;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.loaders.TraktEpisodeHistoryLoader;
import com.battlelancer.seriesguide.ui.movies.TraktRecentMovieHistoryLoader;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import java.util.List;
import retrofit2.Call;

/**
 * Loads the last few movies watched on trakt.
 */
class TraktMovieHistoryLoader extends TraktEpisodeHistoryLoader {

    TraktMovieHistoryLoader(Activity activity) {
        super(activity);
    }

    @NonNull
    @Override
    protected String getAction() {
        return "get user movie history";
    }

    @Override
    protected int getEmptyText() {
        return R.string.user_movie_stream_empty;
    }

    @Override
    protected Call<List<HistoryEntry>> buildCall() {
        return TraktRecentMovieHistoryLoader.buildUserMovieHistoryCall(getContext());
    }
}
