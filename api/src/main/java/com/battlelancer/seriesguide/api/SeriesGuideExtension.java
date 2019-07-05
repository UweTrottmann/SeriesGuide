package com.battlelancer.seriesguide.api;

import static com.battlelancer.seriesguide.api.constants.IncomingConstants.ACTION_SUBSCRIBE;
import static com.battlelancer.seriesguide.api.constants.IncomingConstants.ACTION_UPDATE;
import static com.battlelancer.seriesguide.api.constants.IncomingConstants.EXTRA_ENTITY_IDENTIFIER;
import static com.battlelancer.seriesguide.api.constants.IncomingConstants.EXTRA_EPISODE;
import static com.battlelancer.seriesguide.api.constants.IncomingConstants.EXTRA_MOVIE;
import static com.battlelancer.seriesguide.api.constants.IncomingConstants.EXTRA_SUBSCRIBER_COMPONENT;
import static com.battlelancer.seriesguide.api.constants.IncomingConstants.EXTRA_TOKEN;
import static com.battlelancer.seriesguide.api.constants.IncomingConstants.EXTRA_VERSION;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.ACTION_PUBLISH_ACTION;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.ACTION_TYPE_EPISODE;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.ACTION_TYPE_MOVIE;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.EXTRA_ACTION;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.EXTRA_ACTION_TYPE;

import android.annotation.SuppressLint;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Base class for a SeriesGuide extension. Extensions are a way for other apps to
 * feed actions (represented through {@linkplain Action actions}) for episodes and movies to
 * SeriesGuide. Actions may for example launch other apps or display interesting information related
 * to an episode or movie.
 *
 * <p> Extensions are specialized {@link JobIntentService} classes in combination with a broadcast
 * receiver.
 *
 * <p> Multiple extensions may be enabled within SeriesGuide at the same time. When a SeriesGuide
 * user chooses to enable an extension, SeriesGuide will <em>subscribe</em> to it prior of
 * requesting actions. If the user disables the extension, SeriesGuide will <em>unsubscribe</em>
 * from it.
 *
 * <p> The API is designed such that other applications besides SeriesGuide can subscribe and
 * request actions from an extension.
 *
 * <h3>Subclassing {@link SeriesGuideExtension}</h3>
 *
 * Subclasses must at least implement {@link #onRequest(int, Episode)} or {@link #onRequest(int, Movie)},
 * which is called when SeriesGuide requests actions to display for an episode or movie. Do not
 * perform long running operations here as the user will get frustrated while waiting for the
 * action to appear.
 *
 * <p> To publish an action, call {@link #publishAction(Action)} from {@link #onRequest(int,
 * Episode)} or {@link #onRequest(int, Movie)}. All current subscribers will then immediately
 * receive an update with the new action information. Under the hood, this is done with
 * {@linkplain Context#sendBroadcast(Intent) broadcast intents}.
 *
 * <p> As the subclass is a {@link JobIntentService}, it needs be declared as a
 * <code>&lt;service&gt;</code> component in the application's <code>AndroidManifest.xml</code>. In
 * addition it must be exported and given the {@link JobService#PERMISSION_BIND} permission to
 * integrate with the platforms job scheduler.
 *
 * <h3>Registering your extension</h3>
 *
 * An extension is exposed through a broadcast receiver that SeriesGuide and other apps interact
 * with via {@linkplain Context#sendBroadcast(Intent) broadcast intents}. This receiver enqueues
 * requests from subscribers to be processed by this service. A simple receiver can be implemented
 * by subclassing {@link SeriesGuideExtensionReceiver}.
 *
 * <p> The receiver must be declared as a <code>&lt;receiver&gt;</code> component in the
 * application's <code>AndroidManifest.xml</code> file.
 *
 * <p> The SeriesGuide app and other potential subscribers discover available extensions using
 * Android's {@link Intent} mechanism. Ensure that your <code>receiver</code> definition includes an
 * <code>&lt;intent-filter&gt;</code> with an action of {@link SeriesGuideExtensionReceiver#ACTION_SERIESGUIDE_EXTENSION}.
 *
 * <p> To make your extension easier to identify for users you should add the following attributes
 * to your receiver definition:
 *
 * <ul>
 * <li><code>android:label</code> (optional): the name to display when displaying your extension in
 * the user interface.</li>
 * <li><code>android:description</code> (optional): a few words describing what your actions
 * display or can do (e.g. "Displays interesting stat" or "Searches Google").</li>
 * <li><code>android:icon</code> (optional): a drawable to represent the extension in the user
 * interface.</li>
 * </ul>
 *
 * <p> If you want to provide a settings activity, add the following <code>&lt;meta-data&gt;</code>
 * element to your receiver definition:
 *
 * <ul>
 * <li><code>settingsActivity</code> (optional): if present, should be the qualified
 * component name for a configuration activity in the extension's package that SeriesGuide can
 * offer to the user for customizing the extension. This activity must be exported.</li>
 * </ul>
 *
 * <h3>Example</h3>
 *
 * Below is an example extension declaration in the manifest:
 *
 * <pre class="prettyprint">
 * &lt;receiver android:name=".ExampleExtensionReceiver"
 *     android:description="@string/extension_description"
 *     android:icon="@drawable/ic_extension_example"
 *     android:label="@string/extension_title"&gt;
 *     &lt;intent-filter&gt;
 *         &lt;action android:name="com.battlelancer.seriesguide.api.SeriesGuideExtension" /&gt;
 *     &lt;/intent-filter&gt;
 *     &lt;!-- A settings activity is optional --&gt;
 *     &lt;meta-data android:name="settingsActivity"
 *         android:value=".ExampleSettingsActivity" /&gt;
 * &lt;/receiver&gt;
 * &lt;service
 *     android:name=".ExampleExtension"
 *     android:exported="true"
 *     android:permission="android.permission.BIND_JOB_SERVICE" /&gt;
 * </pre>
 *
 * If a <code>settingsActivity</code> meta-data element is present, an activity with the given
 * component name should be defined and exported in the application's manifest as well. SeriesGuide
 * will set the {@link #EXTRA_FROM_SERIESGUIDE_SETTINGS} extra to true in the launch intent for this
 * activity. An example is shown below:
 *
 * <pre class="prettyprint">
 * &lt;activity android:name=".ExampleSettingsActivity"
 *     android:label="@string/title_settings"
 *     android:exported="true" /&gt;
 * </pre>
 *
 * Finally, below are a simple example {@link SeriesGuideExtensionReceiver} and
 * {@link SeriesGuideExtension} subclass that publishes actions for episodes performing a simple
 * Google search:
 *
 * <pre class="prettyprint">
 * public class ExampleExtensionReceiver extends SeriesGuideExtensionReceiver {
 *     protected int getJobId() {
 *         return 1000;
 *     }
 *
 *     protected Class&lt;? extends SeriesGuideExtension&gt; getExtensionClass() {
 *         return ExampleExtension.class;
 *     }
 * }
 *
 * public class ExampleExtension extends SeriesGuideExtension {
 *     protected void onRequest(int episodeIdentifier, Episode episode) {
 *         publishAction(new Action.Builder("Google search", episodeIdentifier)
 *                 .viewIntent(new Intent(Intent.ACTION_VIEW)
 *                          .setData(Uri.parse("https://www.google.com/#q="
 *                                 + episode.getTitle())))
 *                 .build());
 *     }
 * }
 * </pre>
 *
 * <p> Based on code from <a href="https://github.com/romannurik/muzei">Muzei</a>, an awesome Live
 * Wallpaper by Roman Nurik.
 */
public abstract class SeriesGuideExtension extends JobIntentService {

    private static final String TAG = "SeriesGuideExtension";

    /**
     * Boolean extra that will be set to true when SeriesGuide starts the extensions (optionally)
     * declared settings activity.
     * Check for this extra in your settings activity if you need to adjust your UI depending on
     * whether or not the user came from SeriesGuide's settings screen.
     */
    public static final String EXTRA_FROM_SERIESGUIDE_SETTINGS
            = "com.battlelancer.seriesguide.api.extra.FROM_SERIESGUIDE_SETTINGS";

    private static final String PREF_PREFIX = "seriesguideextension_";
    private static final String PREF_SUBSCRIPTIONS = "subscriptions";
    private static final String PREF_LAST_ACTION = "action";

    private final String name;
    private SharedPreferences sharedPrefs;
    private Map<ComponentName, String> subscribers;

    private Action currentAction;
    private int currentActionType;
    private int currentVersion;

    private Handler handler = new Handler();

    /**
     * Enqueues this service to process the given intent received from a subscriber.
     */
    static void enqueue(Context context, Class cls, int jobId, Intent subscriberIntent) {
        enqueueWork(context, cls, jobId, subscriberIntent);
    }

    /**
     * Call from your default constructor.
     *
     * @param name Gives the extension a name. This is not user-visible, but will be used to store
     * preferences and state for the extension.
     */
    public SeriesGuideExtension(String name) {
        this.name = name;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPrefs = getSharedPreferences();
        loadSubscriptions();
        loadLastAction();
    }

    /**
     * Convenience method for accessing preferences specific to the extension (with the given name
     * within this package. The source name must be the one provided in the
     * {@link #SeriesGuideExtension(String)} constructor. This static method is useful for exposing
     * extension preferences to other application components such as a settings activity.
     *
     * @param context The context; can be an application context.
     * @param extensionName The source name, provided in the {@link #SeriesGuideExtension(String)}
     * constructor.
     */
    protected static SharedPreferences getSharedPreferences(Context context, String extensionName) {
        return context.getSharedPreferences(PREF_PREFIX + extensionName, 0);
    }

    /**
     * Convenience method for accessing preferences specific to the extension.
     *
     * @see #getSharedPreferences(android.content.Context, String)
     */
    protected final SharedPreferences getSharedPreferences() {
        return getSharedPreferences(this, name);
    }

    /**
     * Method called before a new subscriber is added that determines whether the subscription is
     * allowed or not. The default behavior is to allow all subscriptions.
     *
     * @return true if the subscription should be allowed, false if it should be denied.
     */
    @SuppressWarnings("UnusedParameters")
    protected boolean onAllowSubscription(ComponentName subscriber) {
        return true;
    }

    /**
     * Lifecycle method called when a new subscriber is added. Extensions generally don't need to
     * override this.
     */
    @SuppressWarnings("UnusedParameters")
    protected void onSubscriberAdded(ComponentName subscriber) {
    }

    /**
     * Lifecycle method called when a subscriber is removed. Extensions generally don't need to
     * override this.
     */
    @SuppressWarnings("UnusedParameters")
    protected void onSubscriberRemoved(ComponentName subscriber) {
    }

    /**
     * Lifecycle method called when the first subscriber is added. This will be called before
     * {@link #onSubscriberAdded(ComponentName)}. Extensions generally don't need to
     * override this.
     */
    protected void onEnabled() {
    }

    /**
     * Lifecycle method called when the last subscriber is removed. This will be called after
     * {@link #onSubscriberRemoved(ComponentName)}. Extensions generally don't need to
     * override this.
     */
    protected void onDisabled() {
    }

    /**
     * Called when an episode is displayed and the extension should publish the action it wants to
     * display using {@link #publishAction(Action)}.
     *
     * @param episodeIdentifier The episode identifier the extension should submit with the action
     * it wants to publish.
     */
    protected void onRequest(int episodeIdentifier, Episode episode) {
        // do nothing by default, may choose to either supply episode or movie actions
    }

    /**
     * Called when a movie is displayed and the extension should publish the action it wants to
     * display using {@link #publishAction(Action)}.
     *
     * @param movieIdentifier The movie identifier the extension should submit with the action it
     * wants to publish.
     */
    @SuppressWarnings("UnusedParameters")
    protected void onRequest(int movieIdentifier, Movie movie) {
        // do nothing by default, may choose to either supply episode or movie actions
    }

    /**
     * Publishes the provided {@link Action}. It will be sent to all current subscribers.
     */
    protected final void publishAction(Action action) {
        currentAction = action;
        publishCurrentAction();
        saveLastAction();
    }

    /**
     * Returns the most recently published {@link Action}, or null if none was published, yet.
     */
    @SuppressWarnings("unused")
    protected final Action getCurrentAction() {
        return currentAction;
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        String action = intent.getAction();
        if (ACTION_SUBSCRIBE.equals(action)) {
            // just subscribing or unsubscribing
            handleSubscribe(
                    intent.getParcelableExtra(EXTRA_SUBSCRIBER_COMPONENT),
                    intent.getStringExtra(EXTRA_TOKEN));
        } else if (ACTION_UPDATE.equals(action)) {
            // subscriber requests an updated action
            if (intent.hasExtra(EXTRA_ENTITY_IDENTIFIER)) {
                int version = intent.getIntExtra(EXTRA_VERSION, 1);
                if (intent.hasExtra(EXTRA_EPISODE)) {
                    handleEpisodeRequest(intent.getIntExtra(EXTRA_ENTITY_IDENTIFIER, 0),
                            intent.getBundleExtra(EXTRA_EPISODE), version);
                } else if (intent.hasExtra(EXTRA_MOVIE)) {
                    handleMovieRequest(intent.getIntExtra(EXTRA_ENTITY_IDENTIFIER, 0),
                            intent.getBundleExtra(EXTRA_MOVIE), version);
                }
            }
        }
    }

    @SuppressLint("LogNotTimber")
    private synchronized void handleSubscribe(ComponentName subscriber, String token) {
        if (subscriber == null) {
            Log.w(TAG, "No subscriber given.");
            return;
        }

        String oldToken = subscribers.get(subscriber);
        if (TextUtils.isEmpty(token)) {
            if (oldToken == null) {
                return;
            }

            // Unsubscribing
            subscribers.remove(subscriber);
            handleSubscriberRemoved(subscriber);
        } else {
            // Subscribing
            if (!TextUtils.isEmpty(oldToken)) {
                // Was previously subscribed, treat this as a unsubscribe + subscribe
                subscribers.remove(subscriber);
                handleSubscriberRemoved(subscriber);
            }

            if (!onAllowSubscription(subscriber)) {
                return;
            }

            subscribers.put(subscriber, token);
            handleSubscriberAdded(subscriber);
        }

        saveSubscriptions();
    }

    private synchronized void handleSubscriberAdded(ComponentName subscriber) {
        if (subscribers.size() == 1) {
            onEnabled();
        }

        onSubscriberAdded(subscriber);
    }

    private synchronized void handleSubscriberRemoved(ComponentName subscriber) {
        onSubscriberRemoved(subscriber);

        if (subscribers.size() == 0) {
            onDisabled();
        }
    }

    private synchronized void loadSubscriptions() {
        subscribers = new HashMap<>();
        Set<String> serializedSubscriptions = sharedPrefs.getStringSet(PREF_SUBSCRIPTIONS, null);
        if (serializedSubscriptions != null) {
            for (String serializedSubscription : serializedSubscriptions) {
                String[] arr = serializedSubscription.split("\\|", 2);
                ComponentName subscriber = ComponentName.unflattenFromString(arr[0]);
                String token = arr[1];
                subscribers.put(subscriber, token);
            }
        }
    }

    private synchronized void saveSubscriptions() {
        Set<String> serializedSubscriptions = new HashSet<>();
        for (ComponentName subscriber : subscribers.keySet()) {
            serializedSubscriptions.add(subscriber.flattenToShortString() + "|"
                    + subscribers.get(subscriber));
        }
        sharedPrefs.edit().putStringSet(PREF_SUBSCRIPTIONS, serializedSubscriptions).apply();
    }

    @SuppressLint("LogNotTimber")
    private void loadLastAction() {
        String stateString = sharedPrefs.getString(PREF_LAST_ACTION, null);
        if (stateString != null) {
            try {
                currentAction = Action.fromJson(
                        (JSONObject) new JSONTokener(stateString).nextValue());
            } catch (JSONException e) {
                Log.e(TAG, "Couldn't deserialize current state, id=" + name, e);
            }
        } else {
            currentAction = null;
        }
    }

    @SuppressLint("LogNotTimber")
    private void saveLastAction() {
        try {
            sharedPrefs.edit()
                    .putString(PREF_LAST_ACTION, currentAction.toJson().toString())
                    .apply();
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't serialize current state, id=" + name, e);
        }
    }

    private void handleEpisodeRequest(int episodeIdentifier, Bundle episodeBundle, int version) {
        if (episodeIdentifier <= 0 || episodeBundle == null) {
            return;
        }
        currentActionType = ACTION_TYPE_EPISODE;
        currentVersion = version;
        Episode episode = Episode.fromBundle(episodeBundle);
        onRequest(episodeIdentifier, episode);
    }

    private void handleMovieRequest(int movieIdentifier, Bundle movieBundle, int version) {
        if (movieIdentifier <= 0 || movieBundle == null) {
            return;
        }
        currentActionType = ACTION_TYPE_MOVIE;
        currentVersion = version;
        Movie movie = Movie.fromBundle(movieBundle);
        onRequest(movieIdentifier, movie);
    }

    private synchronized void publishCurrentAction() {
        // TODO possibly only publish to requester (identify via token)
        for (ComponentName subscription : subscribers.keySet()) {
            publishCurrentAction(subscription);
        }
    }

    @SuppressLint("LogNotTimber")
    private synchronized void publishCurrentAction(final ComponentName subscriber) {
        String token = subscribers.get(subscriber);
        if (TextUtils.isEmpty(token)) {
            Log.w(TAG, "Not active, canceling update, id=" + name);
            return;
        }

        Intent intent = new Intent(ACTION_PUBLISH_ACTION)
                .setComponent(subscriber)
                .putExtra(EXTRA_TOKEN, token)
                .putExtra(EXTRA_ACTION,
                        (currentAction != null) ? currentAction.toBundle() : null)
                .putExtra(EXTRA_ACTION_TYPE, currentActionType);

        if (currentVersion == 2) {
            // API 2 uses broadcast intents

            // check if the subscriber still exists
            try {
                getPackageManager().getReceiverInfo(subscriber, 0);
            } catch (PackageManager.NameNotFoundException e) {
                // Unsubscribe the now-defunct subscriber
                unsubscribeAsync(subscriber);
                return;
            }

            // Publish update
            sendBroadcast(intent);
        } else if (currentVersion == 1 && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            // API 1 uses service intents, not compatible with O background restrictions

            // Publish update
            try {
                ComponentName returnedSubscriber = startService(intent);
                if (returnedSubscriber == null) {
                    // Unsubscribe the now-defunct subscriber
                    unsubscribeAsync(subscriber);
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Couldn't publish update, id=" + name, e);
            }
        }
    }

    @SuppressLint("LogNotTimber")
    private void unsubscribeAsync(final ComponentName subscriber) {
        Log.e(TAG,
                "Update not published because subscriber no longer exists, id=" + name);
        // post to Handler to avoid concurrent modification of subscribers
        handler.post(() -> handleSubscribe(subscriber, null));
    }
}
