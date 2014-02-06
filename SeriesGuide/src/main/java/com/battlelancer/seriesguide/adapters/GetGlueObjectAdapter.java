/*
 * Copyright 2014 Uwe Trottmann
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.uwetrottmann.getglue.entities.GetGlueObject;
import com.battlelancer.seriesguide.R;

import java.util.List;

/**
 * Displays show title and GetGlue object id of the given {@link GetGlueObject}
 * array.
 */
public class GetGlueObjectAdapter extends ArrayAdapter<GetGlueObject> {

    private static int LAYOUT = R.layout.getglue_item;

    private LayoutInflater mInflater;

    public GetGlueObjectAdapter(Context context) {
        super(context, LAYOUT);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // A ViewHolder keeps references to children views to avoid
        // unnecessary calls to findViewById() on each row.
        ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(LAYOUT, null);

            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.textViewGetGlueShowTitle);
            holder.getGlueId = (TextView) convertView.findViewById(R.id.textViewGetGlueShowId);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // Bind the data efficiently with the holder.
        GetGlueObject glueObject = getItem(position);

        holder.title.setText(glueObject.title);
        holder.getGlueId.setText(glueObject.id);

        return convertView;
    }

    public void setData(List<GetGlueObject> data) {
        clear();
        if (data != null) {
            for (GetGlueObject item : data) {
                add(item);
            }
        }
    }

    static class ViewHolder {
        TextView title;

        TextView getGlueId;

    }

}
