package com.battlelancer.seriesguide.util.tasks;

import android.content.ContentValues;
import android.content.Context;
import android.support.annotation.NonNull;
import com.battlelancer.seriesguide.SgApp;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.ui.ListsActivity;
import com.uwetrottmann.seriesguide.backend.lists.Lists;
import com.uwetrottmann.seriesguide.backend.lists.model.SgList;
import com.uwetrottmann.seriesguide.backend.lists.model.SgListList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.greenrobot.eventbus.EventBus;

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
    protected Integer doBackgroundAction(Void... params) {
        String listId = getListId();

        if (isSendingToHexagon()) {
            HexagonTools hexagonTools = SgApp.getServicesComponent(getContext()).hexagonTools();
            Lists listsService = hexagonTools.getListsService();
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
                HexagonTools.trackFailedRequest(getContext(), "add list", e);
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
