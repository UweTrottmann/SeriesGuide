<a name="top"></a>

Release notes
=============

🌟 = New.
🔧 = Improved or tweaked.
🔨 = Resolved or fixed.
📝 = Notable change.

\* Releases marked with an asterisk (*) are/were available on [the beta program](https://github.com/UweTrottmann/SeriesGuide/wiki/Beta) only.

Version 64
----------
*in development*

#### 64.0.2*
*2022-03-10*

* 🔨 Cloud: when signing in do not add shows that were removed after the TMDB migration.
* 🔨 Fixed: when first tapping any episode in a season always displays the first episode.
* 🔨 Fixed: upcoming/recent does not update over time.
* 🔨 Fixed: guest stars, writers, etc. not separated by comma.
* 📝 Latest user interface translations from Crowdin.

#### 64.0.1*
*2022-03-03*

* 🔧 Move no aired episodes option to shows filter view.
* 🔧 Display state of show filters also as text.
* 🔧 Rephrase TMDB migration info and suggest action (search for a replacement).
* 🔧 Create first list when installing app instead of when visiting lists screen.
* 🔧 Various small design tweaks.
* 🔨 Display error if managing lists of a show is not possible.
* 📝 Latest user interface translations from Crowdin.

Version 63
----------
*2022-02-18*

* 🌟 Some design updates to fit with latest Material style.
* 🌟 Add setting to use system colors on Android 12.
* 🔨 Sort by title properly handles characters with accents.

#### 63.1.0
*2022-03-11*

* 🔨 Cloud: when signing in do not add shows that were removed after the TMDB migration.
* 🔨 Fixed: when first tapping any episode in a season always displays the first episode.

#### 63-beta4
*2022-02-10*

* 🌟 Appearance setting to use system colors on Android 12.
* 📝 Latest user interface translations from Crowdin.

#### 63-beta3
*2022-02-04*

* 🔨 Fix crash when using email sign-in on Android 12.
* 🔨 Fix missing state description for show filters (accessibility).
* 📝 Latest user interface translations from Crowdin.

#### 63-beta2
*2022-02-03*

* 🌟 Some design updates to fit with latest Material style.
* 📝 Latest user interface translations from Crowdin.

#### 63-beta1
*2022-01-21*

* 🔧 Move movie trailer button right below title to make it easier to discover.
* 🔨 Sort by title properly handles characters with accents.
* 🔨 Display correct colors if check-in dialog is launched from notification.
* 📝 Optimization for Android 12. 
* 📝 Latest translations from Crowdin.

Version 62
----------
*2021-12-09*

* 🌟 Discover: filter popular shows and shows with new episodes by streaming service.
* 🌟 Discover: filter popular movies and movies with digital release by streaming service.
* 🔧 App widget: add option to hide watch button.

#### 62.2
*2021-12-21*

* 🔨 Potential fix for crash when updating next episode on Android 5.1 devices.
* 🔨 Fix Amazon extension search link.

#### 62.1
*2021-12-16*

* 🔨 Potential fix for crash when updating next episode on Android 5.1 devices.
* 🔨 Amazon version: do not crash when re-launching the app.
* 📝 Latest translations from Crowdin.

#### 62
*2021-12-09*

* 📝 Add Welsh translation, thanks to NMulholland.

#### 62-beta5
*2021-12-03*

* 🔧 Discover: highlight new show and movie filters.
* 🔨 Do not crash when selecting backup files and no supported file picker is available.
* 🔨 Do not display options in toolbar not applicable when first opening some screens.
* 📝 Latest translations from Crowdin.

#### 62-beta4
*2021-11-25*

* 🌟 Discover: filter popular shows and shows with new episodes by streaming service.
* 🌟 Discover: filter popular movies and movies with digital release by streaming service.
* 🔧 Episode search: match independent of case for Unicode characters, e.g. German umlauts.
* 📝 Latest translations from Crowdin.

#### 62-beta3
*2021-10-20*

* 🔧 Revert the next episode selection change, because Anime. The next episode is again selected
  by release date. Note: to never choose specials as next, see More > Settings > Advanced.
* 📝 Latest translations from Crowdin.

#### 62-beta2
*2021-10-13*

* 🔧 The next episode is now selected based on number, no longer by release date. This means specials
  released in between episodes will no longer appear as next, you'll have to look for them in the
  specials season. Decided to change this based on feedback over the years as many were confused
  why an apparently random episode was next e.g. after marking all specials watched/skipped.
* 🔧 Modernize remaining pager components (e.g. episode tabs, movie tabs) for more reliable behavior.
* 📝 Latest translations from Crowdin.

#### 62-beta1
*2021-09-30*

* 🔧 App widget: add option to hide watch button.
* 🔧 Display error message if creating a shortcut fails.
* 🔨 Only move shows by a day if released in the hour past midnight for the CBS and NBC networks.

Version 61
----------
*2021-09-23*

* 🌟 App widget: add system theme that supports dark mode, colors on Android 12. Modernize existing themes.
* 🌟 App widget: add watch button.
* 🌟 Add upcoming movies screen.
* 🔧 Correctly choose the next episode if episodes are watched multiple times. This may change the next episode for some shows. Just set an episode watched to update.

#### 61
*2021-09-23*

* 🔨 Do not crash episode list if number of episodes and selection changes.
* 📝 Latest translations from Crowdin.

#### 61-beta5
*2021-09-16*

* 🌟 App widget: add system theme that supports dark mode, colors on Android 12.
* 🔧 App widget: modernize theme, update preview.
* 🔧 App widget: use system reconfigure button on Android 12.
* 📝 Latest translations from Crowdin.

#### 61-beta4
*2021-09-10*

* 🔧 Finally add fast scroller to episode list.
* 📝 Latest translations from Crowdin.

#### 61-beta3
*2021-09-03*

* 🌟 Add watch button to app widget.
* 🌟 Add upcoming movies screen.
* 🔨 Set a show not watched actually does something.
* 📝 Latest translations from Crowdin.

#### 61-beta2
*2021-08-12*

* 🔨 Do not select skipped episode as next to watch.
* 🔨 Cloud: on sync update last watched episode to correctly determine next to watch on all devices.
* 🔨 Cloud: do not fail when uploading a lot of movies.
* 📝 Latest translations from Crowdin.

#### 61-beta1
*2021-08-04*

* 🌟 When re-watching an episode, correctly choose the next one to rewatch. Display watch count in overview screen.
* 🔨 Prevent crash when removing show while it was getting updated.
* 🔧 Drop unused storage permissions, should have no impact on supported devices.
* 📝 Latest translations from Crowdin.

Version 60
----------
*2021-07-28*

* 🌟 Support signing into Cloud with email and password. With this update you are asked to sign into Cloud again.
  Choose email if you use other devices that do not support Google sign-in, like Amazon devices.
* 🔧 An episode IMDb link will open the episode page again, if the IMDb ID is available on TMDb.  

#### 60
*2021-07-28*

* 🔧 Auto-dismiss support the dev message.

#### 60-beta4
*2021-07-21*

* 🔧 An episode IMDb link will open the episode page again, if the IMDb ID is available on TMDb.
* 🔨 Fix duplicate error string when adding a show fails.
* 📝 Latest translations from Crowdin.

#### 60-beta3
*2021-07-08*

* 🔨 Do not crash during Cloud sync or actions due to issues with new sign-in.

#### 60-beta2
*2021-07-07*

* 🌟 Sign into Cloud with only email and password. With this update you are signed out of Cloud.
  When signing in again choose whether to switch to email and password or continue to use Google sign-in.
  If you use other devices that do not support Google sign-in, like Amazon Fire devices,
  you should choose email and password sign in.
* 🔧 When signed out of Cloud due to an error, will resume syncing without a full sync when signing
  in again with the same email address.
* 📝 Latest translations from crowdin.

#### 60-beta1
*2021-06-24*

* 🔨 Lists: do not jump to top on database changes.
* 📝 Errors: drop Countly reporting.

Version 59
----------
*2021-06-17*

* 🌟 Add language setting for person details.
* 🌟 Shows: add sort by status option.
* 🔧 Small design updates.
* 🔧 Widget: when displaying shows, exclude shows without next episode.  
* 🔨 Backup: export episodes even if a show was never opened.
* 🔨 Statistics: do not count canceled shows as with next episodes.

#### 59.1
*2021-06-23*

* 🔨 Person details: do not crash in landscape layout.

#### 59
*2021-06-17*

* 🔨 Backup: export episodes even if a show was never opened.

#### 59-beta3
*2021-06-16*

* 🌟 Add language setting for person details.
* 🔧 Add Esperanto translation for app.

#### 59-beta2
*2021-06-02*

* 🔧 If sorting by oldest, latest or remaining episode again sort by status.
* 🔧 Add upcoming range option to include shows with any future episode.
* 🔧 Widget: when displaying shows, never display shows without next episode.
* 🔨 Use correct theme on About screen.

#### 59-beta1
*2021-05-22*

* 🔧 Small design updates.
* 🔧 Sort shows: add new sort by status option. If by oldest, latest or remaining episode do no longer sort by status.
* 🔧 Filter shows: if excluding upcoming, exclude any with future next episode (ignoring upcoming range setting).
* 🔨 If lists Cloud migration fails due to unsaved list, require lists merge.
* 🌟 Statistics: add shows finished watching (all episodes watched, show is canceled or ended). Thanks to @ippschi!
* 🔨 Statistics: do not count canceled shows as with next episodes.
* 🔨 Fix Chinese, French, Spanish language variants.

Version 58
----------
*2021-04-15*

* 🌟 Switch show data source to themoviedb.org (TMDB). Shows need to update before all features,
  like Trakt or Cloud, can be used again. Shows or episodes not available on TMDB remain in your
  library until you remove that show, Trakt and Cloud features do no longer work with them.
* 🌟 Display most popular watch provider. Supports more regions. Powered by JustWatch via TMDb.
* 🔧 Lists can now only contain shows. Existing season and episode list items can only be removed.

#### 58.5
*2021-05-14*

* 🔨 Do not crash when generating list item IDs.

#### 58.4
*2021-05-13*

* 🔨 Fix issues when migrating list items that would prevent Cloud sync to succeed.
* 🔧 Ensure valid list item IDs when importing.
* 🔧 Import legacy season and episode list items again. If restored together with an old shows backup, those can still be displayed.

#### 58.3
*2021-05-12*

* 🔨 Fix issues when migrating list items that would prevent Cloud sync to succeed.

#### 58.2
*2021-05-01*

* 🔧 Display details for legacy season and episode list items.
* 🔨 Display any recently watched show from Trakt in history tab.

#### 58.1
*2021-04-21*

* 🔧 Update episodes not found on TMDb if they get added to TMDb at some later point, if the number matches.

#### 58
*2021-04-15*

* 🔨 Resolve crash when connecting to Trakt in some cases.
* 📝 Latest translations from crowdin.

#### 58-beta9
*2021-04-01*

* 🔧 The add show dialog is now sized bigger in most cases.
* 🔧 Move about screen up to more options.
* 📝 Latest translations from crowdin.

#### 58-beta8
*2021-03-26*

* 🔧 Occasionally suggest to support with a sub as apparently many users do not know it is possible.
* 📝 Latest translations from crowdin.

#### 58-beta7
*2021-03-19*

* 🔧 Lists again display (unsupported) seasons and episodes (as long as a show is not removed and re-added).
* 🔧 Display X Pass detected message, restore X Pass App button, sort subscription tiers by price.
* 📝 Add Twitter account link.
* 📝 Latest translations from crowdin.

#### 58-beta6
*2021-03-17*

* 🌟 Display most popular watch provider inside stream or purchase button. Support more regions. Powered by JustWatch via TMDb.
* 🔧 Do no longer remove episodes that are not on TMDB, instead display info message.
* 🔧 When filtering for continuing shows, include pilot and in production shows. Include canceled if excluding.
* 📝 Latest translations from crowdin.

#### 58-beta5
*2021-03-06*

* 🔨 Resolve (auto) backup failing if lists or movies are missing some properties.
* 🔨 Notification selection: show empty text only if list is empty.
* 📝 Latest translations from crowdin.

#### 58-beta4
*2021-03-05*

* 🔧 Allow adding show with duplicate TheTVDB ID if the TMDB ID is different. Background: TheTVDB
  recently started combining shows that are still separate on TMDB. TMDB may then link to the same TheTVDB ID.
* 🔨 Do not crash if show fallback response failed.
* 🔨 Limit episode search results to 500 to avoid crash.
* 🔧 Report auto backup errors.

#### 58-beta3
*2021-03-04*

* 🔧 Seasons and episodes that are not on TMDB are now removed from shows. This should be a better
  user experience, e.g. it avoids errors when setting watched on Trakt.
* 🔨 Resolve crash when downloading not watched/collected episode info from Cloud.
* 🔨 Do not fail update if a show can not be found on Trakt.

#### 58-beta2
*2021-03-03*

* 🔨 Resolve crash when adding show without run time info.
* 🔨 Resolve crash when downloading not watched/collected episode info from Cloud.

#### 58-beta1
*2021-03-03*

* 🌟 Show data is now powered by themoviedb.org (TMDB). Your shows need to update before some
  functionality, incl. Trakt or Cloud can be used again. Shows or episodes not available on TMDB
  currently remain in your library, but functionality is reduced (e.g. no Trakt or Cloud support).
* 🌟 Share a TMDB show url to SeriesGuide to add a show (support for TVDB URLs was removed).
* 🔧 Lists can now only contain shows. Note: use the backup tool to export your season and episode lists.
* 🔧 Allow longer check in messages.
* 📝 Latest translations from crowdin.

Version 57
----------
*2020-12-04*

* 🌟 For shows, add Portuguese (Brazil) to supported languages.
* 🔧 Detect locked Trakt accounts during sign-in.
* 🔧 Optimization for Android 11.

#### 57
*2020-12-04*

* 🔧 Always display hint about Cloud disabling Trakt features.
* 📝 Latest translations from crowdin.

#### 57-beta5
*2020-11-27*

* 🔧 Detect locked Trakt accounts during sign-in.
* 📝 Latest translations from crowdin.

#### 57-beta4
*2020-11-26*

* 🔧 Experiment with using self-hosted Countly instance to track some network errors.
* 📝 Latest translations from crowdin.

#### 57-beta3
*2020-11-06*

* 🔨 Resolve connection issues due to outdated security settings on some devices.

#### 57-beta2
*2020-10-29*

* 🔨 On Android 11 allow detection of X Pass.
* 🔧 Support Trakt API rate limiting.
* 📝 Latest translations from crowdin.

#### 57-beta1
*2020-10-23*

* 🌟 For shows, add Portuguese (Brazil) to supported languages.
* 🔧 Sort languages in selection dialog.
* 🔨 In movie details, display country if Portuguese is selected as language.
* 🔨 Restore feedback when tapping buttons at bottom, in rate dialog.
* 🔧 Optimization for Android 11 (this time for real).
* 📝 Latest translations from crowdin.

Version 56
----------
*2020-10-15*

* 🌟 Limited support for watching episodes and movies multiple times. Synced with Cloud or Trakt.
  Only available for supporters.
* 🌟 Metacritic search link for shows and movies. Note that only English titles get good results.
* 🔧 Display movies in collection in statistics.
* 🔧 Shorter English episode number formats by default (S01E01 -> S1:E1). The older formats are
  still available in Settings.

#### 56
*2020-10-15*

* 🔧 Upload multiple plays to Trakt during first sync (previously would only upload one).

#### 56-beta4
*2020-10-09*

* 🔧 Improvements to background tasks, billing.
* 🔨 Do not crash when loading movie with invalid release date.

#### 56-beta3
*2020-10-01*

* 🔧 Display number and share of movies in collection in statistics, drop redundant progress bar.
* 📝 Latest translations from crowdin.

#### 56-beta2
*2020-09-11*

* 🌟 Limited support for re-watching episodes and movies. When viewing an episode or movie, tap
  Watched and then Set watched to add another play. To keep things simple, SeriesGuide only keeps
  a count of plays. This is only available for supporters.
* 📝 When connecting Trakt, multiple plays are not uploaded (as Trakt keeps a watched at time,
  not sure how to handle this, yet).
* 📝 The JSON backup format now exports plays count for episodes and movies.
* 🔨 Potential fix for Trakt sign-in issues for some users.
* 🔧 Add additional reporting to help diagnose Trakt sign-in issues.
* 🔧 Add option to turn off sending crash and error reports.
* 🔨 Tapping the launcher icon to open the app now always returns to the previous screen.
* 🔨 Display movie info instead of nothing if sending movie action to Trakt fails.
* 📝 Latest translations from crowdin.

#### 56-beta1
*2020-08-07*

* 🌟 Metacritic search link for shows and movies. Note that only English titles get good results.
* 🔧 Shorter English episode number formats by default (S01E01 -> S1:E1). The older formats are still available in Settings.
* 🔧 Show details layout again always includes status, network and time.
* 🔧 Show overview multi-pane layout requires larger screen width in landscape.
  Most tall phones using gesture navigation should support it.
* 🔧 Update older movies more often (180 -> 90 days), this should resolve broken posters more quickly.
* 🔧 Fast scroller for watched movie list.
* 🔧 Link to battery settings/app info page from notification settings to make users aware of these system settings.
* 📝 Latest translations from crowdin.

Version 55
----------
*2020-07-16*

* 🌟 Movie search results, popular, digital and disc release lists now display all items.
* 🔧 More compact and cleaner statistics.
* 🔧 Small design and layout tweaks.
* 🔨 The app respects the system font size on Android 7 and older again.
* 🔨 Removed subscription expired notification.

#### 55.1
*2020-09-16*

* 🔨 Potential fix for Trakt sign-in issues for some users.
* 🔧 Add additional reporting to help diagnose Trakt sign-in issues.
* 🔨 Display movie info instead of nothing if sending movie action to Trakt fails.

#### 55
*2020-07-16*

* No changes.

#### 55-beta6
*2020-07-10*

* 🔧 Internal updates to Trakt history page, resolves rare crash.
* 🔧 Add more prominent link to full history, fast scroller to Trakt history page.
* 🔧 Display sync status and errors directly under More.
* 🔨 Shows list did not update despite next episodes changing.

#### 55-beta5
*2020-07-02*

* 🔧 Added fast scroller back to shows tab.
* 🔧 Fast scroller indicator should track finger position more closely, feel more precise.
* 📝 Latest translations from crowdin. Dropped Hindi, Latvian, Lithuanian and Slovenian due to
  largely incomplete translation.

#### 55-beta4
*2020-06-26*

* 📝 Make adjustments to meet Google Play requirements.

#### 55-beta3
*2020-06-25*

* 🔧 Update more text styles.
* 🔧 Episode, show and movie buttons display state if enabled instead of action. To display the action tap and hold the buttons as usual.
* 🔨 Fix and update style of some buttons.
* 🔨 Fix the app not respecting system font size on Android 7 and older.
* 📝 Latest translations from crowdin.

#### 55-beta2
*2020-05-20*

* 🔧 Refreshed show info layout, more compact ratings display.
* 🔧 Add remove action if there are no more episodes.
* 🔧 Drop sometimes misleading subscription expired notification, sometimes it is just a temporary
  error with Google Play.
* 🔨 Don't say sending to Cloud when changing a (Trakt) rating.

#### 55-beta1
*2020-05-08*

* 🌟 Movie search results, popular, digital and disc release lists are now (almost) endless.
* 🔧 More compact and cleaner statistics.
* 📝 Latest translations from crowdin.

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
