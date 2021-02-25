package com.battlelancer.seriesguide.extensions;

import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.ACTION_TYPE_EPISODE;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.ACTION_TYPE_MOVIE;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.LruCache;
import com.battlelancer.seriesguide.api.Action;
import com.battlelancer.seriesguide.api.Episode;
import com.battlelancer.seriesguide.api.Movie;
import com.battlelancer.seriesguide.api.SeriesGuideExtensionReceiver;
import com.battlelancer.seriesguide.api.constants.IncomingConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONException;
import timber.log.Timber;

public class ExtensionManager {

    private static final String PREF_FILE_SUBSCRIPTIONS = "seriesguide_extensions";
    private static final String PREF_SUBSCRIPTIONS = "subscriptions";

    private static final int HARD_CACHE_CAPACITY = 5;

    // Cashes received actions for the last few displayed episodes.
    private final static LruCache<Integer, Map<ComponentName, Action>>
            sEpisodeActionsCache = new LruCache<>(HARD_CACHE_CAPACITY);

    // Cashes received actions for the last few displayed movies.
    private final static LruCache<Integer, Map<ComponentName, Action>>
            sMovieActionsCache = new LruCache<>(HARD_CACHE_CAPACITY);

    /**
     * {@link com.battlelancer.seriesguide.extensions.ExtensionManager} has received new {@link
     * com.battlelancer.seriesguide.api.Action} objects from enabled extensions. Receivers might
     * want to requery available actions.
     */
    public static class EpisodeActionReceivedEvent {
        public int episodeTmdbId;

        public EpisodeActionReceivedEvent(int episodeTmdbId) {
            this.episodeTmdbId = episodeTmdbId;
        }
    }

    /**
     * {@link com.battlelancer.seriesguide.extensions.ExtensionManager} has received new {@link
     * com.battlelancer.seriesguide.api.Action} objects from enabled extensions. Receivers might
     * want to requery available actions.
     */
    public static class MovieActionReceivedEvent {
        public int movieTmdbId;

        public MovieActionReceivedEvent(int movieTmdbId) {
            this.movieTmdbId = movieTmdbId;
        }
    }

    @Nullable
    private Map<ComponentName, String> subscriptions; // extension + token = sub
    @Nullable
    private Map<String, ComponentName> tokens; // mirrored map for faster token searching
    @Nullable
    private List<ComponentName> enabledExtensions; // order-preserving list of enabled extensions

    private static ExtensionManager _instance;

    /**
     * When first used {@link #checkEnabledExtensions(Context)} is called to set up the manager.
     */
    public static synchronized ExtensionManager get(Context context) {
        if (_instance == null) {
            _instance = new ExtensionManager(context.getApplicationContext());
        }
        return _instance;
    }

    private ExtensionManager(Context context) {
        checkEnabledExtensions(context);
    }

    /**
     * Queries the {@link android.content.pm.PackageManager} for any installed {@link
     * com.battlelancer.seriesguide.api.SeriesGuideExtension} extensions. Their info is extracted
     * into {@link Extension} objects.
     */
    @NonNull
    public List<Extension> queryAllAvailableExtensions(Context context) {
        Intent queryIntent = new Intent(SeriesGuideExtensionReceiver.ACTION_SERIESGUIDE_EXTENSION);
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfos = pm.queryBroadcastReceivers(queryIntent,
                PackageManager.GET_META_DATA);

        List<Extension> extensions = new ArrayList<>();
        for (ResolveInfo info : resolveInfos) {
            Extension extension = new Extension();
            // get label, icon and component name
            extension.label = info.loadLabel(pm).toString();
            extension.icon = info.loadIcon(pm);
            extension.componentName = new ComponentName(info.activityInfo.packageName,
                    info.activityInfo.name);
            // get description
            Context packageContext;
            try {
                packageContext = context.createPackageContext(
                        extension.componentName.getPackageName(), 0);
                Resources packageRes = packageContext.getResources();
                extension.description = packageRes.getString(info.activityInfo.descriptionRes);
            } catch (SecurityException | PackageManager.NameNotFoundException | Resources.NotFoundException e) {
                Timber.e(e, "Reading description for extension %s failed", extension.componentName);
                extension.description = "";
            }
            // get (optional) settings activity
            Bundle metaData = info.activityInfo.metaData;
            if (metaData != null) {
                String settingsActivity = metaData.getString("settingsActivity");
                if (!TextUtils.isEmpty(settingsActivity)) {
                    extension.settingsActivity = ComponentName.unflattenFromString(
                            info.activityInfo.packageName + "/" + settingsActivity);
                }
            }

            Timber.d("queryAllAvailableExtensions: found extension %s %s", extension.label,
                    extension.componentName);
            extensions.add(extension);
        }

        return extensions;
    }

    /**
     * Enables the default list of extensions that come with this app.
     */
    public void setDefaultEnabledExtensions(Context context) {
        List<ComponentName> defaultExtensions = new ArrayList<>();
        defaultExtensions.add(new ComponentName(context, WebSearchExtensionReceiver.class));
        defaultExtensions.add(new ComponentName(context, YouTubeExtensionReceiver.class));
        setEnabledExtensions(context, defaultExtensions);
    }

    /**
     * Checks if all extensions are still installed, re-subscribes to those that are in case one was
     * updated, removes those unavailable.
     */
    private synchronized void checkEnabledExtensions(Context context) {
        // make a copy of enabled extensions
        List<ComponentName> enabledExtensions = getEnabledExtensions(context);

        Timber.i("App restart: temporarily un-subscribing from all extensions.");
        List<ComponentName> extensionsToKeep = new ArrayList<>();
        setEnabledExtensions(context, extensionsToKeep);

        // check which are still installed
        for (ComponentName extension : enabledExtensions) {
            try {
                context.getPackageManager().getReceiverInfo(extension, 0);
                extensionsToKeep.add(extension);
            } catch (PackageManager.NameNotFoundException e) {
                Timber.i("Extension %s no longer available: removed", extension.toShortString());
            }
        }

        // re-subscribe to those still installed
        Timber.i("App restart: re-subscribing to installed extensions.");
        setEnabledExtensions(context, extensionsToKeep);

        // watch for further changes while the app is running
        ExtensionPackageChangeReceiver packageChangeReceiver = new ExtensionPackageChangeReceiver();
        IntentFilter packageChangeFilter = new IntentFilter();
        packageChangeFilter.addDataScheme("package");
        packageChangeFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packageChangeFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packageChangeFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        context.registerReceiver(packageChangeReceiver, packageChangeFilter);
    }

    /**
     * Compares the list of currently enabled extensions with the given list and enables added
     * extensions and disables removed extensions.
     */
    synchronized void setEnabledExtensions(Context context,
            @NonNull List<ComponentName> extensions) {
        Set<ComponentName> extensionsToEnable = new HashSet<>(extensions);
        boolean isChanged = false;

        // disable removed extensions
        for (ComponentName extension : enabledExtensions(context)) {
            if (!extensionsToEnable.contains(extension)) {
                // disable extension
                disableExtension(context, extension);
                isChanged = true;
            }
            // no need to enable, is already enabled
            extensionsToEnable.remove(extension);
        }

        // enable added extensions
        for (ComponentName extension : extensionsToEnable) {
            enableExtension(context, extension);
            isChanged = true;
        }

        // always save because just the order might have changed
        enabledExtensions = new ArrayList<>(extensions);
        saveSubscriptions(context);

        if (isChanged) {
            // clear actions cache so loaders will request new actions
            sEpisodeActionsCache.evictAll();
            sMovieActionsCache.evictAll();
        }
    }

    /**
     * Returns a copy of the list of currently enabled extensions in the order the user previously
     * determined.
     */
    synchronized List<ComponentName> getEnabledExtensions(Context context) {
        return new ArrayList<>(enabledExtensions(context));
    }

    private void enableExtension(Context context, ComponentName extension) {
        if (extension == null) {
            Timber.e("enableExtension: empty extension");
            return;
        }

        Map<ComponentName, String> subscriptions = subscriptions(context);
        if (subscriptions.containsKey(extension)) {
            // already subscribed
            Timber.d("enableExtension: already subscribed to %s", extension);
            return;
        }

        // subscribe
        String token = UUID.randomUUID().toString();
        Map<String, ComponentName> tokens = tokens(context);
        while (tokens.containsKey(token)) {
            // create another UUID on collision
            /*
              As the number of enabled extensions is rather low compared to the UUID number
              space we shouldn't have to worry about this ever looping.
             */
            token = UUID.randomUUID().toString();
        }
        Timber.d("enableExtension: subscribing to %s", extension);
        subscriptions.put(extension, token);
        tokens.put(token, extension);
        context.sendBroadcast(new Intent(IncomingConstants.ACTION_SUBSCRIBE)
                .setComponent(extension)
                .putExtra(IncomingConstants.EXTRA_SUBSCRIBER_COMPONENT,
                        subscriberComponentName(context))
                .putExtra(IncomingConstants.EXTRA_TOKEN, token));
    }

    @SuppressLint("LogNotTimber")
    private void disableExtension(Context context, ComponentName extension) {
        if (extension == null) {
            Timber.e("disableExtension: extension empty");
        }

        Map<ComponentName, String> subscriptions = subscriptions(context);
        if (!subscriptions.containsKey(extension)) {
            Timber.d("disableExtension: extension not enabled %s", extension);
            return;
        }

        // unsubscribe
        Timber.d("disableExtension: unsubscribing from %s", extension);
        try {
            context.sendBroadcast(new Intent(IncomingConstants.ACTION_SUBSCRIBE)
                    .setComponent(extension)
                    .putExtra(IncomingConstants.EXTRA_SUBSCRIBER_COMPONENT,
                            subscriberComponentName(context))
                    .putExtra(IncomingConstants.EXTRA_TOKEN, (String) null));
        } catch (SecurityException e) {
            // never crash to not block removing broken extensions
            // log in release builds to help extension developers debug
            Log.i("ExtensionManager", "Failed to unsubscribe from extension " + extension + ".", e);
        }
        tokens(context).remove(subscriptions.remove(extension));
    }

    /**
     * Returns the currently available {@link com.battlelancer.seriesguide.api.Action} list for the
     * given episode, identified through its TMDB id. Sorted in the order determined by the user.
     */
    synchronized List<Action> getLatestEpisodeActions(Context context, int episodeTmdbId) {
        Map<ComponentName, Action> actionMap = sEpisodeActionsCache.get(episodeTmdbId);
        return actionListFrom(context, actionMap);
    }

    /**
     * Returns the currently available {@link com.battlelancer.seriesguide.api.Action} list for the
     * given movie, identified through its TMDB id. Sorted in the order determined by the user.
     */
    public synchronized List<Action> getLatestMovieActions(Context context, int movieTmdbId) {
        Map<ComponentName, Action> actionMap = sMovieActionsCache.get(movieTmdbId);
        return actionListFrom(context, actionMap);
    }

    private List<Action> actionListFrom(Context context, Map<ComponentName, Action> actionMap) {
        if (actionMap == null) {
            return null;
        }
        List<Action> sortedActions = new ArrayList<>();
        for (ComponentName extension : enabledExtensions(context)) {
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
    synchronized void requestEpisodeActions(Context context, Episode episode) {
        for (ComponentName extension : subscriptions(context).keySet()) {
            requestEpisodeAction(context, extension, episode);
        }
    }

    /**
     * Ask a single extension to publish an action for the given episode.
     */
    private synchronized void requestEpisodeAction(Context context, ComponentName extension,
            Episode episode) {
        Integer episodeIdentifier = episode.getTmdbId();
        Timber.d("requestAction: requesting from %s for %s", extension, episodeIdentifier);
        // prepare to receive actions for the given episode
        if (sEpisodeActionsCache.get(episodeIdentifier) == null) {
            sEpisodeActionsCache.put(episodeIdentifier, new HashMap<>());
        }
        // actually request actions
        context.sendBroadcast(new Intent(IncomingConstants.ACTION_UPDATE)
                .setComponent(extension)
                .putExtra(IncomingConstants.EXTRA_ENTITY_IDENTIFIER, episodeIdentifier)
                .putExtra(IncomingConstants.EXTRA_EPISODE, episode.toBundle())
                .putExtra(IncomingConstants.EXTRA_VERSION, 2));
    }

    /**
     * Asks all enabled extensions to publish an action for the given movie.
     */
    public synchronized void requestMovieActions(Context context, Movie movie) {
        for (ComponentName extension : subscriptions(context).keySet()) {
            requestMovieAction(context, extension, movie);
        }
    }

    /**
     * Ask a single extension to publish an action for the given movie.
     */
    private synchronized void requestMovieAction(Context context, ComponentName extension,
            Movie movie) {
        Timber.d("requestAction: requesting from %s for %s", extension, movie.getTmdbId());
        // prepare to receive actions for the given episode
        if (sMovieActionsCache.get(movie.getTmdbId()) == null) {
            sMovieActionsCache.put(movie.getTmdbId(), new HashMap<>());
        }
        // actually request actions
        context.sendBroadcast(new Intent(IncomingConstants.ACTION_UPDATE)
                .setComponent(extension)
                .putExtra(IncomingConstants.EXTRA_ENTITY_IDENTIFIER, movie.getTmdbId())
                .putExtra(IncomingConstants.EXTRA_MOVIE, movie.toBundle())
                .putExtra(IncomingConstants.EXTRA_VERSION, 2));
    }

    /**
     * This is thread-safe.
     */
    public void handlePublishedAction(Context context, String token, Action action, int type) {
        if (TextUtils.isEmpty(token) || action == null) {
            // whoops, no token or action received
            Timber.d("handlePublishedAction: token or action empty");
            return;
        }
        if (type != ACTION_TYPE_EPISODE && type != ACTION_TYPE_MOVIE) {
            Timber.d("handlePublishedAction: unknown type of entity");
            return;
        }

        synchronized (this) {
            Map<String, ComponentName> tokens = tokens(context);
            if (!tokens.containsKey(token)) {
                // we are not subscribed, ignore
                Timber.d("handlePublishedAction: token invalid, ignoring incoming action");
                return;
            }

            // check if action entity identifier is for an entity we requested actions for
            Map<ComponentName, Action> actionMap;
            if (type == ACTION_TYPE_EPISODE) {
                // episode
                actionMap = sEpisodeActionsCache.get(action.getEntityIdentifier());
            } else {
                // movie
                actionMap = sMovieActionsCache.get(action.getEntityIdentifier());
            }
            if (actionMap == null) {
                // did not request actions for this episode, or is already out of cache (too late!)
                Timber.d(
                        "handlePublishedAction: ignoring actions for %s, not requested",
                        action.getEntityIdentifier());
                return;
            }
            // store action for this entity
            ComponentName extension = tokens.get(token);
            //noinspection ConstantConditions Should never be null if token exists.
            actionMap.put(extension, action);
        }

        // notify that actions were updated
        if (type == ACTION_TYPE_EPISODE) {
            EventBus.getDefault()
                    .post(new EpisodeActionReceivedEvent(action.getEntityIdentifier()));
        } else {
            EventBus.getDefault().post(new MovieActionReceivedEvent(action.getEntityIdentifier()));
        }
    }

    private synchronized void loadSubscriptions(Context context) {
        if (enabledExtensions != null && subscriptions != null && tokens != null) {
            return; // already loaded subscriptions
        }
        Timber.i("Loading extension subscriptions");
        enabledExtensions = new ArrayList<>();
        subscriptions = new HashMap<>();
        tokens = new HashMap<>();

        String serializedSubscriptions = preferences(context).getString(PREF_SUBSCRIPTIONS, null);
        if (serializedSubscriptions == null) {
            setDefaultEnabledExtensions(context);
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
            if (extension == null) {
                Timber.e("Failed to restore subscription: %s", subscription);
                continue;
            }
            String token = arr[1];
            enabledExtensions.add(extension);
            subscriptions.put(extension, token);
            tokens.put(token, extension);
            Timber.d("Restored subscription: %s token: %s", extension, token);
        }
    }

    private void saveSubscriptions(Context context) {
        List<String> serializedSubscriptions = new ArrayList<>();
        Map<ComponentName, String> subscriptions = subscriptions(context);
        for (ComponentName extension : enabledExtensions(context)) {
            serializedSubscriptions.add(extension.flattenToShortString() + "|"
                    + subscriptions.get(extension));
        }
        Timber.d("Saving %s subscriptions", serializedSubscriptions.size());
        JSONArray json = new JSONArray(serializedSubscriptions);
        preferences(context).edit().putString(PREF_SUBSCRIPTIONS, json.toString()).apply();
    }

    private Map<ComponentName, String> subscriptions(Context context) {
        loadSubscriptions(context);
        return subscriptions;
    }

    private Map<String, ComponentName> tokens(Context context) {
        loadSubscriptions(context);
        return tokens;
    }

    private List<ComponentName> enabledExtensions(Context context) {
        loadSubscriptions(context);
        return enabledExtensions;
    }

    private SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREF_FILE_SUBSCRIPTIONS, 0);
    }

    @NonNull
    private ComponentName subscriberComponentName(Context context) {
        return new ComponentName(context, ExtensionActionReceiver.class);
    }

    /**
     * Removes all currently cached {@link com.battlelancer.seriesguide.api.Action} objects for all
     * enabled {@linkplain Extension}s.
     * Call this e.g. after going into an extensions settings activity.
     */
    synchronized void clearActionsCache() {
        sEpisodeActionsCache.evictAll();
        sMovieActionsCache.evictAll();
    }
}
