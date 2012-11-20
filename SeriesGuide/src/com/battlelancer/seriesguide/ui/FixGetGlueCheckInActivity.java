
package com.battlelancer.seriesguide.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.widget.ArrayAdapter;

import com.battlelancer.seriesguide.getglueapi.GetGlueXmlParser.GetGlueObject;
import com.battlelancer.seriesguide.loaders.GetGlueObjectLoader;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.uwetrottmann.seriesguide.R;

import java.util.List;

/**
 * Displays a list of GetGlue search results to choose from which are used to
 * provide object ids for GetGlue check ins.
 */
public class FixGetGlueCheckInActivity extends BaseActivity implements
        LoaderManager.LoaderCallbacks<List<GetGlueObject>> {

    public interface InitBundle {
        String SHOW_ID = "showid";
    }

    private ArrayAdapter<GetGlueObject> mAdapter;

    @Override
    protected void onCreate(Bundle args) {
        super.onCreate(args);
        setContentView(R.layout.activity_fix_get_glue);

        String showId = null;
        if (args != null) {
            showId = args.getString(InitBundle.SHOW_ID);
        }
        if (TextUtils.isEmpty(showId)) {
            finish();
            return;
        }

        // query for show title
        final Cursor show = getContentResolver().query(Shows.buildShowUri(showId), new String[] {
                Shows._ID, Shows.TITLE
        }, null, null, null);
        if (show != null) {
            if (show.moveToFirst()) {
                String query = show.getString(1);
                Bundle loaderArgs = new Bundle();
                loaderArgs.putString("query", query);

                mAdapter = new ArrayAdapter<GetGlueObject>(this, R.layout.getglue_item,
                        R.id.textViewGetGlueShowTitle);

                getSupportLoaderManager().initLoader(0, loaderArgs, this);
            }

            show.close();
        }

    }

    @Override
    public Loader<List<GetGlueObject>> onCreateLoader(int id, Bundle args) {
        String query = null;
        if (args != null) {
            query = args.getString(InitBundle.SHOW_ID);
        }
        return new GetGlueObjectLoader(query, this);
    }

    @Override
    public void onLoadFinished(Loader<List<GetGlueObject>> loader, List<GetGlueObject> data) {
        mAdapter.clear();
        if (data != null) {
            for (GetGlueObject tvShow : data) {
                mAdapter.add(tvShow);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<List<GetGlueObject>> laoder) {
        mAdapter.clear();
    }
}
