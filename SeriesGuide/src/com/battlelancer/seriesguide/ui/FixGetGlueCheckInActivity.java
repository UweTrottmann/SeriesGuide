
package com.battlelancer.seriesguide.ui;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.TextView;

import com.battlelancer.seriesguide.adapters.GetGlueObjectAdapter;
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
        LoaderManager.LoaderCallbacks<List<GetGlueObject>>, OnItemClickListener {

    public interface InitBundle {
        String SHOW_ID = "showid";
    }

    private ListView mList;
    private GetGlueObjectAdapter mAdapter;
    private TextView mSelectedValue;

    @Override
    protected void onCreate(Bundle args) {
        super.onCreate(args);
        setContentView(R.layout.activity_fix_get_glue);

        setupViews();

        String showId = null;
        if (getIntent() != null) {
            showId = getIntent().getStringExtra(InitBundle.SHOW_ID);
        }
        if (TextUtils.isEmpty(showId)) {
            finish();
            return;
        }

        mAdapter = new GetGlueObjectAdapter(this);
        mList = (ListView) findViewById(R.id.listViewGetGlueResults);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);

        // query for show title
        final Cursor show = getContentResolver().query(Shows.buildShowUri(showId), new String[] {
                Shows._ID, Shows.TITLE
        }, null, null, null);
        if (show != null) {
            if (show.moveToFirst()) {
                String query = show.getString(1);
                Bundle loaderArgs = new Bundle();
                loaderArgs.putString("query", query);

                getSupportLoaderManager().initLoader(0, loaderArgs, this);
            }

            show.close();
        }

    }

    private void setupViews() {
        mSelectedValue = (TextView) findViewById(R.id.textViewSelectedShowValue);
        
        findViewById(R.id.buttonDiscard).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        findViewById(R.id.buttonSaveSelection).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO save magic
                finish();
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        GetGlueObject glueObject = mAdapter.getItem(position);
        mSelectedValue.setText(glueObject.key);
    }

    @Override
    public Loader<List<GetGlueObject>> onCreateLoader(int id, Bundle args) {
        String query = null;
        if (args != null) {
            query = args.getString("query");
        }
        return new GetGlueObjectLoader(query, this);
    }

    @Override
    public void onLoadFinished(Loader<List<GetGlueObject>> loader, List<GetGlueObject> data) {
        mAdapter.setData(data);
    }

    @Override
    public void onLoaderReset(Loader<List<GetGlueObject>> laoder) {
        mAdapter.setData(null);
    }
}
