package com.uwetrottmann.seriesguide;

import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.NamespaceManager;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.users.User;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Helper to ensure data is only returned to the appropriate user.
 *
 * A lot of this code was taken from the <a href="https://github.com/GoogleCloudPlatform/solutions-mobile-backend-starter-java/">Mobile
 * Backend Starter project</a>, licensed under the Apache License, Version 2.0 (the "License").
 */
public class Security {

    public static final String NAMESPACE_DEFAULT = ""; // empty namespace

    public static final String KIND_NAME_USERS = "Users";

    public static final String USERS_PROP_USERID = "userId";

    public static final String USER_ID_PREFIX = "USER_";

    private static final Security _instance = new Security();

    public static Security get() {
        return _instance;
    }

    public static void ensureIsValidUser(User user) throws UnauthorizedException {
        if (user == null) {
            throw new UnauthorizedException("Unauthenticated calls are not allowed");
        }
    }

    private static final DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

    private static final MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

    private static final Map<String, String> userIdCache = new HashMap<String, String>();

    /**
     * Creates a key under a namespace specific to the given user.
     */
    public Key createKey(String kindName, String id, User user) throws UnauthorizedException {
        // preserve namespace
        String origNamespace = NamespaceManager.get();

        // change to a namespace specific to the given user
        NamespaceManager.set(getUserId(user));

        // create user-unique key
        Key k = KeyFactory.createKey(kindName, id);

        // restore namespace
        NamespaceManager.set(origNamespace);
        return k;
    }

    /**
     * Returns a user id for the given user and persists it in the Datastore, or looks it up if it
     * is already.
     */
    public String getUserId(User user) throws UnauthorizedException {
        ensureIsValidUser(user);

        // check if valid email is available
        String email = user.getEmail();
        if (email == null || email.trim().length() == 0) {
            throw new IllegalArgumentException("Illegal email: " + email);
        }

        // try to find it on local cache
        String memKey = USER_ID_PREFIX + email;
        String id = userIdCache.get(memKey);
        if (id != null) {
            return id;
        }

        // try to find it on memcache
        id = (String) memcache.get(memKey);
        if (id != null) {
            userIdCache.put(memKey, id);
            return id;
        }

        // create a key to find the user on Datastore
        String origNamespace = NamespaceManager.get();
        NamespaceManager.set(NAMESPACE_DEFAULT);
        Key key = KeyFactory.createKey(KIND_NAME_USERS, email);
        NamespaceManager.set(origNamespace);

        // try to find it on Datastore
        Entity e;
        try {
            e = datastore.get(key);
            id = (String) e.getProperty(USERS_PROP_USERID);
        } catch (EntityNotFoundException ex) {
            // when the user has not been registered
            e = new Entity(key);
            id = USER_ID_PREFIX + UUID.randomUUID().toString();
            e.setProperty(USERS_PROP_USERID, id);
            datastore.put(e);
        }

        // put the user on memcache and local cache
        userIdCache.put(memKey, id);
        memcache.put(memKey, id);
        return id;
    }

}
