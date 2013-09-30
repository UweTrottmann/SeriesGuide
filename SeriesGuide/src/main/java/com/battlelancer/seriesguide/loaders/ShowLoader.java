
package com.battlelancer.seriesguide.loaders;

import android.content.Context;

import com.battlelancer.seriesguide.items.Series;
import com.battlelancer.seriesguide.util.DBUtils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;

/**
 * Get a {@link Series} object from the database.
 */
public class ShowLoader extends GenericSimpleLoader<Series> {

    private int mShowTvdbId;

    public ShowLoader(Context context, int showTvdbId) {
        super(context);
        mShowTvdbId = showTvdbId;
    }

    @Override
    public Series loadInBackground() {
        return DBUtils.getShow(getContext(), mShowTvdbId);
    }

}
