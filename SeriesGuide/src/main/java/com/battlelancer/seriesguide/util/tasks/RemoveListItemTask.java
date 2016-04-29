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

import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.seriesguide.backend.lists.Lists;
import com.uwetrottmann.seriesguide.backend.lists.model.SgList;
import com.uwetrottmann.seriesguide.backend.lists.model.SgListItem;
import com.uwetrottmann.seriesguide.backend.lists.model.SgListList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

/**
 * Task to remove an item from a single list (basically delete the list item).
 */
public class RemoveListItemTask extends BaseActionTask {

    private final String listItemId;

    public RemoveListItemTask(@NonNull Context context, @NonNull String listItemId) {
        super(context);
        this.listItemId = listItemId;
    }

    @Override
    protected boolean isSendingToTrakt() {
        return false;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        if (isSendingToHexagon()) {
            if (!AndroidUtils.isNetworkConnected(getContext())) {
                return ERROR_NETWORK;
            }

            Lists listsService = HexagonTools.getListsService(getContext());
            if (listsService == null) {
                return ERROR_HEXAGON_API; // no longer signed in
            }

            // extract the list id of this list item
            String[] splitListItemId = SeriesGuideContract.ListItems.splitListItemId(listItemId);
            if (splitListItemId == null) {
                return ERROR_DATABASE;
            }
            String removeFromListId = splitListItemId[2];

            // send the item to be removed from hexagon
            SgListList wrapper = new SgListList();
            List<SgList> lists = buildListItemLists(removeFromListId, listItemId);
            wrapper.setLists(lists);
            try {
                listsService.removeItems(wrapper).execute();
            } catch (IOException e) {
                Timber.e(e, "doInBackground: failed to remove item from lists on hexagon.");
                return ERROR_HEXAGON_API;
            }
        }

        // update local state
        if (!doDatabaseUpdate()) {
            return ERROR_DATABASE;
        }

        return SUCCESS;
    }

    @NonNull
    private static List<SgList> buildListItemLists(String listId, String listItemId) {
        List<SgList> lists = new ArrayList<>(1);
        SgList list = new SgList();
        list.setListId(listId);
        lists.add(list);

        List<SgListItem> items = new ArrayList<>(1);
        list.setListItems(items);

        SgListItem item = new SgListItem();
        items.add(item);
        item.setListItemId(listItemId);
        return lists;
    }

    private boolean doDatabaseUpdate() {
        int deleted = getContext().getContentResolver()
                .delete(SeriesGuideContract.ListItems.buildListItemUri(listItemId), null, null);
        if (deleted == 0) {
            return false; // nothing got deleted
        }

        // notify URI used by list fragments
        getContext().getContentResolver()
                .notifyChange(SeriesGuideContract.ListItems.CONTENT_WITH_DETAILS_URI, null);

        return true;
    }

    @Override
    protected int getSuccessTextResId() {
        return 0; // display no success message
    }
}
