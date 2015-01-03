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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesGuideContract.Shows;
import com.battlelancer.seriesguide.util.RemoveShowWorkerFragment;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import de.greenrobot.event.EventBus;

/**
 * Handles removing a show from the database, ensures it can be removed (its seasons or episodes
 * or itself are not in lists).
 */
public class ConfirmDeleteDialogFragment extends DialogFragment {

    private static final String KEY_SHOW_TVDB_ID = "show_tvdb_id";

    /**
     * Dialog to confirm the removal of a show from the database.
     *
     * @param showTvdbId The TVDb id of the show to remove.
     */
    public static ConfirmDeleteDialogFragment newInstance(int showTvdbId) {
        ConfirmDeleteDialogFragment f = new ConfirmDeleteDialogFragment();

        Bundle args = new Bundle();
        args.putInt(KEY_SHOW_TVDB_ID, showTvdbId);
        f.setArguments(args);

        return f;
    }

    @InjectView(R.id.progressBarRemove) View progressBar;
    @InjectView(R.id.textViewRemove) TextView dialogText;
    @InjectView(R.id.buttonNegative) Button negativeButton;
    @InjectView(R.id.buttonPositive) Button positiveButton;

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
        ButterKnife.inject(this, v);

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

        AndroidUtils.executeOnPool(new CheckForRemovalTask(getActivity()), showTvdbId);
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
        ButterKnife.reset(this);
    }

    private static class CheckForRemovalTask
            extends AsyncTask<Integer, Void, CheckForRemovalTask.CheckForRemovalCompleteEvent> {

        public class CheckForRemovalCompleteEvent {
            public String showTitle;
            public boolean hasListItems;
        }

        private final Context context;

        public CheckForRemovalTask(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        protected CheckForRemovalCompleteEvent doInBackground(Integer... params) {
            int showTvdbId = params[0];

            CheckForRemovalCompleteEvent result = new CheckForRemovalCompleteEvent();

            // make sure this show isn't added to any lists
            /*
             * Selection explanation: Filter for type when looking for show list items, as it looks like
             * the WHERE is pushed down as far as possible, excluding all shows in the original list
             * items query.
             */
            final Cursor itemsInLists = context.getContentResolver().query(
                    ListItems.CONTENT_WITH_DETAILS_URI,
                    new String[] {
                            ListItems.LIST_ITEM_ID
                    },
                    Shows.REF_SHOW_ID + "=" + showTvdbId
                            + " OR ("
                            + ListItems.TYPE + "=" + ListItemTypes.SHOW + " AND "
                            + ListItems.ITEM_REF_ID + "=" + showTvdbId
                            + ")",
                    null, null
            );
            if (itemsInLists == null) {
                return result;
            }

            result.hasListItems = itemsInLists.getCount() > 0;

            itemsInLists.close();

            // determine show title
            final Cursor show = context.getContentResolver().query(
                    Shows.buildShowUri(showTvdbId),
                    new String[] {
                            Shows.TITLE
                    }, null, null, null
            );
            if (show == null) {
                return result;
            }
            if (!show.moveToFirst()) {
                // show not found, abort
                show.close();
                return result;
            }

            result.showTitle = show.getString(0);

            show.close();

            return result;
        }

        @Override
        protected void onPostExecute(CheckForRemovalCompleteEvent result) {
            EventBus.getDefault().post(result);
        }
    }

    public void onEventMainThread(CheckForRemovalTask.CheckForRemovalCompleteEvent event) {
        if (event.showTitle == null) {
            // failed to find show
            dismiss();
        }

        if (event.hasListItems) {
            // prevent removal, there are still list items
            dialogText.setText(getString(R.string.delete_has_list_items, event.showTitle));
            positiveButton.setText(R.string.dismiss);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dismiss();
                }
            });
            negativeButton.setVisibility(View.GONE);
        } else {
            dialogText.setText(getString(R.string.confirm_delete, event.showTitle));
            positiveButton.setText(R.string.delete_show);
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
            negativeButton.setVisibility(View.VISIBLE);
        }

        showProgressBar(false);
    }

    private void showProgressBar(boolean isVisible) {
        progressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        dialogText.setVisibility(isVisible ? View.GONE : View.VISIBLE);
        positiveButton.setEnabled(!isVisible);
    }
}
