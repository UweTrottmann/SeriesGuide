
package com.battlelancer.seriesguide.ui;

import com.google.analytics.tracking.android.EasyTracker;

import com.actionbarsherlock.app.ActionBar;
import com.battlelancer.seriesguide.adapters.GetGlueObjectAdapter;
import com.battlelancer.seriesguide.loaders.GetGlueObjectLoader;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.ShowTools;
import com.uwetrottmann.getglue.entities.GetGlueObject;
import com.uwetrottmann.seriesguide.R;

import android.content.Context;
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

import java.util.List;

/**
 * Displays a list of GetGlue search results to choose from which are used to provide object ids for
 * GetGlue check ins.
 */
public class FixGetGlueCheckInActivity extends BaseActivity implements
        LoaderManager.LoaderCallbacks<List<GetGlueObject>>, OnItemClickListener {

    public interface InitBundle {

        String SHOW_TVDB_ID = "showtvdbid";
    }

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
        setContentView(R.layout.activity_fix_get_glue);

        setTitle(R.string.checkin_fixgetglue);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);

        setupViews();

        // do not check for null, we want to crash if so
        mShowId = getIntent().getExtras().getString(InitBundle.SHOW_TVDB_ID);

        mAdapter = new GetGlueObjectAdapter(this);
        ListView list = (ListView) findViewById(R.id.listViewGetGlueResults);
        list.addHeaderView(mHeaderView, null, false);
        list.addFooterView(mFooterView, null, false);
        list.setAdapter(mAdapter);
        list.setOnItemClickListener(this);

        // query for show title
        final Cursor show = getContentResolver().query(Shows.buildShowUri(mShowId), new String[]{
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
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);
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

        final Context context = this;
        mSaveButton = findViewById(R.id.buttonSaveSelection);
        mSaveButton.setEnabled(false);
        mSaveButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // save new GetGlue object id
                CharSequence text = mSelectedValue.getText();
                ShowTools.get(context).storeGetGlueId(
                        Integer.valueOf(mShowId), text != null ? text.toString() : "");
                finish();
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // we have a header view, subtract one to get actual position
        GetGlueObject glueObject = mAdapter.getItem(position - 1);
        mSelectedValue.setText(glueObject.id);
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
    public void onLoaderReset(Loader<List<GetGlueObject>> loader) {
        mAdapter.setData(null);
    }
}
