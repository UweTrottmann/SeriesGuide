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
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import static com.battlelancer.seriesguide.api.internal.IncomingConstants.ACTION_SUBSCRIBE;
import static com.battlelancer.seriesguide.api.internal.IncomingConstants.ACTION_UPDATE;
import static com.battlelancer.seriesguide.api.internal.IncomingConstants.EXTRA_EPISODE;
import static com.battlelancer.seriesguide.api.internal.IncomingConstants.EXTRA_SUBSCRIBER_COMPONENT;
import static com.battlelancer.seriesguide.api.internal.IncomingConstants.EXTRA_TOKEN;
import static com.battlelancer.seriesguide.api.internal.OutgoingConstants.ACTION_PUBLISH_ACTION;
import static com.battlelancer.seriesguide.api.internal.OutgoingConstants.EXTRA_ACTION;

/**
 * Base class for a SeriesGuide extension.<br/>
 * <br/>
 * Based on code from <a href="https://github.com/romannurik/muzei">Muzei</a>, an awesome Live
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

    private static final int MSG_PUBLISH_CURRENT_ACTION = 1;

    private final String mName;

    private SharedPreferences mSharedPrefs;

    private Map<ComponentName, String> mSubscriptions;

    private Action mCurrentAction;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_PUBLISH_CURRENT_ACTION) {
                publishCurrentAction();
                saveLastAction();
            }
        }
    };

    /**
     * Call from your default constructor.<br/>
     * <br/>
     * Gives the extension a name. This is not user-visible, but will be used to store preferences
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
     */
    protected abstract void onUpdate(Episode episode);

    /**
     * Publishes the provided {@link Action}. It will be sent to all current subscribers.
     */
    protected final void publishAction(Action action) {
        mCurrentAction = action;
        mHandler.removeMessages(MSG_PUBLISH_CURRENT_ACTION);
        mHandler.sendEmptyMessage(MSG_PUBLISH_CURRENT_ACTION);
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
            if (intent.hasExtra(EXTRA_EPISODE)) {
                handleUpdate(intent.getBundleExtra(EXTRA_EPISODE));
            }
        }
    }

    private synchronized void handleSubscribe(ComponentName subscriber, String token) {
        if (subscriber == null) {
            Log.w(TAG, "No subscriber given.");
            return;
        }

        String oldToken = mSubscriptions.get(subscriber);
        if (TextUtils.isEmpty(token)) {
            if (oldToken == null) {
                return;
            }

            // Unsubscribing
            mSubscriptions.remove(subscriber);
            handleSubscriberRemoved(subscriber);
        } else {
            // Subscribing
            if (!TextUtils.isEmpty(oldToken)) {
                // Was previously subscribed, treat this as a unsubscribe + subscribe
                mSubscriptions.remove(subscriber);
                handleSubscriberRemoved(subscriber);
            }

            if (!onAllowSubscription(subscriber)) {
                return;
            }

            mSubscriptions.put(subscriber, token);
            handleSubscriberAdded(subscriber);
        }

        saveSubscriptions();
    }

    private synchronized void handleSubscriberAdded(ComponentName subscriber) {
        if (mSubscriptions.size() == 1) {
            onEnabled();
        }

        onSubscriberAdded(subscriber);
    }

    private synchronized void handleSubscriberRemoved(ComponentName subscriber) {
        onSubscriberRemoved(subscriber);

        if (mSubscriptions.size() == 0) {
            onDisabled();
        }
    }

    private synchronized void loadSubscriptions() {
        mSubscriptions = new HashMap<ComponentName, String>();
        Set<String> serializedSubscriptions = mSharedPrefs.getStringSet(PREF_SUBSCRIPTIONS, null);
        if (serializedSubscriptions != null) {
            for (String serializedSubscription : serializedSubscriptions) {
                String[] arr = serializedSubscription.split("\\|", 2);
                ComponentName subscriber = ComponentName.unflattenFromString(arr[0]);
                String token = arr[1];
                mSubscriptions.put(subscriber, token);
            }
        }
    }

    private synchronized void saveSubscriptions() {
        Set<String> serializedSubscriptions = new HashSet<String>();
        for (ComponentName subscriber : mSubscriptions.keySet()) {
            serializedSubscriptions.add(subscriber.flattenToShortString() + "|"
                    + mSubscriptions.get(subscriber));
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

    private void handleUpdate(Bundle episodeBundle) {
        if (episodeBundle == null) {
            return;
        }
        Episode episode = Episode.fromBundle(episodeBundle);
        onUpdate(episode);
    }

    private synchronized void publishCurrentAction() {
        for (ComponentName subscription : mSubscriptions.keySet()) {
            publishCurrentAction(subscription);
        }
    }

    private synchronized void publishCurrentAction(final ComponentName subscriber) {
        String token = mSubscriptions.get(subscriber);
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
