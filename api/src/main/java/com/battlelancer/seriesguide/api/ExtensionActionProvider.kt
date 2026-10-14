// SPDX-License-Identifier: Apache-2.0
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>

package com.battlelancer.seriesguide.api

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.battlelancer.seriesguide.api.constants.IncomingConstants
import com.battlelancer.seriesguide.api.constants.OutgoingConstants
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Base class for a SeriesGuide extension. Extensions are a way for other apps to
 * feed actions (represented through [actions][Action]) for episodes and movies to
 * SeriesGuide. Actions may for example launch other apps or display interesting information related
 * to an episode or movie.
 *
 * Extensions are broadcast receivers. Actions are published directly from the receiver, so unlike
 * the deprecated [SeriesGuideExtension] there is no delay if the app of the extension is
 * restricted by the system (for example in a rare or restricted app standby bucket).
 *
 * Multiple extensions may be enabled within SeriesGuide at the same time. When a SeriesGuide
 * user chooses to enable an extension, SeriesGuide will *subscribe* to it prior of
 * requesting actions. If the user disables the extension, SeriesGuide will *unsubscribe*
 * from it.
 *
 * The API is designed such that other applications besides SeriesGuide can subscribe and
 * request actions from an extension.
 *
 * ### Subclassing [ExtensionActionProvider]
 *
 * Subclasses must at least implement [onEpisodeRequest] or [onMovieRequest],
 * which is called when SeriesGuide requests an action to display for an episode or movie. Return
 * the [Action] to publish, or null to not publish anything. The action is then sent to all current
 * subscribers. Under the hood, this is done with
 * [broadcast intents][Context.sendBroadcast].
 *
 * ### Time budget
 *
 * The request methods are called on a background thread, but as the system keeps the receiver
 * alive using [goAsync], they must return within about 10 seconds, otherwise the system may stop
 * the app. Do not perform long running operations, the user will get frustrated while waiting for
 * the action to appear. If making network requests, use timeouts.
 *
 * All work (subscribing, unsubscribing and requests) is executed one after another on a single
 * background thread shared by all extensions of the app.
 *
 * ### Registering your extension
 *
 * The receiver must be declared as a `<receiver>` component in the
 * application's `AndroidManifest.xml` file.
 *
 * The SeriesGuide app and other potential subscribers discover available extensions using
 * Android's [Intent] mechanism. Ensure that your `receiver` definition includes an
 * `<intent-filter>` with an action of [ACTION_SERIESGUIDE_EXTENSION].
 *
 * To make your extension easier to identify for users you should add the following attributes
 * to your receiver definition:
 *
 * - `android:label` (optional): the name to display when displaying your extension in
 * the user interface.
 * - `android:description` (optional): a few words describing what your actions
 * display or can do (e.g. "Displays interesting stat" or "Searches Google").
 * - `android:icon` (optional): a drawable to represent the extension in the user
 * interface.
 *
 * If you want to provide a settings activity, add the following `<meta-data>`
 * element to your receiver definition:
 *
 * - `settingsActivity` (optional): if present, should be the qualified
 * component name for a configuration activity in the extension's package that SeriesGuide can
 * offer to the user for customizing the extension. This activity must be exported.
 *
 * ### Example
 *
 * Below is an example extension declaration in the manifest:
 *
 * ```
 * <receiver android:name=".ExampleExtension"
 *     android:description="@string/extension_description"
 *     android:icon="@drawable/ic_extension_example"
 *     android:label="@string/extension_title"
 *     android:exported="true">
 *     <intent-filter>
 *         <action android:name="com.battlelancer.seriesguide.api.SeriesGuideExtension" />
 *         <!-- Android 15 safer intents: explicitly declare subscribe and update events are supported. -->
 *         <action android:name="com.battlelancer.seriesguide.api.action.SUBSCRIBE" />
 *         <action android:name="com.battlelancer.seriesguide.api.action.UPDATE" />
 *     </intent-filter>
 *     <!-- A settings activity is optional -->
 *     <meta-data android:name="settingsActivity"
 *         android:value=".ExampleSettingsActivity" />
 * </receiver>
 * ```
 *
 * If a `settingsActivity` meta-data element is present, an activity with the given
 * component name should be defined and exported in the application's manifest as well. SeriesGuide
 * will set the [EXTRA_FROM_SERIESGUIDE_SETTINGS] extra to true in the launch intent for this
 * activity. An example is shown below:
 *
 * ```
 * <activity android:name=".ExampleSettingsActivity"
 *     android:label="@string/title_settings"
 *     android:exported="true" />
 * ```
 *
 * Finally, below is a simple example [ExtensionActionProvider] subclass that publishes actions for
 * episodes performing a simple Google search. In Kotlin:
 *
 * ```
 * class ExampleExtension : ExtensionActionProvider("ExampleExtension") {
 *     override fun onEpisodeRequest(
 *         context: Context,
 *         episodeIdentifier: Int,
 *         episode: Episode
 *     ): Action? {
 *         val title = episode.title ?: return null
 *         return Action.Builder("Google search", episodeIdentifier)
 *             .viewIntent(
 *                 Intent(Intent.ACTION_VIEW)
 *                     .setData(Uri.parse("https://www.google.com/#q=$title"))
 *             )
 *             .build()
 *     }
 * }
 * ```
 *
 * Or in Java:
 *
 * ```
 * public class ExampleExtension extends ExtensionActionProvider {
 *     public ExampleExtension() {
 *         super("ExampleExtension");
 *     }
 *
 *     // Annotate with Override and Nullable.
 *     public Action onEpisodeRequest(Context context, int episodeIdentifier, Episode episode) {
 *         return new Action.Builder("Google search", episodeIdentifier)
 *                 .viewIntent(new Intent(Intent.ACTION_VIEW)
 *                         .setData(Uri.parse("https://www.google.com/#q=" + episode.getTitle())))
 *                 .build();
 *     }
 * }
 * ```
 *
 * Based on code from [Muzei](https://github.com/romannurik/muzei), an awesome Live
 * Wallpaper by Roman Nurik.
 *
 * @param name Gives the extension a name. This is not user-visible, but will be used to store
 * preferences and state for the extension. If migrating from [SeriesGuideExtension], use the same
 * name to keep existing subscribers.
 */
public abstract class ExtensionActionProvider(
    private val name: String
) : BroadcastReceiver() {

    /**
     * Called when an episode is displayed and the extension should provide the action it wants to
     * display. Called on a background thread, must return within a few seconds.
     *
     * @param episodeIdentifier The episode identifier the extension should submit with the action
     * it wants to publish.
     * @return The action to publish to all subscribers, or null to not publish anything.
     */
    public open fun onEpisodeRequest(
        context: Context,
        episodeIdentifier: Int,
        episode: Episode
    ): Action? = null

    /**
     * Called when a movie is displayed and the extension should provide the action it wants to
     * display. Called on a background thread, must return within a few seconds.
     *
     * @param movieIdentifier The movie identifier the extension should submit with the action it
     * wants to publish.
     * @return The action to publish to all subscribers, or null to not publish anything.
     */
    public open fun onMovieRequest(
        context: Context,
        movieIdentifier: Int,
        movie: Movie
    ): Action? = null

    /**
     * Method called before a new subscriber is added that determines whether the subscription is
     * allowed or not. The default behavior is to allow all subscriptions.
     *
     * @return true if the subscription should be allowed, false if it should be denied.
     */
    public open fun onAllowSubscription(context: Context, subscriber: ComponentName): Boolean = true

    /**
     * Lifecycle method called when a new subscriber is added. Extensions generally don't need to
     * override this.
     */
    public open fun onSubscriberAdded(context: Context, subscriber: ComponentName) {
    }

    /**
     * Lifecycle method called when a subscriber is removed. Extensions generally don't need to
     * override this.
     */
    public open fun onSubscriberRemoved(context: Context, subscriber: ComponentName) {
    }

    /**
     * Lifecycle method called when the first subscriber is added. This will be called before
     * [onSubscriberAdded]. Extensions generally don't need to override this.
     */
    public open fun onEnabled(context: Context) {
    }

    /**
     * Lifecycle method called when the last subscriber is removed. This will be called after
     * [onSubscriberRemoved]. Extensions generally don't need to override this.
     */
    public open fun onDisabled(context: Context) {
    }

    final override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext ?: context
        try {
            EXECUTOR.execute {
                try {
                    handleIntent(appContext, intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to handle intent, id=$name", e)
                } finally {
                    pendingResult.finish()
                }
            }
        } catch (e: Exception) {
            // Executor rejected the work, make sure the receiver is still finished.
            Log.e(TAG, "Failed to schedule work, id=$name", e)
            pendingResult.finish()
        }
    }

    private fun handleIntent(context: Context, intent: Intent) {
        when (intent.action) {
            IncomingConstants.ACTION_SUBSCRIBE -> {
                // just subscribing or unsubscribing
                handleSubscribe(
                    context,
                    getSubscriberComponent(intent),
                    intent.getStringExtra(IncomingConstants.EXTRA_TOKEN)
                )
            }

            IncomingConstants.ACTION_UPDATE -> {
                // subscriber requests an updated action
                val version = intent.getIntExtra(IncomingConstants.EXTRA_VERSION, 1)
                if (version != 2) {
                    // Only the current protocol is supported.
                    Log.w(TAG, "Ignoring unsupported version $version, id=$name")
                    return
                }
                if (!intent.hasExtra(IncomingConstants.EXTRA_ENTITY_IDENTIFIER)) return
                val identifier = intent.getIntExtra(IncomingConstants.EXTRA_ENTITY_IDENTIFIER, 0)
                if (intent.hasExtra(IncomingConstants.EXTRA_EPISODE)) {
                    handleEpisodeRequest(
                        context,
                        identifier,
                        intent.getBundleExtra(IncomingConstants.EXTRA_EPISODE)
                    )
                } else if (intent.hasExtra(IncomingConstants.EXTRA_MOVIE)) {
                    handleMovieRequest(
                        context,
                        identifier,
                        intent.getBundleExtra(IncomingConstants.EXTRA_MOVIE)
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION") // Typed variant requires API 33
    private fun getSubscriberComponent(intent: Intent): ComponentName? {
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(
                IncomingConstants.EXTRA_SUBSCRIBER_COMPONENT,
                ComponentName::class.java
            )
        } else {
            intent.getParcelableExtra(IncomingConstants.EXTRA_SUBSCRIBER_COMPONENT)
        }
    }

    private fun handleEpisodeRequest(
        context: Context,
        episodeIdentifier: Int,
        episodeBundle: Bundle?
    ) {
        if (episodeIdentifier <= 0 || episodeBundle == null) return
        val episode = Episode.fromBundle(episodeBundle)
        val action = try {
            onEpisodeRequest(context, episodeIdentifier, episode)
        } catch (e: Exception) {
            Log.e(TAG, "Episode request failed, id=$name", e)
            null
        }
        publish(context, action, OutgoingConstants.ACTION_TYPE_EPISODE)
    }

    private fun handleMovieRequest(
        context: Context,
        movieIdentifier: Int,
        movieBundle: Bundle?
    ) {
        if (movieIdentifier <= 0 || movieBundle == null) return
        val movie = Movie.fromBundle(movieBundle)
        val action = try {
            onMovieRequest(context, movieIdentifier, movie)
        } catch (e: Exception) {
            Log.e(TAG, "Movie request failed, id=$name", e)
            null
        }
        publish(context, action, OutgoingConstants.ACTION_TYPE_MOVIE)
    }

    private fun handleSubscribe(context: Context, subscriber: ComponentName?, token: String?) {
        if (subscriber == null) {
            Log.w(TAG, "No subscriber given.")
            return
        }

        val prefs = getSharedPreferences(context, name)
        val subscribers = loadSubscriptions(prefs)

        val oldToken = subscribers[subscriber]
        if (token.isNullOrEmpty()) {
            if (oldToken == null) return

            // Unsubscribing
            subscribers.remove(subscriber)
            notifySubscriberRemoved(context, subscriber, subscribers)
        } else {
            // Subscribing
            if (!oldToken.isNullOrEmpty()) {
                // Was previously subscribed, treat this as a unsubscribe + subscribe
                subscribers.remove(subscriber)
                notifySubscriberRemoved(context, subscriber, subscribers)
            }

            if (!onAllowSubscription(context, subscriber)) {
                // Previous subscription (if any) was removed above, so still save.
                saveSubscriptions(prefs, subscribers)
                return
            }

            subscribers[subscriber] = token
            if (subscribers.size == 1) {
                onEnabled(context)
            }
            onSubscriberAdded(context, subscriber)
        }

        saveSubscriptions(prefs, subscribers)
    }

    private fun notifySubscriberRemoved(
        context: Context,
        subscriber: ComponentName,
        remaining: Map<ComponentName, String>
    ) {
        onSubscriberRemoved(context, subscriber)
        if (remaining.isEmpty()) {
            onDisabled(context)
        }
    }

    private fun publish(context: Context, action: Action?, actionType: Int) {
        if (action == null) return

        val prefs = getSharedPreferences(context, name)
        val subscribers = loadSubscriptions(prefs)
        if (subscribers.isEmpty()) return

        val actionBundle = action.toBundle()
        val defunct = mutableListOf<ComponentName>()
        for ((subscriber, token) in subscribers) {
            if (token.isEmpty()) {
                Log.w(TAG, "Not active, canceling update, id=$name")
                continue
            }

            // Check if the subscriber still exists
            try {
                context.packageManager.getReceiverInfo(subscriber, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                defunct.add(subscriber)
                continue
            }

            // Publish update
            val intent = Intent(OutgoingConstants.ACTION_PUBLISH_ACTION)
                .setComponent(subscriber)
                .putExtra(IncomingConstants.EXTRA_TOKEN, token)
                .putExtra(OutgoingConstants.EXTRA_ACTION, actionBundle)
                .putExtra(OutgoingConstants.EXTRA_ACTION_TYPE, actionType)
            try {
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Couldn't publish update, id=$name", e)
            }
        }

        if (defunct.isNotEmpty()) {
            Log.e(TAG, "Update not published to subscribers that no longer exist, id=$name")
            // Unsubscribe the now-defunct subscribers
            for (subscriber in defunct) {
                subscribers.remove(subscriber)
                try {
                    notifySubscriberRemoved(context, subscriber, subscribers)
                } catch (e: Exception) {
                    Log.e(TAG, "Subscriber removed callback failed, id=$name", e)
                }
            }
            saveSubscriptions(prefs, subscribers)
        }
    }

    private fun loadSubscriptions(prefs: SharedPreferences): MutableMap<ComponentName, String> {
        val subscribers = LinkedHashMap<ComponentName, String>()
        val serialized = prefs.getStringSet(PREF_SUBSCRIPTIONS, null) ?: return subscribers
        for (entry in serialized) {
            val parts = entry.split('|', limit = 2)
            if (parts.size != 2) continue
            val subscriber = ComponentName.unflattenFromString(parts[0]) ?: continue
            subscribers[subscriber] = parts[1]
        }
        return subscribers
    }

    private fun saveSubscriptions(
        prefs: SharedPreferences,
        subscribers: Map<ComponentName, String>
    ) {
        val serialized = subscribers.mapTo(HashSet()) { (subscriber, token) ->
            subscriber.flattenToShortString() + "|" + token
        }
        prefs.edit().putStringSet(PREF_SUBSCRIPTIONS, serialized).apply()
    }

    public companion object {
        private const val TAG = "ExtensionActionProvider"

        /**
         * The [Intent] action that this receiver should declare an
         * `<intent-filter>` for to let SeriesGuide pick it up.
         */
        public const val ACTION_SERIESGUIDE_EXTENSION: String =
            "com.battlelancer.seriesguide.api.SeriesGuideExtension"

        /**
         * Boolean extra that will be set to true when SeriesGuide starts the extensions (optionally)
         * declared settings activity.
         * Check for this extra in your settings activity if you need to adjust your UI depending on
         * whether or not the user came from SeriesGuide's settings screen.
         */
        public const val EXTRA_FROM_SERIESGUIDE_SETTINGS: String =
            "com.battlelancer.seriesguide.api.extra.FROM_SERIESGUIDE_SETTINGS"

        // Same names as SeriesGuideExtension, so subscribers are kept when migrating.
        private const val PREF_PREFIX = "seriesguideextension_"
        private const val PREF_SUBSCRIPTIONS = "subscriptions"

        /**
         * Single thread shared by all extensions in the process. Keeps the order of requests and
         * serializes access to shared preferences.
         */
        private val EXECUTOR: Executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "ExtensionActionProvider").apply { isDaemon = true }
        }

        /**
         * Convenience method for accessing preferences specific to the extension (with the given
         * name within this package). The name must be the one provided in the
         * [ExtensionActionProvider] constructor. This static method is useful for exposing
         * extension preferences to other application components such as a settings activity.
         *
         * @param context The context; can be an application context.
         * @param extensionName The name provided in the [ExtensionActionProvider] constructor.
         */
        @JvmStatic
        public fun getSharedPreferences(context: Context, extensionName: String): SharedPreferences {
            return context.getSharedPreferences(PREF_PREFIX + extensionName, 0)
        }
    }
}
