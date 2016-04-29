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

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.DBUtils;
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
 * Task to add or remove an item to and from lists.
 */
public class ChangeListItemListsTask extends BaseActionTask {

    private final int itemTvdbId;
    private final int itemType;
    private final List<String> addToTheseLists;
    private final List<String> removeFromTheseLists;

    public ChangeListItemListsTask(@NonNull Context context, int itemTvdbId, int itemType,
            @NonNull List<String> addToTheseLists, @NonNull List<String> removeFromTheseLists) {
        super(context);
        this.itemTvdbId = itemTvdbId;
        this.itemType = itemType;
        this.addToTheseLists = addToTheseLists;
        this.removeFromTheseLists = removeFromTheseLists;
    }

    @Override
    protected boolean isSendingToTrakt() {
        return false;
    }

    @Override
    protected Integer doInBackground(Void... params) {
        // if sending to service, check for connection
        if (isSendingToHexagon()) {
            if (!AndroidUtils.isNetworkConnected(getContext())) {
                return ERROR_NETWORK;
            }

            Lists listsService = HexagonTools.getListsService(getContext());
            if (listsService == null) {
                return ERROR_HEXAGON_API;
            }

            SgListList wrapper = new SgListList();
            if (addToTheseLists.size() > 0) {
                List<SgList> lists = buildListItemLists(addToTheseLists);
                wrapper.setLists(lists);

                try {
                    listsService.save(wrapper).execute();
                } catch (IOException e) {
                    Timber.e(e, "doInBackground: failed to add item to lists on hexagon.");
                    return ERROR_HEXAGON_API;
                }
            }

            if (removeFromTheseLists.size() > 0) {
                List<SgList> lists = buildListItemLists(removeFromTheseLists);
                wrapper.setLists(lists);

                try {
                    listsService.removeItems(wrapper).execute();
                } catch (IOException e) {
                    Timber.e(e, "doInBackground: failed to remove item from lists on hexagon.");
                    return ERROR_HEXAGON_API;
                }
            }
        }

        // update local state
        if (!doDatabaseUpdate()) {
            return ERROR_DATABASE;
        }

        return SUCCESS;
    }

    @NonNull
    private List<SgList> buildListItemLists(List<String> listsToChange) {
        List<SgList> lists = new ArrayList<>(listsToChange.size());
        for (String listId : listsToChange) {
            SgList list = new SgList();
            list.setListId(listId);
            lists.add(list);

            List<SgListItem> items = new ArrayList<>(1);
            list.setListItems(items);

            String listItemId = SeriesGuideContract.ListItems
                    .generateListItemId(itemTvdbId, itemType, listId);
            SgListItem item = new SgListItem();
            items.add(item);
            item.setListItemId(listItemId);
        }
        return lists;
    }

    private boolean doDatabaseUpdate() {
        ArrayList<ContentProviderOperation> batch = new ArrayList<>(
                addToTheseLists.size() + removeFromTheseLists.size());
        for (String listId : addToTheseLists) {
            String listItemId = SeriesGuideContract.ListItems.generateListItemId(itemTvdbId,
                    itemType, listId);
            batch.add(ContentProviderOperation
                    .newInsert(SeriesGuideContract.ListItems.CONTENT_URI)
                    .withValue(SeriesGuideContract.ListItems.LIST_ITEM_ID, listItemId)
                    .withValue(SeriesGuideContract.ListItems.ITEM_REF_ID, itemTvdbId)
                    .withValue(SeriesGuideContract.ListItems.TYPE, itemType)
                    .withValue(SeriesGuideContract.Lists.LIST_ID, listId)
                    .build());
        }
        for (String listId : removeFromTheseLists) {
            String listItemId = SeriesGuideContract.ListItems.generateListItemId(itemTvdbId,
                    itemType, listId);
            batch.add(ContentProviderOperation
                    .newDelete(SeriesGuideContract.ListItems.buildListItemUri(listItemId))
                    .build());
        }

        // apply ops
        try {
            DBUtils.applyInSmallBatches(getContext(), batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "Applying list changes failed");
            return false;
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
