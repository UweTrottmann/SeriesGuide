package com.battlelancer.seriesguide.util.tasks;

import android.content.Context;
import androidx.annotation.NonNull;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.Errors;
import com.uwetrottmann.seriesguide.backend.lists.Lists;
import com.uwetrottmann.seriesguide.backend.lists.model.SgList;
import com.uwetrottmann.seriesguide.backend.lists.model.SgListItem;
import com.uwetrottmann.seriesguide.backend.lists.model.SgListList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    protected Integer doBackgroundAction(Void... params) {
        if (isSendingToHexagon()) {
            HexagonTools hexagonTools = SgApp.getServicesComponent(getContext()).hexagonTools();
            Lists listsService = hexagonTools.getListsService();
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
                Errors.logAndReportHexagon("remove list item", e);
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
