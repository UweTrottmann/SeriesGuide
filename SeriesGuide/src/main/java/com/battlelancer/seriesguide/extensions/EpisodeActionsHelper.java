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

package com.battlelancer.seriesguide.extensions;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.androidutils.CheatSheet;
import java.util.List;
import timber.log.Timber;

public class EpisodeActionsHelper {

    /**
     * Replaces all child views of the given {@link android.view.ViewGroup} with a {@link
     * android.widget.TextView} per action plus one linking to {@link
     * com.battlelancer.seriesguide.extensions.ExtensionsConfigurationActivity}. Sets up {@link
     * android.view.View.OnClickListener} if {@link com.battlelancer.seriesguide.api.Action#getViewIntent()}
     * of an  {@link com.battlelancer.seriesguide.api.Action} is not null.
     */
    public static void populateEpisodeActions(LayoutInflater layoutInflater,
            ViewGroup actionsContainer, List<Action> data) {
        if (actionsContainer == null) {
            // nothing we can do, view is already gone
            Timber.d("populateEpisodeActions: action view container gone, aborting");
            return;
        }
        actionsContainer.removeAllViews();

        // add a view per action
        if (data != null) {
            for (Action action : data) {
                TextView actionView = (TextView) layoutInflater.inflate(R.layout.item_action,
                        actionsContainer, false);
                actionView.setText(action.getTitle());
                CheatSheet.setup(actionView, action.getTitle());

                final Intent viewIntent = action.getViewIntent();
                if (viewIntent != null) {
                    viewIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    actionView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Utils.tryStartActivity(v.getContext(), viewIntent, true);
                        }
                    });
                }

                actionsContainer.addView(actionView);
            }
        }

        // link to extensions configuration
        TextView configureView = (TextView) layoutInflater.inflate(R.layout.item_action_add,
                actionsContainer, false);
        configureView.setText(R.string.action_extensions_configure);
        configureView.setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), ExtensionsConfigurationActivity.class);
                if (AndroidUtils.isJellyBeanOrHigher()) {
                    v.getContext()
                            .startActivity(intent,
                                    ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(),
                                            v.getHeight()).toBundle());
                } else {
                    v.getContext().startActivity(intent);
                }
            }
        });
        actionsContainer.addView(configureView);
    }
}
