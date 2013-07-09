
package com.battlelancer.seriesguide.ui;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.battlelancer.seriesguide.adapters.GetGlueObjectAdapter;
import com.battlelancer.seriesguide.getglueapi.GetGlueXmlParser.GetGlueObject;
import com.battlelancer.seriesguide.loaders.GetGlueObjectLoader;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.google.analytics.tracking.android.EasyTracker;
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
    private View mHeaderView;
    private EditText mSearchBox;
    private View mFooterView;
    private String mShowId;
    private View mSaveButton;

    @Override
    protected void onCreate(Bundle args) {
        super.onCreate(args);
        getMenu().setContentView(R.layout.activity_fix_get_glue);

        setTitle(R.string.checkin_fixgetglue);
        
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);

        setupViews();

        mShowId = null;
        if (getIntent() != null) {
            mShowId = getIntent().getStringExtra(InitBundle.SHOW_ID);
        }
        if (TextUtils.isEmpty(mShowId)) {
            finish();
            return;
        }

        mAdapter = new GetGlueObjectAdapter(this);
        mList = (ListView) findViewById(R.id.listViewGetGlueResults);
        mList.addHeaderView(mHeaderView, null, false);
        mList.addFooterView(mFooterView, null, false);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(this);

        // query for show title
        final Cursor show = getContentResolver().query(Shows.buildShowUri(mShowId), new String[] {
                Shows._ID, Shows.TITLE, Shows.GETGLUEID
        }, null, null, null);
        if (show != null) {
            if (show.moveToFirst()) {
                String glueId = show.getString(2);
                if (!TextUtils.isEmpty(glueId)) {
                    mSelectedValue.setText(glueId);
                }

                String query = show.getString(1);

                Bundle loaderArgs = new Bundle();
                loaderArgs.putString("query", query);

                getSupportLoaderManager().initLoader(0, loaderArgs, this);
            }

            show.close();
        }

    }
    
    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }

    private void setupViews() {
        mHeaderView = getLayoutInflater().inflate(R.layout.getglue_header, null);
        mSelectedValue = (TextView) mHeaderView.findViewById(R.id.textViewSelectedShowValue);

        mFooterView = getLayoutInflater().inflate(R.layout.getglue_footer, null);
        mSearchBox = (EditText) mFooterView.findViewById(R.id.editTextGetGlueSearch);
        mSearchBox.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                // we only want to react to down events
                if (event.getAction() != KeyEvent.ACTION_DOWN) {
                    return false;
                }

                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    onSearch(mSearchBox.getText().toString());
                    return true;
                } else {
                    return false;
                }
            }
        });
        mFooterView.findViewById(R.id.buttonShowSearch).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setVisibility(View.GONE);
                mSearchBox.setVisibility(View.VISIBLE);
                mSearchBox.requestFocus();
                // TODO animate
            }
        });

        findViewById(R.id.buttonDiscard).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mSaveButton = findViewById(R.id.buttonSaveSelection);
        mSaveButton.setEnabled(false);
        mSaveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // save new GetGlue object id
                ContentValues values = new ContentValues();
                values.put(Shows.GETGLUEID, mSelectedValue.getText().toString());
                getContentResolver().update(Shows.buildShowUri(mShowId), values, null, null);
                finish();
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // we have a header view, subtract one to get actual position
        GetGlueObject glueObject = mAdapter.getItem(position - 1);
        mSelectedValue.setText(glueObject.key);
        mSaveButton.setEnabled(true);
    }

    private void onSearch(String query) {
        Bundle loaderArgs = new Bundle();
        loaderArgs.putString("query", query);

        getSupportLoaderManager().restartLoader(0, loaderArgs, this);
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
