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
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

/**
 * Helps load a fixed number of people into a static layout.
 */
public class PeopleListHelper {

    /**
     * Add views for at most three cast members to the given {@link android.view.ViewGroup} and a
     * "Show all" link if there are more.
     */
    public static void populateCast(Context context, LayoutInflater inflater,
            ViewGroup peopleContainer, List<Credits.CastMember> cast,
            View.OnClickListener clickListener) {
        if (peopleContainer == null) {
            // nothing we can do, view is already gone
            Timber.d("populateCast: container reference gone, aborting");
            return;
        }

        peopleContainer.removeAllViews();

        // show at most 3 cast members
        for (int i = 0; i < Math.min(3, cast.size()); i++) {
            Credits.CastMember castMember = cast.get(i);
            addPersonView(context, inflater, peopleContainer, castMember.name,
                    castMember.character, castMember.profile_path);
        }

        if (cast.size() > 3) {
            addShowAllView(inflater, peopleContainer, clickListener);
        }
    }

    /**
     * Add views for at most three crew members to the given {@link android.view.ViewGroup} and a
     * "Show all" link if there are more.
     */
    public static void populateCrew(Context context, LayoutInflater inflater,
            ViewGroup peopleContainer, List<Credits.CrewMember> crew,
            View.OnClickListener clickListener) {
        if (peopleContainer == null) {
            // nothing we can do, view is already gone
            Timber.d("populateCrew: container reference gone, aborting");
            return;
        }

        peopleContainer.removeAllViews();

        // show at most 3 crew members
        for (int i = 0; i < Math.min(3, crew.size()); i++) {
            Credits.CrewMember castMember = crew.get(i);
            addPersonView(context, inflater, peopleContainer, castMember.name, castMember.job,
                    castMember.profile_path);
        }

        if (crew.size() > 3) {
            addShowAllView(inflater, peopleContainer, clickListener);
        }
    }

    private static void addPersonView(Context context, LayoutInflater inflater,
            ViewGroup peopleContainer,
            String name, String description, String profilePath) {
        View personView = inflater.inflate(R.layout.item_person, peopleContainer, false);

        ServiceUtils.getPicasso(context)
                .load(TmdbTools.buildProfileImageUrl(context, profilePath,
                        TmdbTools.ProfileImageSize.W185))
                .resizeDimen(R.dimen.person_headshot_size, R.dimen.person_headshot_size)
                .centerCrop()
                .into((ImageView) personView.findViewById(R.id.imageViewPerson));

        TextView nameView = (TextView) personView.findViewById(R.id.textViewPerson);
        nameView.setText(name);

        TextView descriptionView = (TextView) personView.findViewById(
                R.id.textViewPersonDescription);
        descriptionView.setText(description);

        peopleContainer.addView(personView);
    }

    private static void addShowAllView(LayoutInflater inflater, ViewGroup peopleContainer,
            View.OnClickListener clickListener) {
        TextView showAllView = (TextView) inflater.inflate(R.layout.item_action,
                peopleContainer, false);
        showAllView.setText(R.string.action_display_all);
        showAllView.setOnClickListener(clickListener);
        peopleContainer.addView(showAllView);
    }

    public static List<Person> transformCastToPersonList(List<Credits.CastMember> cast) {
        List<Person> people = new ArrayList<>();
        for (Credits.CastMember castMember : cast) {
            Person person = new Person();
            person.name = castMember.name;
            person.description = castMember.character;
            person.profilePath = castMember.profile_path;
            people.add(person);
        }
        return people;
    }

    public static List<Person> transformCrewToPersonList(List<Credits.CrewMember> crew) {
        List<Person> people = new ArrayList<>();
        for (Credits.CrewMember crewMember : crew) {
            Person person = new Person();
            person.name = crewMember.name;
            person.description = crewMember.job;
            person.profilePath = crewMember.profile_path;
            people.add(person);
        }
        return people;
    }

    public static class Person {
        public String name;
        public String description;
        public String profilePath;
    }
}
