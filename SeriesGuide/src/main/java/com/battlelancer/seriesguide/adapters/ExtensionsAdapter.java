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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.extensions.ExtensionManager;
import com.battlelancer.seriesguide.extensions.ExtensionsConfigurationFragment;
import com.battlelancer.seriesguide.util.Utils;
import de.greenrobot.event.EventBus;

/**
 * Creates views for a list of {@link com.battlelancer.seriesguide.extensions.ExtensionManager.Extension}.
 */
public class ExtensionsAdapter extends ArrayAdapter<ExtensionManager.Extension> {

    public class ExtensionDisableRequestEvent {
        public int position;

        public ExtensionDisableRequestEvent(int position) {
            this.position = position;
        }
    }

    private static final int LAYOUT_EXTENSION = R.layout.item_extension;
    private static final int LAYOUT_ADD = R.layout.item_extension_add;

    private static final int VIEW_TYPE_EXTENSION = 0;
    private static final int VIEW_TYPE_ADD = 1;

    private final LayoutInflater mLayoutInflater;

    public ExtensionsAdapter(Context context) {
        super(context, 0);
        mLayoutInflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        // extra row for add button
        return super.getCount() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        // last row is an add button
        return position == getCount() - 1 ? VIEW_TYPE_ADD : VIEW_TYPE_EXTENSION;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == VIEW_TYPE_ADD) {
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(LAYOUT_ADD, parent, false);
            }
            // warn non-supporters that they only can add a few extensions
            boolean isAtLimit =
                    getCount() - 1 == ExtensionsConfigurationFragment.EXTENSION_LIMIT_FREE
                            && !Utils.hasAccessToX(getContext());
            convertView.findViewById(R.id.textViewItemExtensionAddLabel)
                    .setVisibility(isAtLimit ? View.GONE : View.VISIBLE);
            convertView.findViewById(R.id.textViewItemExtensionAddLimit)
                    .setVisibility(isAtLimit ? View.VISIBLE : View.GONE);
            return convertView;
        }

        ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(LAYOUT_EXTENSION, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final ExtensionManager.Extension extension = getItem(position);

        viewHolder.description.setText(extension.description);

        // title
        viewHolder.title.setText(extension.label);

        // icon
        if (extension.icon != null) {
            viewHolder.icon.setImageDrawable(extension.icon);
        } else {
            viewHolder.icon.setImageResource(R.drawable.ic_launcher);
        }

        // overflow menu
        viewHolder.settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                popupMenu.getMenuInflater().inflate(R.menu.extension_menu, popupMenu.getMenu());
                if (extension.settingsActivity == null) {
                    MenuItem item = popupMenu.getMenu()
                            .findItem(R.id.menu_action_extension_settings);
                    item.setVisible(false);
                    item.setEnabled(false);
                }
                popupMenu.setOnMenuItemClickListener(new OverflowItemClickListener(position));
                popupMenu.show();
            }
        });

        return convertView;
    }

    private class OverflowItemClickListener implements PopupMenu.OnMenuItemClickListener {

        private final int mPosition;

        public OverflowItemClickListener(int position) {
            mPosition = position;
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_action_extension_settings:
                    ExtensionManager.Extension extension = getItem(mPosition);
                    // launch settings activity
                    Utils.tryStartActivity(getContext(), new Intent()
                                    .setComponent(extension.settingsActivity)
                                    .putExtra(SeriesGuideExtension.EXTRA_FROM_SERIESGUIDE_SETTINGS,
                                            true),
                            true
                    );
                    ExtensionManager.getInstance(getContext()).clearEpisodeActionsCache();
                    return true;
                case R.id.menu_action_extension_disable:
                    EventBus.getDefault()
                            .post(new ExtensionDisableRequestEvent(mPosition));
                    return true;
            }
            return false;
        }
    }

    static class ViewHolder {
        @Bind(R.id.imageViewItemExtensionIcon) ImageView icon;
        @Bind(R.id.textViewItemExtensionTitle) TextView title;
        @Bind(R.id.textViewItemExtensionDescription) TextView description;
        @Bind(R.id.imageViewItemExtensionSettings) ImageView settings;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
