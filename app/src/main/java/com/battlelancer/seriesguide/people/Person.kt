// SPDX-License-Identifier: Apache-2.0
// Copyright 2024 Uwe Trottmann

package com.battlelancer.seriesguide.people

import com.uwetrottmann.tmdb2.entities.CastMember
import com.uwetrottmann.tmdb2.entities.CrewMember

class Person {
    var tmdbId = 0
    var name: String? = null
    var description: String? = null
    var profilePath: String? = null

    companion object {
        fun transformCastToPersonList(cast: List<CastMember>): List<Person> {
            val people: MutableList<Person> = ArrayList()
            for (castMember in cast) {
                val person = Person()
                person.tmdbId = castMember.id
                person.name = castMember.name
                person.description = castMember.character
                person.profilePath = castMember.profile_path
                people.add(person)
            }
            return people
        }

        fun transformCrewToPersonList(crew: List<CrewMember>): List<Person> {
            val people: MutableList<Person> = ArrayList()
            for (crewMember in crew) {
                val person = Person()
                person.tmdbId = crewMember.id
                person.name = crewMember.name
                person.description = crewMember.job
                person.profilePath = crewMember.profile_path
                people.add(person)
            }
            return people
        }
    }
}