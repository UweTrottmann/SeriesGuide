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
import com.battlelancer.seriesguide.enums.Result;
import com.battlelancer.seriesguide.getglueapi.GetGlueAuthActivity;
import com.battlelancer.seriesguide.getglueapi.GetGlueCheckin;
import com.battlelancer.seriesguide.settings.GetGlueSettings;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.ui.FixGetGlueCheckInActivity;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.battlelancer.seriesguide.R;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
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

import de.greenrobot.event.EventBus;

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

    protected CompoundButton mCheckBoxTrakt;

    protected CompoundButton mCheckBoxGetGlue;

    protected View mButtonFixGetGlue;

    private EditText mEditTextMessage;

    private View mButtonCheckIn;

    private View mProgressBar;

    private View mButtonPasteTitle;

    private View mButtonClear;

    private boolean mCheckInActiveGetGlue;

    private boolean mCheckInActiveTrakt;

    private boolean mCheckInSuccessGetGlue;

    private boolean mCheckInSuccessTrakt;

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
        final String defaultMessage = getArguments().getString(InitBundle.DEFAULT_MESSAGE);
        final String itemTitle = getArguments().getString(InitBundle.ITEM_TITLE);

        // get share service enabled settings
        mGetGlueChecked = GetGlueSettings.isSharingWithGetGlue(getSherlockActivity());
        mTraktChecked = TraktSettings.isSharingWithTrakt(getSherlockActivity());

        // Message box, set title as default comment
        mEditTextMessage = (EditText) layout.findViewById(R.id.editTextCheckInMessage);
        if (!TextUtils.isEmpty(defaultMessage)) {
            mEditTextMessage.setText(defaultMessage);
        }

        // Paste episode button
        mButtonPasteTitle = layout.findViewById(R.id.buttonCheckInPasteTitle);
        if (!TextUtils.isEmpty(itemTitle)) {
            mButtonPasteTitle.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int start = mEditTextMessage.getSelectionStart();
                    int end = mEditTextMessage.getSelectionEnd();
                    mEditTextMessage.getText().replace(Math.min(start, end), Math.max(start, end),
                            itemTitle, 0, itemTitle.length());
                }
            });
        }

        // Clear button
        mButtonClear = layout.findViewById(R.id.buttonCheckInClear);
        mButtonClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mEditTextMessage.setText(null);
            }
        });

        // GetGlue toggle
        mCheckBoxGetGlue = (CompoundButton) layout.findViewById(R.id.checkBoxCheckInGetGlue);
        mCheckBoxGetGlue.setChecked(mGetGlueChecked);
        mCheckBoxGetGlue.setOnCheckedChangeListener(new OnCheckedChangeListener() {
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
        mCheckBoxTrakt = (CompoundButton) layout.findViewById(R.id.checkBoxCheckInTrakt);
        mCheckBoxTrakt.setChecked(mTraktChecked);
        mCheckBoxTrakt.setOnCheckedChangeListener(new OnCheckedChangeListener() {
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
        mButtonCheckIn = layout.findViewById(R.id.buttonCheckIn);
        updateCheckInButtonState();
        mButtonCheckIn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                checkIn();
            }
        });

        // progress indicator
        mProgressBar = layout.findViewById(R.id.progressBarCheckIn);

        // fix getglue button
        mButtonFixGetGlue = layout.findViewById(R.id.buttonCheckInFixGetGlue);
        setupButtonFixGetGlue(layout);

        setProgressLock(false);

        return layout;
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        EventBus.getDefault().unregister(this);
    }

    public void onEvent(GetGlueCheckin.GetGlueCheckInTask.GetGlueCheckInCompleteEvent event) {
        mCheckInActiveGetGlue = false;
        mCheckInSuccessGetGlue = event.statusCode == Result.SUCCESS;
        updateViews();
    }

    public void onEvent(TraktTask.TraktActionCompleteEvent event) {
        mCheckInActiveTrakt = false;
        mCheckInSuccessTrakt = event.mWasSuccessful;
        updateViews();
    }

    private void updateViews() {
        if (mCheckInActiveGetGlue || mCheckInActiveTrakt) {
            return;
        }

        // done with checking in, unlock UI
        setProgressLock(false);

        if ((mGetGlueChecked && !mCheckInSuccessGetGlue)
                || (mTraktChecked && !mCheckInSuccessTrakt)) {
            return;
        }

        // all went well, dismiss ourselves
        dismissAllowingStateLoss();
    }

    public void onEvent(TraktTask.TraktCheckInBlockedEvent event) {
        // launch a check-in override dialog
        TraktCancelCheckinDialogFragment newFragment = TraktCancelCheckinDialogFragment
                .newInstance(event.traktTaskArgs, event.waitMinutes);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        newFragment.show(ft, "cancel-checkin-dialog");
    }

    private void checkIn() {
        // connected?
        if (!Utils.isConnected(getActivity(), true)) {
            return;
        }

        // lock down UI
        setProgressLock(true);

        // make sure we can check into...
        boolean canCheckIn = true;
        // ...GetGlue
        if (mGetGlueChecked) {
            if (!GetGlueSettings.isAuthenticated(getActivity())
                    || !setupCheckInGetGlue()) {
                mCheckBoxGetGlue.setChecked(false);
                mGetGlueChecked = false;
                canCheckIn = false;
            }
        }
        // ...trakt
        if (mTraktChecked && !TraktCredentials.get(getActivity()).hasCredentials()) {
            // not connected to trakt
            mCheckBoxTrakt.setChecked(false);
            mTraktChecked = false;
            canCheckIn = false;
        }

        if (!canCheckIn) {
            // abort
            setProgressLock(false);
            updateCheckInButtonState();
            return;
        }

        final String itemTitle = getArguments().getString(InitBundle.TITLE);
        final String message = mEditTextMessage.getText().toString();

        // try check-ins (at least one is true)
        if (mGetGlueChecked) {
            mCheckInActiveGetGlue = true;
            checkInGetGlue(itemTitle, message);
        }
        if (mTraktChecked) {
            mCheckInActiveTrakt = true;
            checkInTrakt(message);
        }
    }

    /**
     * Start the GetGlue check-in task.
     */
    protected abstract void checkInGetGlue(final String title, final String comment);

    /**
     * Start the trakt check-in task.
     */
    protected abstract void checkInTrakt(String message);

    protected abstract void handleGetGlueToggle(boolean isChecked);

    protected void ensureGetGlueAuthAndConnection() {
        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            Toast.makeText(getActivity(), R.string.offline, Toast.LENGTH_LONG).show();
            mCheckBoxGetGlue.setChecked(false);
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

    protected abstract void setupButtonFixGetGlue(View layout);

    /**
     * Perform additional setup and validation required before a GetGlue check-in can be attempted.
     * Valid auth is already covered, no need to check for it again.
     *
     * @return Whether it is save to go ahead and call {@link #checkInGetGlue}.
     */
    protected boolean setupCheckInGetGlue() {
        return true;
    }

    /**
     * Disables all interactive UI elements and shows a progress indicator.
     */
    private void setProgressLock(boolean isEnabled) {
        mProgressBar.setVisibility(isEnabled ? View.VISIBLE : View.GONE);
        mEditTextMessage.setEnabled(!isEnabled);
        mButtonCheckIn.setEnabled(!isEnabled);
        mCheckBoxTrakt.setEnabled(!isEnabled);
        mCheckBoxGetGlue.setEnabled(!isEnabled);
        mButtonPasteTitle.setEnabled(!isEnabled);
        mButtonClear.setEnabled(!isEnabled);
        mButtonFixGetGlue.setEnabled(!isEnabled);
    }

    /**
     * Enables the check-in button if either GetGlue or trakt are enabled.
     */
    private void updateCheckInButtonState() {
        if (mGetGlueChecked || mTraktChecked) {
            mButtonCheckIn.setEnabled(true);
        } else {
            mButtonCheckIn.setEnabled(false);
        }
    }

}
