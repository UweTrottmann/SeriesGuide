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

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.ui.PeopleActivity;
import com.battlelancer.seriesguide.ui.PersonFragment;
import com.uwetrottmann.androidutils.AndroidUtils;
import com.uwetrottmann.tmdb.entities.CastMember;
import com.uwetrottmann.tmdb.entities.Credits;
import com.uwetrottmann.tmdb.entities.CrewMember;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

/**
 * Helps load a fixed number of people into a static layout.
 */
public class PeopleListHelper {

    public static void populateShowCast(Context context, LayoutInflater inflater,
            ViewGroup peopleContainer, Credits credits) {
        populateCast(context, inflater, peopleContainer, credits, PeopleActivity.MediaType.SHOW);
    }

    public static void populateShowCrew(Context context, LayoutInflater inflater,
            ViewGroup peopleContainer, Credits credits) {
        populateCrew(context, inflater, peopleContainer, credits, PeopleActivity.MediaType.SHOW);
    }

    public static void populateMovieCast(Context context, LayoutInflater inflater,
            ViewGroup peopleContainer, Credits credits) {
        populateCast(context, inflater, peopleContainer, credits, PeopleActivity.MediaType.MOVIE);
    }

    public static void populateMovieCrew(Context context, LayoutInflater inflater,
            ViewGroup peopleContainer, Credits credits) {
        populateCrew(context, inflater, peopleContainer, credits, PeopleActivity.MediaType.MOVIE);
    }

    /**
     * Add views for at most three cast members to the given {@link android.view.ViewGroup} and a
     * "Show all" link if there are more.
     */
    private static void populateCast(Context context, LayoutInflater inflater,
            ViewGroup peopleContainer, Credits credits, PeopleActivity.MediaType mediaType) {
        if (peopleContainer == null) {
            // nothing we can do, view is already gone
            Timber.d("populateCast: container reference gone, aborting");
            return;
        }

        peopleContainer.removeAllViews();

        // show at most 3 cast members
        List<CastMember> cast = credits.cast;
        for (int i = 0; i < Math.min(3, cast.size()); i++) {
            CastMember castMember = cast.get(i);

            View personView = createPersonView(context, inflater, peopleContainer, castMember.name,
                    castMember.character, castMember.profile_path);
            personView.setOnClickListener(
                    new OnPersonClickListener(mediaType, credits.id, PeopleActivity.PeopleType.CAST,
                            castMember.id)
            );

            peopleContainer.addView(personView);
        }

        if (cast.size() > 3) {
            addShowAllView(inflater, peopleContainer,
                    new OnPersonClickListener(mediaType, credits.id, PeopleActivity.PeopleType.CAST)
            );
        }
    }

    /**
     * Add views for at most three crew members to the given {@link android.view.ViewGroup} and a
     * "Show all" link if there are more.
     */
    private static void populateCrew(Context context, LayoutInflater inflater,
            ViewGroup peopleContainer, Credits credits, PeopleActivity.MediaType mediaType) {
        if (peopleContainer == null) {
            // nothing we can do, view is already gone
            Timber.d("populateCrew: container reference gone, aborting");
            return;
        }

        peopleContainer.removeAllViews();

        // show at most 3 crew members
        List<CrewMember> crew = credits.crew;
        for (int i = 0; i < Math.min(3, crew.size()); i++) {
            CrewMember castMember = crew.get(i);

            View personView = createPersonView(context, inflater, peopleContainer, castMember.name,
                    castMember.job, castMember.profile_path);
            personView.setOnClickListener(
                    new OnPersonClickListener(mediaType, credits.id, PeopleActivity.PeopleType.CREW,
                            castMember.id)
            );

            peopleContainer.addView(personView);
        }

        if (crew.size() > 3) {
            addShowAllView(inflater, peopleContainer,
                    new OnPersonClickListener(mediaType, credits.id, PeopleActivity.PeopleType.CREW)
            );
        }
    }

    private static View createPersonView(Context context, LayoutInflater inflater,
            ViewGroup peopleContainer, String name, String description, String profilePath) {
        View personView = inflater.inflate(R.layout.item_person, peopleContainer, false);

        // use clickable instead of activatable background
        personView.setBackgroundResource(Utils.resolveAttributeToResourceId(context.getTheme(),
                R.attr.selectableItemBackground));
        // support keyboard nav
        personView.setFocusable(true);

        ServiceUtils.loadWithPicasso(context, TmdbTools.buildProfileImageUrl(context, profilePath,
                TmdbTools.ProfileImageSize.W185))
                .resizeDimen(R.dimen.person_headshot_size, R.dimen.person_headshot_size)
                .centerCrop()
                .error(R.color.protection_dark)
                .into((ImageView) personView.findViewById(R.id.imageViewPerson));

        TextView nameView = (TextView) personView.findViewById(R.id.textViewPerson);
        nameView.setText(name);

        TextView descriptionView = (TextView) personView.findViewById(
                R.id.textViewPersonDescription);
        descriptionView.setText(description);

        return personView;
    }

    private static class OnPersonClickListener implements View.OnClickListener {

        private final int mItemTmdbId;
        private final int mPersonTmdbId;
        private final PeopleActivity.PeopleType mPeopleType;
        private final PeopleActivity.MediaType mMediaType;

        /**
         * Listener that will show cast or crew members for the given TMDb entity.
         */
        public OnPersonClickListener(PeopleActivity.MediaType mediaType, int mediaTmdbId,
                PeopleActivity.PeopleType peopleType) {
            this(mediaType, mediaTmdbId, peopleType, -1);
        }

        /**
         * Listener that will show cast or crew members for the given TMDb entity and pre-selects a
         * specific cast or crew member.
         */
        public OnPersonClickListener(PeopleActivity.MediaType mediaType, int mediaTmdbId,
                PeopleActivity.PeopleType peopleType, int personTmdbId) {
            mItemTmdbId = mediaTmdbId;
            mPeopleType = peopleType;
            mMediaType = mediaType;
            mPersonTmdbId = personTmdbId;
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void onClick(View v) {
            Intent i = new Intent(v.getContext(), PeopleActivity.class);
            i.putExtra(PeopleActivity.InitBundle.ITEM_TMDB_ID, mItemTmdbId);
            i.putExtra(PeopleActivity.InitBundle.PEOPLE_TYPE, mPeopleType.toString());
            i.putExtra(PeopleActivity.InitBundle.MEDIA_TYPE, mMediaType.toString());
            if (mPersonTmdbId != -1) {
                i.putExtra(PersonFragment.InitBundle.PERSON_TMDB_ID, mPersonTmdbId);
            }

            if (AndroidUtils.isJellyBeanOrHigher()) {
                v.getContext()
                        .startActivity(i,
                                ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(),
                                        v.getHeight()).toBundle());
            } else {
                v.getContext().startActivity(i);
            }
        }
    }

    private static void addShowAllView(LayoutInflater inflater, ViewGroup peopleContainer,
            View.OnClickListener clickListener) {
        TextView showAllView = (TextView) inflater.inflate(R.layout.item_action_add,
                peopleContainer, false);
        showAllView.setText(R.string.action_display_all);
        showAllView.setOnClickListener(clickListener);
        peopleContainer.addView(showAllView);
    }

    public static List<Person> transformCastToPersonList(List<CastMember> cast) {
        List<Person> people = new ArrayList<>();
        for (CastMember castMember : cast) {
            Person person = new Person();
            person.tmdbId = castMember.id;
            person.name = castMember.name;
            person.description = castMember.character;
            person.profilePath = castMember.profile_path;
            people.add(person);
        }
        return people;
    }

    public static List<Person> transformCrewToPersonList(List<CrewMember> crew) {
        List<Person> people = new ArrayList<>();
        for (CrewMember crewMember : crew) {
            Person person = new Person();
            person.tmdbId = crewMember.id;
            person.name = crewMember.name;
            person.description = crewMember.job;
            person.profilePath = crewMember.profile_path;
            people.add(person);
        }
        return people;
    }

    public static class Person {
        public int tmdbId;
        public String name;
        public String description;
        public String profilePath;
    }
}
