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

package com.battlelancer.seriesguide.util;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.AsyncTaskCompat;
import android.text.TextUtils;
import com.battlelancer.seriesguide.backend.HexagonTools;
import com.battlelancer.seriesguide.backend.settings.HexagonSettings;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.battlelancer.seriesguide.util.tasks.ChangeListItemListsTask;
import com.google.api.client.util.DateTime;
import com.uwetrottmann.seriesguide.backend.lists.Lists;
import com.uwetrottmann.seriesguide.backend.lists.model.SgList;
import com.uwetrottmann.seriesguide.backend.lists.model.SgListItem;
import com.uwetrottmann.seriesguide.backend.lists.model.SgListList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import timber.log.Timber;

/**
 * Helper tools for SeriesGuide lists.
 */
public class ListsTools {

    private static final int LISTS_MAX_BATCH_SIZE = 10;
    private static final String[] SELECTION_ARG = new String[1];

    interface Query {
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

    public static void changeListsOfItem(@NonNull Context context, int itemTvdbId, int itemType,
            @NonNull List<String> addToTheseLists, @NonNull List<String> removeFromTheseLists) {
        AsyncTaskCompat.executeParallel(
                new ChangeListItemListsTask(context, itemTvdbId, itemType, addToTheseLists,
                        removeFromTheseLists));
    }

    public static boolean uploadAllToHexagon(Context context) {
        Timber.d("uploadAllToHexagon: all lists.");

        SgListList listsWrapper = new SgListList();
        List<SgList> lists = new ArrayList<>(LISTS_MAX_BATCH_SIZE);
        listsWrapper.setLists(lists);

        Cursor listsQuery = context.getContentResolver()
                .query(SeriesGuideContract.Lists.CONTENT_URI,
                        Query.PROJECTION_LIST, null, null, null);
        if (listsQuery == null) {
            return false; // query failed
        }

        while (listsQuery.moveToNext()) {
            SgList list = new SgList();
            lists.add(list);
            // add list properties
            String listId = listsQuery.getString(Query.LIST_ID);
            list.setListId(listId);
            list.setName(listsQuery.getString(Query.NAME));
            int order = listsQuery.getInt(Query.ORDER);
            if (order != 0) {
                list.setOrder(order);
            }
            // add list items
            List<SgListItem> listItems = getListItems(context, listId);
            if (listItems != null) {
                list.setListItems(listItems);
            }

            if (lists.size() == LISTS_MAX_BATCH_SIZE || listsQuery.isLast()) {
                if (!doUploadSomeLists(context, listsWrapper)) {
                    return false; // part upload failed, next sync will try again
                }
            }
        }
        listsQuery.close();

        return true;
    }

    @Nullable
    private static List<SgListItem> getListItems(Context context, String listId) {
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
            Timber.d("getListItems: no lists to upload.");
            return null; // no items in this list
        }

        List<SgListItem> items = new ArrayList<>(itemCount);
        while (query.moveToNext()) {
            SgListItem item = new SgListItem();
            item.setListItemId(query.getString(Query.LIST_ITEM_ID));
            items.add(item);
        }

        query.close();

        return items;
    }

    private static boolean doUploadSomeLists(Context context, SgListList listsWrapper) {
        Lists listsService = HexagonTools.getListsService(context);
        if (listsService == null) {
            return false; // no longer signed in
        }
        try {
            listsService.save(listsWrapper).execute();
        } catch (IOException e) {
            Timber.e(e, "uploadAllToHexagon: failed for lists.");
            return false;
        }
        return true;
    }

    public static boolean downloadFromHexagon(Context context, boolean hasMergedLists) {
        long currentTime = System.currentTimeMillis();
        DateTime lastSyncTime = new DateTime(HexagonSettings.getLastListsSyncTime(context));

        if (hasMergedLists) {
            Timber.d("downloadFromHexagon: downloading lists changed since %s.", lastSyncTime);
        } else {
            Timber.d("downloadFromHexagon: downloading all lists.");
        }

        HashSet<String> localListIds = getListIds(context);
        List<SgList> lists;
        String cursor = null;
        do {
            try {
                Lists listsService = HexagonTools.getListsService(context);
                if (listsService == null) {
                    return false; // no longer signed in
                }

                Lists.Get request = listsService.get().setLimit(LISTS_MAX_BATCH_SIZE);
                if (hasMergedLists) {
                    request.setUpdatedSince(lastSyncTime);
                }
                if (!TextUtils.isEmpty(cursor)) {
                    request.setCursor(cursor);
                }

                SgListList response = request.execute();
                if (response == null) {
                    Timber.d("downloadFromHexagon: failed, response is null.");
                    break;
                }

                cursor = response.getCursor();
                lists = response.getLists();
            } catch (IOException e) {
                Timber.e(e, "downloadFromHexagon: failed to download lists.");
                return false;
            }

            if (lists == null || lists.size() == 0) {
                break; // empty response, assume we are done
            }

            if (!doListsDatabaseUpdate(context, lists, localListIds, hasMergedLists)) {
                return false; // database update failed, abort
            }
        } while (!TextUtils.isEmpty(cursor)); // fetch next batch

        // set new last sync time
        if (hasMergedLists) {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putLong(HexagonSettings.KEY_LAST_SYNC_LISTS, currentTime)
                    .commit();
        }

        return true;
    }

    private static boolean doListsDatabaseUpdate(Context context, List<SgList> lists,
            HashSet<String> localListIds, boolean removeMissingListItems) {
        ArrayList<ContentProviderOperation> batch = new ArrayList<>();
        for (SgList list : lists) {
            // add or update the list
            String listId = list.getListId();
            ContentProviderOperation.Builder builder;
            if (localListIds.contains(listId)) {
                // update
                builder = ContentProviderOperation
                        .newUpdate(SeriesGuideContract.Lists.buildListUri(listId));
            } else {
                // insert
                builder = ContentProviderOperation
                        .newInsert(SeriesGuideContract.Lists.CONTENT_URI)
                        .withValue(SeriesGuideContract.Lists.LIST_ID, listId);
            }
            builder.withValue(SeriesGuideContract.Lists.NAME, list.getName());
            if (list.getOrder() != null) {
                builder.withValue(SeriesGuideContract.Lists.ORDER, list.getOrder());
            }
            batch.add(builder.build());

            // keep track of items not in the list on hexagon
            HashSet<String> listItemsToRemove = null;
            if (removeMissingListItems) {
                listItemsToRemove = getListItemIds(context, listId);
                if (listItemsToRemove == null) {
                    return false; // list item query failed
                }
            }
            // add or update items of the list
            List<SgListItem> listItems = list.getListItems();
            if (listItems != null) {
                for (SgListItem listItem : listItems) {
                    String listItemId = listItem.getListItemId();
                    String[] brokenUpId = SeriesGuideContract.ListItems.splitListItemId(listItemId);
                    if (brokenUpId == null) {
                        continue; // could not break up list item id
                    }
                    int itemTvdbId = -1;
                    int itemType = -1;
                    try {
                        itemTvdbId = Integer.parseInt(brokenUpId[0]);
                        itemType = Integer.parseInt(brokenUpId[1]);
                    } catch (NumberFormatException ignored) {
                    }
                    if (itemTvdbId == -1 || !SeriesGuideContract.ListItems.isValidItemType(
                            itemType)) {
                        continue; // failed to extract item TVDB id or item type not known
                    }

                    // just insert the list item, if the id already exists it will be replaced
                    builder = ContentProviderOperation
                            .newInsert(SeriesGuideContract.ListItems.CONTENT_URI)
                            .withValue(SeriesGuideContract.ListItems.LIST_ITEM_ID, listItemId)
                            .withValue(SeriesGuideContract.ListItems.ITEM_REF_ID, itemTvdbId)
                            .withValue(SeriesGuideContract.ListItems.TYPE, itemType)
                            .withValue(SeriesGuideContract.Lists.LIST_ID, listId);
                    batch.add(builder.build());

                    if (removeMissingListItems) {
                        // do not remove this list item
                        listItemsToRemove.remove(listItemId);
                    }
                }
            }
            if (removeMissingListItems) {
                // remove items no longer in the list
                for (String listItemId : listItemsToRemove) {
                    builder = ContentProviderOperation
                            .newDelete(SeriesGuideContract.ListItems.buildListItemUri(listItemId));
                    batch.add(builder.build());
                }
            }
        }

        try {
            DBUtils.applyInSmallBatches(context, batch);
        } catch (OperationApplicationException e) {
            Timber.e(e, "doListsDatabaseUpdate: failed.");
            return false;
        }

        return true;
    }

    /**
     * Returns a all list ids in the local database.
     *
     * @return null if there was an error, empty list if there are no lists.
     */
    private static HashSet<String> getListIds(Context context) {
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
    private static HashSet<String> getListItemIds(Context context, String listId) {
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
}
