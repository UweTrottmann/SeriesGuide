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
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.PeopleListHelper;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TmdbTools;
import java.util.List;

/**
 * Shows a list of people in rows with headshots, name and description.
 */
public class PeopleAdapter extends ArrayAdapter<PeopleListHelper.Person> {

    private static int LAYOUT = R.layout.item_person;

    private LayoutInflater mInflater;

    public PeopleAdapter(Context context) {
        super(context, LAYOUT);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(LAYOUT, parent, false);

            viewHolder = new ViewHolder();

            viewHolder.name = (TextView) convertView.findViewById(R.id.textViewPerson);
            viewHolder.description = (TextView) convertView.findViewById(
                    R.id.textViewPersonDescription);
            viewHolder.headshot = (ImageView) convertView.findViewById(R.id.imageViewPerson);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        PeopleListHelper.Person person = getItem(position);

        // name and description
        viewHolder.name.setText(person.name);
        viewHolder.description.setText(person.description);

        // load headshot
        ServiceUtils.loadWithPicasso(getContext(),
                TmdbTools.buildProfileImageUrl(getContext(), person.profilePath,
                        TmdbTools.ProfileImageSize.W185))
                .resizeDimen(R.dimen.person_headshot_size, R.dimen.person_headshot_size)
                .centerCrop()
                .error(R.color.protection_dark)
                .into(viewHolder.headshot);

        return convertView;
    }

    /**
     * Replace the data in this {@link android.widget.ArrayAdapter} with the given list.
     */
    public void setData(List<PeopleListHelper.Person> data) {
        clear();
        if (data != null) {
            addAll(data);
        }
    }

    static class ViewHolder {

        TextView name;

        TextView description;

        ImageView headshot;
    }
}
