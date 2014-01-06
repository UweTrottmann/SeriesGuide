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

package com.uwetrottmann.seriesguide;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;
import com.google.appengine.datanucleus.query.JPACursorHelper;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;

@Api(
        name = "episodes",
        version = "v1",
        scopes = {
                Constants.EMAIL_SCOPE
        },
        clientIds = {
                Constants.API_EXPLORER_CLIENT_ID,
                Constants.WEB_CLIENT_ID,
                Constants.ANDROID_BETA_CLIENT_ID,
                Constants.ANDROID_RELEASE_CLIENT_ID
        },
        audiences = {
                Constants.ANDROID_AUDIENCE
        },
        namespace = @ApiNamespace(ownerDomain = "uwetrottmann.com", ownerName = "uwetrottmann.com",
                packagePath = "seriesguide")
)
public class EpisodeEndpoint {

    /**
     * This method lists all the entities inserted in datastore. It uses HTTP GET method and paging
     * support.
     *
     * @return A CollectionResponse class containing the list of all entities persisted and a cursor
     * to the next page.
     */
    @SuppressWarnings({"unchecked", "unused"})
    @ApiMethod(
            name = "list",
            path = "list"
    )
    public CollectionResponse<Episode> listEpisode(
            @Nullable @Named("cursor") String cursorString,
            @Nullable @Named("limit") Integer limit,
            User user) throws UnauthorizedException {
        EntityManager mgr = null;
        List<Episode> execute = null;

        String origNamespace = NamespaceManager.get();
        try {
            // access user specific namespace
            NamespaceManager.set(Security.get().getUserId(user));

            mgr = getEntityManager();
            Query query = mgr.createQuery("select from Episode as Episode");
            Cursor cursor;
            if (cursorString != null && cursorString.trim().length() > 0) {
                cursor = Cursor.fromWebSafeString(cursorString);
                query.setHint(JPACursorHelper.CURSOR_HINT, cursor);
            }

            if (limit != null) {
                query.setFirstResult(0);
                query.setMaxResults(limit);
            }

            execute = (List<Episode>) query.getResultList();
            cursor = JPACursorHelper.getCursor(execute);
            if (cursor != null) {
                cursorString = cursor.toWebSafeString();
            }

            // Tight loop for fetching all entities from datastore and accomodate
            // for lazy fetch.
            for (Episode obj : execute) {
                ;
            }
        } finally {
            if (mgr != null) {
                mgr.close();
            }
            NamespaceManager.set(origNamespace);
        }

        return CollectionResponse.<Episode>builder()
                .setItems(execute)
                .setNextPageToken(cursorString)
                .build();
    }

    /**
     * This method gets the entity having primary key id. It uses HTTP GET method.
     */
    @ApiMethod(
            name = "get",
            path = "get/{tvdbid}"
    )
    public Episode getEpisode(@Named("tvdbid") int tvdbId, User user) throws UnauthorizedException {
        Key key = Security.get()
                .createKey(Show.class.getSimpleName(), String.valueOf(tvdbId), user);

        EntityManager mgr = getEntityManager();
        Episode episode = null;
        try {
            episode = mgr.find(Episode.class, key);
        } finally {
            mgr.close();
        }
        return episode;
    }

    /**
     * Inserts or updates the given episode(s) in(to) the Datastore.
     */
    @ApiMethod(
            name = "save",
            path = "save"
    )
    public EpisodeList saveEpisodes(EpisodeList episodes, User user) throws UnauthorizedException {
        // create user-specific keys for all entities
        for (Episode episode : episodes.getEpisodes()) {
            episode.setKey(Security.get().createKey(Episode.class.getSimpleName(),
                    String.valueOf(episode.getTvdbId()), user));
        }

        // update existing shows
        Map<Integer, Episode> existingEpisodes = findAndUpdateExistingEpisodes(
                episodes.getEpisodes(), user);

        // insert new shows
        List<Episode> newEpisodes = insertNewEpisodes(episodes.getEpisodes(),
                existingEpisodes.keySet(), user);

        // return all shows
        newEpisodes.addAll(existingEpisodes.values());
        episodes.setEpisodes(newEpisodes);

        return episodes;
    }

    private Map<Integer, Episode> findAndUpdateExistingEpisodes(List<Episode> episodes, User user)
            throws UnauthorizedException {
        Map<Integer, Episode> existingEpisodes = new HashMap<Integer, Episode>();

        for (Episode episode : episodes) {
            EntityManager mgr = getEntityManager();
            try {
                // episode with this key already exists?
                Episode existingEpisode = mgr.find(Episode.class, episode.getKey());

                if (existingEpisode != null) {
                    // only update if there are changes
                    if (!existingEpisode.hasSameValues(episode)) {
                        // set updated values
                        existingEpisode.copyPropertyValues(episode);
                        existingEpisode.setUpdatedAt(new Date());

                        // save back to Datastore
                        mgr.merge(existingEpisode);
                    }

                    // flag as existing
                    existingEpisodes.put(existingEpisode.getTvdbId(), existingEpisode);
                }
            } finally {
                mgr.close();
            }
        }

        return existingEpisodes;
    }

    private List<Episode> insertNewEpisodes(List<Episode> episodes, Set<Integer> existingEpisodes,
            User user) {
        List<Episode> newEpisodes = new LinkedList<Episode>();

        for (Episode episode : episodes) {
            if (existingEpisodes.contains(episode.getTvdbId())) {
                // already was updated
                continue;
            }

            // create metadata
            episode.setCreatedAt(new Date());
            episode.setUpdatedAt(episode.getCreatedAt());
            episode.setOwner(user.getEmail());

            // insert into Datastore
            EntityManager mgr = getEntityManager();
            try {
                mgr.persist(episode);
            } finally {
                mgr.close();
            }

            newEpisodes.add(episode);
        }

        return newEpisodes;
    }

    @ApiMethod(
            name = "remove",
            path = "remove/{tvdbid}"
    )
    public Episode removeEpisode(@Named("tvdbid") int tvdbId, User user)
            throws UnauthorizedException {
        Key key = Security.get()
                .createKey(Episode.class.getSimpleName(), String.valueOf(tvdbId), user);

        EntityManager mgr = getEntityManager();
        Episode episode = null;
        try {
            episode = mgr.find(Episode.class, key);
            mgr.remove(episode);
        } finally {
            mgr.close();
        }
        return episode;
    }

    private static EntityManager getEntityManager() {
        return EMF.get().createEntityManager();
    }

}
