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

package com.battlelancer.seriesguide.extensions;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Episode;
import com.battlelancer.seriesguide.api.SeriesGuideExtension;
import com.battlelancer.seriesguide.api.internal.IncomingConstants;
import de.greenrobot.event.EventBus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import timber.log.Timber;

public class ExtensionManager {

    private static final String PREF_FILE_SUBSCRIPTIONS = "seriesguide_extensions";
    private static final String PREF_SUBSCRIPTIONS = "subscriptions";

    private static final int HARD_CACHE_CAPACITY = 5;

    // Cashes received actions for the last few displayed episodes.
    private final static android.support.v4.util.LruCache<Integer, Map<ComponentName, Action>>
            sEpisodeActionsCache = new android.support.v4.util.LruCache<>(HARD_CACHE_CAPACITY);

    private static ExtensionManager _instance;

    public static synchronized ExtensionManager getInstance(Context context) {
        if (_instance == null) {
            _instance = new ExtensionManager(context);
        }
        return _instance;
    }

    public static class EpisodeActionReceivedEvent {
        public int episodeTvdbId;

        public EpisodeActionReceivedEvent(int episodeTvdbId) {
            this.episodeTvdbId = episodeTvdbId;
        }
    }

    private Context mContext;
    private SharedPreferences mSharedPrefs;
    private ComponentName mSubscriberComponentName;

    private Map<ComponentName, String> mSubscriptions;
    private Map<String, ComponentName> mTokens; // mirrored map for faster token searching

    private ExtensionManager(Context context) {
        Timber.d("Initializing extension manager");
        mContext = context.getApplicationContext();
        mSharedPrefs = mContext.getSharedPreferences(PREF_FILE_SUBSCRIPTIONS, 0);
        mSubscriptions = new HashMap<>();
        mTokens = new HashMap<>();
        mSubscriberComponentName = new ComponentName(mContext, ExtensionSubscriberService.class);
        loadSubscriptions();
    }

    public List<Extension> queryAllAvailableExtensions() {
        Intent queryIntent = new Intent(SeriesGuideExtension.ACTION_SERIESGUIDE_EXTENSION);
        PackageManager pm = mContext.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(queryIntent,
                PackageManager.GET_META_DATA);

        List<Extension> extensions = new ArrayList<>();
        for (ResolveInfo info : resolveInfos) {
            Extension extension = new Extension();
            // get label, icon and component name
            extension.label = info.loadLabel(pm).toString();
            extension.icon = info.loadIcon(pm);
            extension.componentName = new ComponentName(info.serviceInfo.packageName,
                    info.serviceInfo.name);
            // get description
            Context packageContext;
            try {
                packageContext = mContext.createPackageContext(
                        extension.componentName.getPackageName(), 0);
                Resources packageRes = packageContext.getResources();
                extension.description = packageRes.getString(info.serviceInfo.descriptionRes);
            } catch (SecurityException | PackageManager.NameNotFoundException e) {
                Timber.e(e, "Reading package resources for extension " + extension.componentName
                        + " failed");
            }
            // get (optional) settings activity
            Bundle metaData = info.serviceInfo.metaData;
            if (metaData != null) {
                String settingsActivity = metaData.getString("settingsActivity");
                if (!TextUtils.isEmpty(settingsActivity)) {
                    extension.settingsActivity = ComponentName.unflattenFromString(
                            info.serviceInfo.packageName + "/" + settingsActivity);
                }
            }

            Timber.d("queryAllAvailableExtensions: found extension " + extension.label + " "
                    + extension.componentName);
            extensions.add(extension);
        }

        return extensions;
    }

    /**
     * Returns whether the given extension is currently enabled and will be nudged for actions when
     * {@link #requestActions(com.battlelancer.seriesguide.api.Episode)} is called.
     */
    public synchronized boolean isExtensionEnabled(ComponentName extension) {
        return mSubscriptions.get(extension) != null;
    }

    public void enableExtension(ComponentName extension) {
        if (extension == null) {
            Timber.e("enableExtension: empty extension");
        }

        synchronized (this) {
            if (mSubscriptions.containsKey(extension)) {
                // already subscribed
                Timber.d("enableExtension: already subscribed to " + extension);
                return;
            }

            // subscribe
            String token = UUID.randomUUID().toString();
            while (mTokens.containsKey(token)) {
                // create another UUID on collision
                /**
                 * As the number of enabled extensions is rather low compared to the UUID number
                 * space we shouldn't have to worry about this ever looping.
                 */
                token = UUID.randomUUID().toString();
            }
            Timber.d("enableExtension: subscribing to " + extension);
            mSubscriptions.put(extension, token);
            mTokens.put(token, extension);
            mContext.startService(new Intent(IncomingConstants.ACTION_SUBSCRIBE)
                    .setComponent(extension)
                    .putExtra(IncomingConstants.EXTRA_SUBSCRIBER_COMPONENT,
                            mSubscriberComponentName)
                    .putExtra(IncomingConstants.EXTRA_TOKEN, token));
        }

        saveSubscriptions();

        // TODO notify about enabled extension
    }

    public void disableExtension(ComponentName extension) {
        if (extension == null) {
            Timber.e("disableExtension: extension empty");
        }

        synchronized (this) {
            if (!mSubscriptions.containsKey(extension)) {
                Timber.d("disableExtension: extension not enabled " + extension);
                return;
            }

            // unsubscribe
            Timber.d("disableExtension: unsubscribing from " + extension);
            mContext.startService(new Intent(IncomingConstants.ACTION_SUBSCRIBE)
                    .setComponent(extension)
                    .putExtra(IncomingConstants.EXTRA_SUBSCRIBER_COMPONENT,
                            mSubscriberComponentName)
                    .putExtra(IncomingConstants.EXTRA_TOKEN, (String) null));
            mTokens.remove(mSubscriptions.remove(extension));
        }

        saveSubscriptions();

        // TODO notify about disabled extension
    }

    /**
     * Returns the currently available {@link com.battlelancer.seriesguide.api.Action} list for the
     * given episode, identified through its TVDb id.
     */
    public synchronized List<Action> getLatestEpisodeActions(int episodeTvdbId) {
        Map<ComponentName, Action> actionMap = sEpisodeActionsCache.get(episodeTvdbId);
        if (actionMap == null) {
            return null;
        }
        return new ArrayList<>(actionMap.values());
    }

    /**
     * Asks all enabled extensions to publish an action for the given episode.
     */
    public synchronized void requestActions(Episode episode) {
        for (ComponentName extension : mSubscriptions.keySet()) {
            requestAction(extension, episode);
        }
    }

    /**
     * Ask a single extension to publish an action for the given episode.
     */
    public synchronized void requestAction(ComponentName extension, Episode episode) {
        Timber.d("requestAction: requesting from " + extension + " for " + episode.getTvdbId());
        // prepare to receive actions for the given episode
        if (sEpisodeActionsCache.get(episode.getTvdbId()) == null) {
            sEpisodeActionsCache.put(episode.getTvdbId(), new HashMap<ComponentName, Action>());
        }
        // actually request actions
        mContext.startService(new Intent(IncomingConstants.ACTION_UPDATE)
                .setComponent(extension)
                .putExtra(IncomingConstants.EXTRA_ENTITY_IDENTIFIER, episode.getTvdbId())
                .putExtra(IncomingConstants.EXTRA_EPISODE, episode.toBundle()));
    }

    public void handlePublishedAction(String token, Action action) {
        if (TextUtils.isEmpty(token) || action == null) {
            // whoops, no token or action received
            Timber.d("handlePublishedAction: token or action empty");
            return;
        }

        synchronized (this) {
            if (!mTokens.containsKey(token)) {
                // we are not subscribed, ignore
                Timber.d("handlePublishedAction: token invalid, ignoring incoming action");
                return;
            }

            // check if action episode identifier is for an episode we requested actions for
            Map<ComponentName, Action> actionMap = sEpisodeActionsCache.get(
                    action.getEntityIdentifier());
            if (actionMap == null) {
                // did not request actions for this episode, or is already out of cache (too late!)
                Timber.d("handlePublishedAction: not interested in actions for "
                        + action.getEntityIdentifier() + ", ignoring incoming action");
                return;
            }
            // store action for this episode
            ComponentName extension = mTokens.get(token);
            actionMap.put(extension, action);
        }

        // notify that actions for an episode were updated
        EventBus.getDefault().post(new EpisodeActionReceivedEvent(action.getEntityIdentifier()));
    }

    private synchronized void loadSubscriptions() {
        mSubscriptions = new HashMap<>();
        mTokens = new HashMap<>();

        String serializedSubscriptions = mSharedPrefs.getString(PREF_SUBSCRIPTIONS, null);
        if (serializedSubscriptions == null) {
            return;
        }

        JSONArray jsonArray;
        try {
            jsonArray = new JSONArray(serializedSubscriptions);
        } catch (JSONException e) {
            Timber.e(e, "Deserializing subscriptions failed");
            return;
        }

        for (int i = 0; i < jsonArray.length(); i++) {
            String subscription = jsonArray.optString(i, null);
            if (subscription == null) {
                continue;
            }
            String[] arr = subscription.split("\\|", 2);
            ComponentName extension = ComponentName.unflattenFromString(arr[0]);
            String token = arr[1];
            mSubscriptions.put(extension, token);
            mTokens.put(token, extension);
            Timber.d("Restored subscription: " + extension + " token: " + token);
        }
    }

    private synchronized void saveSubscriptions() {
        Set<String> serializedSubscriptions = new HashSet<>();
        for (ComponentName extension : mSubscriptions.keySet()) {
            serializedSubscriptions.add(extension.flattenToShortString() + "|"
                    + mSubscriptions.get(extension));
        }
        Timber.d("Saving " + serializedSubscriptions.size() + " subscriptions");
        JSONArray json = new JSONArray(serializedSubscriptions);
        mSharedPrefs.edit().putString(PREF_SUBSCRIPTIONS, json.toString()).commit();
    }

    public class Extension {
        public Drawable icon;
        public String label;
        public ComponentName componentName;
        public String description;
        public ComponentName settingsActivity;
    }
}
