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
import javax.persistence.EntityManager;
import javax.persistence.Query;

@Api(
        name = "shows",
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
public class ShowEndpoint {

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
    public CollectionResponse<Show> listShow(
            @Nullable @Named("cursor") String cursorString,
            @Nullable @Named("limit") Integer limit,
            User user) throws UnauthorizedException {
        EntityManager mgr = null;
        List<Show> execute = null;

        String origNamespace = NamespaceManager.get();
        try {
            // access user specific namespace
            NamespaceManager.set(Security.get().getUserId(user));

            mgr = getEntityManager();
            Query query = mgr.createQuery("select from Show as Show");
            Cursor cursor;
            if (cursorString != null && cursorString.trim().length() > 0) {
                cursor = Cursor.fromWebSafeString(cursorString);
                query.setHint(JPACursorHelper.CURSOR_HINT, cursor);
            }

            if (limit != null) {
                query.setFirstResult(0);
                query.setMaxResults(limit);
            }

            execute = (List<Show>) query.getResultList();
            cursor = JPACursorHelper.getCursor(execute);
            if (cursor != null) {
                cursorString = cursor.toWebSafeString();
            }

            // Tight loop for fetching all entities from datastore and accomodate
            // for lazy fetch.
            for (Show obj : execute) {
                ;
            }
        } finally {
            if (mgr != null) {
                mgr.close();
            }
            NamespaceManager.set(origNamespace);
        }

        return CollectionResponse.<Show>builder()
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
    public Show getShow(@Named("tvdbid") int tvdbId, User user) throws UnauthorizedException {
        Key key = Security.get()
                .createKey(Show.class.getSimpleName(), String.valueOf(tvdbId), user);

        EntityManager mgr = getEntityManager();
        Show show = null;
        try {
            show = mgr.find(Show.class, key);
        } finally {
            mgr.close();
        }
        return show;
    }

    /**
     * Inserts or updates the given show in(to) the Datastore.
     */
    @ApiMethod(
            name = "save",
            path = "save"
    )
    public ShowList saveShows(ShowList shows, User user) throws UnauthorizedException {
        // create user-specific keys for all shows
        for (Show show : shows.getShows()) {
            show.setKey(Security.get().createKey(Show.class.getSimpleName(),
                    String.valueOf(show.getTvdbId()), user));
        }

        // update existing shows
        Map<Integer, Show> existingShows = findAndUpdateExistingShows(shows.getShows(), user);

        // insert new shows
        List<Show> newShows = insertNewShows(shows.getShows(), existingShows.keySet(), user);

        // return all shows
        newShows.addAll(existingShows.values());
        shows.setShows(newShows);

        return shows;
    }

    private Map<Integer, Show> findAndUpdateExistingShows(List<Show> shows, User user)
            throws UnauthorizedException {
        Map<Integer, Show> existingShows = new HashMap<Integer, Show>();

        for (Show show : shows) {
            EntityManager mgr = getEntityManager();
            try {
                // show with this key already exists?
                Show existingShow = mgr.find(Show.class, show.getKey());

                if (existingShow != null) {
                    // only update if there are changes
                    if (!existingShow.shouldUpdateWith(show)) {
                        // set updated values
                        existingShow.updateWith(show);
                        existingShow.setUpdatedAt(new Date());

                        // save back to Datastore
                        mgr.merge(existingShow);
                    }

                    // flag as existing
                    existingShows.put(existingShow.getTvdbId(), existingShow);
                }
            } finally {
                mgr.close();
            }
        }

        return existingShows;
    }

    private List<Show> insertNewShows(List<Show> shows, Set<Integer> existingShows, User user) {
        List<Show> newShows = new LinkedList<Show>();

        for (Show show : shows) {
            if (existingShows.contains(show.getTvdbId())) {
                // already was updated
                continue;
            }

            // create metadata
            show.setCreatedAt(new Date());
            show.setUpdatedAt(show.getCreatedAt());
            show.setOwner(user.getEmail());

            // insert into Datastore
            EntityManager mgr = getEntityManager();
            try {
                mgr.persist(show);
            } finally {
                mgr.close();
            }

            newShows.add(show);
        }

        return newShows;
    }

    @ApiMethod(
            name = "remove",
            path = "remove/{tvdbid}"
    )
    public Show removeShow(@Named("tvdbid") int tvdbId, User user) throws UnauthorizedException {
        Key key = Security.get()
                .createKey(Show.class.getSimpleName(), String.valueOf(tvdbId), user);

        EntityManager mgr = getEntityManager();
        Show show = null;
        try {
            show = mgr.find(Show.class, key);
            mgr.remove(show);
        } finally {
            mgr.close();
        }
        return show;
    }

    private static EntityManager getEntityManager() {
        return EMF.get().createEntityManager();
    }

}
