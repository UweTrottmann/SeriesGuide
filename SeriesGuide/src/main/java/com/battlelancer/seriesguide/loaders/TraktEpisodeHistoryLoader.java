package com.battlelancer.seriesguide.loaders;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.traktapi.SgTrakt;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.trakt5.entities.HistoryEntry;
import com.uwetrottmann.trakt5.services.Users;
import dagger.Lazy;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Loads the last few episodes watched on trakt.
 */
public class TraktEpisodeHistoryLoader
        extends GenericSimpleLoader<TraktEpisodeHistoryLoader.Result> {

    public static class Result {
        public List<HistoryEntry> results;
        public String emptyText;

        public Result(List<HistoryEntry> results, String emptyText) {
            this.results = results;
            this.emptyText = emptyText;
        }
    }

    @Inject Lazy<Users> traktUsers;

    public TraktEpisodeHistoryLoader(Activity activity) {
        super(activity);
        SgApp.from(activity).getServicesComponent().inject(this);
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
                SgTrakt.trackFailedRequest(getContext(), getAction(), response);
            }
        } catch (IOException e) {
            SgTrakt.trackFailedRequest(getContext(), getAction(), e);
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
        return TraktRecentEpisodeHistoryLoader.buildUserEpisodeHistoryCall(traktUsers.get());
    }

    private Result buildResultFailure() {
        return new Result(null, getContext().getString(R.string.api_error_generic,
                getContext().getString(R.string.trakt)));
    }

    private Result buildResultFailure(@StringRes int emptyTextResId) {
        return new Result(null, getContext().getString(emptyTextResId));
    }
}
