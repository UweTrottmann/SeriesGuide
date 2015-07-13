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

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.util.RemoveShowWorkerFragment;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import de.greenrobot.event.EventBus;

/**
 * Dialog asking if a show should be removed from the database.
 */
public class RemoveShowDialogFragment extends DialogFragment {

    private static final String KEY_SHOW_TVDB_ID = "show_tvdb_id";

    /**
     * Dialog to confirm the removal of a show from the database.
     *
     * @param showTvdbId The TVDb id of the show to remove.
     */
    public static void show(FragmentManager fm, int showTvdbId) {
        RemoveShowDialogFragment f = new RemoveShowDialogFragment();

        Bundle args = new Bundle();
        args.putInt(KEY_SHOW_TVDB_ID, showTvdbId);
        f.setArguments(args);

        f.show(fm, "dialog-remove-show");
    }

    @Bind(R.id.progressBarRemove) View progressBar;
    @Bind(R.id.textViewRemove) TextView dialogText;
    @Bind(R.id.buttonNegative) Button negativeButton;
    @Bind(R.id.buttonPositive) Button positiveButton;

    private int showTvdbId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showTvdbId = getArguments().getInt(KEY_SHOW_TVDB_ID);
        if (showTvdbId <= 0) {
            dismiss();
        }

        // hide title, use custom theme
        setStyle(STYLE_NO_TITLE, 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_remove, container, false);
        ButterKnife.bind(this, v);

        showProgressBar(true);
        negativeButton.setText(android.R.string.cancel);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        positiveButton.setText(R.string.delete_show);

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        EventBus.getDefault().register(this);

        AndroidUtils.executeOnPool(new GetShowTitleTask(getActivity()), showTvdbId);
    }

    @Override
    public void onStart() {
        super.onStart();

        Utils.trackView(getActivity(), "Delete Dialog");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        EventBus.getDefault().unregister(this);
        ButterKnife.unbind(this);
    }

    private static class GetShowTitleTask
            extends AsyncTask<Integer, Void, GetShowTitleTask.ShowTitleEvent> {

        public class ShowTitleEvent {
            public String showTitle;
        }

        private final Context context;

        public GetShowTitleTask(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        protected ShowTitleEvent doInBackground(Integer... params) {
            int showTvdbId = params[0];

            ShowTitleEvent result = new ShowTitleEvent();

            // get show title
            final Cursor show = context.getContentResolver().query(
                    Shows.buildShowUri(showTvdbId),
                    new String[] {
                            Shows.TITLE
                    }, null, null, null
            );
            if (show != null) {
                if (show.moveToFirst()) {
                    result.showTitle = show.getString(0);
                }
                show.close();
            }

            return result;
        }

        @Override
        protected void onPostExecute(ShowTitleEvent result) {
            EventBus.getDefault().post(result);
        }
    }

    public void onEventMainThread(GetShowTitleTask.ShowTitleEvent event) {
        if (event.showTitle == null) {
            // failed to find show
            dismiss();
            return;
        }

        dialogText.setText(getString(R.string.confirm_delete, event.showTitle));
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RemoveShowWorkerFragment f = RemoveShowWorkerFragment.newInstance(showTvdbId);
                getFragmentManager().beginTransaction()
                        .add(f, RemoveShowWorkerFragment.TAG)
                        .commit();

                dismiss();
            }
        });

        showProgressBar(false);
    }

    private void showProgressBar(boolean isVisible) {
        progressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        dialogText.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        positiveButton.setEnabled(!isVisible);
    }
}
