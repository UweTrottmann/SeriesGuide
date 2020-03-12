<a name="top"></a>

Release notes for version 46 and older
=============

🌟 = New.
🔧 = Improved or tweaked.
🔨 = Resolved or fixed.
📝 = Notable change.

Version 46
----------
*(2019-01-04)*

📝 Add no more updates notice for devices running Android 4.4 (KitKat) and older. The next version will only be available for Android 5.0 (Lollipop) and newer.

🌟 Add large font option for list widget. This can help with launchers that display widgets smaller than intended.

🔨 Sharing thetvdb.com URLs to SeriesGuide will again suggest to add the show.

🔧 Link to new community site.

🔧 Drop Google Analytics.

#### 46.1
*(2019-01-16)*

* 🔧 After changing the language of a show or the alternative language in Settings, episode descriptions are updated properly again.
* 📝 Latest translations from crowdin.

#### 46
*(2019-01-04)*

* 🔧 Update link to community site, TheTVDB terms. Move them to root settings page.
* 📝 Latest translations from crowdin.

#### 46-beta5
*(2019-01-02)*

* 🔧 Drop Firebase Analytics.
* 📝 Latest translations from crowdin.

#### 46-beta4
*(2018-12-21)*

* 🔧 Tweak reported events so errors can be analyzed again.
* 📝 Latest translations from crowdin.

#### 46-beta3
*(2018-12-17)*

* 📝 Add no more updates notice for Android 4.4 (KitKat) and older. Version 47 will only be available for Android 5.0 (Lollipop) and newer.
* 🔨 Do not display Getting Started multiple times after switching tabs.
* 🔧 Switch to Google Analytics for Firebase. Updated privacy policy in that regard.
* 📝 Latest translations from crowdin.

#### 46-beta2
*(2018-12-14)*

* 🔧 Searching added shows: better results if characters are missing from the title ("Mr Robot" will find "Mr. Robot"). Thanks @thouseef!
* 🔧 Change the data saver (images over WiFi) setting right from the getting started view.
* 🔨 Sharing thetvdb.com URLs to SeriesGuide will again suggest to add the show.
* 🔨 trakt: when posting a comment fails because the account is banned from posting, do not sign out.
* 🔨 The list widget should no longer crash the app if loading a poster fails.
* 🔨 Backup: do not crash if no file URI is returned.
* 📝 Latest translations from crowdin.

#### 46-beta1
*(2018-11-29)*

* 🌟 Add large font option for list widget. This can help with launchers that display widgets 
  smaller than intended.
* 📝 Latest translations from crowdin.

Version 45
----------
*(2018-11-08)*

* Discover: tap a show, then the globe icon to change the language a show is added in.
* Settings: the fallback language for shows (English by default) is used to get show info not 
  available in the desired language.
* Discover: almost endless list of popular shows.
* trakt: sync ratings and watched movies even if Cloud is connected.
* Small icon tweaks to fit in with the latest version of Android.

#### 45.1
*(2018-11-10)*

* 🔨 Discover: correctly use fallback language if search language set to any.
* 🔨 Fix crash when querying purchases.
* 🔨 Potential crash fix for devices using broken theme engines.
* 📝 Latest translations from crowdin.

#### 45
*(2018-11-08)*

* 🔨 Fix crash on Xiaomi devices when using illegal links to notification sounds.
* 🔨 Fix crash when adding show to home screen and leaving app.

#### 45-beta3
*(2018-11-02)*

* 🔧 Speedier scrolling and animations for show list.
* 🔧 Support direct upgrade from SeriesGuide version 21 (previously: 26) or newer.
* 🔧 If Cloud and trakt are connected, also sync ratings and watched movies from trakt.
* 🔨 If Cloud and trakt are connected, do not remove watched flag from movies when syncing.
* 🔧 Use outline icons where appropriate to fit with new platform design.
* 🔨 Fix disappearing info message when setting movie watched that was on the watchlist.
* 🔨 Prevent crash on Cloud setup screen.
* 📝 Latest translations from crowdin.

#### 45-beta2
*(2018-09-28)*

* 🔧 Popular shows is now an (almost) endless list.
* 🔧 Display sync status (and error) for initial trakt sync step.
* 🔨 Fix memory leak when downloading movie info and TMDb asks to try again later.
* 🔨 Correctly back-off if syncing with trakt or Cloud failed.
* 🔨 Allow removing extensions with a remote (menu buttons were not selectable).
* 📝 Latest translations from crowdin.

#### 45-beta1
*(2018-09-21)*

* 🔧 Change the language a show will be added in from add dialog.
* 🔧 Remove 'Preferred content language' setting. Shows that never had a language set will default 
  to English instead.
* 🔧 Shows added from discover sub-sections (popular, recommended, watched, collection and watchlist) 
  skipping the add dialog are added in the language search is set to (or alternative language if set
  to any language).
* 🔧 If a show title is not translated get show title and description in the fall back language. 
  This mirrors existing behavior for episodes.
* 🔨 Potential fixes for crashes with dialogs, in calendar view and during background processing.
* 📝 Latest translations from crowdin.

Version 44
----------
*(2018-09-14)*

* Rename "JustWatch" button to "Stream or purchase", add Reelgood (US only) as an option.
* Tap and hold to change stream search provider.
* Tap and hold to copy various links to clipboard.
* Full episode details loaded when adding or updating a show instead of on-demand.

#### 44
*(2018-09-14)*

* 🔧 Tap and hold to change stream search provider.
* 🔧 Revert experiment with larger text for most descriptions.
* 📝 Latest translations from crowdin.

#### 44-beta3
*(2018-08-30)*

* 🔧 Rename "JustWatch" button to "Stream or purchase", add Reelgood (US only) as an option.
* 🔧 Always show quick-link to "Unlock all features" screen in navigation drawer.
* 🔨 Fix crash when loading person fails.
* 📝 Latest translations from crowdin.

#### 44-beta2
*(2018-08-24)*

* 🔧 Use and share new TVDB links for shows and episodes. Only works after a show is updated.
* 🔧 Tap and hold to copy TVDB, IMDB and trakt links for shows and episodes.
* 🔧 Add button to manage subscriptions (Play Store only).
* 🔧 Experiment with larger text for descriptions.
* 🔧 Move person actions inline, improve error messages.
* 🔧 Distinct icon for add to home screen button.
* 🔨 Drop person image animation, was broken and slow.
* 📝 Latest translations from crowdin.

#### 44-beta1
*(2018-08-09)*

* 🔨 Fix database migration for users that have used the legacy backup tool. Note that upgrading 
  from SeriesGuide 25 (2015) or older still remains not supported. Update to version 42.2 first. 
  Get the APK at https://github.com/UweTrottmann/SeriesGuide/releases/tag/v42.2
* 🔧 Full episode details (notably image path) are again loaded when adding or updating a show and 
  no longer on-demand when viewing an episode. This results in less network calls while browsing 
  episodes.
* 📝 Latest translations from crowdin. Added Cebuano (ceb) and Filipino (fil) (both incomplete).

Version 43
----------
*(2018-07-20)*

* 🌟 Add JustWatch button to quickly perform search of where to stream or purchase.
* 🔧 Migrate database to Room. Remove the legacy backup tool as it will no longer work.
* 🔧 Basic movie details are updated regularly (recent movies weekly, older movies every half year).
* 🔧 Improvements when using a keyboard or remote.
* 🔨 List widget should no longer stop refreshing on Huawei EMUI 8.0 devices.

#### 43.1
*(2018-08-16)*

* 🔨 Fix database migration if the legacy backup tool was used.
* 📝 Upgrading from a very old database (v37 or older) still unsupported. Update SeriesGuide to 
  version 42.2 first. Get the APK at https://github.com/UweTrottmann/SeriesGuide/releases/tag/v42.2
* 📝 Latest translations from crowdin.

#### 43
*(2018-07-20)*

* 🔧 Change 'Shortcut' button to the more descriptive 'Add to home screen'.
* 🔨 When cleaning up seasons, remove their episodes first.
* 🔨 Cloud sync: when cleaning up lists, remove their items first.
* 📝 Latest translations from crowdin.

#### 43-beta5
*(2018-07-13)*

* 🔧 Tap and hold copies text to the clipboard instead of selecting it, works with keyboard/remote.
* 🔧 Improve selection of various items using a keyboard or remote.
* 🔨 List widget should no longer stop refreshing on Huawei EMUI 8.0 devices.
* 🔨 Show wide overview and people layout based on screen width instead of smallest edge size.
* 🔨 Cloud: display error message if Google Play Services are not installed in every case.
* 📝 Latest translations from crowdin.

#### 43-beta4
*(2018-06-22)*

* 🔧 Basic movie details are updated regularly (recent movies weekly, older movies every half year).
* 🔨 Do not crash when migrating episodes with invalid data, skip them instead.
* 📝 Latest translations from crowdin.

#### 43-beta3
*(2018-05-25)*

* 🔨 Restoring shows and lists, removing a list works again.
* 📝 Latest translations from crowdin.

#### 43-beta2
*(2018-05-19)*

* 🔨 Fix Room migration issues.
* 📝 Latest translations from crowdin.

#### 43-beta1
*(2018-05-18)*

* 🌟 Add JustWatch button to quickly perform search of where to stream or purchase.
* 🔧 Add button bar to set a whole season watched or add it to your collection.
* 🔧 When searching suggest to add show if it is not found in show list.
* 🔧 Migrate database to Room. Remove the legacy backup tool as it will no longer work.
* 🔧 Episode history (if not connected to trakt): display up to 50 items instead of the last 24 hours.
* 🔧 Bigger empty state text, more visible buttons.
* 🔧 Change TheTVDB image URLs. All show posters will be downloaded again.
* 🔧 Add explanation for trakt feature symbols.
* 📝 Latest translations from crowdin. Add Galician.

Version 42
----------
*(2018-03-28)*

* 🔧 Run backup immediately after selecting file (Android 4.4 and up).
* 🔧 Sharing: Use TheTVDB link for shows and episodes, use TMDB link for movies.
* 🔨 Calendar: display correct date in headers on Android 5.1 and older.
* 🔨 Android 8: Fix occasional crash when calculating season unwatched count.

#### 42.2
*(2018-04-19)*

* 🔨 Correctly upload lists to Cloud even if one is broken.

#### 42.1
*(2018-04-19)*

* 🔨 Do not crash when viewing show and cast or crew members have no id.

#### 42
*(2018-03-28)*

* 🔨 Calendar: display correct date in headers on Android 5.1 and older.

#### 42-beta5
*(2018-03-22)*

* 🔨 Do not crash when logging.

#### 42-beta4
*(2018-03-22)*

* 🔧 Sharing: Use localized TheTVDB URL for shows and episodes. Use TMDB URL for movies (language is
  detected by website).
* 🔨 Calendar: correctly display "in x weeks" if daylight saving time is about to come in effect.
* 🔨 Support "America/Punta_Arenas" time zone.
* 🔨 Android 8: Fix occasional crash when calculating season unwatched count.
* 📝 Latest translations from crowdin.

#### 42-beta3
*(2018-03-16)*

* 🔧 Run backup immediately after selecting file (Android 4.4 and up).
* 🔨 When sharing text to SeriesGuide, run search immediately.
* 📝 Latest translations from crowdin.

#### 42-beta2
*(2018-03-10)*

* 🔨 Do not crash when parsing server responses fails.
* 📝 Latest translations from crowdin.

#### 42-beta1
*(2018-02-22)*

* 🔨 Fix rare crash when loading data from TMDB fails. 
* 📝 Latest translations from crowdin.

Version 41
----------
*(2018-02-09)*

* 🌟 Add alternative language setting for shows. Currently only used for episodes. After changing it
  shows have to be updated for changes to appear.
* 🌟 New show discover tab replaces add show and trakt tabs. Displays shows with new episodes 
  (powered by TMDb) and a link to popular shows (powered by trakt).
* 🔧 TheTVDB show and episode links will open the translation used inside the app. 

#### 41.2
*(2018-03-23)*

* 🔨 Calendar: display correct week in header if day is during daylight saving time.
* 🔨 Support "America/Punta_Arenas" time zone.
* 🔨 Do not crash when parsing server responses fails.
* 📝 Latest translations from crowdin.

#### 41.1
*(2018-02-23)*

* 🔨 Fix rare crash when loading data from TMDB fails. 
* 📝 Latest translations from crowdin.

#### 41
*(2018-02-09)*

* 🌟 Add alternative language setting for shows. Use it instead of the preferred content language 
  setting. After changing it shows have to be updated for changes to appear.
* 🔧 Opening TheTVDB links will go to the translation shown inside the app. 
* 🔨 Fix crashes in some conditions when viewing seasons, managing a list.
* 📝 Latest translations from crowdin.

#### 41-beta3
*(2018-02-03)*

* 🔨 Fix crashes when viewing statistics, opening trakt account info in settings.

#### 41-beta2
*(2018-02-02)*

* 🌟 Add show and trakt tabs replaced with discover tab. Displays shows with new episodes (powered
  by TMDb) and a link to popular shows (powered by trakt).
* 🔨 Reorder tabs if episode is set watched and episodes are sorted by unwatched first.
* 📝 Latest translations from crowdin.

#### 41-beta1
*(2018-01-11)*

* 🔧 If episode titles or overviews are not available in the language selected for a show, fall back
  to the preferred content language defined in settings.
* 🔧 Use dots to separate season info text.
* 📝 Start switching to Kotlin. The app will require a little more storage space.
* 📝 Latest translations from crowdin.

Version 40
----------
*(2017-12-07)*

* Extensions have changed (API v2): your enabled extensions are replaced with the default selection. 
  You can enable yours again, though the extension may have to be updated by the developer.
* SeriesGuide Cloud and trakt: For movies 'Set watched', 'Add to collection' and 'Add to watchlist'
  is re-tried on failure, queued while offline.
* Going up from the episode list or detail view goes directly to seasons.
* Display placeholder episode title if 'No spoilers' is enabled.
* Widget now updating more frequently on Android 8 (Oreo).
* Add notification channel support on Android 8 (Oreo).

#### 40
*(2017-12-07)*

* 🔧 Display placeholder episode title if 'No spoilers' is enabled.
* 📝 Latest translations from crowdin.

#### 40-beta6
*(2017-12-01)*

* 🔨 Support latest extensions interface. https://seriesgui.de/api/
* 🔨 Notifications not displaying until using the app.
* 🔨 Widget not updating frequently on Android 8 (Oreo).
* 📝 Latest translations from crowdin.

#### 40-beta5
*(2017-11-23)*

* 🔨 Issue where extensions may crash if SeriesGuide is in the background. Third-party 
     extensions need to be updated to work again.
* 🔨 Notifications not displaying on Android 8 (Oreo) while the device is asleep.
* 🔨 Do not group digits in season or episode numbers.
* 📝 Latest translations from crowdin.

#### 40-beta4
*(2017-11-15)*

* 📝 Extensions have changed (API v2): your enabled extensions will be replaced by the default set. 
     Existing extensions need to be updated and enabled to work again.
* 🔧 Drop progress dialog when removing a show. 
* 🔨 Localize more numbers (episodes, stats screen).
* 📝 Targeting Android 8.0 (Oreo).
* 📝 Latest translations from crowdin.

#### 40-beta3
*(2017-10-31)*

* 🔨 Crash: services may start in the background. (Revert targeting Android 8.0)
* 📝 Latest translations from crowdin.

#### 40-beta2
*(2017-10-27)*

* 🔧 SeriesGuide Cloud and trakt: For movies 'Set watched', 'Add to collection' and 
     'Add to watchlist' is re-tried on failure, queued while offline.
* 🔧 trakt: when setting a movie watched, it is removed from your watchlist.
* 📝 Latest translations from crowdin.
* 📝 Targeting Android 8.0 (Oreo).

#### 40-beta1
*(2017-10-19)*

* 🔧 Going up from episode list or page view goes directly to seasons list.
* 🔧 Add notification channel support on Android 8.0 (Oreo).
* 🔨 'Set all older watched' does not work if episodes released at the same time.
* 🔨 Manual time offset not respected for unwatched and upcoming filters.
* 🔨 Statistics are re-calculated when rotating or turning off screen, returning to app.
* 📝 Latest translations from crowdin.

Version 39
----------
*(2017-10-12)*

* SeriesGuide Cloud and trakt: For episodes, 'Set watched' and 'Add to collection' is re-tried on 
  failure, queued while offline.
* Display source of show, episode, movie and person data.
* Allow network text on widget to be wider.
* Improve detecting new, updated and removed extensions.

#### 39
*(2017-10-12)*

* 🔨 Crash if opening external app fails.
* 🔨 Crash if trying to remove broken extension.
* 📝 Latest translations from crowdin.

#### 39-beta6
*(2017-09-29)*

* 🔧 Check for new extensions when returning to manage extensions screen, like after installing one.
* 🔨 Uninstalled and updated extensions not detected. 
* 📝 Latest translations from crowdin.

#### 39-beta5
*(2017-09-22)*

* 🔨 Crash when trying to parse additional episode info.

#### 39-beta4
*(2017-09-22)*

* 🔧 SeriesGuide Cloud, trakt: For episodes, set (not) watched and add to/remove from collection is 
     automatically re-tried, works offline and no longer blocks buttons. If it is impossible due 
     to an error or a show/episode is not available on trakt, a notification is displayed.
* 🔧 Remove fast scrolling for seasons, benefits do not outweigh issues with opening more options
     menu.
* 🔨 Crash if episode or movie description was empty.
* 📝 Latest translations from crowdin.

#### 39-beta3
*(2017-09-02)*

* 🔨 Did not recognize 2016 subscriptions.

#### 39-beta2
*(2017-09-01)*

* 🔧 Widget: allow network text to be wider.
* 🔧 Updated subscription and one-time purchase pricing.
* 🔨 Setting episode watched from notification may not work if device is really busy.
* 📝 Latest translations from crowdin.

#### 39-beta1
*(2017-08-11)*

* 🔧 Replace remaining icons with vector versions, use distinct shape for each state of toggleable icons.
* 🔧 Explicitly display source of show, episode, movie and person data.
* 🔧 Do not focus on show search bar when entering to add shows.
* 📝 Latest translations from crowdin.

Version 38
----------
*(2017-07-26)*

* Highlight show posters, improve text.
* Rename 'Now' to 'History'. 'Released today' moved to calendar.
* Display (expected) posters in history tabs and episode search.
* Season info text no longer says all episodes are watched if none are watched.
* trakt and Cloud account screens display sync details.
* Adaptive icons on Android O.
* Fix Arabic release date and time format.

#### 38
*(2017-07-26)*

* 🔨 Do not show auto backup warning if issue was resolved.
* 🔨 Auto backup screen checks permissions when opening it.
* 🔧 Do not show week in calendar if released in the next or previous 6 days.
* 📝 Latest translations from crowdin.

#### 38-beta5
*(2017-07-14)*

* 🔧 Restore round laucher icon (Android 7).
* 🔧 Change transparent status bars to material style, use dark icons for light theme (Android 6+).
* 🔧 Change app bar color to black for 'Android Dark' theme.
* 🔨 Potential fix for blank space appearing left of network text in rare occasions.
* 📝 Latest translations from crowdin.

#### 38-beta4
*(2017-07-06)*

* 🔧 Rename 'Now' to 'History' tab, drop 'Released today' (still available in upcoming/recent).
* 🔧 Reduce visual clutter of show and history cards, list widget.
* 🔧 Display (expected) posters in show and movie history tabs, episode search. 
* 🔧 Refine app icon, drop round variant.
* 🌟 Adaptive app and shortcut icons on Android O.
* 🌟 Support pinning show shortcuts on Android O.
* 📝 Latest translations from crowdin.

#### 38-beta3
*(2017-06-24)*

* 🔨 Crash when viewing details of episode that is not part of any season.
* 🔧 When updating shows, remove seasons without episodes.

#### 38-beta2
*(2017-06-23)*

* 🔧 SeriesGuide Cloud and trakt account screens display available features and sync status.
* 🔧 Simplified season info text, does not say all watched if none watched.
* 🔧 Enable favorite (star) button of list items.
* 🔧 In show list, do not display day of episode if it is the same as the regular release day.
* 🔧 Skip TheTVDB sync after 3 consecutive timeouts.
* 🔧 Dim episodes until actual release, matching season info text.
* 🔨 Drop-down to change seasons contains empty or duplicate seasons.
* 🔨 Last watched times not updated correctly when syncing with Cloud and show is not on device.
* 🔨 Do not move Hulu shows released at midnight by one day.
* 🔨 Wrong format of release date and time in Arabic.
* 📝 Latest translations from crowdin.

#### 38-beta1
*(2017-05-26)*

* 🔧 Add "Infinite calendar" setting to widget. Disable it for existing widgets.
* 🌟 Add extension for German video-on-demand and streaming search engine vodster.de.
* 🔧 Increase enabled extension limit to 10.
* 🔨 Potential fix for sync adapter crashes.
* 🔨 If adding multiple shows, the first one might never display as added.
* 📝 Latest translations from crowdin.

Version 37
----------
*(2017-05-17)*

* Turn on and off notifications for each show.
* Within limits, select any number of minutes, hours or days to notify before episodes are released.
* Use TheTVDB API v2 to fetch episode data: no longer falls back to English if not translated.
* Add "Only collected episodes" option to calendar and list widget.
* Offset show release times between +/-24 hours in Settings > Basics > Manual time offset.
* Dim titles of episodes that are not released.

#### 37.1
*(2017-06-01)*

* 🔧 Add "Infinite calendar" setting to widget. Disable it for existing widgets.
* 🌟 Add extension for German video-on-demand and streaming search engine vodster.de.
* 🔧 Increase enabled extension limit to 10.
* 🔨 Potential fix for sync adapter crashes.
* 🔨 If adding multiple shows, the first one might never display as added.
* 🔨 Could tap add button for already added show.
* 📝 Latest translations from crowdin.

#### 37
*(2017-05-17)*

* 🔧 Revert to set watched/not watched when pressing the tick icon in calendar.
* 📝 Latest translations from crowdin.

#### 37-beta7
*(2017-05-12)*

* 🔧 Add "Only collected episodes" option to calendar and list widget settings.
* 🔧 Move calendar settings to own button.
* 🔧 Display popup menu with multiple options when pressing tick icon in calendar.
* 🔨 List widget settings are saved even if not tapping "Save selection".
* 🔨 Old content in Now tabs stuck when switching away and back while refresh indicator is visible.
* 🔨 If the notification threshold is 0, display '0 minutes' instead of '0 days'.
* 🔧 Use vector icons in more places: crisp on very high resolution screens, reduced app size. 
* 📝 Latest translations from crowdin.

#### 37-beta6
*(2017-05-06)*

* 🔨 Crash: Correctly parse episode DVD numbers when adding or updating a show.
* 🔨 Some action bar icons are wrongly colored.
* 📝 Latest translations from crowdin.

#### 37-beta5
*(2017-05-05)*

* 🔧 Use TheTVDB API v2 to fetch episode information: if no translation for title or description
  exists, will no longer fall back to English. Image, directors, writers and guest stars are only
  fetched when viewing an episode.
* 🔧 Display generic episode title, such as 'Episode 2', if actual title is not available.
* 🔧 If available, show internal details why backing up or restoring failed.
* 🔨 Do not remove release time when updating a show and talking to trakt fails.
* 📝 Latest translations from crowdin.

#### 37-beta4
*(2017-04-28)*

* 🌟 Turn on and off notifications for each show individually.
* 🔨 Exclude Amazon shows from hour-past midnight correction.
* 🔧 Replace show tab share and shortcut button with notifications and hide button.
* 🔧 Add show share and shortcut buttons to bottom buttons.
* 🔨 Do not auto-select text in time offset dialog. Gboard does not play nice if it is.
* 🔧 Display numbers-only date on movie cards as in some languages the date text was too long.
* 📝 Latest translations from crowdin.

#### 37-beta3
*(2017-04-12)*

* 🔧 Show release times can now be moved between -/+24 hours with Settings > Basics > Manual time
     offset. It also displays an example.
* 🔨 Search history entries contain strange characters. Use Clear search history action to delete them. 
* 🔨 Crash due to episode time parsing failing due to invalid date.
* 🔨 Crash due to missing character encoding class on Android 4.x.
* 📝 Latest translations from crowdin.

#### 37-beta2
*(2017-04-07)*

* 🌟 Within limits, select any number of minutes, hours or days to notify before episodes are released.
* 🔨 Time stamps in trakt history are wrong.
* 🔨 Show add dialog displays "unknown" language.
* 🔧 Let non-supporters see available list widget options.
* 📝 Latest translations from crowdin.

#### 37-beta1
*(2017-03-31)*

* 🔨 Crash when signing in with Cloud on some devices.
* 🔧 Move show search language selector to toolbar.
* 🔧 Display language in native language for shows and movies. Like "German" instead of "Deutsch".
* 🔧 Dim titles of episodes that are not released.
* 🔧 Recognize the URL created when sharing a show, when sharing it back to SeriesGuide: offer to add the show.
* 🔧 Drop the stupid "Check out" from share messages, you are intelligent enough to write your own message.
* 🔧 Layout improvements in backup tools.

Version 36
----------
*(2017-03-30)*

* SeriesGuide Cloud switched to Google Sign-In, no longer requires the Contacts permission.
  But you will have to sign in again.
* Display progress when adding a show, tapping it once added opens the show directly.
* Show always both Cloud and trakt account in navigation drawer.

#### 36
*(2017-03-30)*

* 🔨 Cloud signed out warning displayed after adding show, but was never signed in to Cloud.
* 🔨 Can not swipe away Cloud signed out warning on Android 4.4 (KitKat) and below.
* 📝 Latest translations from crowdin.️️

#### 36-beta5
*(2017-03-24)*

* 🔨 Changing widget settings not applied right away for some Google launchers.
* 🔨 Widget empty text has wrong font color on Android 7.0 devices.
* 🔨 Some rare crashes due to multi-touch use.
* 📝 Latest translations from crowdin.️️

#### 36-beta4
*(2017-03-17)*

* 🔧 Load all show and episode images via the SeriesGuide image cache server to reduce load on TheTVDB.
* 📝 Latest translations from crowdin.️️ Season episode count strings need to be re-translated.
* 📝 Added Indonesian translation.

#### 36-beta3
*(2017-03-14)*

* 🔧 TWEAK Show details why a show can not be added.

#### 36-beta2
*(2017-03-09)*

* 📝 NOTE Users are forced to sign-in again to SeriesGuide Cloud.
* 🔧 TWEAK Use Google Sign-In for SeriesGuide Cloud, does not require Contacts permission.
* 🔧 TWEAK Show Cloud and trakt account in navigation drawer.
* 🔧 TWEAK Show message if signed out of SeriesGuide Cloud.
* 🔧 TWEAK Reduce auto-sync trigger interval when opening app to 5 minutes.
* 🔨 FIX Sometimes selected season is wrong when returning to episode details view. Thanks @cbeyls!
* 🔨 FIX Do not allow multi-line list names.
* 📝 NOTE Latest translations from crowdin.

#### 36-beta1
*(2017-02-18)*

* TWEAK Open show from search result directly if it is added.
* TWEAK Show progress while adding a show.
* TWEAK Show progress while loading popular/digital/disc movies.
* NOTE Latest translations from crowdin.

Version 35
----------
*(2017-02-03)*

* New movie discover section with regional selection, search moved to its own screen.
* Tap a tab to scroll to the top in most places.
* Improved status messages during trakt or Cloud actions.

#### 35.1
*(2017-03-14)*

* TWEAK Show details why a show can not be added.

#### 35
*(2017-02-03)*

* TWEAK Display localized string for language in movie details.
* FIX Crash when loading movie poster, but views are already gone.
* FIX Crash if searching for movies fails.
* NOTE Latest translations from crowdin.

#### 35-beta4
*(2017-01-27)*

* FEATURE Add region selector to movie discovery. Tap the globe to see release dates for your region in discover and search.
* TWEAK Movie search: show keyboard when opening it, add swipe to refresh.
* TWEAK Improve launcher shortcuts icons (Android 7.1). Thanks @cbeyls!
* FIX Image transitions fail after rotating the device. Thanks @cbeyls!
* NOTE Latest translations from crowdin.

#### 35-beta3
*(2017-01-20)*

* FEATURE Tap a tab to scroll to the top in most places.
* TWEAK Improved movie discovery tab, includes in theaters/popular/digital and disc releases for the region of your selected language (like US for en-US).
  A region (like GB, CA) setting will be added later.
* TWEAK Moved movie search to its own screen, now accessible from any movie tab.
* TWEAK Improved movie tablet portrait layout (also landscape on 7inch tablets).
* TWEAK Improved action button design below episodes and movies.
* TWEAK Display actual number for ratings.
* FIX Crash on some Android 4.0 devices which are missing a standard database feature introduced for search.
* FIX Search bar overlaps on small screen devices.
* FIX Ratings are rounded incorrectly on Android 6 and below.
* NOTE Latest translations from crowdin.

#### 35-beta2
*(2016-12-16)*

* TWEAK Display status message while trakt or Cloud network action is processing for movies.
* TWEAK Display localized string for release country.
* FIX Crash when trying to upgrade the database, but it is not writable.
* NOTE Latest translations from crowdin.

#### 35-beta1
*(2016-12-09)*

* TWEAK Display status message while trakt or Cloud network action is processing.
* NOTE Update HTTP networking library.
* NOTE Latest translations from crowdin.

Version 34
----------
*(2016-12-01)*

* Change show sort options: sort by title, latest or oldest next episode, last watched show or remaining number of episodes.
* Display the remaining number of episodes right in the shows list.
* Add separate language selector for movies, supports all languages of The Movie Database.
* For Android 7.1: add round launcher icon, launcher shortcuts.

#### 34.2
*(2016-12-27)*

* FIX Crash on some Android 4.0 devices which are missing a standard database feature introduced for search.

#### 34.1
*(2016-12-19)*

* TWEAK Display localized string for release country.
* FIX Crash when trying to upgrade the database, but it is not writable.

#### 34
*(2016-12-01)*

* NOTE Latest translations from crowdin.

#### 34-beta4
*(2016-11-25)*

* TWEAK Last watched sort order is initialized with data from recently watched episodes in Now tab.
* TWEAK Display message if database is corrupted, advice to backup, re-install and restore.
* FIX Display episodes released before 1970 in infinite calendar.
* FIX Crash in list widget settings.
* NOTE Latest translations from crowdin.

#### 34-beta3
*(2016-11-18)*

* FEATURE Changed show sort options. Shows can now be sorted by title, latest or oldest next episode, last watched or remaining number of episodes.
* FEATURE Support new show sort options for lists and widgets.
* TWEAK In the show list and in lists, shows display the remaining number of released, unwatched episodes right on their card.
* TWEAK Display spoiler setting instead of language setting on getting started card.
* FIX Trim leading and trailing white spaces from show titles.
* NOTE Latest translations from crowdin.

#### 34-beta2
*(2016-11-04)*

* TWEAK Add separate language selector for movies, supports all languages from TMDB.
* TWEAK Show better error messages if loading shows and movies fails.
* TWEAK Show better error message in add dialog if show does not exist on TheTVDB.

#### 34-beta1
*(2016-10-28)*

* TWEAK Use an image caching server, currently only for images from TheTVDB in the show search screen.
* TWEAK Add round launcher icon.
* TWEAK Add launcher shortcuts to "Add show", "Lists" and "Movies".
* TWEAK Improvements to search index generation (use FTS4 instead of FTS3), should require less disk space.
* FIX Text overlaps other content if movie title is very long.
* FIX Handle duplicates when adding episodes while adding or updating a show.

Version 33
----------
*(2016-10-20)*

* "Prevent spoilers" option in Settings > Basics. If enabled, episode details are hidden until an episode is watched.
* Extensions support for movies. See https://seriesgui.de/api for details.
* Share TheTVDB links to SeriesGuide to add a show.
* "Set watched" action in single episode notifications.
* All changes at https://git.io/sgchanges

#### 33
*(2016-10-20)*

* FIX Crash when taping on item in Movies Now tab.
* NOTE Latest translations from crowdin.

#### 33-beta5
*(2016-10-13)*

* TWEAK trakt will stop supplying images end of October. This affects the Now tabs and history
  screens, as well as the search tabs for shows. For shows, SeriesGuide will fall back to a TheTVDB
  poster. For movies, images from trakt have been removed.
* NOTE Latest translations from crowdin.

#### 33-beta4
*(2016-10-06)*

* TWEAK "Prevent spoilers" setting also applies to widgets.
* FIX Revert Android Support library version to avoid rare crash.
* NOTE Latest translations from crowdin.

#### 33-beta3
*(2016-09-30)*

* FEATURE Add "Prevent spoilers" option in Settings. If enabled, episode details are hidden until
  the episode is watched.
* TWEAK Add "Delete" option to show overview menu.
* TWEAK Add "Set watched" action to single episode notifications.
* TWEAK Show "Check in" as a separate full-width button. Hide it if trakt is not connected.
* NOTE Latest translations from crowdin.

#### 33-beta2
*(2016-09-28)*

* FIX Crash when opening show overview or movie details on Android 4.4 and below. New vector icons
  are not supported without some tweaks.
* FIX Crash in movie details when actions are loaded, but there is no view to attach them to.

#### 33-beta1
*(2016-09-23)*

* FEATURE Extensions support for movies. See https://seriesgui.de/api for details.
* FEATURE Share TheTVDB links to SeriesGuide to add a show. For now, if no TheTVDB link is recognized,
  the search field is filled with the shared text.
* TWEAK Display language button right above show description. Easier to find and understand what it does.
* NOTE Latest translations from crowdin.

Version 32
----------
*(2016-08-24)*

* Various layout improvements, most notably for image sizes.
* Remember last selected tab in Lists and Movies.
* The list widget can now be reduced horizontally to 2 blocks.
* Support for Android 7.0 (Nougat).

#### 32.1
*(2016-08-25)*

* FIX Dialogs have a white background, text is unreadable.
* FIX Movie posters have slightly wrong size.
* FIX Crash when trying to purchase subscription.

#### 32
*(2016-08-24)*

* TWEAK Remember last selected tab in Lists and Movies.
* NOTE Support Android 7.0 (Nougat).
* NOTE Latest translations from crowdin. Thanks to translators added Japanese and Tamil.

#### 32-beta3
*(2016-08-13)*

* NOTICE Started some architecture changes, let me know if you see issues with TVDb, trakt and TMDb integration.
* TWEAK Match rating text styles throughout the app.
* NOTICE Latest translations from crowdin.

#### 32-beta2
*(2016-08-04)*

* TWEAK Many layout updates, most notably better image aspect ratios.
* TWEAK Allow list widget to be reduced horizontally to 2 blocks.
* NOTICE Latest translations from crowdin.

#### 32-beta1
*(2016-07-08)*

* TWEAK Movie details include poster, including full screen poster view on clicking poster.
* TWEAK When viewing a poster full screen display the low resolution version until the full version has loaded.
* TWEAK Keyboard is hidden by default when showing comments.
* TWEAK Improved dialog button sizes and stacking. Other layout and design tweaks.
* FIX Do not show full screen poster if there is no poster.
* FIX Show movie details if they are cached even if offline.
* NOTICE Latest translations from crowdin.

Version 31
----------
*(2016-06-23)*

* Faster and more reliable trakt watched and collected episode sync.
* No "hour-past-midnight" correction for Netflix shows (would get moved by one day).

#### 31.1
*(2016-06-29)*

* NOTICE Latest translations from crowdin.

#### 31
*(2016-06-23)*

* TWEAK Ask long time users for feedback.
* TWEAK Fix cut-off card shadows in show overview.
* NOTICE Latest translations from crowdin.

#### 31-beta4
*(2016-06-16)*

* TWEAK No "hour-past-midnight" correction for Netflix shows (would get moved by one day).
* TWEAK Display error if calculating statistics fails.
* TWEAK Enable full auto backup on Android 6.0 (Marshmallow) and up.
* FIX Correctly save trakt ratings for episodes.
* FIX Correctly display show or episode opened via view intent from other apps.
* NOTICE Latest translations from crowdin.

#### 31-beta3
*(2016-06-12)*

* FIX Crash when trying to refresh trakt access token.

#### 31-beta2
*(2016-06-10)*

* TWEAK Rewritten trakt watched and collected episode sync. Should be faster and more reliable.
* TWEAK Show keyboard when opening search for a show or episode (not when trying to add a show).
* TWEAK If available, show trakt user display name in trakt settings.
* NOTICE Latest translations from crowdin.

#### 31-beta1
*(2016-05-27)*

* TWEAK Add note if no translation of a movie description is available.
* TWEAK Move person and movie links to new links menu item with distinct "explore" icon.
* TWEAK Updated pricing for the "Unlock All Subscription". Note: "X Pass" pricing was updated as well.
* TWEAK Reduced the size of the app.
* NOTICE Latest translations from crowdin.

Version 30
----------
*(2016-05-06)*

* On phones: switch seasons from the top of the episode details screen.
* Lists are synced with SeriesGuide Cloud.
* In search screen: add option to add shows to trakt watchlist. Support removing shows from trakt watchlist.

#### 30.0.1
*(2016-05-11)*

* FIX Send automatically created first list to Cloud as well.
* TWEAK "Images via Wi-Fi only" is disabled by default again.

#### 30
*(2016-05-06)*

* NOTICE Latest translations from crowdin.

#### 30-beta5
*(2016-04-29)*

* FEATURE Lists are synced with SeriesGuide Cloud.
* TWEAK When adding a show the chosen language is synced with Cloud.
* FIX Do not set the default language to English when exporting shows. Instead specify no language,
  so when importing the user preferred language is used.
* NOTICE Latest translations from crowdin.

#### 30-beta4
*(2016-04-22)*

* FEATURE In search screen: add option to add shows to trakt watchlist. Support removing shows from
  trakt watchlist.
* FIX Potential fix of search table creation failing in some situations.
* FIX Crash due to broken Persian and Polish translation.
* NOTICE Latest translations from crowdin.

#### 30-beta3
*(2016-04-15)*

* FEATURE On phones: switch seasons from the top of the episode details screen.
* TWEAK If a show title and description are not translated, fall back to the default translation and
  add a note to the description that no translation is available.
* TWEAK Allow any list name as long as it is not empty or the same as an existing list. No more
  special or latin character restrictions.
* FIX Crash when viewing About on devices with Android 5.1 or older.
* FIX Recent search suggestions drop down is colored gray on the light theme.
* NOTICE Latest translations from crowdin.

#### 30-beta2
*(2016-04-09)*

* TWEAK Use TVDB API v2 to search for shows (non-English, not any), fetch show info (not episodes).
  If no show title and overview translation exists English will no longer be used as a fallback.
  A placeholder title and overview is show instead.
* FIX Database error message crashes the app.
* NOTICE Latest translations from crowdin.

#### 30-beta1
*(2016-03-17)*

* TWEAK Offer to open shows in add dialog that are already added to SeriesGuide.
* TWEAK Display pieces of information right in the root settings screen. Like the current supporter
  state or signed-in account names.
* TWEAK Clean up the first run experience. Do not show the nav drawer, show the auto backup
  permission warning only after Getting Started is dismissed.
* TWEAK Display placeholder in search screen if posters are not or could not be loaded.
* NOTICE Latest translations from crowdin.

Version 29 *(2016-03-11)*
-----------------------------

* Combined search and "Add show" screen.
* Improved layout in comments and movie details on large tablets.
* Display episode number in notifications and DashClock extension.
* List widget updates immediately when marking watched, in-app settings affecting it change.
* SeriesGuide Cloud support on Amazon version if Google Play Services are available.

#### 29.0.4 *(2016-03-18)*

* FIX Database error message crashes the app.

#### 29.0.3 *(2016-03-16)*

* FIX Do not crash on some common database problems and display a message instead.
* FIX Crash when starting purchase of subscription if inventory has not been checked.
* FIX Widget providers crash if the received context or intent are null.

#### 29.0.2 *(2016-03-12)*

* FIX Season and episode number are flipped in DashClock extension.

#### 29.0.1 *(2016-03-11)*

* FIX Use correct SKU to validate Amazon subscription purchases.
* FIX Crash when trying to purchase with Google Play, but billing service is not connected any longer.
* FIX Do not leak activity with connectivity manager (androidutils update).

#### 29 *(2016-03-10)*

* FEATURE Enable SeriesGuide Cloud on Amazon version if the device has Google Play Services.
* FIX Price of subscription not displayed on Amazon version.
* NOTICE Latest translations from crowdin.

#### 29-beta3 *(2016-03-03)*

* TWEAK Move appearance settings to root. Move links in about section to Advanced, link directly to About page.
* TWEAK Use HTTPS for SeriesGuide website links, including the help page.
* FIX Movie card layout padding is broken in watchlist and collection.
* FIX Potential improvements to list widget not updating.
* NOTICE Latest translations from crowdin.

#### 29-beta2
*(2016-02-18)*

* TWEAK Update list widget immediately when setting episodes watched.
* TWEAK Display episode number in notifications and DashClock extension.
* FIX Crash due to incorrect Lithuanian translation.
* NOTICE Latest translations from crowdin.

#### 29-beta1
*(2016-02-11)*

* TWEAK Combined search and "Add show" screen.
* TWEAK Wider layout for movie details on 10-inch devices in landscape.
* TWEAK Wide layout for comments on tablets in landscape.
* TWEAK Show the number of replies to a comment.
* FIX Make crashes in Movies section less likely when rotating the screen multiple times.
* NOTICE Translations re-imported to improve support for plural strings. Some strings need to be translated again.

Version 28 *(2016-02-04)*
-----------------------------

* Setting to show absolute ("Oct 31") instead of relative ("in 3 days") time for episodes.
* Dark theme for widget.
* Improvements and fixes.

#### 28 *(2016-02-04)*

* TWEAK Show 3 columns of show cards on Nexus 9.
* NOTICE Latest translations from crowdin.

#### 28-beta3 *(2016-01-29)*

* TWEAK Remember last selected language in "Add show" screen.
* TWEAK Update to okhttp 3 for network connections.
* FIX Signing in with trakt results in crash.
* NOTICE Latest translations from crowdin.

#### 28-beta2 *(2016-01-23)*

* FIX Signing in with trakt results in loops or incorrect cross-site request forgery warning.
* FIX Changing exact date setting does not affect widgets.
* FIX Changing some settings do not affect list widget immediately.
* NOTICE Latest translations from crowdin.

#### 28-beta1 *(2016-01-14)*

* FEATURE Add setting to show absolute (e.g. "Oct 31") instead of relative ("in 3 days") timestamps
  for episodes.
* TWEAK Add "Android Dark" theme to widget, which has a transparent header, similar to the old
  widget design.
* TWEAK `ExtensionsConfigurationActivity` is now exported so extensions can send the user to it.
  Thanks @tasomaniac-taso!
* FIX Prevent crashes in overview, search, certain network and time zone configurations.
* NOTICE Latest translations from crowdin.

Version 27 *(2015-12-21)*
-----------------------------

* trakt login through default browser.
* Improved accessibility.
* More material list widget design.

#### 27.0.3 *(2016-01-24)*

* FIX Signing in with trakt results in crash.

#### 27.0.2 *(2016-01-23)*

* FIX Signing in with trakt results in loops or incorrect cross-site request forgery warning.

#### 27.0.1 *(2016-01-15)*

* FIX Prevent crashes in overview, search, certain network and time zone configurations.
* FIX Improve show and movie card sizes with large fonts.
* NOTICE Latest translations from crowdin.

#### 27 *(2015-12-21)*

* TWEAK Updated list widget design to feel more material.
* NOTICE Latest translations from crowdin.

#### 27-beta4 *(2015-12-17)*

* TWEAK Improve accessibility by adding missing descriptions to buttons and labels, useful titles to screens.
* FIX Could not add show existing on SeriesGuide Cloud in some circumstances.
* FIX trakt login was broken on Android 4.4 and below.
* NOTICE Latest translations from crowdin.

#### 27-beta3 *(2015-12-10)*

* TWEAK Show message if no show or episode description is available.
* TWEAK Show re-try buttons if there are no search results or searching for shows/movies failed.
* NOTICE Latest translations from crowdin.

#### 27-beta2 *(2015-11-27)*

* TWEAK Connect trakt: show integrated browser option also if no auth codes are returned.
* TWEAK Hide check-in buttons when connected to SeriesGuide Cloud. If connected to trakt as well,
  watched items would not sync down. Marking watched manually would create duplicate history entries.
* FIX On some occasions the nav drawer has an incorrect item selected.
* FIX Did not show last watched episode for trakt friends if their username had a space character.
* NOTICE Latest translations from crowdin.

#### 27-beta1 *(2015-11-19)*

* TWEAK When connecting to trakt the default browser is used, which likely already has your credentials.
  The integrated browser is still available as a fall back.
* TWEAK Sync show languages with SeriesGuide Cloud.
* TWEAK If the app bar scrolls out of view, it will now snap to an appropriate position.
* NOTICE Latest translations from crowdin.

Version 26 *(2015-11-06)*
-----------------------------

* Add shows that can not be tracked with trakt (also no ratings, comments), though they will have limited release information.
* Select the language when adding a show through search, change the preferred language of a show after adding it.
* Add a trakt "Collection" tab to the "Add show" screen ("Library" tab is now called "Watched").
* "Favorites" widget type is now "Shows", adds favorites filter and sort order (by latest episode or title).
* "Quick check in" setting.

#### 26.0.1 *(2015-11-12)*

* FIX Language update triggering each time opening show details.
* FIX Crash when updating shows on some devices.
* NOTICE Latest translations from crowdin.

#### 26 *(2015-11-06)*

* TWEAK Clear notification when checking in from it.
* NOTICE Latest translations from crowdin.

#### 26-beta4 *(2015-10-29)*

* TWEAK Change "Favorites" widget type to "Shows", add setting to filter on favorites instead, add
  sort order setting (by latest episode or title).
* TWEAK Show fast scrollbar again in movies watchlist and collection tabs if displaying many items.
* FIX Crash when trying to change show language (should now really be fixed).
* FIX Crash when SeriesGuide Cloud account resolution fails while syncing.
* NOTICE Latest translations from crowdin.

#### 26-beta3 *(2015-10-22)*

* TWEAK Allow to track shows locally if they are not (yet) available on trakt. Show better error
  messages when trying to rate, comment or check-in on a show that is not available on trakt.
* FIX Search history now correctly saves latest search terms, displays them in order of last use.
* FIX Crash when trying to change show language.
* NOTICE Latest translations from crowdin.

#### 26-beta2 *(2015-10-16)*

* NOTICE It is now again possible to add shows that are only on TheTVDB and not on trakt (e.g. if
  they do or can not have an English translation), though they currently display no detailed
  release information (e.g. week day, time or country).
* TWEAK Add language selection when adding a show through search.
* TWEAK Allow to change the preferred content language of a show.
* FIX Resolved some bugs, crashes.
* NOTICE Latest translations from crowdin.

#### 26-beta1 *(2015-10-07)*

* TWEAK Add trakt "Collection" tab in "Add show" screen. Rename "Library" to "Watched".
* FEATURE Add "Quick check in" setting so the check in dialog will not ask for a check in message.
* TWEAK Trakt links for shows and episodes now always open the item directly instead of showing search results.
* TWEAK Show shortcuts now have nicer icons (thanks to a code contribution from @adneal).
* NOTICE Latest translations from crowdin.

Version 25 *(2015-10-01)*
-----------------------------

* Support Android 6.0 (Marshmallow), will now ask for permissions.
* On Android 4.4 (KitKat) and up, you can now select custom (auto) backup files. These may be
  on external storage, Google Drive, etc. (provider needs to support Storage Access Framework).
* On Android 5.0 (Lollipop) and up, the app bar hides in some screens when scrolling.
* Restructured settings, open help page using Chrome custom tabs, other improvements.

#### 25.0.2 *(2015-10-11)*

* FIX Add show button could show in "Now" tab.
* FIX Some crashes, bugs.

#### 25.0.1 *(2015-10-02)*

* FIX Error messages in Now tabs not always visible.
* NOTICE Latest translations from crowdin.

#### 25 *(2015-10-01)*

* TWEAK Allow to select between importing shows, lists and movies.
* TWEAK Show help and feedback page using Chrome custom tabs, if available. More prominent feedback action.
* FIX Crash when choosing backup files and the system is configured not to allow it.
* FIX Don't fail import when using default folders and a file is missing, but at least one other exists.
* NOTICE Latest translations from crowdin.

#### 25-beta4 *(2015-09-25)*

* TWEAK On Android KitKat (4.4) and up, you can now select custom (auto) backup files. These may be
  on external storage, Google Drive, etc (provider needs to support Storage Access Framework).
* FIX The sync indicator has moved into the app bar and is now circular and less distracting.
* NOTICE Latest translations from crowdin.

#### 25-beta3 *(2015-09-18)*

* TWEAK Hide app bar when scrolling on Android 5.0 and up in Shows, Lists, Movies, Add show screens.
* TWEAK Hide app bar when scrolling in episode details view on phones.
* TWEAK Always display add show button, only hide it when moving between tabs.
* TWEAK Move "Upcoming range" setting to Shows tab filter menu.
* TWEAK Rename "Content language" setting to "Preferred content language" as a fallback
  to English is always possible.
* TWEAK Use switch preference in settings where appropriate.
* NOTICE Latest translations from crowdin.

#### 25-beta2 *(2015-09-12)*

* TWEAK Support Android 6.0, including new permissions.
* TWEAK Restructured settings, added important ones to the main screen.
* NOTICE Latest translations from crowdin.

#### 25-beta1 *(2015-08-26)*

* TWEAK Full height nav drawer on Android 5 Lollipop and up.
* FIX trakt friend activity was not displayed if one friend had a username with a period in it.

Version 24 *(2015-08-21)*
-----------------------------

* Now tab for movies. Displays your recently watched movies on trakt, as well as the last watched
  movie of your trakt friends.
* Do not load images over metered connections if "Images only over Wi-Fi" is enabled.

#### 24 *(2015-08-21)*

* TWEAK More readable color for default theme navigation items.
* TWEAK Get rid of fast scrollers in most places.
* NOTICE Latest translations from crowdin.

#### 24-beta4 *(2015-08-15)*

* FEATURE Now tab for movies. Displays your recently watched movies on trakt, as well as the last
  watched movie of your trakt friends.
* FIX Now tab correctly shows content while refreshing.
* NOTICE Reduction of app size by removing more unused code from libraries. Let me know if anything breaks!
* NOTICE Latest translations from crowdin.

#### 24-beta3 *(2015-07-05)*

* TWEAK Do not load images over metered connections if "Images only over Wi-Fi" is enabled. Update
  setting description.
* FIX Do only search once when pressing enter in show/movie search.
* FIX Fixes from release 23.1.1.
* NOTICE Updated SeriesGuide Cloud connector libraries. Let me know of any issues.
* NOTICE Updated Extension API: now sending absolute episode number.
* NOTICE Latest translations from crowdin.

#### 24-beta2 *(2015-05-11)*

* TWEAK SG pushes the release time by a day if a show releases between 12AM and 1AM now only or US
  shows. E.g. US late-night shows are typically listed in TheTVDB like "airing Monday night, 12:30AM",
  which is actually Tuesday when looking at the date. This is not common for e.g. Japanese anime,
  hence the limiting to US shows.
* TWEAK Display TMDb rating vote count for movies.
* FIX Do not fail merging shows with SeriesGuide Cloud if a show does not exist any longer on TheTVDB.
  This would cause a full show merge running repeatedly.
* NOTICE Latest translations from crowdin.

#### 24-beta1 *(2015-04-27)*

* TWEAK Better material design on older versions of Android, especially in dialogs.
* TWEAK Nav drawer highlight now colors text and icon.
* NOTICE Latest translations from crowdin.

Version 23.1 *(2015-05-14)*
-------------------------

* Improved material design on older versions of Android.
* Bugfixes (crash when opening Now tab; midnight offset now only applies to US shows).
* Display TMDb rating vote count for movies.
* Note: SG pushes the release time by a day if a show releases between 12AM and 1AM now only or US
  shows. E.g. US late-night shows are typically listed in TheTVDB like "airing Monday night, 12:30AM",
  which is actually Tuesday when looking at the date. This is not common for e.g. Japanese anime,
  hence this is now limited to US shows.

#### 23.1.1 *(2015-05-23)*

* FIX Crash in Now tab on certain devices (on app start).
* FIX Crash when importing shows without status.
* TWEAK Allow importing shows without a title.
* NOTICE Latest translations from crowdin.

Version 23 *(2015-04-14)*
-------------------------

* List tabs can be reordered (see Menu > Reorder lists). Order is preserved on backup/restore.
* If connected to trakt, any movie can be set watched and any watched movie can be rated.
* In search, show cards now have all expected menu options (e.g. manage lists, remove).
* trakt search replaces TheTVDB search in "Add show". Please report any issues, or if the results are better or worse.

#### 23.0.1 *(2015-04-18)*

* FIX Crash when using accessibility tools (like TalkBack) in shows section.
* NOTICE Latest translations from crowdin.

#### 23.0.0 *(2015-04-14)*

* NOTICE Latest translations from crowdin.

#### 23-beta5 *(2015-04-09)*

* TWEAK Use trakt for search in "Add show". Please report any issues, or if the results are better or worse.

#### 23-beta4 *(2015-04-09)*

* TWEAK If connected to trakt, display and allow changing watched state of any movie. Requires trakt
  sync to run at least once after updating so watched movies are downloaded.
* TWEAK Display sync progress bar in Lists and Movies as well.
* TWEAK Add option to hide "Released today" from Now tab.
* TWEAK Add "Open in browser" action to help.
* TWEAK Use different ellipsis algorithm for single line text, displaying as many characters as possible.
* TWEAK Show scrollbar in Now tab.
* NOTICE Latest translations from crowdin.

#### 23-beta3 *(2015-04-02)*

* TWEAK Add skip options to episodes list menus.
* FIX Buttons on Android 4.3 and below do not have enough padding.
* FIX Use white instead of dark underline for transparent tabs for Android Dark theme.
* FIX Improve padding for fast scrollbars and margin for add show button on Android 5.1.
* NOTICE Latest translations from crowdin.

#### 23-beta2 *(2015-03-21)*

* TWEAK If a trakt comment fails due to breaking the new commenting rules, show an error message.
* TWEAK If a trakt check-in fails due to the item not found on trakt, show an error message.
* TWEAK Add "Update" action to search card menu.
* FIX Use latest Amazon In-App billing, fixes crashes on Lollipop (Amazon version only).
* FIX Toolbar icons use wrong color on Android KitKat and below in some circumstances.
* FIX Using the "Add all" button in "Add show" now correctly marks all shows as added.
* NOTICE Latest translations from crowdin.

#### 23-beta1 *(2015-03-11)*

* FEATURE List tabs can be reordered (see Menu > Reorder lists). Order is preserved on backup/restore.
* TWEAK Toggle favorite state of shows in lists.
* TWEAK Add menu to show search result cards (Toggle favorite and hidden state, manage lists, remove).
* TWEAK Slightly faster export and auto backup due to removal of indents in exported JSON files.
* TWEAK Show higher resolution background on Android Wear notifications.
* FIX Show status is now correctly not displayed if it is unknown (= not set on TheTVDB).
* FIX Shows tab next episodes update correctly after setting watched episodes in Upcoming/Recent.

Version 22 *(2015-03-02)*
-------------------------

* New "Now" tab displays episodes released today, recently watched (device only or trakt history)
  and watched by your trakt friends.
* Further updates towards material design.
* When setting a season or show watched, only already released episodes will be set watched.

#### 22.0.2 *(2015-03-09)*

* FIX When using manual time offset, episodes now correctly show up in "Released today".
* FIX Potential fix for crash in "Now" tab.
* FIX Abort loading friends early.

#### 22.0.1 *(2015-03-02)*

* FIX "Now" tab would show multiple "Recently watched" headers for local history.
* FIX Update scroll bars, text fields and progress bars on Android 4.4 and below with new accent color.
* TWEAK Hide add-show-button when scrolling show list.
* NOTICE Latest translations from crowdin.

#### 22.0.0 *(2015-02-27)*

* TWEAK Move "Add show" action to a floating action button.
* TWEAK Remove "events and reminders" category from SeriesGuide episode notifications. To continue
  receiving episode notifications while Priority mode is active, make SeriesGuide a priority app.
  Only on Android Lollipop and up.
* TWEAK Enable Samsung multi-window. However, layouts are not optimized for multi-window.
* TWEAK Some minor design updates.
* NOTICE Latest translations from crowdin.

#### 22.0.0-beta5 *(2015-02-24)*

* TWEAK Use new CardView to replace old card drawables. Other design updates and fixes.
* TWEAK Experimental change of the accent color to teal.
* TWEAK Now tab has better error messages and feedback about loading data.
* TWEAK Add "Web search" action to movie details menu.
* FIX Now tab would not respect manual time offset.
* NOTICE Latest translations from crowdin.

#### 22.0.0-beta4 *(2015-02-17)*

* TWEAK On Now tab, added "More history" shortcut in "Recently watched" section. Removed history tab.
* TWEAK Support trakt refresh tokens. trakt sign-ins are now permanent, unless you disconnect or
  revoke SeriesGuide's permission on the trakt website.
* NOTICE If you are connected to trakt, you will have to sign in again once at the latest in 90 days.
* TWEAK Display search icon in shows section if there is room.
* FIX Would not offer to add show from "Recently watched" if connected to trakt.
* NOTICE Latest translations from crowdin.

#### 22.0.0-beta3 *(2015-02-12)*

* TWEAK Recently watched in "Now" is replaced with trakt episode history if connected to trakt.
* TWEAK The "Add show" screen now displays already added shows, but with an added indicator.
  Add dialog also prevents adding shows already in the local database.
* TWEAK When setting a season or show watched, only already released episodes will be set watched.
* TWEAK When setting a show watched or collected, will now exclude specials. This lines the behavior
  up with the indicators.
* FIX When setting a season or show watched/not watched, will now only upload changed episodes to
  trakt. This prevents new watch dates from being created on trakt.
* FIX When adding a show from the add dialog, will now correctly display show title in status toast.
* NOTICE Latest translations from crowdin.

#### 22.0.0-beta2 *(2015-02-05)*

* FEATURE Experimental "Now" tab in shows section. Displays recently watched episodes,
  episodes released today and recent episodes of trakt friends. Note: Recently watched are currently
  local only, even if connected to trakt; friends activity loads any last episode, even if it
  was watched a long time ago.
* TWEAK After adding show from add dialog, add button will disappear in show lists. Thanks @migueljteixeira!
* TWEAK Switch to new dedicated trakt v2 API endpoint.
* TWEAK Update string related to hiding shows for clarity.
* FIX When adding a show and connected to trakt, would add show even if loading watched or collected
  episodes failed. Now fails with trakt error message.
* NOTICE Changed Android Beam NDEF mime type. Beaming shows from their overview will only work
  against this or a later version.
* NOTICE Latest translations from crowdin.

#### 22.0.0-beta1 *(2015-01-28)*

* FEATURE Sort lists by title or next episode. When sorted by next episode, seasons are treated
  like shows (e.g. next episode of show determines order).
* TWEAK Sort movies without release date as newest.
* TWEAK Backup/Restore tool includes user ratings. Restore sets sensible defaults, skips broken data.
* TWEAK Movie trakt links now immediately redirect to movie (could show multiple results before).
* TWEAK Allow removing a show even if parts of it are added to lists. List items will disappear
  until show is added again.
* FIX "Manage Lists" dialog does not remember user choice when scrolling list item out of view.
* FIX Movies could be rated even after removing them from watchlist and collection.
* FIX Crash in "Unlock features".
* NOTICE Latest translations from crowdin.

Version 21.1 *(2015-01-19)*
---------------------------

* Improved error messages when interacting with trakt.
* Combined search and trending tab in "Add show" screen.

#### 21.1.3 *(2015-02-05)*

* TWEAK Switch to new dedicated trakt v2 API endpoint.

#### 21.1.2 *(2015-01-21)*

* TWEAK Always open help page in-app.
* FIX Crash when displaying results in "Add show" tabs and other locations.
* FIX Crash when trying to launch extension action that was removed or is not currently available.
* FIX Crash when trying to modify sync settings, but sync account is not available.
* TWEAK Partially localized error message when in-app billing actions fail.
* FIX Crash when in-app billing actions fail.
* FIX Text-to-speech service leak in WebViews (trakt auth and help page) on Android 4.2.2.
* FIX Crash when Android settings could not be found when trying to display system app settings.
* TWEAK Button text less likely to get cut off in add dialog.

#### 21.1.1 *(2015-01-19)*

* FIX Crash when trakt credentials are invalid and trying to load user history.
* FIX Crash when timezone is not recognized.

#### 21.1.0 *(2015-01-19)*

* TWEAK Better error messages when connecting trakt.
* TWEAK Smaller sync status progress bar.

#### 21.1.0-beta5 *(2015-01-16)*

* TWEAK When rating, wait for sending to trakt to complete before changing local rating.
* FIX Rare crash in show overview and history tab when trying to display data, but user is moving away.
* NOTICE Latest translations from crowdin.

#### 21.1.0-beta4 *(2015-01-14)*

* TWEAK If connected to trakt or Cloud and changing movie watchlist/collection state, will only change local state if sending was successful.
* NOTICE Latest translations from crowdin.

#### 21.1.0-beta3 *(2015-01-13)*

* TWEAK Re-enable watched button for movies in either watchlist or collection, if connected to trakt.

#### 21.1.0-beta2 *(2015-01-12)*

* FIX Do not try to send watched/collected episodes to trakt if not connected.

#### 21.1.0-beta1 *(2015-01-12)*

* TWEAK If connected to trakt or Cloud and setting episodes watched or collected, will wait until network operation has completed before changing local state.
* TWEAK Combine trending shows and search tab. Display more trending shows.
* TWEAK Better error messages if loading data from trakt fails.
* FIX Skipped episodes were removed if syncing an unwatched/uncollected local show with trakt.
* NOTICE Latest translations from crowdin.

Version 21 *(2015-01-02)*
-------------------------

* Support for the new trakt (v2). You will have to sign in and re-approve SeriesGuide.

#### 21.0.4 *(2015-01-05)*

* FIX Crash when leaving episode details view while it reloads.
* FIX Crash if notification intent is null.
* FIX Crash if content provider id is unknown at runtime.
* FIX Disabled Javascript on the trakt sign-in page blocked some Google accounts from signing in.

#### 21.0.3 *(2015-01-05)*

* FIX Crash in some circumstances when loading data from trakt fails.

#### 21.0.2 *(2015-01-04)*

* TWEAK Better error reports.
* NOTICE Latest translations from crowdin.

#### 21.0.1 *(2015-01-03)*

* FIX Crash when loading movie, but trakt credentials are invalid.
* FIX trakt links open the development site.
* FIX Crash when upgrading database is interrupted.
* TWEAK Search TheTVDB in all languages if there are no results using the user preferred language.
* NOTICE Latest translations from crowdin.

#### 21.0.0 *(2015-01-02)*

* FEATURE Support for trakt v2.
* TWEAK Improved trakt sync, now ratings are available offline as well.
* NOTICE Friend streams and movie watched toggles are gone for now.
* NOTICE Release time and country support improved. E.g. for non-US users, US shows are now all on east coast time.
* TWEAK Small design and layout fixes in some places.
* FIX Movie could show wrong poster, if it did not have one.
* FIX List widget layout is broken on RTL layouts.
* NOTICE tvtag has closed down and was removed.
* NOTICE Latest translations from crowdin.

Version 20.1 *(2014-12-08)*
-----------------------------

* Fix crash on launch for certain Samsung and Wiko devices running Android 4.2.2.

Version 20 *(2014-12-02)*
-----------------------------

* Further design updates towards material design.
* Favorite shows filter for list widget.
* Collection indicator in Upcoming/Recent.

#### 20.0.1 *(2014-12-03)*

* FIX Dividers in nav drawer are clickable.

#### 20 *(2014-12-02)*

* TWEAK Show connected SeriesGuide Cloud or trakt account in nav drawer.
* TWEAK Show ripples when touching list widget items.

#### 20-beta3 *(2014-12-01)*

* TWEAK Do not focus on movie search bar by default.
* FIX Extension configure button text cut off.
* FIX Duplicate list selector in search results. Improved other selectors as well.
* NOTICE Latest translations from crowdin.

#### 20-beta2 *(2014-11-21)*

* TWEAK Add "Only favorite shows" setting to list widget. Setting is now independent from Upcoming/Recent filter settings.
* TWEAK Move season watched/collected all/none options to inline icons.
* TWEAK Display collected indicator in Upcoming/Recent. (You can still touch and hold to add/remove from collection.)
* TWEAK Update app and notification icon.
* FIX Light theme transparent tab text color is now black again.

#### 20-beta1 *(2014-11-17)*

* FEATURE Integrate with Google Now, say "Search for Newsroom on SeriesGuide".
* TWEAK Combine tabs with action bar, add shadow on Android Lollipop.
* TWEAK Text edit bar overlays action bar instead of pushing it down.
* FIX Can touch tabs through open nav drawer.
* FIX Better action bar height on phones in landscape.
* NOTICE Latest translations from crowdin.


Version 19 *(2014-10-30)*
-----------------------------

* Further design updates towards material design.
* Android 5.0 support (notification color, settings, material theme).
* List widget light theme.

#### 19.0.2 *(2014-11-03)*

* FIX Crash when creating menus in some situations.
* FIX Potential fix for crash when displaying upgrade toast on L preview.

#### 19.0.1 *(2014-11-01)*

* TWEAK Add toolbar to Amazon feature unlock screen.

#### 19 *(2014-10-30)*

* NOTICE Latest translations from crowdin.

#### 19-beta3 *(2014-10-28)*

* TWEAK Go directly to search from overview action.
* TWEAK Set notification color, category (event) and link to settings (Android Lollipop and up).
* TWEAK Increase drawer width, add larger top space for future use.
* FIX Update DashClock settings to new theme.
* FIX Some devices crash if action items are customized.
* FIX Restore progress bars for TheTVDB and TMDB search.
* FIX Show progress indicator when initially loading trakt streams and comments.
* FIX Crash when trying to swipe to refresh.
* FIX Button text getting cut off in dialogs.
* FIX Check-in and comment button enabled, even though no action possible.

#### 19-beta2 *(2014-10-25)*

* TWEAK More Material design updates (icons, colors, action bar).
* FIX Potential fix for list widget crash (mostly on Samsung GT I9500).
* NOTICE Latest translations from crowdin.

#### 19-beta1 *(2014-10-08)*

* FEATURE Light theme for list widget.
* TWEAK Display "Unlock all features" link in nav drawer if user is no supporter. Update strings related to unlocking all features.
* FIX Create calendar events with new method available since Android ICS. Enables creating events in Sunrise calendar.
* NOTICE Latest translations from crowdin.

Version 18 *(2014-09-26)*
-----------------------------

* Display more details like release time, network and genre in the "Add show" show info dialog.
* Better support for "Large text" accessibility option.
* Share statistics.
* Amazon App Store version.

#### 18.1.0 *(2014-10-09) Amazon only*

* TWEAK Make subscription optional, like Google version.
* FEATURE Light theme for list widget.
* TWEAK Display "Unlock all features" link in nav drawer if user is no supporter. Update strings related to unlocking all features.
* FIX Create calendar events with new method available since Android ICS. Enables creating events in Sunrise calendar.
* NOTICE Dropped SeriesGuide X migration tool.

#### 18.0.3 *(2014-10-02)*

* FIX Movie watched state not changing for movies in watchlist or collection.
* TWEAK Better action button descriptions.
* NOTICE trakt ratings, comments and movie watched state are currently incorrectly displayed due to issues at trakt. They are, however, correctly sent to trakt.

#### 18.0.2 *(2014-09-28) Amazon only*

* FIX Remove all Google Play search links from Amazon version.

#### 18.0.1 *(2014-09-26)*

* FIX Use lighter text color in search box when using Light theme.

#### 18 *(2014-09-26)*

* TWEAK Remove manually marked watched episodes and movies from "Friends" stream to improve load time. In return extend episode friends stream to 7 days.
* TWEAK "Collected all" indicator now only takes into account released episodes.
* TWEAK Add search button in lists section.
* NOTE Provide a version for the Amazon App Store.
* NOTICE Latest translations from crowdin.

#### 18-beta3 *(2014-09-15)*

* TWEAK Display additional details in show add dialog.
* TWEAK Display genres for movies.
* TWEAK Simply display year of original release for shows instead of exact date.

#### 18-beta2 *(2014-09-10)*

* FEATURE Remember past search queries for TheTVDB and TMDB search.
* FIX Interface handles "Large text" accessibility option better (no overlapping or cut off text).
* FIX Always show cheat sheet for touch and hold on inline actions (like action bar buttons).
* NOTICE Latest translations from crowdin.

#### 18-beta1 *(2014-09-01)*

* FEATURE Add shows tab to search. Results update as you type.
* FEATURE Share your statistics: export stats screen as text for email, IM, etc.
* TWEAK Update season and episode list designs.
* FIX trakt "Friends" and "You" tabs appear immediately after signing in, previously they would only appear on restarting the main activity.
* NOTICE Latest translations from crowdin.

Version 17 *(2014-08-27)*
-----------------------------

* Design updates, moving towards Material Design language.
* Access trakt and TMDb over a secure connection (HTTPS).

#### 17.0.1 *(2014-09-01)*

* TWEAK Cut down on excessive log reports.
* FIX Potential fix for very rare crash in show overview.
* NOTICE Latest translations from crowdin.

#### 17 *(2014-08-27)*

* TWEAK Restore larger nav drawer items on tablets. Also increase people tile size.

#### 17-beta5 *(2014-08-25)*

* FIX Crash due to Android L developer preview bug. Temporarily repackage okio and okhttp dependencies. See https://github.com/square/okhttp/issues/967.

#### 17-beta4 *(2014-08-24)*

* TWEAK Pressing back when third-party app linked to show or episode returns to third-party app (see https://github.com/UweTrottmann/SeriesGuide/wiki/Extension-API).
* TWEAK Accept stale images when offline. This should improve images displaying if offline for longer periods of time.
* FIX Potential fix for image cache getting corrupted. Also, images are now exclusively cached on internal storage (avoids external storage format issues).
* FIX Small UI fixes.
* NOTICE Latest translations from crowdin.

#### 17-beta3 *(2014-08-15)*

* TWEAK Remove show dialog opens without delay.
* FIX Crash when authenticating with tvtag.
* FIX Discard old list widget data only after query for new data has finished.

#### 17-beta2 *(2014-08-13)*

* TWEAK Use platform activity animations to better fit in with apps for that version of Android.
* TWEAK Large episode actions in overview, larger touch area for menu buttons on shows and movies, star button on shows.
* TWEAK Display interim results when calculating total time of watched episodes for many shows.
* TWEAK Download movie posters over HTTPS, use HTTPS when talking to trakt.
* TWEAK Cache TVDb and TMDb requests (e.g. search results, biography, movie details).
* NOTICE Latest translations from crowdin.

#### 17-beta1 *(2014-08-06)*

* TWEAK Design updates, moving towards Material Design language.
* TWEAK Show actions are embedded for easier access.

Version 16 *(2014-07-28)*
-----------------------------

* Cast and crew info with photos and biography for shows and movies.
* Added some movie statistics.
* trakt activity streams display items manually marked as watched.
* Fixed notification text color on Android Wear devices.
* Improved image loading, using Android designated cache folders.
* Dropped dedicated check-in screen.
* SeriesGuide Cloud experiment: save and sync shows and movies. (If connected to trakt at the same time, will only sync with SeriesGuide Cloud. Check-Ins, marking watched/collected still sent to trakt.)

#### 16.0.1 *(2014-07-31)*

* FIX Crash when loading movie poster, but no external storage is available.
* NOTICE Latest translations from crowdin.

#### 16 *(2014-07-28)*

* FIX List widget poster placeholder icon cut off.
* NOTICE Latest translations from crowdin.

#### 16-beta8 *(2014-07-24)*

* FIX Crash when loading images.

#### 16-beta7 *(2014-07-24)*

* FEATURE SeriesGuide Cloud v2 with support for episode and movie backup and sync.
* TWEAK Display episodes or movies marked as seen in trakt activity streams.
* TWEAK Trakt connect screens wrap content better on tablet displays.
* FIX Guest star names sometimes contain leading or trailing spaces.
* NOTICE The original SeriesGuide Cloud experiment is now deprecated (and will be disabled soon). You will have to sign in again to connect to Cloud v2 on each device.
* NOTICE When connecting to SeriesGuide Cloud, activity on trakt will no longer be downloaded to SeriesGuide.

#### 16-beta6 *(2014-07-04)*

* FEATURE Person details view, currently with bio, TMDb and web search link. Try in landscape!
* FEATURE Support shows released in Canada. See https://github.com/UweTrottmann/SeriesGuide/issues/382 for details.
* TWEAK Ensure share button is always on the right.
* TWEAK Clean up action bar on many screens by moving infrequent actions to the overflow menu.
* TWEAK Drop the dedicated check in screen.
* TWEAK New transition animations, making more clear where content came from and where it is going in the visual stack.
* FIX Don't display error message while loading cast or crew members.
* FIX Could not swipe to refresh if trakt check-in stream was empty.

#### 16-beta5 *(2014-06-30)*

* FEATURE Display cast and crew with headshots, name and character respectively job.
* TWEAK Increase tab size on large+ screens (tablets).

#### 16-beta4 *(2014-06-27)*

* FIX Episode title in notifications not visible on Android Wear devices.
* FIX Episode list sometimes incorrectly re-selects the first episode.

#### 16-beta3 *(2014-06-25)*

* FEATURE Add some movie statistics.
* FIX Potentially fix crash in episode pages view if number of episodes changes.
* FIX On tablets, going into episode view the shown episode is correctly selected in list. Selection is correctly kept when changing sort order.
* NOTICE Latest translations from crowdin.

#### 16-beta2 *(2014-06-20)*

* TWEAK UI improvements (episode list, rate dialog, light theme immersive theme).
* TWEAK Sort ended after continuing shows when ordering by next episode.
* NOTICE Latest translations from crowdin (some updates for Spanish).

#### 16-beta1 *(2014-06-18)*

* TWEAK Better way of loading show posters and episode images, also now using designated cache folders.
* NOTICE Dropped now deprecated poster loading tool, clear image cache setting now links to app info where you can 'Clear Cache'.
* NOTICE Latest translations from crowdin.

Version 15 *(2014-06-03)*
-----------------------------

* UI improvements and bug fixes.
* Supports Android 4.0.3 (Ice Cream Sandwich) and up. If you are running Android 3.x you will only receive important bug fixes from now on.

#### 15.0.6 *(2014-06-16)*

* FIX Creating a backup, adding or removing shows takes a long time when there are many shows added to SG.

#### 15.0.5 *(2014-06-12)*

* FIX Crash when calculating next episode.

#### 15.0.4 *(2014-06-11)*

* FIX Jank when scrolling the show list.
* FIX Crash when retrieving show poster from cache.

#### 15.0.3 *(2014-06-09)*

* FIX Movies removed from watchlist or collection are added again when syncing with trakt.

#### 15.0.2 *(2014-06-06)*

* FIX Crash when upgrading database.
* FIX Crash when creating SeriesGuide account.
* FIX Crash when decoding downloaded image consumes too much memory.

#### 15.0.1 *(2014-06-04)*

* FIX Sync progress bar showing briefly when launching even if no actual sync is occuring.

#### 15 *(2014-06-03)*

* TWEAK Design tweaks.
* TWEAK Moved movie search tab to first position.
* FIX Notification settings not disabled when returning after disabling notifications.
* NOTICE Latest translations from crowdin. Added (partial) Catalan.

#### 15-beta1 *(2014-05-23)*

* TWEAK The context menu is dead! Long live the popup menu! Migrated all context menus to popup menus.
* TWEAK Updated action bar design, updated some icons.
* TWEAK Simplified getting started instructions, dropped migration quick link (still available in settings).
* TWEAK Pitch black background for fullscreen poster/image viewer.
* FIX Notification sometimes links to wrong episode.
* FIX In movie search, the popup menu now shows add to/remove from options depending on a movie being added to the watchlist or collection.
* NOTICE Drop ActionBarSherlock. It was nice, while it lasted. Goodbye old friend!
* NOTICE Supports Android 4.0.3 (Ice Cream Sandwich) and up. If you are running Android 3.x you will only receive important bug fixes from now on.

Version 14 *(2014-05-14)*
-----------------------------

* View a stream of your and your friends trakt check-ins for shows and movies.
* Option to sort shows and movies by title while ignoring articles (English only).
* Easier trakt setup: after connecting your shows and movies are automatically merged with your trakt profile.
* Extensions replace watch options for episodes. Added extension for Google Play, YouTube and Web search.

#### 14.0.4 *(2014-05-21)*

* FIX Crash when database upgrade did not succeed.

#### 14.0.3 *(2014-05-15)*

* FIX Actually fix crash when leaving right after starting to connect to trakt.

#### 14.0.2 *(2014-05-15)*

* FIX Crash when leaving right after starting to connect to trakt.
* FIX German translation.
* NOTICE Latest translations from crowdin.

#### 14.0.1 *(2014-05-14)*

* FIX Crash when access to network detection is prevented (e.g. Xposed privacy module).
* NOTICE Latest translations from crowdin (Finish, Portuguese Brazil).

#### 14 *(2014-05-12)*

* FIX Error message not displaying in some cases when failing to add a show.
* FIX Crash when sharing, but no app available to handle a share intent.
* NOTICE Latest translations from crowdin (Arabic, French, Polish).

#### 14-beta5 *(2014-05-09)*

* TWEAK Greatly improved experience after connecting to trakt: watched and collected episodes on the device are now automatically merged with your trakt profile.
* NOTICE Deprecated trakt upload tool. To merge your watched and collected episodes, disconnect, then connect again to trakt.
* NOTICE Latest translations from crowdin (Danish).

#### 14-beta4 *(2014-05-06)*

* FEATURE 'Ignore articles' option for show/movies sort order.
* TWEAK Swipe to refresh in comments.
* TWEAK If relative time (e.g. "in 5 min") is lower than possible resolution, display "now".
* TWEAK Hide manual update and load poster actions in advanced menu.
* NOTICE Latest translations from crowdin.

#### 14-beta3 *(2014-04-30)*

* TWEAK Friends and your trakt activity grouped by day, similar to Upcoming/Recent.
* FIX Fullscreen images are cut-off.
* FIX Crash when returning to Upcoming/Recent and data has changed.

#### 14-beta2 *(2014-04-28)*

* FEATURE trakt activity streams: recently watched shows for your friends and yourself, same for movies. Streams support pull to refresh.
* TWEAK Added extensions for Google Play, YouTube and Web search. These replace the current watch links below episodes.
* NOTICE Latest translations from crowdin.

#### 14-beta1 *(2014-04-24)*

* TWEAK Fullscreen show poster view loads image with highest resolution.
* TWEAK Show list and activity refresh data when returning to app, every 5 minutes when kept open.
* TWEAK Use 'Add to/Remove from collection' for shows and episodes instead of 'Collected/Not yet collected' where appropriate.
* NOTICE Cloud beta setup moved to Settings > Services.

Version 13.1.1 *(2014-04-22)*
-----------------------------

* TWEAK 'Add to collection' option in activity context menus.
* TWEAK Neutral touch feedback on list widget.
* TWEAK Support for shows with 'Netherlands' release times.
* TWEAK When removing a show its episodes are removed immediately from activity stream.
* FIX Incorrect episode flagged watched in activity stream.
* FIX Text drawing issue on Android 4.0 (Ice Cream Sandwich) devices in episode details.
* NOTICE Latest translations from crowdin.

Version 13.1 *(2014-04-17)*
---------------------------

* Extensions API: now all users can add up to two extensions. To add more subscribe or buy the X Pass!

Version 13.0.1 *(2014-04-17)*
-----------------------------

* Bugfixes based on past crash reports.

Version 13 *(2014-04-16)*
-----------------------------

* Extensions API: provide custom quick actions below episodes. Learn how to [build one](https://github.com/UweTrottmann/SeriesGuide/wiki/Extension-API).
* Design refresh.
* Sort movies.
* Supports Android 3.0 (Honeycomb) and up. If you are running Android 2.3 you will only receive important bug fixes from now on.

#### 13-beta7 *(2014-04-11)*

* TWEAK Purple action bar for SeriesGuide Light theme.
* FEATURE Backup tool now also exports/imports movies. They are stored in a separate file called 'sg-movies-export.json'.

#### 13-beta6 *(2014-04-08)*

* TWEAK Do not show fast scroll bar in 'Lists'.
* TWEAK Fresh coat of paint for SeriesGuide (default) and SeriesGuide Light theme. Various other design tweaks.
* TWEAK 'Hide special episodes' setting respected by 'Statistics', added toggle for the setting to 'Statistics' menu.
* NOTICE Latest translations from crowdin.

#### 13-beta5 *(2014-04-02)*

* TWEAK Always show fast scroll bar when appropriate to allow easy jumping inside of lists.
* TWEAK Do not show '<show> was successfully added' toasts anymore. If there was an error adding a show it will still display, but only if the app is in the foreground.
* FIX 'Manage Lists' and 'Share' action were shown in overview if there was no episode.
* NOTICE New [Extensions API](https://github.com/UweTrottmann/SeriesGuide/wiki/Extension-API) release.

#### 13-beta4 *(2014-03-27)*

* TWEAK Add all remaining Amazon domains.
* FIX full trakt sync did not correctly download unwatched and uncollected episodes.
* FIX Some GA issues.
* NOTICE Latest translations from crowdin.

#### 13-beta3 *(2014-03-26)*

* FEATURE SeriesGuide extensions, learn how to [get started](https://github.com/UweTrottmann/SeriesGuide/wiki/Extension-API).
* TWEAK Episode details load faster.
* FIX Scroll bar overlaps episode list too much.
* FIX tvtag auth token marked for refresh a week before expiry (was 24 hours), reducing the chance the user has to manually re-authenticate.
* NOTICE SeriesGuide for Android 2.3 (Gingerbread) only receiving minor updates from now on, e.g. it will not get extension support.
* NOTICE New permission: "Prevent device from sleeping". Read more at [http://seriesgui.de/privacy](http://seriesgui.de/privacy).

#### 13-beta2 *(2014-03-17)*

* TWEAK trakt comments do not auto-refresh anymore, added refresh button instead.
* FIX Some bug fixes and optimizations.

#### 13-beta1 *(2014-03-11)*

* FEATURE Sort movies by title or release date (touch again to reverse sort).
* TWEAK Toned down tab strip highlights, movie button bar.
* TWEAK Auto-complete email accounts when signing up for trakt.
* TWEAK Hidden shows will not display notifications any longer.
* TWEAK Display absolute episode number more prominently in episode detail view, drop duplicate season and episode number.
* FIX Exclude special episodes from determining if all episodes of a show are collected as well (already in effect for watched all marker).

Version 12.4 *(2014-03-27)*
---------------------------

* FIX Full trakt sync not downloading unwatched and uncollected episodes correctly.
* FIX Crash when adding movie.

Version 12.3 *(2014-03-08)*
---------------------------

* Trakt show upload was broken.

Version 12.2 *(2014-03-03)*
---------------------------

* Comments for episodes did wrongly display show comments.

#### 12.2-beta2 *(2014-03-02)*

* FIX Comments for episodes did wrongly display show comments.
* FIX Crashes when doing (bulk) database operations. Recover in more cases.
* NOTICE Updates to Chinese Simplified translation.

#### 12.2-beta1 *(2014-03-01)*

* FIX Crashes when doing (bulk) database operations. This might have performance impacts regarding adding shows, sync time, etc. Please report anything out of the ordinary in that regard.
* NOTICE Updated Romanian translation.

Version 12.1 *(2014-02-27)*
---------------------------

* Bugfixes.
* Updated translations.

Version 12 *(2014-02-26)*
-------------------------

* Improved international show support: release times + notifications might be off until a full sync will run (by opening the app). You may trigger a sync manually by touching Menu in the upper right corner (or by pressing the menu button on your device), then Update.
* Build a movie watchlist and collection.
* Requires at least Android 2.3 (Gingerbread).
* Bugfixes and design updates.

#### 12-beta10 *(2014-02-24)*

* FIX Tabs are crammed together.

#### 12-beta9 *(2014-02-24)*

* TWEAK DashClock extension now prominently displays show title, is formatted similar to DashClock's calendar extension.
* TWEAK 'Manual sync with trakt' is now simply 'Upload existing shows'. The regular (Auto) Update mechanism will take care of syncing watched and collected episodes.
* TWEAK More reliable trakt credentials validation.
* FIX Tab backgrounds not themed.
* FIX No error message if movie details could not be loaded, lonely cast and crew labels.
* FIX Displayed last 'Auto-Backup' time even if there are no actual backup files.
* FIX Various other bug fixes.
* NOTICE Themes are now exclusive to X subscribers/X pass holders.

#### 12-beta8 *(2014-02-19)*

* TWEAK Better crash reports.
* FIX Movie posters and trakt avatars downloaded despite on mobile connection and 'Images via Wi-Fi only' was enabled.

#### 12-beta7 *(2014-02-18)*

* FIX Crash when movie details did not load due to network error, but movie was added to watchlist or collection.

#### 12-beta6 *(2014-02-18)*

* NOTICE This version requires at least Android 2.3 (Gingerbread).
* FEATURE Movie watchlist and collection support. If connected to trakt, syncs watchlist and collection and in addition allows to flag movies watched.
* TWEAK Display movie runtime, cast and crew as well as TMDb and trakt ratings.
* TWEAK Support sharing movie (using secure trakt web link), added TMDb link.
* TWEAK Sharing shows and movies uses a secure trakt web link which creates nice info cards on Google+, Facebook and Twitter.
* TWEAK Moved Settings and Help back to nav drawer, cleaned up some duplicate overflow actions.
* TWEAK Support translucent system bars with Android Dark app theme.
* TWEAK Support release times for Finland (Europe/Helsinki time zone).
* FIX Single episode notification opens wrong episode.
* FIX Show and episode ratings did not update because they were cached for too long.
* FIX Using the back button on the show list sometimes navigates to the show list again.
* FIX Setting a manual time offset breaks episodes moving correctly into 'Recent', notifications.
* FIX When dismissing the check-in dialog coming from a notification quick action a blank screen is shown.
* FIX When changing settings for multiple widgets the configuration dialog for the first configured widget was shown for all widgets.
* FIX Going back from a widget configuration dialog or notification quick-action check in returns to the app instead of the home screen/previous task.
* FIX Syncing on Android 2.3 (Gingerbread) crashes SeriesGuide.
* FIX Flagging an episode as unwatched could lead to unexpected (= much later) next episode.
* NOTICE Contributor branch changed from `beta` to `dev`. Please submit any new pull requests against `dev`. Also updated contributer guidelines.
* NOTICE Switched to a new crash reporting tool. For the privacy policy see http://seriesgui.de/privacy.
* NOTICE Latest translations from crowdin.
* NOTICE Database upgraded to version 32.

#### 12-beta5 *(2014-02-04)*

* NOTICE A full sync is scheduled after upgrading. Details below.
* TWEAK Release times are calculated with a show's country of origin (sourced from trakt) in mind. When upgrading a full sync is triggered. You might see incorrect release times and receive incorrect notifications until the upgrade is complete.
* TWEAK On the info tab of a show 'Release times' will list the country that determines which time zone is used to calculate the episode release times. Currently supported are Australia, Germany, Japan, United States, United Kingdom. United States is used for all others. Please share others you want supported!
* TWEAK Shows airing from 12:00am to 12:59am are now associated with the day before (e.g. Late Night with Jimmy Fallon).
* TWEAK On Android 4.4 (KitKat) and up the episode details screen uses translucent system bars (phones only).
* FIX Daily trakt sync now properly removes watched or collected flags for shows with no watched or collected episode on trakt.
* FIX Crash when connecting to trakt and credentials could not be stored correctly.
* FIX Sync did not update all shows it was told to update.
* NOTICE The JSON import/export tool now exports the 'country' property for shows.
* NOTICE Latest translations from crowdin.

#### 12-beta4 *(2014-01-30)*

* FIX Crash when adding/removing movie from watchlist, shouting.
* NOTICE Latest translations from crowdin.

#### 12-beta3 *(2014-01-29)*

* NOTICE GetGlue is now tvtag.
* TWEAK Check-in dialog waits for trakt and/or tvtag check-in to succeed before vanishing with your message (you can still dismiss anytime).
* TWEAK Auto-Update gets watched and collected episodes from trakt and matches them in SeriesGuide. At most every 24 hours (always if update is triggered manually).
* TWEAK Show show title in show list context menu, season number in seasons context menus.
* NOTICE Latest translations from crowdin.

#### 12-beta2 *(2014-01-21)*

* FIX Mirrored shows and movies grid on some Android 4.1 tablets.

#### 12-beta1 *(2014-01-20)*

* TWEAK Light theme now features a light action bar and nav drawer. Both SeriesGuide Light and Android Dark theme have a more pleasant flat background.
* TWEAK Drop user to home screen after creating show shortcut.
* FIX Crash when rating while using SeriesGuide with ART.
* FIX Incorrect movie posters displaying in movie search.
* NOTICE Reenable access to SeriesGuide cloud.

Version 11.1 *(2014-01-21)*
-------------------------

* FIX Mirrored shows and movies grid on some Android 4.1 tablets.
* FIX Crash when rating episode if SeriesGuide is running on ART.

Version 11 *(2014-01-17)*
-------------------------

* Show activity integrated into shows section. Just swipe to get from your show list to upcoming episodes or friends activity (last one requires connecting to trakt).
* Better offline detection, new 'Images via Wi-Fi only' setting replaces previous 'Update via Wi-Fi only' setting.
* Many small fixes and tweaks (e.g. scroll bars, list widget config).

#### 11-beta5 *(2014-01-13)*

* FIX Lists in shows section overlap with tab strip on Android 2.3 (Gingerbread).
* TWEAK Better explanation of upgrade options, fall back to X Pass link if Billing service unavailable.
* NOTICE Latest translations from crowdin.

#### 11-beta4 *(2014-01-09)*

* TWEAK Store trakt passwords with Android account system, will hopefully lead to less errors. Also, disconnecting from trakt will not force you back to the previous screen anymore.
* TWEAK Rework sync account handling, leading to potentially less crashes.
* FIX List widget does not refresh items immediately after changing its settings. Thanks Tomas for pushing me to fix this.
* NOTICE Latest translations from crowdin, now including Latvian.

#### 11-beta3 *(2014-01-03)*

* TWEAK Require confirmation before toggling watched or collected flag for all seasons.
* TWEAK Show tab displays more info right beside show poster.
* TWEAK Finally move activity tabs into shows section now that filtering is done via an action item (funnel icon).
* TWEAK Other minor layout tweaks.
* FIX Superfluous space character in trakt movie check-in confirmation message.
* FIX Scrollbars overlap content, make item actions hard to press.
* FIX Library tab in 'Add shows' screen does not display show descriptions.

#### 11-beta2 *(2013-12-22)*

* FIX Crash when doing network operations in libssl due to issue with OkHttp (see commit c73265d240bdf168d02e704ecda43fa653068c49).
* FIX Crash when downloading shows from hexagon, but no valid property values exist (all null).

#### 11-beta1 *(2013-12-20)*

* FEATURE SeriesGuide cloud experiment which can backup/sync your shows across devices. Episodes and movies are NOT yet included, use trakt.tv integration for that.
* TWEAK Better offline detection in many places.
* TWEAK New 'Images via Wi-Fi only' setting. By default SeriesGuide uses mobile networks as well. If the new setting is set, SeriesGuide will not download images if only a mobile connection is available keeping the mobile data charges at a minimum. ('Update only via Wi-Fi' setting is removed.)
* TWEAK 'Remove all filters' action in 'Filter shows' menu.
* TWEAK Display shorter movie release date in movie grids.
* FIX Episode not updating in overview after trakt sync flagged it watched/collected (e.g. shortly after a check-in).
* NOTICE Latest translations from crowdin, now including Latvian.

Version 10 *(2013-12-06)*
-------------------------

* Movie search results and movie details are displayed in the content language set in Settings (if available).
* Small design tweaks, icon updates.
* Layout fixes for some Android 4.1 tablets.

#### 10-beta3 *(2013-12-03)*

* TWEAK Also use new card layout in 'Add show' screen.
* TWEAK Use new placeholder image for show list as well, fix placeholder blinking on list update.
* FIX Certain Android 4.1 tablets incorrectly use 4.2 and up RTL layouts.
* NOTICE Latest translations from crowdin.

#### 10-beta2 *(2013-11-26)*

* TWEAK Some design updates (more visually separated items, icon tweaks).

#### 10-beta1 *(2013-11-20)*

* TWEAK trakt credentials are sanitized more rigorously. If you have issues, try disconnecting and re-connecting again.
* TWEAK Movie search results and movie details are displayed in the content language set in Settings, if a translation is available on themoviedb.org.
* TWEAK Use Android 4.4 style settings icon on the list widget, change its layout a little.

Version 9.3 *(2013-11-13)*
--------------------------

Disable encryption when talking to GetGlue. Android can't validate the certificate chain (either certificates were switched or RC 128 encryption is not supported).

Version 9.1 *(2013-11-12)*
--------------------------

Resolved crashes and layout issues. Latest translations.

### Detailed changes:

* TWEAK Highlight currently active navigation drawer item. Only available on Android 3.0 (Honeycomb) and up.
* TWEAK Reorganize settings. Notably extracted notification related settings to their own page. Notify about all shows by default (previously only favorites).
* TWEAK Updated shouts screen to be more in line with the rest of the app, e.g. added action bar.
* TWEAK Removed navigation drawer from some not directly content related screens (e.g. help, tools).
* FIX Screen partially blacks out on certain Nexus devices. Also see #309.
* FIX Crashes when connecting to trakt, showing manage lists dialog.
* FIX Crash in full screen mode on Android 3.x (Honeycomb) devices.
* FIX Slow-down in add activity when trakt was offline, crash when trakt was disconnected.
* FIX In some cases the subscription check crashes if the user leaves the app in the meantime.
* NOTICE Latest translations from crowdin.

Version 9 *(2013-11-08)*
----------------------------

* Skip an episode or an entire season if you don't want to watch right now.
* Android 4.4 (KitKat) compatible.
* Moved 'Migrate from X' action to service settings.
* New backend for GetGlue and trakt.

### Detailed changes:

#### 9-beta5 *(2013-11-06)*

* TWEAK Always crop episode image in episode details, tap to view original.
* TWEAK Use dark action bar and white item selector for Android Dark theme.
* TWEAK Removed duplicate search action from shows menu, is accessible via nav drawer.
* FIX Action items not readable in episode details using white theme on phones.
* FIX Random crash on Android 4.4 (KitKat), when syncing with trakt, when shows got added automatically, when adding shows with lots of episodes.
* FIX Check marks in activity stream only flagging as watched.
* FIX GetGlue id has to be entered multiple times when checking a show in for the first time.
* FIX Search result images are too small on high resolution phones.

#### 9-beta4 *(2013-11-04)*

* FEATURE Skip an episode or an entire season you don't want to watch right now.
* TWEAK Speeded up screen transitions, making the app even faster.
* TWEAK Built and optimized for Android 4.4 (KitKat).
* FIX Various crashes, related to in-app billing, syncing with trakt, connecting to trakt.

#### 9-beta3 *(2013-10-31)*

* TWEAK Moved trakt integration to new backend.

#### 9-beta2 *(2013-10-29)*

* FIX Crash fix for In-App billing.

#### 9-beta1 *(2013-10-28)*

* TWEAK Switch to new GetGlue v3 API. Requires giving permission to SeriesGuide once. Setting a GetGlue id once before checking into a new show.
* TWEAK Move update progress bar below action bar. Use it for poster loading progress as well.
* TWEAK Improve poster and episode image full screen view: two-finger zoom, navigation stays visible.
* TWEAK Change filter icon state if a filter is active. Thanks @adneal!
* TWEAK Moved 'Migrate from X' action to service settings. Grouped SeriesGuide and GetGlue service settings

Version 8.3 *(2013-10-29)*
-----------------

* Crash fix for In-App billing.

Version 8.2 *(2013-10-23)*
-----------------

* Security updates and bug fixes for In-App billing.
* Disable manual creation of sync account.

Version 8.1 *(2013-10-16)*
-----------------

* Restored the one-row widget on Android 4.0 (Ice Cream Sandwich) and below. Added a new compact mode to the list widget on Android 4.1 (Jelly Bean) and up.
* Updates for translations.
* Some minor sync related fixes.

Version 8 *(2013-10-08)*
-----------------

* SeriesGuide X users, time to migrate your shows to the free version! Use the migration assistants, keep X installed to unlock all features in the free version.
* Revamped filter and sort options for shows. Apply sort order twice to reverse it.
* YouTube and web search button for shows and episodes.
* Reduced minimum size of list widget, removed legacy one row widget on Android 3.0 and up.
* Bug and crash fixes.

### Detailed changes:

#### 8 *(2013-10-07)*

* FEATURE Support Hebrew and Japanese TVDb content languages.
* FEATURE 'Web search' button for shows and episodes. Thanks Andrew Neal (@adneal)!
* TWEAK Correct air times for Australian shows using information from trakt.tv.

#### 8-beta2 *(2013-10-04)*

* FEATURE New filter and sort options for shows. Apply sort order twice to reverse it. Removed old categories.
* FIX Some reported crashes.
* NOTICE Latest translations from crowdin.

#### 7.2-beta1 *(2013-10-01)*

* NOTICE SeriesGuide X users, time to migrate your shows to the free version! Keep X installed to unlock all features in the free version.
* FEATURE YouTube search link on show and episode pages. Thanks Andrew Neal (@adneal)!
* FEATURE Migration assistant. Helps export shows and install SeriesGuide in X, import shows within SeriesGuide.
* TWEAK Refined first run info card, now a less distracting dismiss button.
* TWEAK Some layout tweaks for larger screens (7-inch, 10-inch).
* TWEAK Refreshed icons. Search inside show action now shown with any tab in show overview.
* TWEAK List widget can be resized down to about one row in height and three columns in width. Removed legacy one row widget.
* TWEAK Display subscription button right in the top menu for better discovery.
* FIX Day headers on activity screen blacked out.
* FIX Going back from X subscription screen dropped you out of the app.
* FIX Layout animations in season list caused wierd overlaps.
* NOTICE Switched to Gradle build system.
* NOTICE Latest translations from crowdin.

Version 7.1 *(2013-08-10)*
-----------------

* NOTICE Allow removal of SeriesGuide account from Android settings. If you experience random crashes, try removing and then adding the SeriesGuide account again.
* FIX Crash when disconnecting from trakt.
* FIX Crash when checking for in-app purchases (X subscription).
* FIX Only update a show once every 12 hours when accessing its overview page.

Version 7 *(2013-08-08)*
-----------------

* NOTICE Introducing the X Subscription. You can try it for free for 30 days. Users who have purchased the old X Upgrade or install the SeriesGuide X Life-Time Pass side-by-side will get access to the X Subscription for free until SeriesGuide will cease to exist. Please write in if there are problems!
* FEATURE Display show posters and episode images full screen by touching them.
* X FEATURE New widget type: favorite shows.
* X FEATURE Add shortcuts to shows to your home screen from their overview page.
* TWEAK Add Google Play search link to movie detail page.
* TWEAK Check validity of trakt credentials when updating, auto-disconnect if they are invalid (e.g. if you change your password).
* TWEAK Improvements to support restricted profiles in Android 4.3.

#### 7beta3 *(2013-08-07)*

* FEATURE Display show posters and episode images full screen by touching them. Thanks Andrew Neal (@adneal)!
* TWEAK Improvements to support restricted profiles in Android 4.3.
* TWEAK New sliding tab indicators, is now behind the new overlaying nav drawer.
* TWEAK Check validity of trakt credentials when updating, auto-disconnect if they are invalid (e.g. if you change your password).
* TWEAK Add Google Play search link to movie detail page. Thanks Andrew Neal (@adneal)!
* FIX Reenable search within shows from their overview page. Suggestions still show for all shows though :(
* FIX Also correctly remember last notified about episode when touching a notification instead of swiping it away.
* FIX Activity wrongly limits number of episodes after rotating after enabling 'Infinte Activity'.
* NOTICE Updated ActionBarSherlock to 4.4.0.

#### 7beta2 *(2013-07-20)*

* NOTICE The X Upgrade is now a yearly X Subscription. You can try it for free for 30 days. Users who have purchased the old X Upgrade or install SeriesGuide X side-by-side will get access to the X Subscription for free until SeriesGuide will cease to exist. Please write in if there are problems!
* X FEATURE New widget type: display favorite shows.
* X FEATURE Add shortcuts to shows to your home screen from their overview page. Thanks Andrew Neal (@adneal)!
* TWEAK Tapping the Google Play link now searches exclusively for movies and TV shows. Thanks Andrew Neal (@adneal)!
* TWEAK Title and description in the add show dialog are now selectable (Android 3+). Thanks Andrew Neal (@adneal)!
* TWEAK The search bar is now shown on older versions of Android, too. Thanks Andrew Neal (@adneal)!
* TWEAK Tab bars are now only as wide as they need to be, this looks way better especially on large screens.
* FIX Strict mode testing: some unclosed cursors are now closed correctly, freeing up resources correctly. Thanks Andrew Neal (@adneal)!
* FIX If the X subscription is canceled, actually remove access to X features once it has expired.
* FIX Ensure valid trakt credentials before rating a show. Thanks Andrew Neal (@adneal)!
* FIX Actions now properly hide in all cases when opening the navigation drawer.
* FIX Some Google Play reported crashes in the backup tool, with in-app billing.
* NOTICE For contributers: all libraries live now inside the SeriesGuide repository, no need to clone multiple repositories anymore.

#### 7beta1 *(2013-07-10)*

* TWEAK Use new navigation drawer that overlays content. Does not overlay tabs, not a bug!
* TWEAK Use minimal activity stream to get trakt activity, apparently reducing download size up to 90%.

Version 6.1x *(2013-06-29)*
------------------------
This was released for SeriesGuide X only.
* FIX Crash when trying to open in-app upgrade activity.

Version 6 *(2013-06-28)*
------------------------

* Unlock X features through 'X Upgrade' in-app purchase (see Settings, Services). Purchased SeriesGuide X already? Install it to unlock the purchase.
* Beam shows to another device from the shows overview page (Android 4+).
* Activity stream displays 30 days into the future/past. Option for 'Infinite activity'. List widget is always infinite.
* Updates are handled in the background by Android (see the SeriesGuide Sync account in Android settings).
* Navigation drawer only opens if swiping from the edge.
* Added setting to disable Auto-Backup.
* Toned down default dark design.

#### 6 *(2013-06-28)*

* TWEAK Always sync if manually updating via any update action menu items. Previously this was dependent on global and automatic sync being on.
* TWEAK Reduce minimum update interval on opening the app from 30min to 20min. Checked in episodes should now show up as watched as expected.
* TWEAK Add trakt page button to shows, episodes and movies.
* FIX Play store reported crashes: auto-backup disk full, check-in failure dialog after leaving app and it was destroyed for resources.
* FIX Flagging whole season watched now correctly sets the last watched episode to the last of that season.
* NOTICE Latest translations from crowdin.

#### 6beta4 *(2013-06-16)*

* TWEAK Change up some colors, remove some accent color, added light separators to make the app look more clean.
* FIX Crash after running the 'Load posters' task due to missing success string resource.
* FIX Crash when filtering in activity results in no more items shown.

#### 6beta3 *(2013-06-08)*

* FEATURE Support unlocking X features through new 'X Upgrade' in-app purchase (see Settings, Services). If you have already purchased SeriesGuide X, just install it side-by-side to unlock the purchase.
* TWEAK Display confirmation toasts when triggering manual updates, show descriptive error messages. For now Auto Update needs to be turned on to manually update.
* TWEAK Syncs triggered through the app (not the regular periodic sync) now only run on WiFi if the user asked so.
* TWEAK New export tool shows exact progress. Removed warning from old tool.
* TWEAK Revamp service links below each episode and show.
* TWEAK Change 'Air date' to the more general 'Release date', this better includes non-TV shows. Not having an air time or date is now not displayed as something wrong.
* FIX Fix crash when new show got added from trakt activity screen if the updater did run in the background.

#### 6beta2 *(2013-05-24)*

* New! Beam your shows to a friends device from the shows overview page (Android 4+).
* Updates are now handled in the background by Android (see the SeriesGuide Sync accounts in Android settings). As a side-effect update progress notifications are gone, but those did only distract anyway.
* The action bar shows the new navigation drawer icon. The drawer does now only open if swiping from the edge.
* Increased transparency of background posters to increase readability.
* Remove split action bar in Overview, use inline buttons. All this new space!
* Move rate actions to overflow menu, inline rating is much easier.
* Add context menu buttons to movie and show items.
* Latest translations from crowdin.

#### 6beta1 *(2013-05-14)*

* Activity stream now by default shows 30 days of activity into the future/into the past. Added option to show 'Infinite activity' to display all activity. List widget displays infinite activity again.
* Redesigned menu to be more in line with other apps. Looks like there will be a Google Pattern at I/O, so it will probably change again, soon.
* Android 3.0+ tablets now use a multi-pane overview layout which also displays show information.
* Support different location of numbers for season and episode strings. Translations pending.
* Design tweaks for action bar and tabs featuring less prominent text.
* Design tweaks for movie section to fit more items on screen, less prominent text.
* Added setting to disable Auto-Backup.
* Fixed crash in movies section when movies did not have a proper release date set.
* Latest translations from crowdin.

Version 5.2 *(2013-05-16)*
--------------------------

* FIX Crash in movie section if movie did not have a release date.

Version 5.1 *(2013-05-01)*
--------------------------

* FIX Disable trakt offline support for now, causes crashes on too many devices.
* FIX 'Manage lists' dialog would hide OK button when too many lists were shown.
* TWEAK Temporarily increase Activity stream limits to 90 days in each direction until we can make it unlimited again.
* NOTICE Latest translations from crowdin.

Version 5 *(2013-04-27)*
--------------------------

* FEATURE Integrate the trakt movie watchlist.
* TWEAK Redesigned Overview, now displays seasons and show info in tabs.
* TWEAK Episodes in Activity are now grouped by day, check-in by touching and holding.
* TWEAK Better support for rating on trakt.
* TWEAK Episode notifications will queue up until 12 hours have passed or they are dismissed (Android 3+ only). Notifications will only be sent out if there are actually new episodes to notify about.
* NOTICE The old backup system will go away with v6. Restore your old backups and export them with the new backup tool.
* FEATURE A new JSON backup tool.
* TWEAK Collect entire seasons or shows. Display collected flags in episode lists.
* NOTICE Bump minimum supported version to Android 2.2 (from 2.1).

#### 5beta6 *(2013-04-23)*

* TWEAK Episode notifications will queue up until 12 hours have passed or they are dismissed. Notifications will only be sent out if there are actually new episodes to notify about.

#### 5beta5 *(2013-04-17)*

* TWEAK Episodes in Activity are now grouped by day. This is highly untested and might eat you alive. Just so you know.
* FIX Upcoming widgets and the DashClock extension were broken. Now they are not. Or are they?
* FIX Crash when rotating your device in Overview. Fixed courtesy of best bug report evar: #242.

#### 5beta4 *(2013-04-16)*

* FEATURE Integrate the trakt movie watchlist, support removing and adding movies by touching and holding.
* TWEAK Finally display a seasons tab right next to the overview on phones. Bonus: display a show tab to the left, too.
* TWEAK Correct air times for British (e.g. Doctor Who 2005) and German (e.g. heute-show) television using information from trakt.tv.
* TWEAK Display a check-in button on episodes in Upcoming/Recent on larger screens.
* TWEAK Check-in from Upcoming/Recent by touching and holding episodes.
* TWEAK Don't display ticker text for auto-update notification in overview.
* TWEAK Reduce font-sizes on 7 inch devices.
* TWEAK Use Roboto Light in more places (like descriptions, action bar titles).
* TWEAK Activity only shows episodes at most one month into the future/past.
* TWEAK More prominently display own trakt ratings, updates immediately upon rating.
* TWEAK IMDb button for movies.
* TWEAK Accumulate episode notifications only for episodes of the last 12 hours.
* FIX Crashed when going up from new backup tool.
* FIX Restore UI state on rotating the device while backup/import is running in new backup tool.
* FIX Broken search layout on legacy Android versions.
* NOTICE Latest translations from crowdin.

#### 5beta3 *(2013-04-05)*

* FIX Collecting whole seasons and shows was broken, as well as flagging seen all previously aired episodes.

#### 5beta2 *(2013-04-05)*

* NOTICE The old backup system will go away with v6. Restore your old backups and export them with the new backup tool.
* FEATURE The old backup assistant is now deprecated in favor of a new JSON exporter. Backups will take a little longer, but will be completely compatible between devices and less prone to version issues. Also you can easily edit or reuse the JSON files yourself. New (auto-)backups will for now be stored in your Downloads folder. Auto-Backups will dump a minimal export which does not include descriptions, actors, ratings, etc. These will get filled in on your next update after restoring.
* FEATURE Flagging watched and collected now works offline when connected to a trakt account. Once you open the app with a working connection your last actions will be sent to trakt. Also if you set SeriesGuide to use Wi-Fi only that will apply to those trakt actions now, too. Once you have an allowed connection you can force sending by manually updating.
* TWEAK Collect entire seasons or shows. Display collected flags in episode lists.
* TWEAK Episodes in notifications will now persist until you either clear the notification (Android 3.0+), watch the episode or more than 24 hours have passed since it aired.
* TWEAK List items have a 'Manage lists' context menu option.
* FIX Shows in lists now always update their next episode.
* FIX Auto backup did not work correctly, now, it does. Once a week.
* FIX List items only display stars for favorited shows.
* FIX Remove list item from correct list when having more than one list.
* FIX Really prevent shows from getting removed when they are added to a list.

#### 5beta1 *(2013-03-29)*

* FEATURE Support Quick-Check-In action from single episode notifications.
* FEATURE Add episodes within setting to DashClock extension.
* FEATURE Trakt comments for movies.
* TWEAK Trakt comments now link to their page on trakt.tv.
* TWEAK Display show link in episode details in activity tablet layout.
* TWEAK New search results layout, use Search view on Android 3+.
* FIX Support canceling movie check-ins.
* NOTICE Bump minimum supported version to Android 2.2 (from 2.1).
* NOTICE Latest translations from crowdin.

Version 4.1 *(2013-03-22)*
--------------------------

* FIX Properly register DashClock extension for stable versions.
* FIX Remove wrong parent activity tags.
* NOTICE Latest translations from crowdin.

Version 4 *(2013-03-21)*
--------------------------

### Notable changes:

* FEATURE Add statistics.
* FEATURE New next episode algorithm: remembers what you last watched and offers later episodes as next. Improves watching older seasons.
* FEATURE Supply extension for Roman Nurik's DashClock.
* TWEAK Change widget settings from new widget settings shortcut.
* TWEAK Rate on trakt by just tapping on the rating values.
* TWEAK Make favorite stars touchable.
* TWEAK Text in detail views is mostly selectable (Android 3.0+). Easily copy actor names or descriptions.
* NOTICE Database upgraded to version 31. Starting SeriesGuide the first time after installing the update may take a little longer than usual.

### Detailed changes:

#### 4beta4 *(2013-03-21)*

* FIX Remove decor view background to reduce overdraw due to android-menudrawer usage, should very slightly improve performance.
* NOTICE Latest translations from crowdin.

#### 4beta3 *(2013-03-14)*

* FEATURE Add statistics.
* TWEAK Use new improved swipe menu library (MenuDrawer from SimonVT).
* TWEAK Change widget settings from new widget settings shortcut.
* TWEAK Show title in activity activity.
* FIX Spinner on Gingerbread (Android 2.3) and below was not readable (reverted to platform style).
* TWEAK Increase line height for description text for easier reading.
* TWEAK Rate on trakt by just tapping on the rating values.
* TWEAK Overall design tweaks (removals, text size improvements, layout changes).
* TWEAK Add favorite button in overview.
* TWEAK Make favorite stars in the show list touchable.
* TWEAK Improve TVDb reliability by removing www from TVDB API url.
* TWEAK Displays your own trakt ratings in brackets.

#### 4beta2 *(2013-02-22)*

* FEATURE New next episode algorithm: remembers what you last watched and offers later episodes as next. Improves watching older seasons.
* FEATURE Allow setting the widget opacity level, for X only.
* FEATURE Started movie details pages, adds description and a trailer button.
* TWEAK Display the online help page inline, revamped the online help page.
* TWEAK Overview updates on new information (e.g. a picture).
* TWEAK Load movies playing now for empty search query.
* FIX Restore correct text styles for season watch count.
* NOTICE Database upgraded to version 31. Starting SeriesGuide the first time after installing the update may take a little longer than usual.

#### 4beta1 *(2013-02-12)*

* FEATURE Supply extension for Roman Nurik's DashClock.
* TWEAK Text in detail views is mostly selectable (Android 3.0+). Easily copy actor names or descriptions.
* TWEAK New screen animations.
* TWEAK New side-attached large-screen layout for activity screen.
* TWEAK Link to app settings from Android network manager tool (Android 4.0+).
* TWEAK Smoother transition after pressing swipe-menu item.
* TWEAK Display show poster in show info, other tweaks for more prettiness.
* TWEAK Display type of list item if it is an episode or a season.
* FIX Full screen swipe opens menu if first episode detail page is shown.
* FIX Very long network names wrap correctly in show list.
* FIX Crash with list widget.
* NOTICE Latest translations from crowdin.

Version 3 *(2013-01-29)*
------------------------

* FEATURE Swipe anywhere to show the new menu drawer for quick navigation within the app. Swipe from the left margin in view pagers.
* FEATURE Add Movies menu item to allow searching for and checking into movies.
* FEATURE Choose when notifications should appear from a hand full of options (one hour until on air).
* TWEAK New trakt connect wizard. Helpful empty messages.

### Detailed changes:

#### 3beta4 *(2013-01-29)*

* FIX Crash when flagging episodes watched in lists.
* FIX Crash when checking trying to check into a movie, then navigating away.
* NOTICE Updated Google Analytics Android SDK to 2.0beta4, reduces power consumption.

#### 3beta3 *(2013-01-27)*

* TWEAK New trakt connect wizard.
* TWEAK Link to community and uservoice page from settings.
* TWEAK Fix visible but useless divider in movie check in dialog.
* TWEAK Fix movies grid view size and overlap issues.
* TWEAK Open search UI after pressing search button in slide menu.
* TWEAK Add custom empty message for trakt add tabs.
* TWEAK Add visible, easier discoverable context menu buttons for seasons and episodes in lists.
* TWEAK Enable enter key for movie search (GTV support).
* TWEAK Add contributing file, update credits with links to licences.
* TWEAK Clarified manual trakt sync description.
* TWEAK Show delete progress dialog through the whole removal process again.
* TWEAK Prevent auto-updater running on first launch.
* TWEAK Enable up-button for check-in activity.
* TWEAK Always display shouts in their own activity. No xlarge screen layouts, yet.
* FIX Avoid crash when rotating in first run fragment.
* FIX Fix crash due to remaining merge tag in episode details activity.
* FIX Hopefully finally fix the crash in add fragment by setting the click listener on eac
* FIX Layout weight should be float (fixed for episode pager).
* FIX Fix poster download task crash.
* NOTICE Latest translations from crowdin.

#### 3beta2 *(2013-01-13)*

* FEATURE Add Movies menu item to allow searching for and checking into movies.
* FEATURE Add absolute episode number for episodes to database, parse it on updating.
* TWEAK Hide the guest star and DVD number label for episodes if there is no data.
* TWEAK Only show DVD number if it is different to episode number.
* TWEAK Navigation improvements, setting up proper back stacks. Add up navigation for seasons and episodes. Fix some existing ups.
* TWEAK Add check in and search action back to home activity.
* TWEAK Launch to show list from app icon in list widget.
* TWEAK Add check-in button to all top shows activities, remove it from global menu.
* TWEAK Add custom cancel drawables.
* TWEAK Informative empty views for show list.
* FIX Fix enabling full-screen swiping in activity activity.
* FIX Remove Android Beam until we can resolve pre-GB compatibility.
* NOTICE Latest translations from crowdin.

#### 3beta1 *(2012-12-25)*

* FEATURE Swipe anywhere to show the new menu drawer for quick navigation within the app. Swipe from the left margin in view pagers.
* FEATURE Just for fun: beam a show to another device from its overview page.
* FEATURE Choose when notifications should appear from a hand full of options (one hour until on air).
* TWEAK Lists have there own page now.
* TWEAK Proper episode deep linking, now uses dual-pane layout on tablets.
* TWEAK Add icons for some menu actions.
* TWEAK Fix search tab in 'Add show' to second position.
* TWEAK Check in confirmation message for trakt is now localized and uses custom numbering schema.
* FIX Flagging a whole show now properly updates the screen.
* FIX Never notify about an episode twice, except others start airing shortly after.
* NOTICE Updated credits.

Version 2.11 *(2012-12-08)*
--------------------------------

* FEATURE 'Fix GetGlue check in' in check in dialog for shows without or with wrongly mapped IMDb ids.
* FEATURE Automatically add new shows from your trakt activity stream. Can be disabled in settings.
* FEATURE Experimenting with Google Play and Amazon search links below each episode.
* TWEAK Make episode in overview clickable, new layout.
* NOTICE Database upgraded to version 29. Starting SeriesGuide the first time after installing the update may take a little longer than usual.

### Detailed changes:

#### 2.11.5beta *(2012-12-03)*

* TWEAK Make episode in overview clickable, new layout.
* TWEAK Also search with show title in overview (Google Play, Amazon buttons).
* TWEAK Remember sync unseen episode preference. Closes #195.
* FIX Enable home button in Fix GetGlue activity, but not as up affordance.
* FIX Correctly color borderless buttons text, and regular buttons text for v11+.
* FIX Handle a requested downgrade by reinitializing the database instead of crashing.
* FIX Prevent adding new trakt shows multiple times. Closes #197.
* FIX Update lists content provider upon updating next episode. Closes #193.
* FIX Prevent removing shows which are in any list. Closes #198.

#### 2.11.4beta *(2012-11-28)*

* FEATURE Automatically add new shows from your trakt activity stream. Can be disabled in settings.
* FEATURE Experimenting with Google Play and Amazon search links below each episode.
* TWEAK Use custom drawables for most of the UI on Android 2.3 and lower when using one of the two SeriesGuide themes.
* TWEAK Updated list widget preview image.
* TWEAK Updated first run layout.
* TWEAK Dismiss first run fragment if forwarding to add screen.
* TWEAK Enable home button in FixGetGlueCheckInActivity.
* TWEAK Remove episode remove button, SeriesGuide is cleaning up orphaned episodes by itself.
* TWEAK Postpone launching notifications service on boot for a minute.
* FIX Shouts now use the selected theme.

#### 2.11.3beta *(2012-11-25)*

* FEATURE 'Fix GetGlue check in' in check in dialog for shows without or with wrongly mapped IMDb ids.
* NOTICE Database upgraded to version 29. Starting SeriesGuide the first time after installing the update may take a little longer than usual.
* FIX Icons overlap in check in dialog.
* FIX Never select non-existing activity tab. Fixes #187.
* TWEAK Trakt add tabs use higher resolution images on high-res tablets (e.g. Nexus 10).
* TWEAK Add custom button disabled drawable.
* TWEAK Refresh check box and edit text drawables with more acurate color, higher resolution variants.
* TWEAK Better exception tracking so problems are easier to pinpoint.

Version 2.10.2 *(2012-11-14)*
--------------------------------

* FEATURE Android 4.2 lock screen widget support.
* FIX Adding a list widget on Android 4.2 crashes SeriesGuide.
* FIX Could not interact with trakt on Android 4.2 as password could not be decrypted.

### Detailed changes:

#### 2.11.2beta (2012-11-14)

* FIX Adding a list widget on Android 4.2 crashes SeriesGuide.
* FIX Could not interact with trakt as password could not be decrypted.

#### 2.11.1beta (2012-11-13)

* FEATURE Lock screen widget support.
* TWEAK Episode images and button bar tweaks.
* NOTE Now with Chinese translation! And latest translations from crowdin.

Version 2.10.1 and 2.10.4beta *(2012-11-10)*
--------------------------------

* TWEAK Add high resolution launcher icon for upcoming Nexus devices.
* NOTE Latest translations from crowdin.
* NOTE Fixed Google Analytics.

Version 2.10 *(2012-11-05)*
--------------------------------

* FEATURE Light theme, can be enabled in advanced settings.
* FEATURE trakt watchlist in add screen.
* FEATURE Episodes are sortable by rating.
* TWEAK Some love to the list widget, increased font-sizes on Nexus 7-like screens.
* NOTE Latest translations from crowdin.

### Detailed changes:

#### 2.10.3beta (2012-11-05)

* FEATURE trakt watchlist in add screen.
* TWEAK Display empty message instead of progress indicator in add screens.
* FIX Broken placeholders in Arabic strings.
* FIX Fast scrolling in add screen crashed the app.
* NOTE Latest translations from crowdin.

#### 2.10.2beta (2012-10-16)

* FEATURE Episodes are sortable by rating.
* TWEAK Use episode title as default check in comment to avoid confusion about what is actually checked in.
* TWEAK Change first list name to 'First list' to avoid confusion with Favorites.
* TWEAK Move recommended and library tabs right next to trending if connected to a trakt account.
* TWEAK Lists: shows display next episode, episodes display number and air date.
* TWEAK Show which items list associations are manipulated.
* TWEAK Dropped rarely used reverse alphabetical sorting.
* FIX Light theme for all languages.
* FIX Recommended tab text was cut off on small screens.
* NOTE Latest translations from crowdin.

#### 2.10.1beta (2012-10-16)

* FEATURE Light theme, can be enabled in advanced settings.
* FEATURE Add all button for trakt library.
* TWEAK Some love to the list widget, increased font-sizes on Nexus 7-like screens.
* TWEAK Custom selected color (seen when navigating with D-PAD).
* FIX Action items sometimes invisible on tablets.
* FIX Shouts dialog resizing properly, only showing on large or xlarge screens.
* FIX Do not create auto-backup on first launch, at earliest after a week.

Version 2.9.3 *(2012-10-18)*
--------------------------------

* FEATURE Setting to display notifications for all shows, not just favorites.
* FEATURE Remember last used activity tab.
* NOTICE Use ActionBarSherlock 4.2.0: notably, the menu buttons are gone on Android 2.3 and lower.
* NOTICE The content provider URI for SeriesGuide (free, not beta or X) has changed to com.battlelancer.seriesguide.provider (appended .provider).

### Detailed changes:

#### 2.9.9beta (2012-10-18)

* FEATURE Remember last used activity tab.
* TWEAK Remove RECEIVE_BOOT_COMPLETED permission in free version (only needed for notifications in X version).
* FIX Action bar items sometimes disappear in show list.
* FIX Crash on leaving overview while still determining remaining episodes.
* FIX Correctly use episode time if adding a calendar event from overview.

#### 2.9.8beta (2012-10-10)

* TWEAK Let update task wait on failure before trying again.
* FEATURE Setting to display notifications for all shows, not just favorites.
* TWEAK Display 'Shows updated.' instead of generic 'Update successful.'.
* NOTICE Use ActionBarSherlock 4.2.0: notably, the menu buttons are gone on Android 2.3 and lower.
* NOTICE The content provider URI for SeriesGuide (free, not beta or X) has changed to com.battlelancer.seriesguide.provider (appended .provider).

Version 2.9.2/2.9.7beta *(2012-10-08)*
--------------------------------

* TWEAK Enabled fast scrolling for season and episode lists.
* FIX Episode images sometimes did not download successfully due to size constraints: increased the maximum size.
* TWEAK Revert back to the regular GetGlue OAuth URL.
* TWEAK Display info toasts for watched, collected and calendar buttons on long click.

Version 2.9.1/2.9.6beta *(2012-10-01)*
--------------------------------

* TWEAK Speed up overview, displaying show and seasons link right away.
* FIX Next episode algorithm will show episodes before 1970.
* FIX Action bar items sometimes disappear.

### Detailed changes:

#### 2.9.6beta (2012-10-01)

* TWEAK Speed up overview by using loaders, displaying show and seasons link right away.
* TWEAK Update season counters starting with latest season (previously oldest).
* FIX Next episode algorithm will show episodes before 1970.
* FIX Action bar items sometimes disappear.
* INFO Latest translations from crowdin.

Version 2.9 *(2012-09-27)*
--------------------------------

* FEATURE Basic lists, add shows, seasons or episodes to them. No syncing with trakt.tv, yet.
* FEATURE Submit flagging whole seasons, shows and previously aired episodes to trakt.tv.
* FEATURE Automatically back up show database every week on starting the app. File is named seriesdatabase_auto.db and stored in the backup folder (See Backup/Restore in settings).
* TWEAK Refined UI design with more custom elements, layout unification.
* TWEAK Sharing an episode now includes its IMDb page link, falls back to the show IMDb page if it does not exist.
* FIX Multiple fixes for crashes reported via the Play store.

### Detailed changes:

#### 2.9.5beta (2012-09-26)

* TWEAK Use faster and light weight HttpURLConnection for data connections.
* TWEAK Set smaller timeouts on connections.
* TWEAK Simplified first run information.
* FIX Updater was supposed to always update year old TVDb episodes, but did update everything. Now works as expected, therefore finishes updating faster.
* FIX GetGlue check in comments now allow all characters, previously non-A-to-Z chars failed the check in.
* FIX Crash when trying to load posters in lists view, also freshened up the progress layout for that a bit.
* FIX Crash when flagging episodes and moving to different screen before update was done.
* FIX Multiple fixes for crashes reported via the Play store.
* FIX Respect theme setting in new UI, widget config screen.

#### 2.9.4beta (2012-09-25)

* TWEAK Display which show got checked in on GetGlue.
* TWEAK More subtle app logo.
* TWEAK Dual pane upcoming layout only on larger screens to avoid text cut off.
* TWEAK Remove episodes deleted from TVDb, hide empty seasons.
* FIX Show list layout bleeding out.
* FIX Better show info layout.
* NOTICE Latest translations from crowdin. 

#### 2.9.3beta (2012-09-23)

* FEATURE Basic lists, add shows, seasons or episodes to them. No syncing with trakt.tv, yet.
* TWEAK More UI tweaks, e.g. nicer wide layouts on large tablets and it should look nicer on the Nexus 7.
* TWEAK When sorting by alphabet, ignore case.
* TWEAK GetGlue authentication in internal WebView instead of launching browser. Check if this works on Google TV!
* TWEAK Remove text from progress indicators as suggested by the ADG.
* TWEAK Added 'Check in' to shows long-press menus.
* TWEAK More values for Time Offset.
* FIX Assume correct default disabled 'Update only via Wi-Fi' in overview.
* FIX Hide episodes airing later than in an hour from check in screen.
* FIX Episodes did not get flagged when flagging whole season watched.
* FIX Always update the TVDb rating, allow to update everything when changing the content language.
* FIX Do not notify about specials if the user chose to hide them.
* FIX Next episode algorithm will show all specials again if the user did not hide them.
* FIX Display correct collected flag state in overview.
* FIX Jelly Bean: Up in activity screen always returns to the show list.
* NOTICE Latest translations from crowdin.

#### 2.9.2beta (2012-09-16)

* TWEAK Refined UI design with more custom elements, layout unification.
* TWEAK Improved trakt.tv credentials dialog behavior, set up instructions.
* TWEAK Name list widget according to app version. Yay, no more guesswork!
* NOTICE Updated ViewPagerIndicator to 2.4.1.

#### 2.9.1beta (2012-08-19)

* NOTICE Database upgraded to version 27. Starting SeriesGuide the first time after installing the update may take a little longer than usual.
* FEATURE Display IMDb pages for episodes (IMDb app or website).
* TWEAK Sharing an episode now includes its IMDb page link, falls back to the show IMDb page if it does not exist.
* TWEAK Episode and show detail screens display time of last edit on theTVDb.com.
* TWEAK Download images in add screen and shout screen one-by-one.
* TWEAK Run GetGlue check in on thread pool, it should not be blocked by other background activity anymore.
* TWEAK Only update episode information if it actually changed according to theTVDB.com leading to slightly faster udpates.
* NOTICE Updated Gson to 2.2.2. Possibly faster interaction with trakt.tv.

#### 2.9beta (2012-08-16)

* FEATURE Fetch collected flags when adding a show from trakt.tv.
* FEATURE Submit flagging whole seasons, shows and previously aired episodes to trakt.tv.
* FEATURE Automatically back up show database every week on starting the app. File is named seriesdatabase_auto.db and stored in the backup folder (See Backup/Restore in settings).
* FIX Add low resolution first started background, fix scaling of existing one.
* FIX Up in activity screen always returns back to show list.

Version 2.8 *(2012-08-15)*
--------------------------------

* FEATURE Quickly add shows by tapping a + button.
* FEATURE Use the new expandable notifications on Android 4.1 Jelly Bean to display more content.
* TWEAK Calculate next episodes differently: display the next highest (season and number) episode regarding all watched episodes.
* TWEAK New layouts for 10 inch tablets.
* TWEAK Update single shows more often if their overview screen is used.
* TWEAK 'Update only via WiFi' setting respected by episode image and ratings downloads.
* TWEAK Relayouted list widget, updated widget backgrounds.

### Detailed changes:

#### 2.8.6beta (2012-08-05)

* FEATURE Quickly add shows by tapping a + button.
* TWEAK Display text for and hide certain action items.
* TWEAK Tweaked add show screen, larger layout for large tablets. This does not work great with D-PADs (Google TV), yet.
* TWEAK Run more tasks in parallel, if possible.
* TWEAK Refined first run experience. Trending shows tab is now first.
* TWEAK New settings screen.
* TWEAK Clear images on large tablets, schedule re-downloading to get hihger resolution thumbnails.
* TWEAK Revert to old show list layout on 7inch tablets.

#### 2.8.5beta (2012-08-03)

* TWEAK Shows appear to delete significantly faster.
* TWEAK 'Update only via WiFi' setting respected by episode image and ratings downloads.
* TWEAK Disable 'Update only via WiFi' setting by default.
* TWEAK Display current values for list settings as their summary.
* TWEAK Allow new large-size show list layout on devices with smallest width of 600dp, e.g. a 7in tablet (600x1024 mdpi).
* TWEAK Optimize screen tracking.
* TWEAK Initialize app settings on first run.
* FIX Empty message for check in screen.
* FIX Apply app theme immediately after changing the setting.
* FIX Expanded notification shows correct amount of shows.

#### 2.8.4beta (2012-07-25)

* TWEAK New show list layout for 10 inch tablets.
* TWEAK Pre-fill check in message, again.
* FIX Connecting to a trakt account using your email address instead of your user name now works.

#### 2.8.3beta (2012-07-20)

* TWEAK Calculate next episodes differently: display the next highest (season and number) episode regarding all watched episodes.
* TWEAK Cache images using a LRU cache as recommended by Google devs.
* TWEAK Added 'now' and 'today' to upcoming shows limit setting.
* FIX Display 'Recent' on list widget, if appropiate, again.
* FIX Crash on selecting notification for deleted show.
* MISC Use preview version (2.0beta2) of Google Analytics v2.
* MISC Use latest support library release (r9).

#### 2.8.2beta (2012-07-04)

* FIX Run the notification service in time for notifications, again.

#### 2.8.1beta (2012-06-28)

* FEATURE Use the new expandable notifications on Android 4.1 Jelly Bean to display more content.
* TWEAK Relayouted list widget, updated widget backgrounds.

#### 2.8beta (2012-06-27)

* TWEAK Update single shows more often if their overview screen is used.

Version 2.7 *(2012-06-27)*
--------------------------------

* FEATURE Quickly check into shows from the main screen.
* FEATURE Add notification settings for vibrating (thus requiring new VIBRATE permission) and ringtone selection.
* FEATURE Configuration for list widgets: choose between upcoming or recent episodes (only SeriesGuide X), hide watched episodes.
* FEATURE More filter options for activity screen (hide watched, special episodes)
* TWEAK Auto-update when launching any part of the app.

### Detailed changes:

#### 2.6.15beta (2012-06-25)

* FIX Crash on pre-3.0 devices in overview screen on small screen devices.

#### 2.6.14beta (2012-06-24)

* FEATURE Display remaining unseen episodes in overview in portrait mode.
* TWEAK Auto-update when launching any part of the app.
* TWEAK Hide episodes with no air date from Recent.
* FIX Do not display hidden shows on check in screen.

#### 2.6.13beta (2012-06-09)

* NOTICE Please re-add your list widgets!
* FEATURE Quickly check into shows from the main screen.
* FEATURE More filter options for activity screen (hide watched, special episodes)
* TWEAK Let most of the AsyncTasks run in parallel to speed up response times.
* FIX List widget configuration used layout ids which resulted in crashes on new releases.
* FIX List widgets do not crash the app on an invalid configuration anymore.

#### 2.6.12beta (2012-06-02)

* FEATURE Configuration for widgets: choose between upcoming or recent episodes (only SeriesGuide X), hide watched episodes.
* TWEAK Remove sorting preferences from settings screen (still available in seasons and episode lists).
* TWEAK Remove Paypal link.
* FIX New website url in About dialog.

#### 2.6.11beta (2012-05-27)

* FEATURE Add notification settings for vibrating (thus requiring new VIBRATE permission) and ringtone selection.
* TWEAK Nicer loading behavior in shows overview screens.
* TWEAK No initial notification toast when auto-updating.
* TWEAK A lot of code cleanup and restructuring, see commit logs.
* FIX Updated to ActionBarSherlock 4.1
* FIX Updated to ViewPagerIndicator 2.3.1

Version 2.6.1 *(2012-05-04)*
--------------------------------

* FEATURE New theme. Old ICS Base theme accessible via settings.
* FEATURE Support for trakt advanced 10-heart ratings.

### Detailed changes:

#### 2.6.10beta (2012-05-04)

* New trakt 10-heart rating system. No visual feedback on buttons, no auto-detection if user uses basic or advanced ratings (defaults to advanced).
* Honor theme setting through restarts (thanks Roman).

#### 2.6.9beta (2012-04-27)

* Darker watched and collected icons.
* Brought back ICS default theme via settings switch.

#### 2.6.8beta (2012-04-12)

* I found my fancy color pen and redrew some lines. Lame.

Version 2.6 *(2012-04-06)*
--------------------------------

* Exclusive SeriesGuide X feature: Notifications for favorite/starred shows.
* Trakt.tv shouts: episode airing right now? Open Shouts and discuss it with other people.
* One button to rule, ahem, check into trakt.tv and GetGlue.
* Want to watch, but do not have the episode, yet? Mark episodes as 'collected' to keep track of your library.
* With a trakt account marking single episodes as seen/collected is submitted to trakt. Other devices will sync automagically.
* Search through a show's episodes inside its Overview screen.
* Fixes, improvements and design refinements.

### Detailed changes:

#### 2.6.7beta (2012-04-06)

* Update fetches recently collected episodes.
* Shouts refresh automatically every 60 seconds. May the next Game of Thrones come :)
* Some animations.

#### 2.6.6beta (2012-04-04)

* Marking single episodes as seen/collected is submitted to trakt if you setup your trakt account.
* Sort search results by show, season and episode.

#### 2.6.5beta (2012-03-30)

* Searching from overview lists only episodes of the displayed show (suggestions are still for all shows).
* Design refinements.
* Better log output.

#### 2.6.4beta (2012-03-26)

* Rate and share shows.
* Unified check in dialog.
* New 'collected' property for episodes. Only on detail pages for now. (Database upgrade!)
* Inline button bar.
* Fix Auto-Update not working until first manual update.
* Crash fixes, other minor improvements and changes.
* Better error log output.
* Icon updates.

#### 2.6.2beta/2.6.3beta (2012-03-20)

* Support for trakt.tv shouts.
* Display ratings (Loves) from trakt.tv.
* Content language chooser in welcome dialog.
* Crash fixes, other minor improvements and changes.
* Better offline handling.
* Quick fix for jean-luc.

#### 2.6.1beta (2012-03-15)

* Notifications should now work correctly, are restored on reboot
* Mark episodes seen immediately when marking seen on trakt
* Some minor improvements.

#### 2.6beta (2012-03-11)

* Greatly improved time zone and summer time handling: update your shows!
* Notifications about upcoming favorite shows, enabled by default

Version 2.5.1 *(2012-03-28)*
--------------------------------

* Display correct time for non-US users, fix summer time issues. (All shows will update as a result of this).
* Content language chooser in welcome dialog.

Version 2.5 *(2012-03-08)*
--------------------------------

* Database upgrade for better time representation. It may take a few seconds for the database to upgrade.
* Hide individual shows.
* 'Only favorites' option for Upcoming/Recent, also respected by widgets.
* 'Watched all previously aired' option when long-tapping an episode.
* Unified interface across all devices thanks to ActionBarSherlock 4.0.
* Removed middle and large widget on Android 3.0+ devices. Use the new list widget!

### Detailed changes:

#### 2.5.6beta (2012-03-08)

* Modify design to be closer to Android Design guidelines.
* Highlight selected item in multi-pane episode view.
* UI for custom upcoming interval (currently 24 hours).
* Posters and no already added shows in Add Show (not for the search screen).
* When adding a show get seen episodes from trakt.
* Download half the data when syncing from trakt via Settings.
* Bigger sized list widget items on xlarge devices. Smaller minimum list widget size, therefore smaller on GoogleTV.
* Fixed stuck progress dialog when checking in with trakt.
* Some other minor improvements.

#### 2.5.5beta (2012-02-20):

* Fix for crash when marking episodes watched.
* Always show the 'Add to calendar' button, if room.
* Fix #53 for episodes list.

#### 2.5.4beta (2012-02-17):

* Hide legacy widgets except small size on Android 3.0+ devices.
* Fix incorrect next episodes if two air the same day. The lower numbered one is now assumed to be aired first (though still at the same time).
* Fix sorting and filtering if shows have older episodes listed under next.
* Reduced auto-update interval to at least every 15min.
* New notification icon (finally!).
* Layout tweaks in Upcoming and episode details.
* Hide 'Add to calendar' button if it is useless.
* Use ActionBarSherlock 4.0.
* Display search term, title and search soft button in `SearchActivity`.
* Enabled home button (app icon in the ActionBar) everywhere.
* New welcome and beta dialog.

#### 2.5.3beta (2012-01-29)

* Updated design towards ICS styles.
* Fix for crash when trying to create a trakt account.
* Use user-specified episode numbers in Upcoming/Recent.
* Fix GetGlue comment box text not wrapping.
* Latest translations from crowdin. Thanks everyone for the big effort!

#### 2.5.2beta (2012-01-28):

* Better internal representation for time. It may take a while until your shows appear (the database is upgrading in the background). YOU MAY NEED to update all of your shows afterwards. Episodes move now correctly from Upcoming to Recent 1 hour after they aired.

* Hide shows, they will disappear from everywhere except the new 'Hidden' show filter.
* Swipe between episodes on xlarge screens (10 inch tablets) or in landscape, too.
* 'Only favorites' option for Upcoming/Recent, also respected by widget.
* Add menu button to quickly share with last used sharing option.
* Use progress indicators in `AddActivity`.
* Friends layout more similar to Upcoming.
* Use ViewPagerIndicator 2.2.2 release.
* Fixes for the updater.
* Fixes for market reported crashes.
* Cleaner welcome dialog.
* Improvements for descriptions and text everywhere.
* Hide friends tab in Activity if trakt account is not set up.
* Don't use beta icon for stable release (only devices with extra high resolution displays were affected).
* Hide text of some action bar buttons.
* Support for 'Daily' air day.
* Latest translations from crowdin, now with Arabic by mohd55 and Hungarian by devilinside.

#### 2.5beta/2.5.1beta (2012-01-13):
 
* Add 'Watched all previously aired' option to episode list of a season.
* Add show posters in Upcoming and Recent.
* Fix to display correct episode title.

Version 2.4.2/2.4.3beta *(2012-01-11)*
-------------------------------

* Fix for a crash while updating and fetching trakt activity.
* Fix for wrong show poster displaying in list widget.
* Latest translations from crowdin.

Version 2.4.1 *(2012-01-07)*
-------------------------------

* New list widget for Android 3.0+.
* Remade all widgets sticking to design guidelines, this means they now display 1 (small), 3 (middle) or 7 (large) items and have correct padding.
* Latest translations from crowdin.

### Detailed changes:

#### 2.4.2beta (2012-01-07)

* Tapping list widget items takes you to the respective episode page.
* Tweaked list widget layout a little.
* Made widget backgrounds more transparent.

#### 2.4.1beta (2012-01-06)

* Resizable list widget for Android 3.0+. Uses an alarm to refresh (this needs testing) every 5 mins if the device is awake.
* Remade all widgets sticking to design guidelines, this means they now display 1 (small), 3 (middle) or 7 (large) items and have correct padding.
* Latest translations from crowdin.


Version 2.4 *(2011-12-25)*
--------------------------------

* Check into a show on trakt. After checking in, trakt will automatically display it as watching then switch over to watched status once the runtime of the show has elapsed.
* Recently watched episodes are fetched from trakt on each update: allows easy syncing of devices. After adding a show the initial fetch of watched episodes should still be done via `Settings` >> `Sync with trakt` >> `Sync to SeriesGuide`.
* `Upcoming` renamed `Activity`.

### Detailed changes:

#### 2.3.9beta (2011-12-21):

* Check into a show on trakt. After checking in, trakt will automatically display it as watching then switch over to watched status once the runtime of the show has elapsed.
* Recently watched episodes are fetched from trakt on each update: allows easy syncing of devices. After adding a show the initial fetch of watched episodes should still be done via `Settings` >> `Sync with trakt` >> `Sync to SeriesGuide`. Enabled by default on new installations.
* `Integrate with trakt.tv` now controls the former behaviour. The eye icon does not automatically mark episodes as seen on trakt anymore. Use the check in functionality.
* Auto update enabled by default on new installations (only over WiFi, only on opening the show list, once a day).
* `Upcoming` renamed `Activity`, now includes the latest activity of friends on trakt.
* Optimized for Android 4.0.
* TVDb button in `Overview`.
* Make the backup screen a little more informative.
* Add a welcome dialog to get users started.
* Set auto-update limit to 12 hours (down from 23 hours).
* Use ActionBarSherlock 3.5.0.

#### 2.4beta (2011-12-24):

* Fix text color for notification on Android 2.2 and below.
* Display failed shows in update error message.
* Latest translations from crowdin.net.

Version 2.3 *(2011-11-26)*
--------------------------------

* Revamped updater: shows get outdated after 7 days and get included in the next update
* AutoUpdate: presses the update button on opening the app for you
* Updating now only works on Wi-Fi, this can be disabled in settings
* Revamped adding of shows (incl. more trakt integration)
* Episode pager on phones/small screens
* TVDb buttons

### Detailed changes:

#### 2.3.2beta *(2011-11-22)*

* Thrown the TVDb updater out the airlock (so soon): now SeriesGuide will update your show if it has not been for more than a week when pressing the update button.
* AutoUpdate (finally...): as updating now happens truly in the background (sorry, manual abort is gone for now, just drop your connection...) enabling this will press the update button for you at most once a day. You have to open the app though.
* The thresholds (7 days for updating, 11 hours before next auto-update) are subject to discussion.
* 'Update on Wi-Fi only' (enabled by default) will prevent the updater from doing anything if you don't have a Wi-Fi connection to the internet.

#### 2.3.1beta *(2011-11-19)*

* Two more air time parsing schemas (e.g. '9:00PM' and '9PM' instead of quasi-standard '9:00 PM'), let me know if there are errors!
* Major cleanup, report any issues/broken features!
* Latest translations from crowdin.net. Now with Bulgarian thanks to Martin. Italian and Slovenian included for now despite low translation level.
* TVDb buttons on show info and episode page
* Editing of trakt credentials in settings
* Fixed broken background in episode pager

#### 2.3beta *(2011-11-06)*

* You might need to clear your trakt.tv credentials if you encounter problems
* Revamped adding of shows (better trakt.tv integration, recommended and library only for logged in trakt users)
* Episode details shown in swipeable pager (only in non-dual-pane layout, yes, the background is broken)
* Shows will get updated by the incremental updater after at least every 7 days
* Fix crash when adding shows on certain HTC devices (Desire HD, Mytouch 4G, ...)
* Layout/Design fixes

Version 2.2.2 *(2011-11-01)*
--------------------------------

* Fix crash when adding shows on certain HTC devices (Desire HD, Mytouch 4G, ...)

Version 2.2.1 *(2011-10-31)*
--------------------------------

* Fix layouts on small tablet (large screen) devices
* Validate trakt.tv credentials
* Secure the trakt.tv password even better (you will have to reenter it again, sorry)
* Don't rebuild the search table if nothing was updated
* Latest translations from crowdin. Now with Hungarian thanks to devilinside and uw11!

Version 2.2.3beta *(2011-10-30)*
--------------------------------

* Clear old trakt credentials correctly

Version 2.2.2beta *(2011-10-30)*
--------------------------------

* Fix layouts on small tablet (large) devices
* Validate trakt.tv credentials
* Secure the trakt.tv password even better (you will have to reenter it again, sorry)
* Don’t rebuild the search table if nothing was updated

Version 2.2.1beta *(2011-10-18)*
--------------------------------

* Relayout widget as suggested by Allen
* Some bug fixes

Version 2.2 *(2011-10-18)*
--------------------------------

* Desire HD users: please try updating your phone to the latest firmware
* Filter options for show list (replaces 'Hide watched shows' setting)
* Layout improvements all over the app (spot the differences!), esp. on tablet/Google TV (large+) devices
* Dual pane layout for Upcoming on large+ devices
* Display which show is currently getting updated
* New number format brought to you by dqdb via GitHub
* Latest translations from crowdin
* Many bug fixes, optimizations
* Thanks to everyone who sent in bug reports, suggested improvements or helps translate!

Version 2.2beta *(2011-10-16)*
--------------------------------

* Tell which show is currently getting updated
* Dual pane layout for Upcoming on large+ devices
* New number format brought to you by dqdb via GitHub
* Small tweaks everywhere
* Layout improvements all over the app (spot the differences!), esp. on tablet/Google TV (large+) devices
* Display correct time stamps for US Central users
* Latest translations from crowdin (Danish now in again)

Version 2.1.5beta *(2011-10-02)*
--------------------------------

* Don't reload the show list on config changes (e.g. orientation changes): scrolling state is remembered again
* Image loader now checks faster for existing images and only downloads images up to 100KB in size (most are around 30K)
* Display confirmation message when adding show instantly after pressing the 'Add Show' button
* Latest translations (mlucas beefed up Dutch)

Version 2.1.2 *(2011-10-01)*
--------------------------------

* Use new trakt library release (better error handling)
* New show sorting: favorites by next episode
* Clean up images when deleting a show
* Show first episode of season when using dual-pane layout
* Bugfixes, Improvements

Version 2.1.4beta *(2011-10-01)*
--------------------------------

* Filter options for show list (replaces 'Hide watched shows' setting).
* Rearranged show list menu items to make room for filter.
* Latest translations from crowdin.
* Bug fixes and improvements.

Version 2.1.3beta *(2011-09-11)*
--------------------------------

* Use new trakt library release (better error handling)
* New show sorting: favorites by next episode
* Clean up images when deleting a show
* Store images in correct folder for beta users (you have to redownload them, sorry)
* Show first episode of season when using dual-pane layout
* Bugfixes, Improvements

Version 2.1.1 (04.09.2011)
------------------------

* Better user communication when doing delta and full update (now called 'Update All')
* Use TVDB id instead of IMDb id to mark episodes as seen on trakt
* Don't require a touchscreen to use SeriesGuide (upcoming Google TV support)
* Always use a GridLayout for the show list.
* Tidy up settings.
* Change URLs to new website.
* Improvements for DeltaUpdate.
* Latest translations.

Version 2.1.2beta *(2011-09-04)*
--------------------------------

* Please do an 'Update All' because of: Revert some time calc code that accidentially slipped into the last beta
* Better user communication when doing delta and full update (now called 'Update All')
* Use TVDB id instead of IMDb id to mark episodes as seen on trakt
* Don't require a touchscreen to use SeriesGuide (upcoming Google TV support)

Version 2.1.1beta *(2011-08-27)*
--------------------------------

* Always use a GridLayout for the show list.
* Tidy up settings.
* Change URLs to new webiste.
* Improvements for DeltaUpdate.
* Latest translations.

Version 2.1 (13.08.2011)
------------------------

* Design updates (incl. usage of ActionBarSherlock)
* Delta-Updates (Full update option remains available)
* Experimental syncing with trakt.tv
* 'Hide watched shows' and 'No special episodes' options brought to you by Jake Wharton (big thanks!)
* Progress bars in season list
* Show trending or your shows on trakt.tv when adding shows
* New 'Favorites first' show sorting, ability to add shows to favorites
* Many other small improvements
* Thanks to all the beta channel users!


Version 2.0.6 (24.07.2011)
--------------------------------

* Fix calculation of air time and day when using time offset (and in general it is now done right)
* Fixes for some crashes reported by Android Market
* If your device is set to GMT-5:00 (US Central) SeriesGuide automagically corrects airtimes by 1 hour


Version 2.0.5 (08.07.2011)
--------------------------------

* using trakt does not require an API key anymore
* Removed languages with little translation (due to user requests)
* Reworded some strings (English only)


Version 2.0.4 (17.06.2011)
--------------------------------

* Fix: Various rare crashes
* Now with two variants of Portuguese


Version 2.0.3 (16.06.2011)
--------------------------------

* Fix: Crash when updating a show failed.
* trakt.tv rating via the share button.
* Latest translations from crowdin.
* Some other rare crash fixes.


Version 2.0.2 (15.06.2011)
--------------------------------

* Fix: Crashes while database ops were running in parallel
* Latest translations from crowdin.


Version 2.0.1 (14.06.2011)
--------------------------------

* Fix: German whats new was missing.


Version 2.0 (14.06.2011)
--------------------------------

* Redesign of UI and backend (try using it in landscape, or on Android 3.0+!).
* New time offset setting.
* Remove single episodes.
* Long-clicking on Back takes you to the show list.
* Dropped support for Android 1.6
* Much more... (see the source changes)
