<a name="top"></a>

Release notes
=============

🌟 = New.
🔧 = Improved or tweaked.
🔨 = Resolved or fixed.
📝 = Notable change.

Version 54
----------
*2020-04-30*

* 🌟 Switch to bottom navigation bar.
* 🔧 Auto backups are created in an app specific directory, no longer requiring any setup. For most
  users it will be backed up by Android (Android 6+, up to 25 MB total) and can be restored from
  after re-installing the app.
* 🔧 When connecting Trakt, do not clear movies that are only watched on the device. Instead upload
  them. Trakt will set them as watched on their release date.
* 🔨 Support adding and updating shows without episodes (e.g. upcoming shows).
* 🔧 Add JustWatch Turkey to streaming search links.

#### 54
*2020-04-30*

* 📝 Latest translations from crowdin.

#### 54-beta4
*2020-04-24*

* 🔧 Add JustWatch Turkey to streaming search links.
* 🔧 Tapping a bottom nav item now scrolls the visible list to the top.
* 🔨 Fix widget crashing if an item has no poster.
* 🔨 Fix crash when changing calendar settings in some situations.
* 🔨 Fix crash when pinning shortcut in some situations.
* 🔨 Fix crash if external storage is not available to read auto backups from.

#### 54-beta3
*2020-04-03*

* 🔧 Move community and translation links to More from Settings.
* 🔧 Less bright empty message icons on dark theme.
* 🔨 Fix conflict that prevented side-by-side installation of the Amazon and Play Store version.
* 🔨 Correctly color add all icon on Trakt lists screens.
* 🔧 A bunch of internal improvements.
* 📝 Latest translations from crowdin.

#### 54-beta2
*2020-03-26*

* 🌟 Replaced the navigation drawer with an easier to use and discover bottom navigation bar.
* 🔧 Removed unlock and services settings that are now shown under More.
* 🔧 When connecting Trakt, do not clear movies that are only watched on the device, instead upload
  them. Trakt will set them as watched on their release date.
* 🔨 Support adding and updating shows without episodes (e.g. upcoming shows).
* 🔨 The last auto backup date was off by a month.
* 📝 Latest translations from crowdin.

#### 54-beta1
*2020-03-20*

* 🔧 Auto backups are now always created in an app specific directory on external storage, not
  requiring a special permission or any setup. The last two backups are kept. They are not available
  if the app is installed again, unless Android's app data backup has backed them up (Android 6+, up to 25 MB total).
* 🔧 After installing the app and an auto backup is detected (e.g. Android has restored app data), offer to restore it.
* 🔧 After creating a backup, auto backup can copy it to files you specify.
* 🔧 Show a message if the last auto backup failed.
* 🔧 Ability to run auto backup right away, e.g. to test if creating copies is successful.
* 🔧 Suggest more recognizable names for backup files.
* 🔧 If a show or movie failed to update, also display its title.
* 📝 Auto backup will be turned on for all users. If you do not need auto backups,
  you can turn it off again.
* 📝 Auto backup will now create an empty file instead of none if there is no data to backup.
* 📝 Importing an empty backup file will no longer fail and just remove existing data.
* 📝 Support Android 10.

Version 53
----------
*2020-03-12*

* New Dark and Light app and widget theme.
* By default, set app theme based on system setting (Android 10) or by Battery Saver (Android 9 and older).
* New notification option to only notify if the new episode is the next episode to watch.

#### 53
*2020-03-12*

* 🔨 In some cases when backing up and the new backup is smaller,
  the resulting JSON might be corrupted.
* 🔧 If a show or movie failed to update, display which one (see Trakt/Cloud screens).
* 📝 Latest translations from crowdin.

#### 53-beta5
*2020-03-05*

* 🔧 Replace compass with link icon for movie links option.
* 🔧 Display country for Portuguese variants when selecting movie language.
* 🔨 Use less bright selected state for people list as well.
* 🔨 Restore icon for add to home screen button.
* 🔨 Crash when a movie result does not exist.
* 📝 Latest translations from crowdin.

#### 53-beta4
*2020-02-20*

* 🌟 Notifications: option to only notify if the new episode is the next episode to watch.
* 🔧 Add link to release announcements from app update notification.
* 🔨 Fix colors in debug view.
* 📝 Latest translations from crowdin.

#### 53-beta3
*2020-02-13*

* 🌟 New Dark and Light widget themes replace old themes, with more compact and less colorful header.
* 🔧 Widgets: prevent setting only premieres option if displaying shows, it has no effect.
* 🔨 Crash when using the new backup agent.
* 🔨 Crash when receiving malformed response from Cloud.
* 🔨 List add and edit dialog text box not full width.
* 📝 Latest translations from crowdin.

#### 53-beta2
*2020-02-07*

* 🌟 New Dark and Light theme replace old themes. By default theme is chosen by system setting
  (Android 10) or depending on Battery Saver being active (Android 9 and older). Set the theme
  permanently to Dark or Light in Settings.
* 📝 The theme update is still incomplete (e.g. widgets) or might be broken on some devices. Let me know!
* 🔧 Confirm set all episodes up to here watched.
* 🔧 On Android 6 and newer improve system app data backup by only including settings.
* 📝 Latest translations from crowdin.

#### 53-beta1
*2020-01-10*

* 🔧 Fetch images from new TheTVDB artworks subdomain, provide fall back for old image links.
* 🔨 Episodes screen may crash in certain situations.
* 🔨 Background work may crash on some devices in certain situations.
* 📝 Latest translations from crowdin.

Version 52
----------
*2019-12-05*

* 🌟 Calendar: add option to only display premieres.
* 🔧 Episodes: button to set all episodes watched up to (including) the current one.
* 🔧 Episodes: on phones, combine list and page view, add switch view button.
* 🔧 Discover: also use improved search by TheTVDB.com when set to English.
* 🔧 Discover: drop any language option, just type a show title in any language to get a match.

#### 52
*2019-12-05*

* 📝 Latest translations from crowdin.

#### 52-beta5
*2019-11-28*

* 🔧 Add new languages supported by TheTVDB.com.
* 📝 Add more translations of the new description on Play Store. Thanks to all translators!
* 📝 Latest translations from crowdin.

#### 52-beta4
*2019-11-21*

* 🔧 Switch English language show search to the new and improved search by TheTVDB.com.
* 🔧 Also drop any languages option. Just enter a show title in any language to get a match.
* 🔧 Discover: add Trakt logo to links connected to current Trakt profile.
* 📝 Latest translations from crowdin. Now including Hindi thanks to a new translator!

#### 52-beta3
*2019-11-15*

* 🔧 Episode view remembers if season was last viewed as list, goes back to list if page was shown
  by tapping on list.
* 🔨 Correctly tint switch view icon on light theme.
* 🔨 Resolve crash when opening episodes view.
* 📝 Latest translations from crowdin.

#### 52-beta2
*2019-11-08*

* 🔧 On phones, combine episode list and page view into one.
  Switch between them with a button in the top right.
* 🔧 Move episode share, add to calendar and manage lists buttons to bottom of screen.
* 🔨 Watched up to here no longer marks unreleased episodes watched.
* 🔨 In debug mode, log show TheTVDB ID if it fails to update.
* 🔨 Do not crash on backing up if file provider has issues.
* 📝 Latest translations from crowdin.

#### 52-beta1
*2019-10-31*

* 🌟 Calendar: add option to only display premieres (first episodes).
* 🔧 Show overview: if there is no next episode, suggest to look for similar shows.
* 🔧 Episode details: button to set all episodes watched up to (including) the current one.
* 📝 Latest translations from crowdin.

Version 51
----------
*2019-10-02*

* 🌟 Display similar shows from the show details dialog and screen. Powered by themoviedb.org!
* 🔧 Display streaming search in show details dialog, if it was configured.
* 🔧 Move advanced settings up to the first settings section.
* 🔧 Remove DashClock extension, DashClock has been unpublished for a long time.
* 🔧 Allow users to enable debug mode, for example to share log output.

#### 51
*2019-10-02*

* 🔨 Do not crash when trying to display details for a show not existing on TheTVDB.com.
* 🔨 Do not crash if there is no app available to select notification sound.
* 📝 Latest translations from crowdin.

#### 51-beta6
*2019-09-26*

* 🔧 Show a close instead of an up button for screens that have no parent screen.
* 🔧 Migrate widget settings and Amazon extension settings to new implementation.
* 🔧 Allow users to enable debug mode, for example to share log output.
* 🔨 Do not show movie history tab at wrong position after connecting trakt. Wait until the movies
  section is left and visited again.
* 📝 Latest translations from crowdin.

#### 51-beta5
*2019-09-20*

* 🌟 Display similar shows from the show details dialog and screen. Powered by themoviedb.org!
* 🔧 Display streaming search in show details dialog if it was configured.
* 📝 Latest translations from crowdin.

#### 51-beta4
*2019-09-14*

* 🔨 Add movies to watchlist, collection or watched in all cases when syncing with trakt or Cloud.
  On upgrading to this version the next sync will add missing movies.
* 🔧 Remove DashClock extension, DashClock has been unpublished for a long time.
* 🔧 Switch settings to new underlying implementation.
* 🔧 Move basic settings link up to the first section, rename it to Advanced.

#### 51-beta3
*2019-09-04*

* 🔨 Resolve connection issues with TheTVDB and trakt.

#### 51-beta2
*2019-08-30*

* 🔨 Do not crash when viewing an episode and there is no show title or poster. 

#### 51-beta1
*2019-08-29*

* 🔧 Fetch show small poster path instead of constructing it, to future proof for upcoming changes at TheTVDB.com.
* 🔨 Fix discover screen displaying shows that can not be added.
* 🔨 Fix the subscriptions screen displaying a developer error in some cases.

Version 50
----------
*2019-08-16*

* 🌟 Add Sponsor and Supporter subscriptions. If you can or want to you can now make a more
  significant contribution to help me make future updates.
* 📝 Existing subscription is now All Access. Reduced price (for existing subscribers, too) so
  more people can get access to Cloud.
* 🔧 Add option to turn off infinite calendar.
* 🔧 Movie release times setting also affects popular, search. Watchlist, collection, watched and
  details views will start using it.

#### 50.1
*2019-09-18*

* 🔨 Add movies to watchlist, collection or watched in all cases when syncing with trakt or Cloud.
  On updating to this version the next sync will add missing movies.

#### 50
*2019-08-14*

* 📝 Latest translations from crowdin.

#### 50-beta5
*2019-07-25*

* 🔧 The list of popular movies and movie search display release dates depending on the selected
  region.
* 🔧 The movie watchlist, collection, watched tab and the details view will start to display the 
  release date depending on the selected region. Preferably the theatrical one.
* 🔨 Correctly detect active subscription after restarting the app.
* 🔨 Do not crash if subscription title can not be parsed.
* 📝 Distribute as Android App Bundle. This can not be sideloaded, use the official APK from the website!
* 📝 Latest translations from crowdin.

#### 50-beta4
*2019-07-19*

* 🌟 Support upgrading subscription to new Sponsor and Supporter tiers.
* 🔧 Show icon which subscription tier is active.
* 📝 Latest translations from crowdin.

#### 50-beta3
*2019-07-18*

* 🌟 Introduce Sponsor and Supporter subscriptions so people who can or want to can make a more
  significant contribution. This helps me make future updates.
* 📝 Rename existing subscription to All Access. Reduced price for new and existing subscribers so
  more people can get access to Cloud.
* 🔧 Move subscriptions to new Google Play billing library.
* 📝 Latest translations from crowdin.

#### 50-beta2
*2019-07-06*

* 🔧 Shows/Movies: move search action left-most as likely most used. Show refresh action on history tabs.
* 🔧 Show overview: move share action to more options to reduce clutter.
* 🔧 Episodes list: show sort by action.
* 🔨 Enable crash reporting.

#### 50-beta1
*2019-07-05*

* 🔧 Restore infinite calendar option due to feedback. Defaults to enabled for new and existing users.
* 🔨 Potential fixes for crashes due to extensions.

Version 49
----------
*2019-06-28*

* 🔧 Calendar is always infinite, uses all available space on large screens, has larger fast 
  scroller that is easier to grab.
* 🌟 Add setting to ignore hidden shows for notifications (defaults to enabled).
* 🌟 Filters: add option to make all hidden shows visible at once.
* 🔧 History: Add link to trakt history website. Show up to 50 items (was 25).
* 🔧 Streaming search: add JustWatch for Poland.
* 🔧 Movies: Add set watched option to more options (three dots) menu.

#### 49
*2019-06-28*

* 📝 Latest translations from crowdin.

#### 49-beta6
*2019-06-21*

* 🔨 When making all hidden shows visible also upload changes to Cloud.
* 🔨 trakt sign-in: do not crash if WebView is currently unavailable (e.g. it is updated).
* 🔨 Potential fix for crashes when receiving actions from extensions.
* 🔧 When changing the state of a show (e.g. favoriting or hiding it), will wait until sent to Cloud 
  before applying the change locally.
* 📝 Latest translations from crowdin.

#### 49-beta5
*2019-06-07*

* 🌟 Add setting to ignore hidden shows for notifications (defaults to enabled).
* 🌟 Filters: add option to make all hidden shows visible at once.
* 🔨 Do not crash when changing show states (favorite, hidden, notify).
* 📝 Latest translations from crowdin.

#### 49-beta4
*2019-05-31*

* 🔧 Streaming search: add JustWatch for Portugal (but appears to be broken) and Poland.
* 🔧 Add set watched option to movie more options (three dots) menu.
* 🔨 Movie not in collection or watchlist is properly added after setting it watched.
* 🔨 trakt sync adds movies that are just watched.
* 🔧 TMDb sync now reports failure if any movie failed to update.
* 📝 Latest translations from crowdin.

#### 49-beta3
*2019-05-24*

* 🔧 Experimental internal improvements when changing favorite, notify or hidden state of a show.
* 🔨 Potential fix for calendar jumping away from first item.
* 📝 Latest translations from crowdin.

#### 49-beta2
*2019-05-10*

* 🔧 The new calendar is now always infinite. If multiple columns are shown, groups are no longer 
  broken into a new row, instead using all available space.
* 📝 Latest translations from crowdin.

#### 49-beta1
*2019-05-10*

* 🔧 Add link to trakt history website on history screen. Show up to 50 items (was 25).
* 🔧 If connected to trakt, show at most 10 recently watched episodes or movies on history tabs (was 25).
* 🔨 Switched upcoming/recent tabs to RecyclerView, should resolve various crashes.
* 📝 The infinite calendar option has been removed. Instead upcoming/recent now show up to 50 episodes.
* 📝 Drop support for beaming shows from overview screen. Share the TheTVDB link instead.

Version 48
----------
*2019-05-02*

* Support 'Upcoming' status for shows.
* Add watched movies tab.
* Statistics: display number and run time of watched movies.
* Color navigation bar black for dark themes, white on light theme.

#### 48
*2019-05-02*

* 🔨 Do not crash if updating security provider fails.
* 📝 Latest translations from crowdin.

#### 48-beta6
*2019-04-17*

* 🔧 Support 'Upcoming' status for shows.
* 🔧 Ask Google Play Services (if available) to update security provider.
* 🔨 Fix crashes in movie details view and when pinning shortcuts.

#### 48-beta5
*2019-04-12*

* 🔧 Experiment: refresh season watched counts using new Worker API.
* 📝 Latest translations from crowdin.

#### 48-beta4
*2019-03-29*

* 🔧 Do not ask for storage permission in backup/restore tool (still required for auto-backup).
* 📝 Latest translations from crowdin.

#### 48-beta3
*2019-03-21*

* 🌟 Statistics: display number and run time of watched movies. Might be incorrect until movies are updated.

#### 48-beta2
*2019-03-15*

* 🌟 Add watched movies tab. Might show blank items until movies are updated.
* 🔧 Force black navigation bar on OnePlus devices as well.
* 🔧 Use white navigation bar on light theme if on Android 8.1 or higher for burn-in protection.
* 📝 Latest translations from crowdin.

#### 48-beta1
*2019-03-08*

* 🔧 Force black navigation bar.
* 🔧 Use darker overlay action and status bar for better readability (movie details).
* 🔧 Backup screens: show file path below button for better readability.
* 📝 Latest translations from crowdin.

Version 47
----------
*2019-02-22*

* 🔧 Show list: replace favorite button with set watched button.
* 🌟 Set movies watched (previously only when connected to trakt).
* 🌟 New show list filters that can be set to include (+), exclude (-) or disabled.
* 🌟 Added filter for continuing shows (exclude to display ended shows).
* 📝 Show list filter settings are set back to defaults.
* 🔧 Sharing old TheTVDB links to SeriesGuide to add shows works again.

#### 47
*2019-02-22*

* 🔧 Sharing old TheTVDB links to SeriesGuide to add shows works again.
* 🔧 Switch to improved error reporting to better pinpoint issues.
* 📝 Latest translations from crowdin.

#### 47-beta7
*2019-02-16*

* 🔧 Experiment with improved error reporting to better pinpoint issues.
* 🔨 Crashes and errors are reported again.

#### 47-beta6
*2019-02-08*

* 🔨 Do not crash when loading show discover screen.

#### 47-beta5
*2019-02-08*

* 🔨 Do not crash when opening movie with unknown running time.
* 🔨 Do not crash when opening show sort options with deprecated sort order.

#### 47-beta4
*2019-02-01*

* 🌟 Set movies watched (previously only when connected to trakt).
* 🌟 Cloud: sync watched movies. If trakt is connected, too, will upload existing watched movies, 
  then use Cloud to sync them going forward. Watched changes are still sent to trakt.
* 📝 Latest translations from crowdin.

#### 47-beta3
*2019-01-25*

* 🌟 New show list filters that can be set to include (+), exclude (-) or disabled.
* 🌟 Added filter for continuing shows (exclude to display ended shows).
* 📝 Show list filter settings are set back to defaults.
* 🔧 Upcoming range setting moved to button next to upcoming filter.
* 🔧 Show list filter view scrolls if screen is not tall enough.
* 📝 Target Android 9.0 (Pie).
* 📝 Latest translations from crowdin.

#### 47-beta2
*2019-01-18*

* 🔧 After changing the language of a show or the alternative language in Settings, episode descriptions are updated properly again.
* 🔧 Combine show filter and sort options into single view. Stays visible until tap outside or back button press.
* 📝 Latest translations from crowdin.

#### 47-beta1
*2019-01-11*

* 🔧 Show list: replace favorite button with set watched button. Display an indicator if a show is a favorite instead.
* 🔧 Cloud: update and improve Google Sign-In.
* 🔧 Tablets: move add show button on discover screen to top right to match placement of primary action in other places.
* 🔧 Discover: remove trakt recommendations. They were never useful. Send in feedback if they are for you!
* 🔨 Only remove movie from watchlist if it actually was on it. This avoids a confusing confirmation message.
* 📝 Only support Android 5.0 (Lollipop) and newer.
* 📝 Latest translations from crowdin.

Version 46 and older
----------

See [CHANGELOG-K.md](CHANGELOG-K.md).
