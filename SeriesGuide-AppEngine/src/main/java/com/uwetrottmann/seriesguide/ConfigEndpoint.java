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
import com.google.api.server.spi.response.UnauthorizedException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.users.User;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

@Api(
        name = "config",
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
public class ConfigEndpoint {

    private static final String DEFAULT_CONFIG_ID = "default";

    /**
     * This method gets the default config entity.
     */
    @ApiMethod(
            name = "get",
            path = "get"
    )
    public Config getDefaultConfig(User user) throws UnauthorizedException {
        Key key = Security.get().createKey(Config.class.getSimpleName(), DEFAULT_CONFIG_ID, user);

        EntityManager mgr = getEntityManager();
        Config config = null;
        try {
            config = mgr.find(Config.class, key);
        } finally {
            mgr.close();
        }
        return config;
    }

    /**
     * Inserts or updates the given configuration.
     */
    @ApiMethod(
            name = "save",
            path = "save"
    )
    public Config saveDefaultConfig(Config config, User user) throws UnauthorizedException {
        // create user-specific key
        config.setKey(
                Security.get().createKey(Config.class.getSimpleName(), DEFAULT_CONFIG_ID, user));

        EntityManager mgr = getEntityManager();
        try {
            Config existingConfig = mgr.find(Config.class, config.getKey());
            if (existingConfig != null) {
                // only update if there are valid changes
                if (existingConfig.shouldUpdateWith(config)) {
                    existingConfig.updateWith(config);
                    existingConfig.setUpdatedAt(new Date());

                    // save back to Datastore
                    mgr.merge(existingConfig);
                }

                config = existingConfig;
            } else {
                if (config.hasValidValues()) {
                    // no existing config, create new one
                    config.setCreatedAt(new Date());
                    config.setUpdatedAt(config.getCreatedAt());
                    config.setOwner(user.getEmail());

                    // save to Datastore
                    mgr.persist(config);
                } else {
                    config = null;
                }
            }
        } finally {
            mgr.close();
        }

        return config;
    }

    private static EntityManager getEntityManager() {
        return EMF.get().createEntityManager();
    }

}
