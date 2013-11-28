package com.uwetrottmann.seriesguide;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.api.server.spi.response.CollectionResponse;
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;
import com.google.appengine.datanucleus.query.JPACursorHelper;

import java.util.List;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityManager;
import javax.persistence.Query;

@Api(name = "shows",
        scopes = {Constants.EMAIL_SCOPE},
        clientIds = {com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID},
        namespace = @ApiNamespace(ownerDomain = "uwetrottmann.com", ownerName = "uwetrottmann.com",
                packagePath = "seriesguide"))
public class ShowEndpoint {

    /**
     * This method lists all the entities inserted in datastore. It uses HTTP GET method and paging
     * support.
     *
     * @return A CollectionResponse class containing the list of all entities persisted and a cursor
     * to the next page.
     */
    @SuppressWarnings({"unchecked", "unused"})
    @ApiMethod(name = "listShow")
    public CollectionResponse<Show> listShow(
            @Nullable @Named("cursor") String cursorString,
            @Nullable @Named("limit") Integer limit) {

        EntityManager mgr = null;
        List<Show> execute = null;

        try {
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
            name = "shows.get",
            path = "shows/{id}"
    )
    public Show getShow(@Named("id") Key id, User user) throws UnauthorizedException {
        Security.ensureIsValidUser(user);
        EntityManager mgr = getEntityManager();
        Show show = null;
        try {
            show = mgr.find(Show.class, id);
        } finally {
            mgr.close();
        }
        return show;
    }

    /**
     * This inserts a new entity into App Engine datastore. If the entity already exists in the
     * datastore, an exception is thrown. It uses HTTP POST method.
     */
    @ApiMethod(
            name = "shows.insert",
            path = "shows/add"
    )
    public Show insertShow(Show show, User user) throws UnauthorizedException {
        // create user-specific key
        show.setKey(Security.get().createKey(Show.class.getSimpleName(),
                String.valueOf(show.getTvdbId()), user));

        EntityManager mgr = getEntityManager();
        try {
            if (containsShow(show)) {
                throw new EntityExistsException("Object already exists");
            }
            mgr.persist(show);
        } finally {
            mgr.close();
        }
        return show;
    }

    /**
     * This method is used for updating an existing entity. If the entity does not exist in the
     * datastore, an exception is thrown. It uses HTTP PUT method.
     *
     * @param show the entity to be updated.
     * @return The updated entity.
     */
    @ApiMethod(name = "updateShow")
    public Show updateShow(Show show) {
        EntityManager mgr = getEntityManager();
        try {
            if (!containsShow(show)) {
                throw new EntityNotFoundException("Object does not exist");
            }
            mgr.persist(show);
        } finally {
            mgr.close();
        }
        return show;
    }

    /**
     * This method removes the entity with primary key id. It uses HTTP DELETE method.
     *
     * @param id the primary key of the entity to be deleted.
     * @return The deleted entity.
     */
    @ApiMethod(name = "removeShow")
    public Show removeShow(@Named("id") Key id) {
        EntityManager mgr = getEntityManager();
        Show show = null;
        try {
            show = mgr.find(Show.class, id);
            mgr.remove(show);
        } finally {
            mgr.close();
        }
        return show;
    }

    private boolean containsShow(Show show) {
        EntityManager mgr = getEntityManager();
        boolean contains = true;
        try {
            Show item = mgr.find(Show.class, show.getKey());
            if (item == null) {
                contains = false;
            }
        } finally {
            mgr.close();
        }
        return contains;
    }

    private static EntityManager getEntityManager() {
        return EMF.get().createEntityManager();
    }

}
