// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.people

import com.uwetrottmann.tmdb2.entities.Credits

class Person {
    var tmdbId = 0
    var name: String? = null
    var description: String? = null
    var profilePath: String? = null

    companion object {
        fun transformCastToPersonList(credits: Credits?): List<Person> {
            if (credits == null) return emptyList()
            val cast = credits.cast ?: return emptyList()
            val people: MutableList<Person> = ArrayList()
            for (member in cast) {
                val id = member.id ?: continue
                val name = member.name ?: continue

                val person = Person()
                person.tmdbId = id
                person.name = name
                person.description = member.character
                person.profilePath = member.profile_path
                people.add(person)
            }
            return people
        }

        fun transformCrewToPersonList(credits: Credits?): List<Person> {
            if (credits == null) return emptyList()
            val crew = credits.crew ?: return emptyList()
            val people: MutableList<Person> = ArrayList()
            for (member in crew) {
                val id = member.id ?: continue
                val name = member.name ?: continue

                val person = Person()
                person.tmdbId = id
                person.name = name
                person.description = member.job
                person.profilePath = member.profile_path
                people.add(person)
            }
            return people
        }
    }
}