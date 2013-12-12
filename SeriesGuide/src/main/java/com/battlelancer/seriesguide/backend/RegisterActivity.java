package com.battlelancer.seriesguide.backend;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.provider.SeriesContract;
import com.battlelancer.seriesguide.ui.BaseNavDrawerActivity;
import com.battlelancer.seriesguide.ui.BaseTopActivity;
import com.battlelancer.seriesguide.util.ShowTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;
import com.uwetrottmann.seriesguide.shows.model.Show;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

/**
 * Helps connecting a device to Hexagon: sign in via Google account, initial uploading of shows.
 */
public class RegisterActivity extends BaseTopActivity {

    public static final String TAG = "Hexagon";

    private static final int REQUEST_ACCOUNT_PICKER = 0;

    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1;

    private GoogleAccountCredential mCredential;

    private Button mButtonAction;

    private TextView mTextViewDescription;

    private ProgressBar mProgressBar;

    private RadioGroup mRadioGroupPriority;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        setupNavDrawer();

        mCredential = GoogleAccountCredential.usingAudience(this, HexagonSettings.AUDIENCE);
        setAccountName(HexagonSettings.getAccountName(this));

        setupViews();
    }

    private void setupViews() {
        mButtonAction = (Button) findViewById(R.id.buttonRegisterAction);
        mTextViewDescription = (TextView) findViewById(R.id.textViewRegisterDescription);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBarRegister);
        mRadioGroupPriority = (RadioGroup) findViewById(R.id.radioGroupRegisterPriority);

        updateViewsStates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGooglePlayServicesAvailable();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setDrawerSelectedItem(BaseNavDrawerActivity.MENU_ITEM_CLOUD_POSITION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER: {
                if (data != null && data.getExtras() != null) {
                    String accountName = data.getExtras()
                            .getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (!TextUtils.isEmpty(accountName)) {
                        storeAccountName(accountName);
                        setAccountName(accountName);
                        if (isSignedIn()) {
                            setupHexagon();
                        } else {
                            updateViewsStates();
                        }
                    }
                }
                break;
            }
        }
    }

    /**
     * Ensure Google Play Services is up to date, if not help the user update it.
     */
    private void checkGooglePlayServicesAvailable() {
        final int connectionStatusCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            GooglePlayServicesUtil
                    .getErrorDialog(connectionStatusCode, this, REQUEST_GOOGLE_PLAY_SERVICES)
                    .show();
        } else if (connectionStatusCode != ConnectionResult.SUCCESS) {
            Log.i(TAG, "This device is not supported.");
            finish();
        }
    }

    private boolean isSignedIn() {
        return mCredential.getSelectedAccountName() != null;
    }

    private void setAccountName(String accountName) {
        mCredential.setSelectedAccountName(accountName);
    }

    private void setupHexagon() {
        // set setup incomplete flag
        PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean(HexagonSettings.KEY_SETUP_COMPLETED, false).commit();

        mProgressBar.setVisibility(View.VISIBLE);

        // TODO
    }

    private void storeAccountName(String accountName) {
        // store account name in settings
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
                .edit();
        editor.putString(HexagonSettings.KEY_ACCOUNT_NAME, accountName);
        editor.commit();

        ShowTools.get(this).setShowsServiceAccountName(accountName);
    }

    private void signIn() {
        // launch account picker
        startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    private void signOut() {
        // remove account name from settings
        storeAccountName(null);
        setAccountName(null);

        updateViewsStates();
    }

    private void updateViewsStates() {
        if (isSignedIn()) {
            if (HexagonSettings.hasCompletedSetup(this)) {
                mButtonAction.setText(R.string.hexagon_signout);
                mButtonAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        signOut();
                    }
                });
                mTextViewDescription.setText(R.string.hexagon_signed_in);
                mProgressBar.setVisibility(View.GONE);
                mRadioGroupPriority.setVisibility(View.GONE);
            } else {
                mButtonAction.setText(R.string.hexagon_setup_complete);
                mButtonAction.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setupHexagon();
                    }
                });
                mTextViewDescription.setText(R.string.hexagon_setup_incomplete);
                mProgressBar.setVisibility(View.GONE);
                mRadioGroupPriority.setVisibility(View.GONE);
            }
        } else {
            // not signed in
            mButtonAction.setText(R.string.hexagon_signin);
            mButtonAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signIn();
                }
            });
            mTextViewDescription.setText(R.string.hexagon_description);
            mProgressBar.setVisibility(View.GONE);
            mRadioGroupPriority.setVisibility(View.GONE);
        }
    }

    /**
     * Uploads new shows to Hexagon, offers to upload all of them (overwriting existing shows).
     */
    private void doInitialUpload() {
        new InitialUploadTask(this, mButtonAction).execute();
    }

    private static class InitialUploadTask extends AsyncTask<Void, Void, List<Show>> {

        private final Context mContext;

        private View mButtonUpload;

        public InitialUploadTask(Context context, View buttonUpload) {
            mContext = context;
            mButtonUpload = buttonUpload;
        }

        @Override
        protected void onPreExecute() {
            mButtonUpload.setEnabled(false);
        }

        @Override
        protected List<Show> doInBackground(Void... params) {
            // TODO existing show detection, upload only new ones, offer to upload all

            // upload all local shows
            ShowTools.Upload.showsAllLocal(mContext);

            return null;
        }

        @Override
        protected void onPostExecute(List<Show> result) {
            mButtonUpload.setEnabled(true);
        }

        private List<Show> getLocalShowsAsList() {
            List<Show> shows = new LinkedList<>();

            Cursor query = mContext.getContentResolver()
                    .query(SeriesContract.Shows.CONTENT_URI, new String[]{
                            SeriesContract.Shows._ID, SeriesContract.Shows.FAVORITE,
                            SeriesContract.Shows.HIDDEN, SeriesContract.Shows.GETGLUEID,
                            SeriesContract.Shows.SYNCENABLED
                    }, null, null, null);
            if (query == null) {
                return null;
            }

            while (query.moveToNext()) {
                Show show = new Show();
                show.setTvdbId(query.getInt(0));
                show.setIsFavorite(query.getInt(1) == 1);
                show.setIsHidden(query.getInt(2) == 1);
                show.setGetGlueId(query.getString(3));
                show.setIsSyncEnabled(query.getInt(4) == 1);
                shows.add(show);
            }

            query.close();

            return shows;
        }
    }

    @Override
    protected void fireTrackerEvent(String label) {
        Utils.trackAction(this, TAG, label);
    }
}
