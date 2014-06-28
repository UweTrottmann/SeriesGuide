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

package com.battlelancer.seriesguide.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.uwetrottmann.tmdb.entities.Credits;
import java.util.List;
import timber.log.Timber;

/**
 * Helps load a fixed number of people into a static layout.
 */
public class PeopleListHelper {

    public static void populateCast(Context context, LayoutInflater layoutInflater,
            ViewGroup peopleContainer, List<Credits.CastMember> cast) {
        if (peopleContainer == null) {
            // nothing we can do, view is already gone
            Timber.d("populateCast: container reference gone, aborting");
            return;
        }
        peopleContainer.removeAllViews();

        // show at most 3 cast members
        if (cast == null) {
            // TODO show placeholder
            return;
        }

        for (int i = 0; i < Math.min(3, cast.size()); i++) {
            Credits.CastMember castMember = cast.get(i);
            View personView = layoutInflater.inflate(R.layout.item_person,
                    peopleContainer, false);

            ServiceUtils.getPicasso(context)
                    .load(TmdbTools.buildProfileImageUrl(context, castMember.profile_path,
                            TmdbTools.ProfileImageSize.W185))
                    .resizeDimen(R.dimen.person_headshot_size, R.dimen.person_headshot_size)
                    .centerCrop()
                    .into((ImageView) personView.findViewById(R.id.imageViewPerson));

            TextView name = (TextView) personView.findViewById(R.id.textViewPerson);
            name.setText(castMember.name);

            TextView character = (TextView) personView.findViewById(
                    R.id.textViewPersonDescription);
            character.setText(castMember.character);

            peopleContainer.addView(personView);
        }

        if (cast.size() > 3) {
            TextView configureView = (TextView) layoutInflater.inflate(R.layout.item_action,
                    peopleContainer, false);
            configureView.setText(R.string.action_display_all);
            configureView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // TODO link to all list
                }
            });
            peopleContainer.addView(configureView);
        }
    }
}
