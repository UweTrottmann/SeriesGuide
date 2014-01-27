/*
 * Copyright 2014 Uwe Trottmann
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
 */

package com.battlelancer.seriesguide.ui.dialogs;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.battlelancer.seriesguide.getglueapi.GetGlueAuthActivity;
import com.battlelancer.seriesguide.settings.GetGlueSettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.ui.ConnectTraktActivity;
import com.battlelancer.seriesguide.ui.FixGetGlueCheckInActivity;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.ShareUtils.ProgressDialog;
import com.battlelancer.seriesguide.util.TraktTask.OnTraktActionCompleteListener;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;

public abstract class GenericCheckInDialogFragment extends SherlockDialogFragment {

    public interface InitBundle {

        /**
         * Title of show or movie. <b>Required.</b>
         */
        String TITLE = "title";

        /**
         * Title of episode or movie. <b>Required.</b>
         */
        String ITEM_TITLE = "itemtitle";

        /**
         * Default check-in message.
         */
        String DEFAULT_MESSAGE = "message";

        /**
         * Movie IMDb id. <b>Required for movies.</b>
         */
        String MOVIE_IMDB_ID = "movieimdbid";

        /**
         * Show TVDb id. <b>Required for episodes.</b>
         */
        String SHOW_TVDB_ID = "showtvdbid";

        /**
         * Show GetGlue id. <b>Required for episodes.</b>
         */
        String SHOW_GETGLUE_ID = "showgetglueid";

        /**
         * Season number. <b>Required for episodes.</b>
         */
        String SEASON = "season";

        /**
         * Episode number. <b>Required for episodes.</b>
         */
        String EPISODE = "episode";
    }

    protected boolean mGetGlueChecked;

    protected boolean mTraktChecked;

    protected CompoundButton mToggleTraktButton;

    protected CompoundButton mToggleGetGlueButton;

    protected OnTraktActionCompleteListener mListener;

    private EditText mMessageBox;

    private View mCheckinButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide title
        if (SeriesGuidePreferences.THEME == R.style.AndroidTheme) {
            setStyle(STYLE_NO_TITLE, 0);
        } else {
            setStyle(STYLE_NO_TITLE, R.style.SeriesGuideTheme_Dialog_CheckIn);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View layout = inflater.inflate(R.layout.checkin_dialog, null);
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getSherlockActivity());

        // some required values
        final String title = getArguments().getString(InitBundle.TITLE);
        final String defaultMessage = getArguments().getString(InitBundle.DEFAULT_MESSAGE);
        final String itemTitle = getArguments().getString(InitBundle.ITEM_TITLE);

        // get share service enabled settings
        mGetGlueChecked = GetGlueSettings.isSharingWithGetGlue(getSherlockActivity());
        mTraktChecked = TraktSettings.isSharingWithTrakt(getSherlockActivity());

        // Message box, set title as default comment
        mMessageBox = (EditText) layout.findViewById(R.id.editTextCheckInMessage);
        if (!TextUtils.isEmpty(defaultMessage)) {
            mMessageBox.setText(defaultMessage);
        }

        // Paste episode button
        if (!TextUtils.isEmpty(itemTitle)) {
            layout.findViewById(R.id.buttonCheckInPasteTitle).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int start = mMessageBox.getSelectionStart();
                    int end = mMessageBox.getSelectionEnd();
                    mMessageBox.getText().replace(Math.min(start, end), Math.max(start, end),
                            itemTitle, 0, itemTitle.length());
                }
            });
        }

        // Clear button
        layout.findViewById(R.id.buttonCheckInClear).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mMessageBox.setText(null);
            }
        });

        // GetGlue toggle
        mToggleGetGlueButton = (CompoundButton) layout.findViewById(R.id.checkBoxCheckInGetGlue);
        mToggleGetGlueButton.setChecked(mGetGlueChecked);
        mToggleGetGlueButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                handleGetGlueToggle(isChecked);

                mGetGlueChecked = isChecked;
                prefs.edit().putBoolean(GetGlueSettings.KEY_SHARE_WITH_GETGLUE, isChecked)
                        .commit();
                updateCheckInButtonState();
            }
        });

        // Trakt toggle
        mToggleTraktButton = (CompoundButton) layout.findViewById(R.id.checkBoxCheckInTrakt);
        mToggleTraktButton.setChecked(mTraktChecked);
        mToggleTraktButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // ask the user for credentials if there are none
                    TraktCredentials.ensureCredentials(getActivity());
                }

                mTraktChecked = isChecked;
                prefs.edit().putBoolean(TraktSettings.KEY_SHARE_WITH_TRAKT, isChecked)
                        .commit();
                updateCheckInButtonState();
            }
        });

        // Checkin Button
        mCheckinButton = layout.findViewById(R.id.buttonCheckIn);
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
                    boolean shouldAbort = onGetGlueCheckin(title, message);
                    if (shouldAbort) {
                        return;
                    }
                }

                if (mTraktChecked) {
                    if (!TraktCredentials.get(getActivity()).hasCredentials()) {
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

                        onTraktCheckIn(message);
                    }
                }

                dismiss();
            }
        });

        return layout;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (OnTraktActionCompleteListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnTraktActionCompleteListener");
        }
    }

    protected void setupFixGetGlueButton(View layout, boolean isEnabled, final int tvdbId) {
        View fixButton = layout.findViewById(R.id.buttonCheckInFixGetGlue);
        View divider = layout.findViewById(R.id.dividerHorizontalCheckIn);
        if (isEnabled) {
            fixButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchFixGetGlueCheckInActivity(v, tvdbId);
                }
            });
        } else {
            fixButton.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
        }
    }

    /**
     * Start the GetGlue check-in task.
     *
     * @return Return whether the check-in should be aborted.
     */
    protected abstract boolean onGetGlueCheckin(final String title, final String comment);

    /**
     * Start the trakt check-in task.
     */
    protected abstract void onTraktCheckIn(String message);

    protected void updateCheckInButtonState() {
        if (mGetGlueChecked || mTraktChecked) {
            mCheckinButton.setEnabled(true);
        } else {
            mCheckinButton.setEnabled(false);
        }
    }

    protected abstract void handleGetGlueToggle(boolean isChecked);

    protected void ensureGetGlueAuthAndConnection() {
        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            Toast.makeText(getActivity(), R.string.offline, Toast.LENGTH_LONG).show();
            mToggleGetGlueButton.setChecked(false);
        } else {
            // authenticate already here
            Intent i = new Intent(getSherlockActivity(),
                    GetGlueAuthActivity.class);
            startActivity(i);
        }
    }

    protected void launchFixGetGlueCheckInActivity(View v, int showTvdbId) {
        Intent i = new Intent(getActivity(), FixGetGlueCheckInActivity.class);
        i.putExtra(FixGetGlueCheckInActivity.InitBundle.SHOW_TVDB_ID, String.valueOf(showTvdbId));
        ActivityCompat.startActivity(getActivity(), i,
                ActivityOptionsCompat.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight())
                        .toBundle());
    }

}
