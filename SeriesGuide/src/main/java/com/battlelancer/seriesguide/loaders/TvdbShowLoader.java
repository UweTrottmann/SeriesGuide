package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.settings.DisplaySettings;
import com.battlelancer.seriesguide.thetvdbapi.TheTVDB;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.battlelancer.seriesguide.util.DBUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import timber.log.Timber;

/**
 * Loads show details from TVDb.
 */
public class TvdbShowLoader extends GenericSimpleLoader<TvdbShowLoader.Result> {

    public static class Result {
        public Show show;
        public boolean isAdded;
    }

    private final int showTvdbId;
    private String language;

    public TvdbShowLoader(@NonNull Context context, int showTvdbId, @Nullable String language) {
        super(context);
        this.showTvdbId = showTvdbId;
        this.language = language;
    }

    @Override
    public Result loadInBackground() {
        Result result = new Result();

        result.isAdded = DBUtils.isShowExists(getContext(), showTvdbId);
        try {
            if (TextUtils.isEmpty(language)) {
                // fall back to user preferred language
                language = DisplaySettings.getContentLanguage(getContext());
            }
            result.show = TheTVDB.getShowDetails(getContext(), showTvdbId, language);
        } catch (TvdbException e) {
            Timber.e(e, "Downloading TVDb show failed");
            result.show = null;
        }

        return result;
    }
}
