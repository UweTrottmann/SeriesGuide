package com.battlelancer.seriesguide.ui.streams;

import android.app.Activity;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import com.uwetrottmann.trakt5.entities.UserSlug;
import com.uwetrottmann.trakt5.enums.HistoryType;
import com.uwetrottmann.trakt5.services.Users;
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
        Users traktUsers = SgApp.getServicesComponent(getContext()).traktUsers();
        return traktUsers.history(UserSlug.ME, HistoryType.MOVIES, 1, MAX_HISTORY_SIZE,
                null, null, null);
    }
}
