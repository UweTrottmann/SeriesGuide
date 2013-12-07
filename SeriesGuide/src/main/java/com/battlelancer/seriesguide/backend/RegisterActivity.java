package com.battlelancer.seriesguide.backend;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson.JacksonFactory;

import com.battlelancer.seriesguide.provider.SeriesContract;
import com.battlelancer.seriesguide.util.DBUtils;
import com.uwetrottmann.seriesguide.R;
import com.uwetrottmann.seriesguide.messageEndpoint.MessageEndpoint;
import com.uwetrottmann.seriesguide.messageEndpoint.model.CollectionResponseMessageData;
import com.uwetrottmann.seriesguide.messageEndpoint.model.MessageData;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.uwetrottmann.seriesguide.shows.Shows;
import com.uwetrottmann.seriesguide.shows.model.CollectionResponseShow;
import com.uwetrottmann.seriesguide.shows.model.Show;
import com.uwetrottmann.seriesguide.shows.model.ShowList;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * An activity that communicates with your App Engine backend via Cloud Endpoints.
 *
 * When the user hits the "Register" button, a message is sent to the backend (over endpoints)
 * indicating that the device would like to receive broadcast messages from it. Clicking "Register"
 * also has the effect of registering this device for Google Cloud Messaging (GCM). Whenever the
 * backend wants to broadcast a message, it does it via GCM, so that the device does not need to
 * keep polling the backend for messages.
 *
 * If you've generated an App Engine backend for an existing Android project, this activity will not
 * be hooked in to your main activity as yet. You can easily do so by adding the following lines to
 * your main activity:
 *
 * Intent intent = new Intent(this, RegisterActivity.class); startActivity(intent);
 *
 * To make the sample run, you need to set your PROJECT_NUMBER in GCMIntentService.java. If you're
 * going to be running a local version of the App Engine backend (using the DevAppServer), you'll
 * need to toggle the LOCAL_ANDROID_RUN flag in CloudEndpointUtils.java. See the javadoc in these
 * classes for more details.
 *
 * For a comprehensive walkthrough, check out the documentation at http://developers.google.com/eclipse/docs/cloud_endpoints
 */
public class RegisterActivity extends Activity {

    private static final int REQUEST_ACCOUNT_PICKER = 0;

    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1;

    private static final String TAG = "Hexagon";

    private GoogleAccountCredential mCredential;

    private Shows mShowsService;

    private Button mButtonDownload;

    private Button mButtonUpload;

    enum State {
        REGISTERED, REGISTERING, UNREGISTERED, UNREGISTERING
    }

    private State curState = State.UNREGISTERED;

    private OnTouchListener registerListener = null;

    private OnTouchListener unregisterListener = null;

    private MessageEndpoint messageEndpoint = null;

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

        /*
         * build the messaging endpoint so we can access old messages via an endpoint call
         */
        MessageEndpoint.Builder endpointBuilder = new MessageEndpoint.Builder(
                AndroidHttp.newCompatibleTransport(),
                new JacksonFactory(),
                new HttpRequestInitializer() {
                    public void initialize(HttpRequest httpRequest) {
                    }
                });

        messageEndpoint = CloudEndpointUtils.updateBuilder(endpointBuilder).build();

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

        mButtonDownload = (Button) findViewById(R.id.buttonRegisterDownload);
        mButtonDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadShows();
            }
        });
        mButtonDownload.setEnabled(isSignedIn());

        Button regButton = (Button) findViewById(R.id.regButton);

        registerListener = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        if (GCMIntentService.PROJECT_NUMBER == null
                                || GCMIntentService.PROJECT_NUMBER.length() == 0) {
                            showDialog("Unable to register for Google Cloud Messaging. "
                                    + "Your application's PROJECT_NUMBER field is unset! You can change "
                                    + "it in GCMIntentService.java");
                        } else {
                            updateState(State.REGISTERING);
                            try {
                                GCMIntentService.register(getApplicationContext());
                            } catch (Exception e) {
                                Log.e(RegisterActivity.class.getName(),
                                        "Exception received when attempting to register for Google Cloud "
                                                + "Messaging. Perhaps you need to set your virtual device's "
                                                + " target to Google APIs? "
                                                + "See https://developers.google.com/eclipse/docs/cloud_endpoints_android"
                                                + " for more information.", e);
                                showDialog("There was a problem when attempting to register for "
                                        + "Google Cloud Messaging. If you're running in the emulator, "
                                        + "is the target of your virtual device set to 'Google APIs?' "
                                        + "See the Android log for more details.");
                                updateState(State.UNREGISTERED);
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                    default:
                        return false;
                }
            }
        };

        unregisterListener = new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        updateState(State.UNREGISTERING);
                        GCMIntentService.unregister(getApplicationContext());
                        return true;
                    case MotionEvent.ACTION_UP:
                        return true;
                    default:
                        return false;
                }
            }
        };

        regButton.setOnTouchListener(registerListener);
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

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

    /*
     * If we are dealing with an intent generated by the GCMIntentService
     * class, then display the provided message.
     */
        if (intent.getBooleanExtra("gcmIntentServiceMessage", false)) {

            showDialog(intent.getStringExtra("message"));

            if (intent.getBooleanExtra("registrationMessage", false)) {

                if (intent.getBooleanExtra("error", false)) {
          /*
           * If we get a registration/unregistration-related error,
           * and we're in the process of registering, then we move
           * back to the unregistered state. If we're in the process
           * of unregistering, then we move back to the registered
           * state.
           */
                    if (curState == State.REGISTERING) {
                        updateState(State.UNREGISTERED);
                    } else {
                        updateState(State.REGISTERED);
                    }
                } else {
          /*
           * If we get a registration/unregistration-related success,
           * and we're in the process of registering, then we move to
           * the registered state. If we're in the process of
           * unregistering, the we move back to the unregistered
           * state.
           */
                    if (curState == State.REGISTERING) {
                        updateState(State.REGISTERED);
                    } else {
                        updateState(State.UNREGISTERED);
                    }
                }
            } else {
        /*
         * if we didn't get a registration/unregistration message then
         * go get the last 5 messages from app-engine
         */
                new QueryMessagesTask(this, messageEndpoint).execute();
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
    }

    private void updateState(State newState) {
        Button registerButton = (Button) findViewById(R.id.regButton);
        switch (newState) {
            case REGISTERED:
                registerButton.setText("Unregister");
                registerButton.setOnTouchListener(unregisterListener);
                registerButton.setEnabled(true);
                break;

            case REGISTERING:
                registerButton.setText("Registering...");
                registerButton.setEnabled(false);
                break;

            case UNREGISTERED:
                registerButton.setText("Register");
                registerButton.setOnTouchListener(registerListener);
                registerButton.setEnabled(true);
                break;

            case UNREGISTERING:
                registerButton.setText("Unregistering...");
                registerButton.setEnabled(false);
                break;
        }
        curState = newState;
    }

    private void downloadShows() {
        new ShowsDownloadTask(this, mShowsService, mButtonDownload).execute();
    }

    private static class ShowsDownloadTask extends AsyncTask<Void, Void, Void> {

        private final Context mContext;

        private final Shows mShowsService;

        private final View mButtonDownload;

        public ShowsDownloadTask(Context context, Shows showsService, View buttonDownload) {
            mContext = context;
            mShowsService = showsService;
            mButtonDownload = buttonDownload;
        }

        @Override
        protected void onPreExecute() {
            mButtonDownload.setEnabled(false);
        }

        @Override
        protected Void doInBackground(Void... params) {
            // download shows
            CollectionResponseShow remoteShows = null;
            try {
                remoteShows = mShowsService.list().execute();
            } catch (IOException e) {
                Log.w(TAG, e.getMessage(), e);
            }

            // abort if no response
            if (remoteShows == null) {
                return null;
            }

            // extract list of remote shows
            List<Show> shows = remoteShows.getItems();
            if (shows == null || shows.size() == 0) {
                return null;
            }

            // update all received shows, ContentProvider will ignore those not added locally
            ArrayList<ContentProviderOperation> batch = buildShowUpdateOps(shows);
            DBUtils.applyInSmallBatches(mContext, batch);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mButtonDownload.setEnabled(true);
        }

        private ArrayList<ContentProviderOperation> buildShowUpdateOps(List<Show> shows) {
            ArrayList<ContentProviderOperation> batch = new ArrayList<>();

            ContentValues values = new ContentValues();
            for (Show show : shows) {
                putSyncedShowPropertyValues(show, values);

                // build update op
                ContentProviderOperation op = ContentProviderOperation
                        .newUpdate(SeriesContract.Shows.buildShowUri(show.getTvdbId()))
                        .withValues(values).build();
                batch.add(op);

                // clean up for re-use
                values.clear();
            }

            return batch;
        }

        private void putSyncedShowPropertyValues(Show show, ContentValues values) {
            putPropertyValueIfNotNull(values, SeriesContract.Shows.FAVORITE, show.getIsFavorite());
            putPropertyValueIfNotNull(values, SeriesContract.Shows.HIDDEN, show.getIsHidden());
            putPropertyValueIfNotNull(values, SeriesContract.Shows.SYNCENABLED,
                    show.getIsSyncEnabled());
            putPropertyValueIfNotNull(values, SeriesContract.Shows.GETGLUEID, show.getGetGlueId());
        }

        private void putPropertyValueIfNotNull(ContentValues values, String key, Boolean value) {
            if (value != null) {
                values.put(key, value.booleanValue());
            }
        }

        private void putPropertyValueIfNotNull(ContentValues values, String key, String value) {
            if (value != null) {
                values.put(key, value);
            }
        }

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

    private void showDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        }).show();
    }

    /*
     * Need to run this in background so we don't hold up the UI thread,
     * this task will ask the App Engine backend for the last 5 messages
     * sent to it
     */
    private class QueryMessagesTask
            extends AsyncTask<Void, Void, CollectionResponseMessageData> {

        Exception exceptionThrown = null;

        MessageEndpoint messageEndpoint;

        public QueryMessagesTask(Activity activity, MessageEndpoint messageEndpoint) {
            this.messageEndpoint = messageEndpoint;
        }

        @Override
        protected CollectionResponseMessageData doInBackground(Void... params) {
            try {
                CollectionResponseMessageData messages =
                        messageEndpoint.listMessages().setLimit(5).execute();
                return messages;
            } catch (IOException e) {
                exceptionThrown = e;
                return null;
                //Handle exception in PostExecute
            }
        }

        protected void onPostExecute(CollectionResponseMessageData messages) {
            // Check if exception was thrown
            if (exceptionThrown != null) {
                Log.e(RegisterActivity.class.getName(),
                        "Exception when listing Messages", exceptionThrown);
                showDialog("Failed to retrieve the last 5 messages from " +
                        "the endpoint at " + messageEndpoint.getBaseUrl() +
                        ", check log for details");
            } else {
                TextView messageView = (TextView) findViewById(R.id.msgView);
                messageView.setText("Last 5 Messages read from " +
                        messageEndpoint.getBaseUrl() + ":\n");
                for (MessageData message : messages.getItems()) {
                    messageView.append(message.getMessage() + "\n");
                }
            }
        }
    }
}
