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

package com.battlelancer.seriesguide.ui;

import com.actionbarsherlock.app.SherlockFragment;
import com.battlelancer.seriesguide.enums.TraktAction;
import com.battlelancer.seriesguide.enums.TraktStatus;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.ShareUtils.ProgressDialog;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.battlelancer.seriesguide.util.SimpleCrypto;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.Utils;
import com.jakewharton.trakt.Trakt;
import com.jakewharton.trakt.entities.Response;
import com.jakewharton.trakt.services.AccountService;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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

import retrofit.RetrofitError;

public class ConnectTraktCredentialsFragment extends SherlockFragment {

    private boolean isForwardingGivenTask;

    public static ConnectTraktCredentialsFragment newInstance(Bundle traktData) {
        ConnectTraktCredentialsFragment f = new ConnectTraktCredentialsFragment();
        f.setArguments(traktData);
        f.isForwardingGivenTask = true;
        return f;
    }

    public static ConnectTraktCredentialsFragment newInstance() {
        ConnectTraktCredentialsFragment f = new ConnectTraktCredentialsFragment();
        f.isForwardingGivenTask = false;
        return f;
    }

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), "Connect Trakt Credentials");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final Context context = getActivity().getApplicationContext();
        final View layout = inflater.inflate(R.layout.trakt_credentials_dialog, container, false);
        final FragmentManager fm = getFragmentManager();
        final Bundle args = getArguments();

        // restore the username from settings
        final String username = TraktSettings.getUsername(context);

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

                final String username = ((EditText) layout.findViewById(R.id.username)).getText()
                        .toString();
                final String passwordHash = Utils.toSHA1(context, ((EditText) layout
                        .findViewById(R.id.password)).getText().toString());
                final String email = ((EditText) layout.findViewById(R.id.email)).getText()
                        .toString();
                final boolean isNewAccount = ((CheckBox) layout.findViewById(R.id.checkNewAccount))
                        .isChecked();
                final String traktApiKey = getResources().getString(R.string.trakt_apikey);

                AsyncTask<String, Void, Response> accountValidatorTask
                        = new AsyncTask<String, Void, Response>() {

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
                        final Trakt manager = new Trakt();
                        manager.setApiKey(traktApiKey);
                        manager.setAuthentication(username, passwordHash);

                        Response response = null;

                        try {
                            if (isNewAccount) {
                                // create new account
                                response = manager.accountService().create(
                                        new AccountService.NewAccount(username, passwordHash,
                                                email));
                            } else {
                                // validate existing account
                                response = manager.accountService().test();
                            }
                        } catch (RetrofitError e) {
                            response = null;
                        }

                        return response;
                    }

                    @Override
                    protected void onPostExecute(Response response) {
                        progressbar.setVisibility(View.GONE);
                        connectbtn.setEnabled(true);

                        if (response == null) {
                            status.setText(R.string.trakt_error_credentials);
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
                            status.setText(R.string.trakt_error_credentials);
                            return;
                        }

                        // prepare writing credentials to settings
                        final Editor editor = PreferenceManager.getDefaultSharedPreferences(context)
                                .edit();
                        editor.putString(TraktSettings.KEY_USERNAME, username).putString(
                                TraktSettings.KEY_PASSWORD_SHA1_ENCR, passwordEncr);

                        if (response.status.equals(TraktStatus.SUCCESS)
                                && passwordEncr.length() != 0 && editor.commit()) {
                            // try setting new auth data for service manager
                            if (ServiceUtils.getTraktServiceManagerWithAuth(context, true)
                                    == null) {
                                status.setText(R.string.trakt_error_credentials);
                                return;
                            }

                            if (isForwardingGivenTask) {
                                // continue with original task
                                if (TraktAction.values()[args.getInt(ShareItems.TRAKTACTION)]
                                        == TraktAction.CHECKIN_EPISODE) {
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
                                        new TraktTask(context, args, null), new Void[]{
                                        null
                                });

                                FragmentActivity activity = getActivity();
                                if (activity != null) {
                                    activity.finish();
                                }
                            } else {
                                // show options after successful connection
                                FragmentManager fm = getFragmentManager();
                                if (fm != null) {
                                    ConnectTraktFinishedFragment f = new ConnectTraktFinishedFragment();
                                    FragmentTransaction ft = fm.beginTransaction();
                                    ft.replace(android.R.id.content, f);
                                    ft.commit();
                                }
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
                        ServiceUtils.clearTraktCredentials(context);
                        return null;
                    }
                }.execute();

                getActivity().finish();
            }
        });

        return layout;
    }
}
