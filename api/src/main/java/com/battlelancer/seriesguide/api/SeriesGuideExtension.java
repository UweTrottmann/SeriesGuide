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

package com.battlelancer.seriesguide.api;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import static com.battlelancer.seriesguide.api.constants.IncomingConstants.ACTION_SUBSCRIBE;
import static com.battlelancer.seriesguide.api.constants.IncomingConstants.ACTION_UPDATE;
import static com.battlelancer.seriesguide.api.constants.IncomingConstants.EXTRA_ENTITY_IDENTIFIER;
import static com.battlelancer.seriesguide.api.constants.IncomingConstants.EXTRA_EPISODE;
import static com.battlelancer.seriesguide.api.constants.IncomingConstants.EXTRA_SUBSCRIBER_COMPONENT;
import static com.battlelancer.seriesguide.api.constants.IncomingConstants.EXTRA_TOKEN;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.ACTION_PUBLISH_ACTION;
import static com.battlelancer.seriesguide.api.constants.OutgoingConstants.EXTRA_ACTION;

/**
 * Base class for a SeriesGuide extension. Extensions are a way for other apps to
 * feed actions (represented through {@linkplain Action actions}) for media items to SeriesGuide.
 * Actions may for example launch other apps or just display interesting information related to a
 * media item. Extensions are specialized {@link IntentService} classes.
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
 * Subclasses must at least implement {@link #onRequest(int, Episode)}, which is called when
 * SeriesGuide requests actions to display for a media item. Do not perform long running operations
 * here as the user will get frustrated while waiting for this extensions action to be published.
 *
 * <p> To publish an action, call {@link #publishAction(Action)} from {@link #onRequest(int,
 * Episode)}. All current subscribers will then immediately receive an update with the new action
 * information. Under the hood, this is all done with {@linkplain Context#startService(Intent)
 * service intents}.
 *
 * <h3>Registering your extension</h3>
 *
 * An extension is simply a service that SeriesGuide and other apps interact with via
 * {@linkplain Context#startService(Intent) service intents}. Subclasses of {@link
 * SeriesGuideExtension} should thus be declared as <code>&lt;service&gt;</code> components in the
 * application's <code>AndroidManifest.xml</code> file.
 *
 * <p> The SeriesGuide app and other potential subscribers discover available extensions using
 * Android's {@link Intent} mechanism. Ensure that your <code>service</code> definition includes an
 * <code>&lt;intent-filter&gt;</code> with an action of {@link #ACTION_SERIESGUIDE_EXTENSION}.
 *
 * <p> To make your extension easier to identify for users you should add the following attributes
 * to your service definition:
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
 * <p> Lastly, you may want to add the following <code>&lt;meta-data&gt;</code> element to your
 * service definition:
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
 * &lt;service android:name=".ExampleExtension"
 *     android:label="@string/extension_title"
 *     android:icon="@drawable/ic_extension_example"
 *     android:description="@string/extension_description"&gt;
 *     &lt;intent-filter&gt;
 *         &lt;action android:name="com.battlelancer.seriesguide.api.SeriesGuideExtension" /&gt;
 *     &lt;/intent-filter&gt;
 *     &lt;!-- A settings activity is optional --&gt;
 *     &lt;meta-data android:name="settingsActivity"
 *         android:value=".ExampleSettingsActivity" /&gt;
 * &lt;/service&gt;
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
 * Finally, below is a simple example {@link SeriesGuideExtension} subclass that publishes actions
 * for episodes performing a simple Google search:
 *
 * <pre class="prettyprint">
 * public class ExampleExtension extends SeriesGuideExtension {
 *     protected void onRequest(int episodeIdentifier, Episode episode) {
 *         publishAction(new Action.Builder("Google search", episodeIdentifier)
 *                 .viewIntent(new Intent(Intent.ACTION_VIEW)
 *                          .setData(Uri.parse("https://www.google.com/#q="
 *                                 + episode.getTitle())))
                   .build());
 *     }
 * }
 * </pre>
 *
 * <p> Based on code from <a href="https://github.com/romannurik/muzei">Muzei</a>, an awesome Live
 * Wallpaper by Roman Nurik.
 */
public abstract class SeriesGuideExtension extends IntentService {

    private static final String TAG = "SeriesGuideExtension";

    /**
     * The {@link Intent} action that this extension should declare an
     * <code>&lt;intent-filter&gt;</code> for to let SeriesGuide pick it up.
     */
    public static final String ACTION_SERIESGUIDE_EXTENSION
            = "com.battlelancer.seriesguide.api.SeriesGuideExtension";

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

    private final String mName;

    private SharedPreferences mSharedPrefs;

    private Map<ComponentName, String> mSubscribers;

    private Action mCurrentAction;

    private Handler mHandler = new Handler();

    /**
     * Call from your default constructor.
     *
     * <p> Gives the extension a name. This is not user-visible, but will be used to store preferences
     * and state for the extension.
     */
    public SeriesGuideExtension(String name) {
        super(name);
        mName = name;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPrefs = getSharedPreferences();
        loadSubscriptions();
        loadLastAction();
    }

    /**
     * Convenience method for accessing preferences specific to the extension (with the given name
     * within this package. The source name must be the one provided in the
     * {@link #SeriesGuideExtension(String)} constructor. This static method is useful for exposing
     * extension preferences to other application components such as a settings activity.
     *
     * @param context       The context; can be an application context.
     * @param extensionName The source name, provided in the {@link #SeriesGuideExtension(String)}
     *                      constructor.
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
        return getSharedPreferences(this, mName);
    }

    /**
     * Method called before a new subscriber is added that determines whether the subscription is
     * allowed or not. The default behavior is to allow all subscriptions.
     *
     * @return true if the subscription should be allowed, false if it should be denied.
     */
    protected boolean onAllowSubscription(ComponentName subscriber) {
        return true;
    }

    /**
     * Lifecycle method called when a new subscriber is added. Extensions generally don't need to
     * override this.
     */
    protected void onSubscriberAdded(ComponentName subscriber) {
    }

    /**
     * Lifecycle method called when a subscriber is removed. Extensions generally don't need to
     * override this.
     */
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
     * Called when a new episode is displayed and the extension should publish the action it wants
     * to display using {@link #publishAction(Action)}.
     *
     * @param episodeIdentifier The episode identifier the extension should submit with the action
     *                          it wants to publish.
     */
    protected abstract void onRequest(int episodeIdentifier, Episode episode);

    /**
     * Publishes the provided {@link Action}. It will be sent to all current subscribers.
     */
    protected final void publishAction(Action action) {
        mCurrentAction = action;
        publishCurrentAction();
        saveLastAction();
    }

    /**
     * Returns the most recently published {@link Action}, or null if none was published, yet.
     */
    protected final Action getCurrentAction() {
        return mCurrentAction;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();
        if (ACTION_SUBSCRIBE.equals(action)) {
            // just subscribing or unsubscribing
            handleSubscribe(
                    (ComponentName) intent.getParcelableExtra(EXTRA_SUBSCRIBER_COMPONENT),
                    intent.getStringExtra(EXTRA_TOKEN));
        } else if (ACTION_UPDATE.equals(action)) {
            // subscriber requests an updated action
            if (intent.hasExtra(EXTRA_ENTITY_IDENTIFIER) && intent.hasExtra(EXTRA_EPISODE)) {
                handleEpisodeRequest(intent.getIntExtra(EXTRA_ENTITY_IDENTIFIER, 0),
                        intent.getBundleExtra(EXTRA_EPISODE));
            }
        }
    }

    private synchronized void handleSubscribe(ComponentName subscriber, String token) {
        if (subscriber == null) {
            Log.w(TAG, "No subscriber given.");
            return;
        }

        String oldToken = mSubscribers.get(subscriber);
        if (TextUtils.isEmpty(token)) {
            if (oldToken == null) {
                return;
            }

            // Unsubscribing
            mSubscribers.remove(subscriber);
            handleSubscriberRemoved(subscriber);
        } else {
            // Subscribing
            if (!TextUtils.isEmpty(oldToken)) {
                // Was previously subscribed, treat this as a unsubscribe + subscribe
                mSubscribers.remove(subscriber);
                handleSubscriberRemoved(subscriber);
            }

            if (!onAllowSubscription(subscriber)) {
                return;
            }

            mSubscribers.put(subscriber, token);
            handleSubscriberAdded(subscriber);
        }

        saveSubscriptions();
    }

    private synchronized void handleSubscriberAdded(ComponentName subscriber) {
        if (mSubscribers.size() == 1) {
            onEnabled();
        }

        onSubscriberAdded(subscriber);
    }

    private synchronized void handleSubscriberRemoved(ComponentName subscriber) {
        onSubscriberRemoved(subscriber);

        if (mSubscribers.size() == 0) {
            onDisabled();
        }
    }

    private synchronized void loadSubscriptions() {
        mSubscribers = new HashMap<>();
        Set<String> serializedSubscriptions = mSharedPrefs.getStringSet(PREF_SUBSCRIPTIONS, null);
        if (serializedSubscriptions != null) {
            for (String serializedSubscription : serializedSubscriptions) {
                String[] arr = serializedSubscription.split("\\|", 2);
                ComponentName subscriber = ComponentName.unflattenFromString(arr[0]);
                String token = arr[1];
                mSubscribers.put(subscriber, token);
            }
        }
    }

    private synchronized void saveSubscriptions() {
        Set<String> serializedSubscriptions = new HashSet<>();
        for (ComponentName subscriber : mSubscribers.keySet()) {
            serializedSubscriptions.add(subscriber.flattenToShortString() + "|"
                    + mSubscribers.get(subscriber));
        }
        mSharedPrefs.edit().putStringSet(PREF_SUBSCRIPTIONS, serializedSubscriptions).commit();
    }

    private void loadLastAction() {
        String stateString = mSharedPrefs.getString(PREF_LAST_ACTION, null);
        if (stateString != null) {
            try {
                mCurrentAction = Action.fromJson(
                        (JSONObject) new JSONTokener(stateString).nextValue());
            } catch (JSONException e) {
                Log.e(TAG, "Couldn't deserialize current state, id=" + mName, e);
            }
        } else {
            mCurrentAction = null;
        }
    }

    private void saveLastAction() {
        try {
            mSharedPrefs.edit()
                    .putString(PREF_LAST_ACTION, mCurrentAction.toJson().toString())
                    .commit();
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't serialize current state, id=" + mName, e);
        }
    }

    private void handleEpisodeRequest(int episodeIdentifier, Bundle episodeBundle) {
        if (episodeIdentifier <= 0 || episodeBundle == null) {
            return;
        }
        Episode episode = Episode.fromBundle(episodeBundle);
        onRequest(episodeIdentifier, episode);
    }

    private synchronized void publishCurrentAction() {
        // TODO possibly only publish to requester (identify via token)
        for (ComponentName subscription : mSubscribers.keySet()) {
            publishCurrentAction(subscription);
        }
    }

    private synchronized void publishCurrentAction(final ComponentName subscriber) {
        String token = mSubscribers.get(subscriber);
        if (TextUtils.isEmpty(token)) {
            Log.w(TAG, "Not active, canceling update, id=" + mName);
            return;
        }

        // Publish update
        Intent intent = new Intent(ACTION_PUBLISH_ACTION)
                .setComponent(subscriber)
                .putExtra(EXTRA_TOKEN, token)
                .putExtra(EXTRA_ACTION,
                        (mCurrentAction != null) ? mCurrentAction.toBundle() : null);
        try {
            ComponentName returnedSubscriber = startService(intent);
            if (returnedSubscriber == null) {
                Log.e(TAG, "Update wasn't published because subscriber no longer exists"
                        + ", id=" + mName);
                // Unsubscribe the now-defunct subscriber
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        handleSubscribe(subscriber, null);
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Couldn't publish update, id=" + mName, e);
        }
    }
}
