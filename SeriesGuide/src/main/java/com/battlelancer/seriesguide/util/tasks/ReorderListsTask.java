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
import com.battlelancer.seriesguide.ui.ListsActivity;
import com.battlelancer.seriesguide.util.DBUtils;
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
 * Task to reorder all lists.
 */
public class ReorderListsTask extends BaseActionTask {

    @NonNull private final List<String> listIdsInOrder;

    /**
     * The given lists order number will be changed to their position in the given list.
     */
    public ReorderListsTask(@NonNull Context context, @NonNull List<String> listIdsInOrder) {
        super(context);
        this.listIdsInOrder = listIdsInOrder;
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

            // send lists with updated order to hexagon
            SgListList wrapper = new SgListList();
            List<SgList> lists = buildListsList(listIdsInOrder);
            wrapper.setLists(lists);
            try {
                listsService.save(wrapper).execute();
            } catch (IOException e) {
                Timber.e(e, "doInBackground: failed to save reordered lists to hexagon.");
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
    private List<SgList> buildListsList(List<String> listsToChange) {
        List<SgList> lists = new ArrayList<>(listsToChange.size());
        for (int position = 0; position < listsToChange.size(); position++) {
            String listId = listsToChange.get(position);
            SgList list = new SgList();
            list.setListId(listId);
            list.setOrder(position);
            lists.add(list);
        }
        return lists;
    }

    private boolean doDatabaseUpdate() {
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (int position = 0; position < listIdsInOrder.size(); position++) {
            String listId = listIdsInOrder.get(position);
            batch.add(ContentProviderOperation.newUpdate(
                    SeriesGuideContract.Lists.buildListUri(listId))
                    .withValue(SeriesGuideContract.Lists.ORDER, position)
                    .build());
        }

        try {
            DBUtils.applyInSmallBatches(getContext(), batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "doDatabaseUpdate: failed to save reordered lists.");
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