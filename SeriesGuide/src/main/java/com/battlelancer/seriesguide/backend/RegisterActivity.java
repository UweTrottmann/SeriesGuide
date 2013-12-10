package com.battlelancer.seriesguide.backend;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.json.jackson.JacksonFactory;

import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.provider.SeriesContract;
import com.battlelancer.seriesguide.util.ShowTools;
import com.uwetrottmann.seriesguide.R;
import com.uwetrottmann.seriesguide.shows.Shows;
import com.uwetrottmann.seriesguide.shows.model.Show;
import com.uwetrottmann.seriesguide.shows.model.ShowList;

import android.accounts.AccountManager;
import android.app.Activity;
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
import android.widget.Toast;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Helps connecting a device to Hexagon: sign in via Google account, initial uploading of shows.
 */
public class RegisterActivity extends Activity {

    private static final int REQUEST_ACCOUNT_PICKER = 0;

    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1;

    private static final String TAG = "Hexagon";

    private GoogleAccountCredential mCredential;

    private Shows mShowsService;

    private Button mButtonUpload;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mCredential = GoogleAccountCredential.usingAudience(this, HexagonSettings.AUDIENCE);
        setAccountName(HexagonSettings.getAccountName(this));

        // build show service endpoint
        Shows.Builder builder = new Shows.Builder(
                AndroidHttp.newCompatibleTransport(), new JacksonFactory(), mCredential
        );
        mShowsService = CloudEndpointUtils.updateBuilder(builder).build();

        setupViews();
    }

    private void setupViews() {
        Button signInButton = (Button) findViewById(R.id.buttonRegisterSignIn);
        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSignedIn()) {
                    signOut();
                    Toast.makeText(RegisterActivity.this, "Signed out.", Toast.LENGTH_SHORT).show();
                } else {
                    signIn();
                }
            }
        });

        mButtonUpload = (Button) findViewById(R.id.buttonRegisterUpload);
        mButtonUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadShows();
            }
        });
        mButtonUpload.setEnabled(isSignedIn());
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGooglePlayServicesAvailable();
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
                        mButtonUpload.setEnabled(true);
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
        mCredential.setSelectedAccountName(null);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this)
                .edit();
        editor.putString(HexagonSettings.KEY_ACCOUNT_NAME, null);
        editor.commit();
        mButtonUpload.setEnabled(false);

        ShowTools.get(this).setShowsServiceAccountName(null);
    }

    private void uploadShows() {
        new ShowsUploadTask(this, mShowsService, mButtonUpload).execute();
    }

    private static class ShowsUploadTask extends AsyncTask<Void, Void, Void> {

        private final Context mContext;

        private Shows mShowsService;

        private View mButtonUpload;

        public ShowsUploadTask(Context context, Shows showsService, View buttonUpload) {
            mContext = context;
            mShowsService = showsService;
            mButtonUpload = buttonUpload;
        }

        @Override
        protected void onPreExecute() {
            mButtonUpload.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... params) {
            // get a list of all local shows
            List<Show> shows = getLocalShowsAsList();
            if (shows == null || shows.size() == 0) {
                return null;
            }
            ShowList showList = new ShowList();
            showList.setShows(shows);

            // upload shows
            try {
                ShowList savedShows = mShowsService.save(showList).execute();
            } catch (IOException e) {
                Log.w(TAG, e.getMessage(), e);
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
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
}
