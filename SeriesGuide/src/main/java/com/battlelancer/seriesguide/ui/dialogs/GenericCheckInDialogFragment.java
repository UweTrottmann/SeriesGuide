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
import com.battlelancer.seriesguide.ui.FixGetGlueCheckInActivity;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.TraktTask;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.R;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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

    private static final String TAG_PROGRESS_FRAGMENT = "progress-dialog";

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

    private EditText mEditTextMessage;

    private View mButtonCheckIn;

    private View mProgressBar;

    private View mButtonPasteTitle;

    private View mButtonClear;

    private View mButtonFixGetGlue;

    public static void dismissProgressDialog(FragmentManager fragmentManager) {
        // dismiss a potential progress dialog
        Fragment prev = fragmentManager.findFragmentByTag(TAG_PROGRESS_FRAGMENT);
        if (prev != null) {
            FragmentTransaction ft = fragmentManager.beginTransaction();
            ft.remove(prev);
            ft.commit();
        }
    }

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

    public void onEvent(TraktTask.TraktActionCompleteEvent event) {
        if (event.mWasSuccessful) {
            dismissAllowingStateLoss();
            return;
        }

        // something went wrong, let the user try again
        setProgressLock(false);
    }

    public void onEvent(TraktTask.TraktCheckInBlockedEvent event) {
        // make sure we are still visible
        if (!isVisible()) {
            return;
        }
        // launch a check-in override dialog
        TraktCancelCheckinDialogFragment newFragment = TraktCancelCheckinDialogFragment
                .newInstance(event.traktTaskArgs, event.waitMinutes);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        newFragment.show(ft, "cancel-checkin-dialog");
    }

    protected void setupFixGetGlueButton(View layout, boolean isEnabled, final int tvdbId) {
        View divider = layout.findViewById(R.id.dividerHorizontalCheckIn);
        if (isEnabled) {
            mButtonFixGetGlue.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchFixGetGlueCheckInActivity(v, tvdbId);
                }
            });
        } else {
            mButtonFixGetGlue.setVisibility(View.GONE);
            divider.setVisibility(View.GONE);
        }
    }

    private void checkIn() {
        // connected?
        if (!AndroidUtils.isNetworkConnected(getActivity())) {
            Toast.makeText(getActivity(), R.string.offline, Toast.LENGTH_LONG).show();
            return;
        }

        // lock down UI
        setProgressLock(true);

        final String itemTitle = getArguments().getString(InitBundle.TITLE);
        final String message = mEditTextMessage.getText().toString();

        // try GetGlue check-in
        if (mGetGlueChecked) {
            boolean shouldAbort = checkInGetGlue(itemTitle, message);
            if (shouldAbort) {
                // something is missing, can't check in
                setProgressLock(false);
                return;
            }
        }

        // try trakt check-in
        if (mTraktChecked) {
            if (!TraktCredentials.get(getActivity()).hasCredentials()) {
                // not connected to trakt
                mCheckBoxTrakt.setChecked(false);
                mTraktChecked = false;
                setProgressLock(false);
                updateCheckInButtonState();
                return;
            }

            checkInTrakt(message);
            return;
        }

        // no trakt check-in? release UI and finish
        setProgressLock(false);
        dismiss();
    }

    /**
     * Start the GetGlue check-in task.
     *
     * @return Return whether the check-in should be aborted.
     */
    protected abstract boolean checkInGetGlue(final String title, final String comment);

    /**
     * Start the trakt check-in task.
     */
    protected abstract void checkInTrakt(String message);

    protected void updateCheckInButtonState() {
        if (mGetGlueChecked || mTraktChecked) {
            mButtonCheckIn.setEnabled(true);
        } else {
            mButtonCheckIn.setEnabled(false);
        }
    }

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

}
