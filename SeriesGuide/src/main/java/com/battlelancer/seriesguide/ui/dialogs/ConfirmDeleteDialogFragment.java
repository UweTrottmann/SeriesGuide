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

import com.battlelancer.seriesguide.provider.SeriesContract.ListItemTypes;
import com.battlelancer.seriesguide.provider.SeriesContract.ListItems;
import com.battlelancer.seriesguide.provider.SeriesContract.Shows;
import com.battlelancer.seriesguide.util.DBUtils;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.seriesguide.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * Handles removing a show from the show list, ensures it can be removed (its seasons or episodes or
 * itself are not in lists).
 */
public class ConfirmDeleteDialogFragment extends DialogFragment {

    /**
     * Dialog to confirm the removal of a show from the database.
     *
     * @param showTvdbId The TVDb id of the show to remove.
     */
    public static ConfirmDeleteDialogFragment newInstance(int showTvdbId) {
        ConfirmDeleteDialogFragment f = new ConfirmDeleteDialogFragment();

        Bundle args = new Bundle();
        args.putInt("showid", showTvdbId);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onStart() {
        super.onStart();
        Utils.trackView(getActivity(), "Delete Dialog");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final int showTvdbId = getArguments().getInt("showid");

        // make sure this show isn't added to any lists
        boolean hasListItems = true;
        /*
         * Selection explanation: Filter for type when looking for show list items, as it looks like
         * the WHERE is pushed down as far as possible, excluding all shows in the original list
         * items query.
         */
        final Cursor itemsInLists = getActivity().getContentResolver().query(
                ListItems.CONTENT_WITH_DETAILS_URI,
                new String[]{
                        ListItems.LIST_ITEM_ID
                },
                Shows.REF_SHOW_ID + "=" + showTvdbId
                        + " OR ("
                        + ListItems.TYPE + "=" + ListItemTypes.SHOW + " AND "
                        + ListItems.ITEM_REF_ID + "=" + showTvdbId
                        + ")",
                null, null);
        if (itemsInLists != null) {
            hasListItems = itemsInLists.getCount() > 0;
            itemsInLists.close();
        }

        // determine show title
        final Cursor show = getActivity().getContentResolver().query(Shows.buildShowUri(showTvdbId),
                new String[]{
                        Shows.TITLE
                }, null, null, null);
        String showTitle = getString(R.string.unknown);
        if (show != null) {
            if (show.moveToFirst()) {
                showTitle = show.getString(0);
            }
            show.close();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setNegativeButton(getString(R.string.dontdelete_show), null);
        if (hasListItems) {
            // Prevent deletion, tell user there are still list items
            builder.setMessage(getString(R.string.delete_has_list_items, showTitle));
        } else {
            builder.setMessage(getString(R.string.confirm_delete, showTitle)).setPositiveButton(
                    getString(R.string.delete_show), new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final ProgressDialog progress = new ProgressDialog(getActivity());
                    progress.setCancelable(false);
                    progress.show();

                    new Thread(new Runnable() {
                        public void run() {
                            DBUtils.deleteShow(getActivity(), showTvdbId, progress);
                        }
                    }).start();
                }
            });
        }

        return builder.create();
    }
}
