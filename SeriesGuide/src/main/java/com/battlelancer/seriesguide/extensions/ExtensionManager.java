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
import com.battlelancer.seriesguide.api.constants.IncomingConstants;
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

    /**
     * {@link com.battlelancer.seriesguide.extensions.ExtensionManager} has received new {@link
     * com.battlelancer.seriesguide.api.Action} objects from enabled extensions. Receivers might
     * want to requery available actions.
     */
    public static class EpisodeActionReceivedEvent {
        public int episodeTvdbId;

        public EpisodeActionReceivedEvent(int episodeTvdbId) {
            this.episodeTvdbId = episodeTvdbId;
        }
    }

    private Context mContext;
    private SharedPreferences mSharedPrefs;
    private ComponentName mSubscriberComponentName;

    private Map<ComponentName, String> mSubscriptions; // extension + token = sub
    private Map<String, ComponentName> mTokens; // mirrored map for faster token searching

    private List<ComponentName> mEnabledExtensions; // order-preserving list of enabled extensions

    private ExtensionManager(Context context) {
        Timber.d("Initializing extension manager");
        mContext = context.getApplicationContext();
        mSharedPrefs = mContext.getSharedPreferences(PREF_FILE_SUBSCRIPTIONS, 0);
        mSubscriberComponentName = new ComponentName(mContext, ExtensionSubscriberService.class);
        loadSubscriptions();
    }

    /**
     * Queries the {@link android.content.pm.PackageManager} for any installed {@link
     * com.battlelancer.seriesguide.api.SeriesGuideExtension} extensions. Their info is extracted
     * into {@link com.battlelancer.seriesguide.extensions.ExtensionManager.Extension} objects.
     */
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
            } catch (SecurityException | PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                Timber.e(e, "Reading description for extension " + extension.componentName
                        + " failed");
                extension.description = "";
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
     * Enables the default list of extensions that come with this app.
     */
    public void setDefaultEnabledExtensions() {
        List<ComponentName> defaultExtensions = new ArrayList<>();
        defaultExtensions.add(new ComponentName(mContext, YouTubeExtension.class));
        setEnabledExtensions(defaultExtensions);
    }

    /**
     * Compares the list of currently enabled extensions with the given list and enables added
     * extensions and disables removed extensions.
     */
    public synchronized void setEnabledExtensions(List<ComponentName> extensions) {
        Set<ComponentName> extensionsToEnable = new HashSet<>(extensions);
        boolean isChanged = false;

        // disable removed extensions
        for (ComponentName extension : mEnabledExtensions) {
            if (!extensionsToEnable.contains(extension)) {
                // disable extension
                disableExtension(extension);
                isChanged = true;
            }
            // no need to enable, is already enabled
            extensionsToEnable.remove(extension);
        }

        // enable added extensions
        for (ComponentName extension : extensionsToEnable) {
            enableExtension(extension);
            isChanged = true;
        }

        // always save because just the order might have changed
        mEnabledExtensions = new ArrayList<>(extensions);
        saveSubscriptions();

        if (isChanged) {
            // clear actions cache so loaders will request new actions
            sEpisodeActionsCache.evictAll();
        }
    }

    /**
     * Returns a copy of the list of currently enabled extensions in the order the user previously
     * determined.
     */
    public synchronized List<ComponentName> getEnabledExtensions() {
        return new ArrayList<>(mEnabledExtensions);
    }

    private void enableExtension(ComponentName extension) {
        if (extension == null) {
            Timber.e("enableExtension: empty extension");
        }

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

    private void disableExtension(ComponentName extension) {
        if (extension == null) {
            Timber.e("disableExtension: extension empty");
        }

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

    /**
     * Returns the currently available {@link com.battlelancer.seriesguide.api.Action} list for the
     * given episode, identified through its TVDb id. Sorted in the order determined by the user.
     */
    public synchronized List<Action> getLatestEpisodeActions(int episodeTvdbId) {
        Map<ComponentName, Action> actionMap = sEpisodeActionsCache.get(episodeTvdbId);
        if (actionMap == null) {
            return null;
        }
        List<Action> sortedActions = new ArrayList<>();
        for (ComponentName extension : mEnabledExtensions) {
            Action action = actionMap.get(extension);
            if (action != null) {
                sortedActions.add(action);
            }
        }
        return sortedActions;
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
        mEnabledExtensions = new ArrayList<>();
        mSubscriptions = new HashMap<>();
        mTokens = new HashMap<>();

        String serializedSubscriptions = mSharedPrefs.getString(PREF_SUBSCRIPTIONS, null);
        if (serializedSubscriptions == null) {
            setDefaultEnabledExtensions();
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
            mEnabledExtensions.add(extension);
            mSubscriptions.put(extension, token);
            mTokens.put(token, extension);
            Timber.d("Restored subscription: " + extension + " token: " + token);
        }
    }

    private synchronized void saveSubscriptions() {
        List<String> serializedSubscriptions = new ArrayList<>();
        for (ComponentName extension : mEnabledExtensions) {
            serializedSubscriptions.add(extension.flattenToShortString() + "|"
                    + mSubscriptions.get(extension));
        }
        Timber.d("Saving " + serializedSubscriptions.size() + " subscriptions");
        JSONArray json = new JSONArray(serializedSubscriptions);
        mSharedPrefs.edit().putString(PREF_SUBSCRIPTIONS, json.toString()).apply();
    }

    /**
     * Removes all currently cached {@link com.battlelancer.seriesguide.api.Action} objects for all
     * enabled {@linkplain com.battlelancer.seriesguide.extensions.ExtensionManager.Extension}s.
     * Call this e.g. after going into an extensions settings activity.
     */
    public synchronized void clearEpisodeActionsCache() {
        sEpisodeActionsCache.evictAll();
    }

    public class Extension {
        public Drawable icon;
        public String label;
        public ComponentName componentName;
        public String description;
        public ComponentName settingsActivity;
    }
}
