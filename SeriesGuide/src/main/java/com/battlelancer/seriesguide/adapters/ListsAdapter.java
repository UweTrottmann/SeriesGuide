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

package com.battlelancer.seriesguide.adapters;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.provider.SeriesGuideContract;

/**
 * Used with {@link com.battlelancer.seriesguide.ui.dialogs.ListsReorderDialogFragment}.
 */
public class ListsAdapter extends CursorAdapter {

    public ListsAdapter(Context context) {
        super(context, null, 0);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_list, parent, false);

        ListsViewHolder viewHolder = new ListsViewHolder(v);
        v.setTag(viewHolder);

        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ListsViewHolder viewHolder = (ListsViewHolder) view.getTag();

        viewHolder.name.setText(cursor.getString(ListsQuery.NAME));
    }

    static class ListsViewHolder {
        public TextView name;

        public ListsViewHolder(View v) {
            name = (TextView) v.findViewById(R.id.textViewItemListName);
        }
    }

    public interface ListsQuery {
        String[] PROJECTION = new String[] {
                SeriesGuideContract.Lists._ID,
                SeriesGuideContract.Lists.NAME
        };

        int ID = 0;
        int NAME = 1;
    }
}
