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
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.ListsActivity;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.backend.lists.Lists;
import com.uwetrottmann.seriesguide.backend.lists.model.SgList;
import com.uwetrottmann.seriesguide.backend.lists.model.SgListList;
import de.greenrobot.event.EventBus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

/**
 * Task to add a new list.
 */
public class AddListTask extends BaseActionTask {

    @NonNull protected final String listName;

    public AddListTask(@NonNull Context context, @NonNull String listName) {
        super(context);
        this.listName = listName;
    }

    @Override
    protected boolean isSendingToTrakt() {
        return false;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        String listId = getListId();

        if (isSendingToHexagon()) {
            if (!AndroidUtils.isNetworkConnected(getContext())) {
                return ERROR_NETWORK;
            }

            Lists listsService = HexagonTools.getListsService(getContext());
            if (listsService == null) {
                return ERROR_HEXAGON_API; // no longer signed in
            }

            // send list to be added to hexagon
            SgListList wrapper = new SgListList();
            List<SgList> lists = buildList(listId, listName);
            wrapper.setLists(lists);
            try {
                listsService.save(wrapper).execute();
            } catch (IOException e) {
                Timber.e(e, "doInBackground: failed to add list to hexagon.");
                return ERROR_HEXAGON_API;
            }
        }

        // update local state
        if (!doDatabaseUpdate(listId)) {
            return ERROR_DATABASE;
        }

        return SUCCESS;
    }

    protected String getListId() {
        return SeriesGuideContract.Lists.generateListId(listName);
    }

    @NonNull
    private static List<SgList> buildList(@NonNull String listId, @NonNull String listName) {
        List<SgList> lists = new ArrayList<>(1);
        SgList list = new SgList();
        list.setListId(listId);
        list.setName(listName);
        lists.add(list);
        return lists;
    }

    protected boolean doDatabaseUpdate(String listId) {
        ContentValues values = new ContentValues();
        values.put(SeriesGuideContract.Lists.LIST_ID, listId);
        values.put(SeriesGuideContract.Lists.NAME, listName);
        getContext().getContentResolver().insert(SeriesGuideContract.Lists.CONTENT_URI, values);

        // notify lists activity
        EventBus.getDefault().post(new ListsActivity.ListsChangedEvent());

        return true;
    }

    @Override
    protected int getSuccessTextResId() {
        return 0; // display no success message
    }
}
