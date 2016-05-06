/*
 * Copyright 2016 Uwe Trottmann
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

package com.battlelancer.seriesguide.util.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.ListsActivity;
import de.greenrobot.event.EventBus;

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
    public String getListId() {
        return listId;
    }

    @Override
    protected boolean doDatabaseUpdate(String listId) {
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Lists.NAME, listName);
        int updated = getContext().getContentResolver()
                .update(SeriesGuideContract.Lists.buildListUri(listId), values, null, null);
        if (updated == 0) {
            return false;
        }

        // notify lists activity
        EventBus.getDefault().post(new ListsActivity.ListsChangedEvent());

        return true;
    }

    @Override
    protected int getSuccessTextResId() {
        return 0; // display no success message
    }
}
