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

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.settings.TraktCredentials;
import com.battlelancer.seriesguide.settings.TraktSettings;
import com.battlelancer.seriesguide.ui.SeriesGuidePreferences;
import com.battlelancer.seriesguide.util.TraktTask;
import com.battlelancer.seriesguide.util.Utils;
import de.greenrobot.event.EventBus;

public abstract class GenericCheckInDialogFragment extends DialogFragment {

    public interface InitBundle {

        /**
         * Title of episode or movie. <b>Required.</b>
         */
        String ITEM_TITLE = "itemtitle";

        /**
         * Movie TMDb id. <b>Required for movies.</b>
         */
        String MOVIE_TMDB_ID = "movietmdbid";

        /**
         * Season number. <b>Required for episodes.</b>
         */
        String EPISODE_TVDB_ID = "episodetvdbid";
    }

    public class CheckInDialogDismissedEvent {
    }

    @BindView(R.id.editTextCheckInMessage) EditText editTextMessage;
    @BindView(R.id.buttonCheckIn) View buttonCheckIn;
    @BindView(R.id.buttonCheckInPasteTitle) View buttonPasteTitle;
    @BindView(R.id.buttonCheckInClear) View buttonClear;
    @BindView(R.id.progressBarCheckIn) View progressBar;

    private Unbinder unbinder;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // hide title, use special theme with exit animation
        if (SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_DarkBlue) {
            setStyle(STYLE_NO_TITLE, R.style.Theme_SeriesGuide_DarkBlue_Dialog_CheckIn);
        } else if (SeriesGuidePreferences.THEME == R.style.Theme_SeriesGuide_Light) {
            setStyle(STYLE_NO_TITLE, R.style.Theme_SeriesGuide_Light_Dialog_CheckIn);
        } else {
            setStyle(STYLE_NO_TITLE, R.style.Theme_SeriesGuide_Dialog_CheckIn);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.dialog_checkin, container, false);
        unbinder = ButterKnife.bind(this, v);

        // Paste episode button
        final String itemTitle = getArguments().getString(InitBundle.ITEM_TITLE);
        if (!TextUtils.isEmpty(itemTitle)) {
            buttonPasteTitle.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    int start = editTextMessage.getSelectionStart();
                    int end = editTextMessage.getSelectionEnd();
                    editTextMessage.getText().replace(Math.min(start, end), Math.max(start, end),
                            itemTitle, 0, itemTitle.length());
                }
            });
        }

        // Clear button
        buttonClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                editTextMessage.setText(null);
            }
        });

        // Checkin Button
        buttonCheckIn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                checkIn();
            }
        });

        setProgressLock(false);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // immediately start to check-in if the user has opted to skip entering a check-in message
        if (TraktSettings.useQuickCheckin(getContext())) {
            checkIn();
        }
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

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        EventBus.getDefault().post(new CheckInDialogDismissedEvent());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        unbinder.unbind();
    }

    public void onEvent(TraktTask.TraktActionCompleteEvent event) {
        // done with checking in, unlock UI
        setProgressLock(false);

        if (event.mWasSuccessful) {
            // all went well, dismiss ourselves
            dismissAllowingStateLoss();
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(TraktTask.TraktCheckInBlockedEvent event) {
        // launch a check-in override dialog
        TraktCancelCheckinDialogFragment newFragment = TraktCancelCheckinDialogFragment
                .newInstance(event.traktTaskArgs, event.waitMinutes);
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        newFragment.show(ft, "cancel-checkin-dialog");
    }

    private void checkIn() {
        // lock down UI
        setProgressLock(true);

        // connected?
        if (Utils.isNotConnected(getActivity(), true)) {
            // no? abort
            setProgressLock(false);
            return;
        }

        // launch connect flow if trakt is not connected
        if (!TraktCredentials.ensureCredentials(getActivity())) {
            // not connected? abort
            setProgressLock(false);
            return;
        }

        // try to check in
        checkInTrakt(editTextMessage.getText().toString());
    }

    /**
     * Start the trakt check-in task.
     */
    protected abstract void checkInTrakt(String message);

    /**
     * Disables all interactive UI elements and shows a progress indicator.
     */
    private void setProgressLock(boolean lock) {
        progressBar.setVisibility(lock ? View.VISIBLE : View.GONE);
        editTextMessage.setEnabled(!lock);
        buttonPasteTitle.setEnabled(!lock);
        buttonClear.setEnabled(!lock);
        buttonCheckIn.setEnabled(!lock);
    }
}
