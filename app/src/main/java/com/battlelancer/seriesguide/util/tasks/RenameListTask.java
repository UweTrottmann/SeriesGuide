package com.battlelancer.seriesguide.util.tasks;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
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
        return 0; // display no success message
    }
}
