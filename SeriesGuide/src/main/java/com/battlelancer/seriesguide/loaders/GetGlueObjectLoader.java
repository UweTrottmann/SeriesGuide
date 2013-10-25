
package com.battlelancer.seriesguide.loaders;

import android.content.Context;

import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import com.uwetrottmann.getglue.GetGlue;
import com.uwetrottmann.getglue.entities.GetGlueObject;
import com.uwetrottmann.getglue.entities.GetGlueObjects;

import java.util.List;

import retrofit.RetrofitError;

/**
 * Loads a list of TV shows using a search term against GetGlue's
 * glue/findObjects end point.
 *
 * @see <a
 * href="http://getglue.com/api#networkwide-methods">http://getglue.com/api#networkwide-methods</a>
 */
public class GetGlueObjectLoader extends GenericSimpleLoader<List<GetGlueObject>> {

    private static final String TAG = "GetGlueObjectLoader";
    private String mQuery;

    public GetGlueObjectLoader(String query, Context context) {
        super(context);
        mQuery = query;
    }

    @Override
    public List<GetGlueObject> loadInBackground() {
        GetGlue getglue = new GetGlue();

        try {
            GetGlueObjects results = getglue.searchService().searchTvShows(mQuery);
            return results.objects;
        } catch (RetrofitError e) {
            Utils.trackExceptionAndLog(TAG, e);
            return null;
        }
    }

}
