/*
 * Copyright 2015 Uwe Trottmann
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

package com.battlelancer.seriesguide.loaders;

import android.content.Context;
import android.database.Cursor;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;
import com.uwetrottmann.androidutils.GenericSimpleLoader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads all user-created lists from the database into a list.
 */
public class OrderedListsLoader extends GenericSimpleLoader<List<OrderedListsLoader.OrderedList>> {

    public static class OrderedList {

        public String id;
        public String name;

        public OrderedList(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public OrderedListsLoader(Context context) {
        super(context);
    }

    @Override
    public List<OrderedList> loadInBackground() {
        List<OrderedList> items = new ArrayList<>();

        Cursor query = getContext().getContentResolver()
                .query(SeriesGuideContract.Lists.CONTENT_URI,
                        ListsQuery.PROJECTION, null, null,
                        SeriesGuideContract.Lists.SORT_ORDER_THEN_NAME);
        if (query == null) {
            return items;
        }

        while (query.moveToNext()) {
            items.add(new OrderedList(
                    query.getString(ListsQuery.ID),
                    query.getString(ListsQuery.NAME)
            ));
        }

        query.close();

        return items;
    }

    public interface ListsQuery {
        String[] PROJECTION = new String[] {
                SeriesGuideContract.Lists._ID,
                SeriesGuideContract.Lists.LIST_ID,
                SeriesGuideContract.Lists.NAME
        };

        int ID = 1;
        int NAME = 2;
    }
}
