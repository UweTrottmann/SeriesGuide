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
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.extensions.ExtensionManager;
import com.battlelancer.seriesguide.util.Utils;

/**
 * Creates views for a list of {@link com.battlelancer.seriesguide.extensions.ExtensionManager.Extension}.
 */
public class ExtensionsAdapter extends ArrayAdapter<ExtensionManager.Extension> {

    private static final int LAYOUT = R.layout.item_extension;

    private final LayoutInflater mLayoutInflater;

    public ExtensionsAdapter(Context context) {
        super(context, 0);
        mLayoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = mLayoutInflater.inflate(LAYOUT, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final ExtensionManager.Extension extension = getItem(position);

        viewHolder.title.setText(extension.label);
        viewHolder.description.setText(extension.description);
        if (extension.icon != null) {
            viewHolder.icon.setImageDrawable(extension.icon);
        } else {
            viewHolder.icon.setImageResource(R.drawable.ic_launcher);
        }
        if (extension.settingsActivity != null) {
            viewHolder.settings.setVisibility(View.VISIBLE);
            viewHolder.settings.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // launch settings activity
                    Utils.tryStartActivity(v.getContext(), new Intent()
                            .setComponent(extension.settingsActivity)
                            .putExtra(SeriesGuideExtension.EXTRA_FROM_SERIESGUIDE_SETTINGS,
                                    true), true);
                }
            });
        } else {
            viewHolder.settings.setVisibility(View.GONE);
            viewHolder.settings.setOnClickListener(null);
        }

        return convertView;
    }

    static class ViewHolder {
        @InjectView(R.id.imageViewItemExtensionIcon) ImageView icon;
        @InjectView(R.id.textViewItemExtensionTitle) TextView title;
        @InjectView(R.id.textViewItemExtensionDescription) TextView description;
        @InjectView(R.id.imageViewItemExtensionSettings) ImageView settings;

        public ViewHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
