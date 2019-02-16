package com.battlelancer.seriesguide.ui.streams;

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.battlelancer.seriesguide.traktapi.TraktCredentials;
import com.battlelancer.seriesguide.util.Errors;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import com.uwetrottmann.trakt5.entities.UserSlug;
import com.uwetrottmann.trakt5.enums.HistoryType;
import com.uwetrottmann.trakt5.services.Users;
import java.util.List;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Loads the last few episodes watched on trakt.
 */
class TraktEpisodeHistoryLoader extends GenericSimpleLoader<TraktEpisodeHistoryLoader.Result> {

    protected static final int MAX_HISTORY_SIZE = 50;

    static class Result {
        public List<HistoryEntry> results;
        public String emptyText;

        public Result(List<HistoryEntry> results, String emptyText) {
            this.results = results;
            this.emptyText = emptyText;
        }
    }

    TraktEpisodeHistoryLoader(Activity activity) {
        super(activity);
    }

    @Override
    public Result loadInBackground() {
        if (!TraktCredentials.get(getContext()).hasCredentials()) {
            return buildResultFailure(R.string.trakt_error_credentials);
        }

        List<HistoryEntry> history = null;
        try {
            Response<List<HistoryEntry>> response = buildCall().execute();
            if (response.isSuccessful()) {
                history = response.body();
            } else {
                if (SgTrakt.isUnauthorized(getContext(), response)) {
                    return buildResultFailure(R.string.trakt_error_credentials);
                }
                Errors.logAndReport(getAction(), response);
            }
        } catch (Exception e) {
            Errors.logAndReport(getAction(), e);
            return AndroidUtils.isNetworkConnected(getContext())
                    ? buildResultFailure() : buildResultFailure(R.string.offline);
        }

        if (history == null) {
            return buildResultFailure();
        } else {
            return new Result(history, getContext().getString(getEmptyText()));
        }
    }

    @NonNull
    protected String getAction() {
        return "get user episode history";
    }

    @StringRes
    protected int getEmptyText() {
        return R.string.user_stream_empty;
    }

    protected Call<List<HistoryEntry>> buildCall() {
        Users traktUsers = SgApp.getServicesComponent(getContext()).traktUsers();
        return traktUsers.history(UserSlug.ME, HistoryType.EPISODES, 1, MAX_HISTORY_SIZE,
                null, null, null);
    }

    private Result buildResultFailure() {
        return new Result(null, getContext().getString(R.string.api_error_generic,
                getContext().getString(R.string.trakt)));
    }

    private Result buildResultFailure(@StringRes int emptyTextResId) {
        return new Result(null, getContext().getString(emptyTextResId));
    }
}
