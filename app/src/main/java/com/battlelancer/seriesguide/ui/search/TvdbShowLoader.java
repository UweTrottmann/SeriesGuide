package com.battlelancer.seriesguide.ui.search;

import android.content.Context;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.dataliberation.model.Show;
import com.battlelancer.seriesguide.thetvdbapi.TvdbException;
import com.battlelancer.seriesguide.thetvdbapi.TvdbTools;
import com.battlelancer.seriesguide.util.DBUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import timber.log.Timber;

/**
 * Loads show details from TVDb.
 */
class TvdbShowLoader extends GenericSimpleLoader<TvdbShowLoader.Result> {

    static class Result {
        public Show show;
        public boolean isAdded;
        public boolean doesNotExist;
    }

    private final int showTvdbId;
    @NonNull private String language;

    TvdbShowLoader(Context context, int showTvdbId, @NonNull String language) {
        super(context);
        this.showTvdbId = showTvdbId;
        this.language = language;
    }

    @Override
    public Result loadInBackground() {
        Result result = new Result();

        result.isAdded = DBUtils.isShowExists(getContext(), showTvdbId);
        try {
            TvdbTools tvdbTools = SgApp.getServicesComponent(getContext()).tvdbTools();
            result.show = tvdbTools.getShowDetails(showTvdbId, language);
        } catch (TvdbException e) {
            Timber.e(e, "Downloading TVDb show failed");
            result.show = null;
            if (e.itemDoesNotExist()) {
                result.doesNotExist = true;
            }
        }

        return result;
    }
}
