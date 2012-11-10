<a name="top"></a>

Changelog
=========

All dates are in the European Central timezone.

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
