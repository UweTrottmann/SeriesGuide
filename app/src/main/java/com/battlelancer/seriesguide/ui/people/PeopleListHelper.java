package com.battlelancer.seriesguide.ui.people;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.battlelancer.seriesguide.R;
import com.battlelancer.seriesguide.util.ServiceUtils;
import com.battlelancer.seriesguide.util.TmdbTools;
import com.battlelancer.seriesguide.util.Utils;
import com.uwetrottmann.tmdb2.entities.CastMember;
import com.uwetrottmann.tmdb2.entities.Credits;
import com.uwetrottmann.tmdb2.entities.CrewMember;
import java.util.ArrayList;
import java.util.List;
import timber.log.Timber;

/**
 * Helps load a fixed number of people into a static layout.
 */
public class PeopleListHelper {

    public static boolean populateShowCast(Activity activity,
            ViewGroup peopleContainer, Credits credits) {
        return populateCast(activity, peopleContainer, credits, PeopleActivity.MediaType.SHOW);
    }

    public static boolean populateShowCrew(Activity activity,
            ViewGroup peopleContainer, Credits credits) {
        return populateCrew(activity, peopleContainer, credits, PeopleActivity.MediaType.SHOW);
    }

    public static boolean populateMovieCast(Activity activity,
            ViewGroup peopleContainer, Credits credits) {
        return populateCast(activity, peopleContainer, credits, PeopleActivity.MediaType.MOVIE);
    }

    public static boolean populateMovieCrew(Activity activity,
            ViewGroup peopleContainer, Credits credits) {
        return populateCrew(activity, peopleContainer, credits, PeopleActivity.MediaType.MOVIE);
    }

    /**
     * Add views for at most three cast members to the given {@link android.view.ViewGroup} and a
     * "Show all" link if there are more.
     */
    private static boolean populateCast(Activity activity, ViewGroup peopleContainer,
            Credits credits, PeopleActivity.MediaType mediaType) {
        if (peopleContainer == null) {
            // nothing we can do, view is already gone
            Timber.d("populateCast: container reference gone, aborting");
            return false;
        }
        if (credits.id == null) {
            return false; // missing required values
        }

        peopleContainer.removeAllViews();

        // show at most 3 cast members
        LayoutInflater inflater = LayoutInflater.from(peopleContainer.getContext());
        List<CastMember> cast = credits.cast;
        int added = 0;
        for (CastMember castMember : cast) {
            if (added == 3) {
                break; // not more than 3
            }
            if (castMember.id == null) {
                continue; // missing required values
            }

            View personView = createPersonView(activity, inflater, peopleContainer, castMember.name,
                    castMember.character, castMember.profile_path);
            personView.setOnClickListener(
                    new OnPersonClickListener(activity, mediaType, credits.id,
                            PeopleActivity.PeopleType.CAST, castMember.id)
            );

            peopleContainer.addView(personView);
            added++;
        }

        if (cast.size() > 3) {
            addShowAllView(inflater, peopleContainer,
                    new OnPersonClickListener(activity, mediaType, credits.id,
                            PeopleActivity.PeopleType.CAST)
            );
        }

        return added > 0;
    }

    /**
     * Add views for at most three crew members to the given {@link android.view.ViewGroup} and a
     * "Show all" link if there are more.
     */
    private static boolean populateCrew(Activity activity, ViewGroup peopleContainer,
            Credits credits, PeopleActivity.MediaType mediaType) {
        if (peopleContainer == null) {
            // nothing we can do, view is already gone
            Timber.d("populateCrew: container reference gone, aborting");
            return false;
        }
        if (credits.id == null) {
            return false; // missing required values
        }

        peopleContainer.removeAllViews();

        // show at most 3 crew members
        LayoutInflater inflater = LayoutInflater.from(peopleContainer.getContext());
        List<CrewMember> crew = credits.crew;
        int added = 0;
        for (CrewMember crewMember : crew) {
            if (added == 3) {
                break; // not more than 3
            }
            if (crewMember.id == null) {
                continue; // missing required values
            }

            View personView = createPersonView(activity, inflater, peopleContainer, crewMember.name,
                    crewMember.job, crewMember.profile_path);
            personView.setOnClickListener(
                    new OnPersonClickListener(activity, mediaType, credits.id,
                            PeopleActivity.PeopleType.CREW, crewMember.id)
            );

            peopleContainer.addView(personView);
            added++;
        }

        if (crew.size() > 3) {
            addShowAllView(inflater, peopleContainer,
                    new OnPersonClickListener(activity, mediaType, credits.id,
                            PeopleActivity.PeopleType.CREW)
            );
        }

        return added > 0;
    }

    private static View createPersonView(Context context, LayoutInflater inflater,
            ViewGroup peopleContainer, String name, String description, String profilePath) {
        View personView = inflater.inflate(R.layout.item_person, peopleContainer, false);

        // use clickable instead of activatable background
        personView.setBackgroundResource(
                Utils.resolveAttributeToResourceId(peopleContainer.getContext().getTheme(),
                        R.attr.selectableItemBackground));
        // support keyboard nav
        personView.setFocusable(true);

        ServiceUtils.loadWithPicasso(context, TmdbTools.buildProfileImageUrl(context, profilePath,
                TmdbTools.ProfileImageSize.W185))
                .resizeDimen(R.dimen.person_headshot_size, R.dimen.person_headshot_size)
                .centerCrop()
                .error(R.color.protection_dark)
                .into((ImageView) personView.findViewById(R.id.imageViewPerson));

        TextView nameView = personView.findViewById(R.id.textViewPerson);
        nameView.setText(name);

        TextView descriptionView = personView.findViewById(R.id.textViewPersonDescription);
        descriptionView.setText(description);

        return personView;
    }

    private static class OnPersonClickListener implements View.OnClickListener {

        private final Activity activity;
        private final int itemTmdbId;
        private final int personTmdbId;
        private final PeopleActivity.PeopleType peopleType;
        private final PeopleActivity.MediaType mediaType;

        /**
         * Listener that will show cast or crew members for the given TMDb entity.
         */
        public OnPersonClickListener(Activity activity, PeopleActivity.MediaType mediaType,
                int mediaTmdbId, PeopleActivity.PeopleType peopleType) {
            this(activity, mediaType, mediaTmdbId, peopleType, -1);
        }

        /**
         * Listener that will show cast or crew members for the given TMDb entity and pre-selects a
         * specific cast or crew member.
         */
        public OnPersonClickListener(Activity activity, PeopleActivity.MediaType mediaType,
                int mediaTmdbId, PeopleActivity.PeopleType peopleType, int personTmdbId) {
            this.activity = activity;
            this.itemTmdbId = mediaTmdbId;
            this.peopleType = peopleType;
            this.mediaType = mediaType;
            this.personTmdbId = personTmdbId;
        }

        @Override
        public void onClick(View v) {
            Intent i = new Intent(v.getContext(), PeopleActivity.class);
            i.putExtra(PeopleActivity.InitBundle.ITEM_TMDB_ID, itemTmdbId);
            i.putExtra(PeopleActivity.InitBundle.PEOPLE_TYPE, peopleType.toString());
            i.putExtra(PeopleActivity.InitBundle.MEDIA_TYPE, mediaType.toString());
            if (personTmdbId != -1) {
                // showing a specific person
                i.putExtra(PersonFragment.ARG_PERSON_TMDB_ID, personTmdbId);
            }
            Utils.startActivityWithAnimation(activity, i, v);
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
