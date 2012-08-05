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

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.battlelancer.seriesguide.beta.R;
import com.battlelancer.seriesguide.getglueapi.GetGlue;
import com.battlelancer.seriesguide.getglueapi.GetGlue.CheckInTask;
import com.battlelancer.seriesguide.getglueapi.PrepareRequestTokenActivity;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.ShareUtils.ProgressDialog;
import com.battlelancer.seriesguide.util.ShareUtils.ShareItems;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.Utils;
import com.google.analytics.tracking.android.EasyTracker;
import com.uwetrottmann.androidutils.AndroidUtils;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

public class CheckInDialogFragment extends SherlockDialogFragment {

    public static CheckInDialogFragment newInstance(String imdbid, int tvdbid, int season,
            int episode, String defaultMessage) {
        CheckInDialogFragment f = new CheckInDialogFragment();
        Bundle args = new Bundle();
        args.putString(ShareItems.IMDBID, imdbid);
        args.putInt(ShareItems.TVDBID, tvdbid);
        args.putInt(ShareItems.SEASON, season);
        args.putInt(ShareItems.EPISODE, episode);
        args.putString(ShareItems.SHARESTRING, defaultMessage);
        f.setArguments(args);
        return f;
    }

    protected boolean mGetGlueChecked;

    protected boolean mTraktChecked;

    private CompoundButton mToggleTraktButton;

    private CompoundButton mToggleGetGlueButton;

    private EditText mMessageBox;

    private View mCheckinButton;

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getTracker().trackView("Check In Dialog");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().setTitle(R.string.checkin);
        final View layout = inflater.inflate(R.layout.checkin_dialog, null);
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getSherlockActivity());

        // some required values
        final String imdbid = getArguments().getString(ShareItems.IMDBID);
        final int tvdbid = getArguments().getInt(ShareItems.TVDBID);
        final int season = getArguments().getInt(ShareItems.SEASON);
        final int episode = getArguments().getInt(ShareItems.EPISODE);
        final String defaultMessage = getArguments().getString(ShareItems.SHARESTRING);

        // get share service enabled settings
        mGetGlueChecked = prefs.getBoolean(SeriesGuidePreferences.KEY_SHAREWITHGETGLUE, false);
        mTraktChecked = prefs.getBoolean(SeriesGuidePreferences.KEY_SHAREWITHTRAKT, false);

        // Message box
        mMessageBox = (EditText) layout.findViewById(R.id.message);

        // Paste episode button
        layout.findViewById(R.id.pasteEpisode).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int start = mMessageBox.getSelectionStart();
                int end = mMessageBox.getSelectionEnd();
                mMessageBox.getText().replace(Math.min(start, end), Math.max(start, end),
                        defaultMessage, 0, defaultMessage.length());
            }
        });

        // Clear button
        layout.findViewById(R.id.textViewClear).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMessageBox.setText(null);
            }
        });

        // GetGlue toggle
        mToggleGetGlueButton = (CompoundButton) layout.findViewById(R.id.toggleGetGlue);
        mToggleGetGlueButton.setChecked(mGetGlueChecked);
        mToggleGetGlueButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!GetGlue.isAuthenticated(prefs)) {
                        if (!AndroidUtils.isNetworkConnected(getActivity())) {
                            Toast.makeText(getActivity(), R.string.offline, Toast.LENGTH_LONG)
                                    .show();
                            buttonView.setChecked(false);
                            return;
                        } else {
                            // authenticate already here
                            Intent i = new Intent(getSherlockActivity(),
                                    PrepareRequestTokenActivity.class);
                            startActivity(i);
                        }
                    }
                }

                mGetGlueChecked = isChecked;
                prefs.edit().putBoolean(SeriesGuidePreferences.KEY_SHAREWITHGETGLUE, isChecked)
                        .commit();
                updateCheckInButtonState();
            }
        });

        // Trakt toggle
        mToggleTraktButton = (CompoundButton) layout.findViewById(R.id.toggleTrakt);
        mToggleTraktButton.setChecked(mTraktChecked);
        mToggleTraktButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if (!Utils.isTraktCredentialsValid(getSherlockActivity())) {
                        // authenticate already here
                        TraktCredentialsDialogFragment newFragment = TraktCredentialsDialogFragment
                                .newInstance();
                        newFragment.show(getFragmentManager(), "traktdialog");
                    }
                }

                mTraktChecked = isChecked;
                prefs.edit().putBoolean(SeriesGuidePreferences.KEY_SHAREWITHTRAKT, isChecked)
                        .commit();
                updateCheckInButtonState();
            }
        });

        // Checkin Button
        mCheckinButton = layout.findViewById(R.id.checkinButton);
        updateCheckInButtonState();
        mCheckinButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!AndroidUtils.isNetworkConnected(getActivity())) {
                    Toast.makeText(getActivity(), R.string.offline, Toast.LENGTH_LONG).show();
                    return;
                }

                final String message = mMessageBox.getText().toString();

                if (mGetGlueChecked) {
                    if (!GetGlue.isAuthenticated(prefs) || imdbid.length() == 0) {
                        // cancel if required auth data is missing
                        mToggleGetGlueButton.setChecked(false);
                        mGetGlueChecked = false;
                        updateCheckInButtonState();
                        return;
                    } else {
                        // check in
                        new CheckInTask(imdbid, message, getActivity()).execute();
                    }
                }

                if (mTraktChecked) {
                    if (!Utils.isTraktCredentialsValid(getActivity())) {
                        // cancel if required auth data is missing
                        mToggleTraktButton.setChecked(false);
                        mTraktChecked = false;
                        updateCheckInButtonState();
                        return;
                    } else {
                        // check in

                        // We want to remove any currently showing
                        // dialog, so make our own transaction and
                        // take care of that here.
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        Fragment prev = getFragmentManager().findFragmentByTag("progress-dialog");
                        if (prev != null) {
                            ft.remove(prev);
                        }
                        ProgressDialog newFragment = ProgressDialog.newInstance();
                        newFragment.show(ft, "progress-dialog");

                        // start the trakt check in task
                        AndroidUtils.executeAsyncTask(new TraktTask(getActivity(),
                                getFragmentManager(), null).checkin(tvdbid, season, episode,
                                message), new Void[] {
                            null
                        });
                    }
                }

                dismiss();
            }
        });

        return layout;
    }

    private void updateCheckInButtonState() {
        if (mGetGlueChecked || mTraktChecked) {
            mCheckinButton.setEnabled(true);
        } else {
            mCheckinButton.setEnabled(false);
        }
    }

}
