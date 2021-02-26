# SeriesGuide API change log

## 2.2.1 (2021-02-26, supported as of SeriesGuide 58-beta1)
- View intents now expect number and season for episode instead of TMDB ID.

## 2.2.0 (2021-02-25, supported as of SeriesGuide 58-beta1)
- Episode info now contains TMDB ID of episode and show, and may now contain 0 for TVDB IDs.
- View intents now expect TMDB IDs for shows and episodes.

## 2.1.0 (2020-09-17)
- Migrate to AndroidX.
- Use Java 8 language features.

## 2.0.3 (2017-11-30, required as of SeriesGuide 40-beta6)
- Restored backwards compatibility (SeriesGuide 39 and lower) by sending version when requesting an action.
- Until your extension is updated its actions will not be displayed in SeriesGuide. It will stay enabled though.

## 2.0.2 (2017-11-23, required as of SeriesGuide 40-beta5)
- Extensions now publish actions via `sendBroadcast(Intent)` instead of `startService(Intent)`. This is to handle an edge case where SeriesGuide was idle and the extension would crash when publishing an action on Android O.
- Until your extension is updated its actions will not be displayed in SeriesGuide. It will stay enabled though.

## 2.0.0 (2017-11-10, required as of SeriesGuide 40-beta4)
- **Extensions must now be registered through a `<receiver>` component instead of a  `<service>`.** This is required to work around [background restrictions](https://developer.android.com/about/versions/oreo/background.html#services) introduced for apps targeting SDK 26. You can subclass the new [`SeriesGuideExtensionReceiver`](https://seriesgui.de/api/reference/com/battlelancer/seriesguide/api/SeriesGuideExtensionReceiver.html) and copy the properties of your existing `<service>` component in your manifest to the new `<receiver>` tag.
- **[`SeriesGuideExtension`](https://seriesgui.de/api/reference/com/battlelancer/seriesguide/api/SeriesGuideExtension.html) now implements [`JobIntentService`](https://developer.android.com/reference/android/support/v4/app/JobIntentService.html) instead of [`IntentService`](https://developer.android.com/reference/android/app/IntentService.html).** This requires your subclass to be exported and given the `BIND_JOB_SERVICE` permission in your manifest. In addition, on SDK 25 and lower the library will add the `WAKE_LOCK` permission required by `JobIntentService` to your manifest.
- **Existing extensions will be disabled when updating to SeriesGuide 40.** They can be enabled again by the user (assuming they have been updated as stated above).
- See [the example project](https://github.com/UweTrottmann/SeriesGuide-Extension-Example) for guidance on [how to keep your existing `SeriesGuideExtension` subclass](https://github.com/UweTrottmann/SeriesGuide-Extension-Example/blob/main/app/src/main/java/com/uwetrottmann/seriesguide/extensionexample/app/ExampleExtension.java) and [manifest tags](https://github.com/UweTrottmann/SeriesGuide-Extension-Example/blob/main/app/src/main/AndroidManifest.xml) for compatibility with old versions of SeriesGuide.

## 1.4.0 (2017-10-28)
- Converted the library to an Android library (AAR).

## 1.3.0 (2016-09-22, supported as of SeriesGuide 33-beta1)
- Add show release date to [`Episode`](http://seriesgui.de/api/reference/com/battlelancer/seriesguide/api/Episode.html). Formatted as an ISO string, for example: `2016-09-22T02:00:00.000Z`.
- Support for movies. Added [`Movie`](http://seriesgui.de/api/reference/com/battlelancer/seriesguide/api/Movie.html) and an additonal `onRequest` method specific for movie actions. Extensions can choose if to implement episode or movie actions, or both.

## 1.2.0 (2015-07-13, supported as of SeriesGuide 24-beta3)
- Add absolute number to [`Episode`](http://seriesgui.de/api/reference/com/battlelancer/seriesguide/api/Episode.html).

## 1.1.1 (2014-08-21)
- Updated [`Intents`](http://seriesgui.de/api/reference/com/battlelancer/seriesguide/api/Intents.html) helper class to create intents that do not create new tasks.

## 1.1.0 (2014-04-02)
- Create [`Intents`](http://seriesgui.de/api/reference/com/battlelancer/seriesguide/api/Intents.html) class to create intents for viewing shows or episodes with SeriesGuide.

## 1.0.1 (2014-03-26)
- Initial release. Extensions can provide actions for episodes.
