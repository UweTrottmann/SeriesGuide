// SPDX-License-Identifier: Apache-2.0
// Copyright 2016-2018, 2021, 2023 Uwe Trottmann

package com.battlelancer.seriesguide.util.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;

/**
 * Task to rename a list.
 */
public class RenameListTask extends AddListTask {

    @NonNull private final String listId;

    public RenameListTask(@NonNull Context context, @NonNull String listId,
            @NonNull String listName) {
        super(context, listName);
        this.listId = listId;
    }

    @Override
    protected boolean isSendingToTrakt() {
        return false;
    }

    @Override
    @NonNull
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public String getListId() {
        return listId;
    }

    @Override
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public boolean doDatabaseUpdate(ContentResolver contentResolver, String listId) {
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Lists.NAME, listName);
        int updated = contentResolver
                .update(SeriesGuideContract.Lists.buildListUri(listId), values, null, null);
        return updated != 0;
    }

    @Override
    protected int getSuccessTextResId() {
        if (isSendingToHexagon()) {
            return R.string.ack_list_renamed;
        } else {
            return 0;
        }
    }
}
