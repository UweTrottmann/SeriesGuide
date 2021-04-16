package com.battlelancer.seriesguide.ui.lists;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.tasks.AddListTask;
import com.battlelancer.seriesguide.util.tasks.ChangeListItemListsTask;
import com.battlelancer.seriesguide.util.tasks.RemoveListItemTask;
import com.battlelancer.seriesguide.util.tasks.RemoveListTask;
import com.battlelancer.seriesguide.util.tasks.RenameListTask;
import com.battlelancer.seriesguide.util.tasks.ReorderListsTask;
import com.uwetrottmann.seriesguide.backend.lists.model.SgListItem;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Helper tools for SeriesGuide lists.
 */
public class ListsTools {

    private static final String[] SELECTION_ARG = new String[1];

    public interface Query {
        String[] PROJECTION_LIST_ID = new String[] {
                SeriesGuideContract.Lists.LIST_ID
        };
        String[] PROJECTION_LIST = new String[] {
                SeriesGuideContract.Lists.LIST_ID,
                SeriesGuideContract.Lists.NAME,
                SeriesGuideContract.Lists.ORDER
        };
        int LIST_ID = 0;
        int NAME = 1;
        int ORDER = 2;

        String[] PROJECTION_LIST_ITEMS = new String[] {
                SeriesGuideContract.ListItems.LIST_ITEM_ID
        };
        int LIST_ITEM_ID = 0;
    }

    private ListsTools() {
    }

    public static void addList(@NonNull Context context, @NonNull String listName) {
        new AddListTask(context, listName).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    static void renameList(@NonNull Context context, @NonNull String listId,
            @NonNull String listName) {
        new RenameListTask(context, listId, listName).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR);
    }

    static void removeList(@NonNull Context context, @NonNull String listId) {
        new RemoveListTask(context, listId).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    static void reorderLists(@NonNull Context context,
            @NonNull List<String> listIdsInOrder) {
        new ReorderListsTask(context, listIdsInOrder).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR);
    }

    static void changeListsOfItem(@NonNull Context context, int itemStableId, int itemType,
            @NonNull List<String> addToTheseLists, @NonNull List<String> removeFromTheseLists) {
        new ChangeListItemListsTask(context, itemStableId, itemType, addToTheseLists,
                removeFromTheseLists).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    static void removeListItem(@NonNull Context context, @NonNull String listItemId) {
        new RemoveListItemTask(context, listItemId).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * Returns a all list ids in the local database.
     *
     * @return null if there was an error, empty list if there are no lists.
     */
    public static HashSet<String> getListIds(Context context) {
        Cursor query = context.getContentResolver().query(SeriesGuideContract.Lists.CONTENT_URI,
                Query.PROJECTION_LIST_ID, null, null, null);
        if (query == null) {
            return null;
        }

        HashSet<String> listIds = new HashSet<>();
        while (query.moveToNext()) {
            listIds.add(query.getString(Query.LIST_ID));
        }

        query.close();

        return listIds;
    }

    /**
     * Returns a all list item ids of the given list in the local database.
     *
     * @return null if there was an error, empty list if there are no list items in this list.
     */
    public static HashSet<String> getListItemIds(Context context, String listId) {
        SELECTION_ARG[0] = listId;
        Cursor query = context.getContentResolver().query(SeriesGuideContract.ListItems.CONTENT_URI,
                Query.PROJECTION_LIST_ITEMS,
                SeriesGuideContract.ListItems.SELECTION_LIST,
                SELECTION_ARG, null);
        if (query == null) {
            return null;
        }

        HashSet<String> listItemIds = new HashSet<>();
        while (query.moveToNext()) {
            listItemIds.add(query.getString(Query.LIST_ITEM_ID));
        }

        query.close();

        return listItemIds;
    }

    @Nullable
    public static List<SgListItem> getListItems(Context context, String listId) {
        SELECTION_ARG[0] = listId;
        Cursor query = context.getContentResolver()
                .query(SeriesGuideContract.ListItems.CONTENT_URI,
                        Query.PROJECTION_LIST_ITEMS,
                        SeriesGuideContract.ListItems.SELECTION_LIST,
                        SELECTION_ARG, null);
        if (query == null) {
            return null; // query failed
        }

        int itemCount = query.getCount();
        if (itemCount == 0) {
            query.close();
            return null; // no items in this list
        }

        List<SgListItem> items = new ArrayList<>(itemCount);
        while (query.moveToNext()) {
            SgListItem item = new SgListItem();
            String itemId = query.getString(Query.LIST_ITEM_ID);
            if (TextUtils.isEmpty(itemId)) {
                continue; // skip, no item id
            }
            item.setListItemId(itemId);
            items.add(item);
        }

        query.close();

        return items;
    }
}
