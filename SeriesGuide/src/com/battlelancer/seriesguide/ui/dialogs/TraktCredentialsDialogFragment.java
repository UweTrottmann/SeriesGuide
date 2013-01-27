/*
 * Copyright 2012 Uwe Trottmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */

package com.battlelancer.seriesguide.ui.dialogs;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.TextView;

import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.enums.TraktStatus;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils.ProgressDialog;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.battlelancer.seriesguide.util.SimpleCrypto;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.jakewharton.apibuilder.ApiException;
import com.jakewharton.trakt.ServiceManager;
import com.jakewharton.trakt.TraktException;
import com.jakewharton.trakt.entities.Response;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

public class TraktCredentialsDialogFragment extends DialogFragment {

    private boolean isForwardingGivenTask;

    public static TraktCredentialsDialogFragment newInstance(Bundle traktData) {
        TraktCredentialsDialogFragment f = new TraktCredentialsDialogFragment();
        f.setArguments(traktData);
        f.isForwardingGivenTask = true;
        return f;
    }

    public static TraktCredentialsDialogFragment newInstance() {
        TraktCredentialsDialogFragment f = new TraktCredentialsDialogFragment();
        f.isForwardingGivenTask = false;
        return f;
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getTracker().trackView("Trakt Credentials Dialog");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide title, use custom theme
        if (SeriesGuidePreferences.THEME == R.style.ICSBaseTheme) {
            setStyle(STYLE_NO_TITLE, 0);
        } else {
            setStyle(STYLE_NO_TITLE, R.style.SeriesGuideTheme_Dialog);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Context context = getActivity().getApplicationContext();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final View layout = inflater.inflate(R.layout.trakt_credentials_dialog, null);
        final FragmentManager fm = getFragmentManager();
        final Bundle args = getArguments();

        // restore the username from settings
        final String username = prefs.getString(SeriesGuidePreferences.KEY_TRAKTUSER, "");

        // new account toggle
        final View mailviews = layout.findViewById(R.id.mailviews);
        mailviews.setVisibility(View.GONE);

        CheckBox newAccCheckBox = (CheckBox) layout.findViewById(R.id.checkNewAccount);
        newAccCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mailviews.setVisibility(View.VISIBLE);
                } else {
                    mailviews.setVisibility(View.GONE);
                }
            }
        });

        // status strip
        final TextView status = (TextView) layout.findViewById(R.id.status);
        final View progressbar = layout.findViewById(R.id.progressbar);
        final View progress = layout.findViewById(R.id.progress);
        progress.setVisibility(View.GONE);

        final Button connectbtn = (Button) layout.findViewById(R.id.connectbutton);
        final Button disconnectbtn = (Button) layout.findViewById(R.id.disconnectbutton);

        // enable buttons based on if there are saved credentials
        if (TextUtils.isEmpty(username)) {
            // user has to enable first
            disconnectbtn.setEnabled(false);
        } else {
            // make it obvious trakt is connected
            connectbtn.setEnabled(false);

            EditText usernameField = (EditText) layout.findViewById(R.id.username);
            usernameField.setEnabled(false);
            usernameField.setText(username);

            EditText passwordField = (EditText) layout.findViewById(R.id.password);
            passwordField.setEnabled(false);
            passwordField.setText("********"); // fake password

            newAccCheckBox.setEnabled(false);
        }

        connectbtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // prevent multiple instances
                connectbtn.setEnabled(false);
                disconnectbtn.setEnabled(false);

                // prevent user canceling the dialog
                setCancelable(false);

                final String username = ((EditText) layout.findViewById(R.id.username)).getText()
                        .toString();
                final String passwordHash = Utils.toSHA1(((EditText) layout
                        .findViewById(R.id.password)).getText().toString().getBytes());
                final String email = ((EditText) layout.findViewById(R.id.email)).getText()
                        .toString();
                final boolean isNewAccount = ((CheckBox) layout.findViewById(R.id.checkNewAccount))
                        .isChecked();
                final String traktApiKey = getResources().getString(R.string.trakt_apikey);

                AsyncTask<String, Void, Response> accountValidatorTask = new AsyncTask<String, Void, Response>() {

                    @Override
                    protected void onPreExecute() {
                        progress.setVisibility(View.VISIBLE);
                        progressbar.setVisibility(View.VISIBLE);
                        status.setText(R.string.waitplease);
                    }

                    @Override
                    protected Response doInBackground(String... params) {
                        // check if we have any usable data
                        if (username.length() == 0 || passwordHash == null) {
                            return null;
                        }

                        // check for connectivity
                        if (!AndroidUtils.isNetworkConnected(context)) {
                            Response r = new Response();
                            r.status = TraktStatus.FAILURE;
                            r.error = context.getString(R.string.offline);
                            return r;
                        }

                        // use a separate ServiceManager here to avoid
                        // setting wrong credentials
                        final ServiceManager manager = new ServiceManager();
                        manager.setApiKey(traktApiKey);
                        manager.setAuthentication(username, passwordHash);
                        manager.setUseSsl(true);

                        Response response = null;

                        try {
                            if (isNewAccount) {
                                // create new account
                                response = manager.accountService()
                                        .create(username, passwordHash, email).fire();
                            } else {
                                // validate existing account
                                response = manager.accountService().test().fire();
                            }
                        } catch (TraktException te) {
                            response = te.getResponse();
                        } catch (ApiException ae) {
                            response = null;
                        }

                        return response;
                    }

                    @Override
                    protected void onPostExecute(Response response) {
                        progressbar.setVisibility(View.GONE);
                        connectbtn.setEnabled(true);
                        setCancelable(true);

                        if (response == null) {
                            status.setText(R.string.trakt_generalerror);
                            return;
                        }
                        if (response.status.equals(TraktStatus.FAILURE)) {
                            status.setText(response.error);
                            return;
                        }

                        // try to encrypt the password before storing it
                        String passwordEncr = SimpleCrypto.encrypt(passwordHash, context);
                        if (passwordEncr == null) {
                            // password encryption failed
                            status.setText(R.string.trakt_generalerror);
                            return;
                        }

                        // prepare writing credentials to settings
                        Editor editor = prefs.edit();
                        editor.putString(SeriesGuidePreferences.KEY_TRAKTUSER, username).putString(
                                SeriesGuidePreferences.KEY_TRAKTPWD, passwordEncr);

                        if (response.status.equals(TraktStatus.SUCCESS)
                                && passwordEncr.length() != 0 && editor.commit()) {
                            // try setting new auth data for service manager
                            if (ServiceUtils.getTraktServiceManagerWithAuth(context, true) == null) {
                                status.setText(R.string.trakt_generalerror);
                                return;
                            }

                            // all went through
                            dismiss();

                            if (isForwardingGivenTask) {
                                if (TraktAction.values()[args.getInt(ShareItems.TRAKTACTION)] == TraktAction.CHECKIN_EPISODE) {
                                    FragmentTransaction ft = fm.beginTransaction();
                                    Fragment prev = fm.findFragmentByTag("progress-dialog");
                                    if (prev != null) {
                                        ft.remove(prev);
                                    }
                                    ProgressDialog newFragment = ProgressDialog.newInstance();
                                    newFragment.show(ft, "progress-dialog");
                                }

                                // relaunch the trakt task which called us
                                AndroidUtils.executeAsyncTask(
                                        new TraktTask(context, fm, args, null), new Void[] {
                                            null
                                        });
                            }
                        }
                    }
                };

                accountValidatorTask.execute();
            }
        });

        disconnectbtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // clear trakt credentials
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        clearTraktCredentials(prefs);

                        // force removing credentials from memory
                        ServiceManager manager = ServiceUtils.getTraktServiceManagerWithAuth(context, false);
                        if (manager != null) {
                            manager.setAuthentication(null, null);
                        }

                        return null;
                    }
                }.execute();

                dismiss();
            }
        });

        return layout;
    }

    public static void clearTraktCredentials(final SharedPreferences prefs) {
        Editor editor = prefs.edit();
        editor.putString(SeriesGuidePreferences.KEY_TRAKTUSER, "").putString(
                SeriesGuidePreferences.KEY_TRAKTPWD, "");
        editor.commit();
    }
}
