# Release notes

🌟 = New.  
🔧 = Improved or tweaked.  
🔨 = Resolved or fixed.  
📝 = Notable change.

Releases marked with 🧪 (or previously with the "beta" suffix) were released on
[the preview program](https://www.seriesgui.de/help/how-to/basics/preview) only.

## Version 2025.3

* 🔧 If the screen is wide enough, display a more compact bottom navigation bar.
* 🔧 Color the active item in the bottom navigation bar.
* 🔨 Correctly color the person placeholder symbol to match the current theme.

### 2025.3.1 - 2025-10-30 🧪

* 🔧 Thanks to a United States court ordered injunction in the Google Play Antitrust case coming 
  into effect, again display links to TMDB, Trakt and other external websites if your device region
  is set to "United States". Note that links remain displayed for devices set to a region in the
  European Economic Area. For all other regions, Google Play Payments policy continues to disallow
  this.
* 🔧 Make active tab indicator rounded to better fit with other rounded user interface elements.

### 2025.3.0 - 2025-10-24 🧪

* 📝 Require Android 6.0 or higher.
* 🔧 Upcoming/Recent: rename "Infinite calendar" option to the more applicable "Unlimited days".
* 🔧 Widget: note general item limit in configuration screen, rename "Infinite calendar" option to
  the more applicable "Unlimited days", move hide watch button option to appearance category.
* 🔧 On Android 16 on Samsung devices, increase the limit of widget items back to 100.
* 🔧 Shows: only display stream and purchase options specific to the season of a displayed episode.
* 🔧 Shows: update ended shows less often (90+ days) to speed up updating of large libraries and 
  reduce load on TMDB.
* 📝 Import latest user interface translations.

## Version 2025.2

* 🔧 Movie details: adjust buttons to be more similar to episodes, other minor improvements.
* 🔧 Discover: for consistency with movies, move search button back to app bar.
* 🔧 Backup: rename to "Export" and "Import", moved up to More screen, improvements to display, file
  naming and documentation.
* 🔧 Auto Backup: moved up to More screen.
* 🔧 Diagnostics: debug log can be turned on without restarting the app, saved to a file.
* 📝 2025.2 will be the last release available on devices running Android 5.0 and 5.1.
* 📝 Removed the unsupported release for devices running Android 4 from Google Play, it is no longer
  working properly.

### 2025.2.13 - 2025-10-30

* 🔧 Thanks to a United States court ordered injunction in the Google Play Antitrust case coming
  into effect, again display links to TMDB, Trakt and other external websites if your device region
  is set to "United States". Note that links remain displayed for devices set to a region in the
  European Economic Area. For all other regions, Google Play Payments policy continues to disallow
  this.
* 🔧 On Android 16 on Samsung devices, increase the limit of widget items back to 100.

### 2025.2.12 - 2025-09-19

* 🔨 On Android 16 on Samsung devices, correctly update the widget on changes.

### 2025.2.11 - 2025-09-11

* 🔨 On Android 16, when the widget updates, display the correct poster for items.
* 🔧 On Android 16, display at most 25 items on the widget to improve performance.
* 🔧 For widgets, also limit the number of displayed items when using the type "shows" to improve
  performance.

### 2025.2.10 - 2025-09-04

* 🔨 Scrolling upcoming and recent tabs should now actually no longer result in a crash in rare
  circumstances.

### 2025.2.9 - 2025-08-28

* 🔨 Make crashes when scrolling upcoming and recent tabs less likely.
* 📝 Import latest user interface translations. Significant updates to Hebrew thanks to yonatando!
  (Still no proper right-to-left support though.)

### 2025.2.8 - 2025-08-14

* 🔧 Movies: display rental options in stream and purchase dialog.
* 🔨 Movies: when viewing a collection the navigation bar no longer overlaps content on Android 10
  or older and the top app bar collapses when scrolling.
* 🔨 Cloud: sign in screens are no longer cut off on Android 15 and newer.
* 📝 Import latest user interface translations.

### 2025.2.7 - 2025-07-31

* 🔧 To comply with Google Play policies, remove links to third-party websites when needed.
* 🔧 Stream and purchase: display available providers inside the app.
* 📝 Import latest user interface translations.

### 2025.2.6 - 2025-07-24

* 🔧 Show, episode and movie details: add a "More information" header to external detail page links.
* 📝 Import latest user interface translations.

### 2025.2.5 - 2025-07-17 🧪

* 🔨 Cloud: correctly download skipped episodes when adding a previously removed show again.
* 🔧 App icon: remove shadow, increase background gradient to match style of many other apps.
* 🔨 Properly align status messages to the bottom navigation bar if using button navigation.
* 📝 Import latest user interface translations.

### 2025.2.4 - 2025-06-27 🧪

* 🔨 Import and Auto Backup: do not crash if reading file name fails.
* 🔧 Debug log: include system log (logcat) in log file.

### 2025.2.3 - 2025-06-26 🧪

* 🔧 Backup: rename to "Export" and "Import", move them and Auto Backup up to More screen.
* 🔧 Import: display names of selected files, select for import after selecting a file.
* 🔧 Auto Backup: display names of selected files.
* 🔧 Export: add timestamp to file names.
* 🔧 Diagnostics: debug log can be turned on without restarting the app.
* 🔧 Diagnostics: instead of sharing it, the debug log file can be saved to a custom location. This
  allows to view and share it in any way that is desired.
* 🔨 Properly align status messages to the bottom navigation bar.
* 🔨 Shows: when updating, also update season number of episodes (useful when importing minimal
  data).
* 📝 Import latest user interface translations.

### 2025.2.2 - 2025-05-22 🧪

* 🔨 Shows: correctly display initial tab indicator if there is an active show filter.
* 🔧 For external links, replace chain with open icon.
* 🔨 Trakt: potentially resolves frequently asking to reconnect to Trakt.

### 2025.2.1 - 2025-04-11 🧪

* 🔨 Widget: immediately update after syncing.
* 🔧 Discover: for consistency with movies, move search button back to app bar.
* 🔧 Movie details: for consistency with shows, display links and metacritic search as buttons.
* 🔧 Movie details: increase title on all, increase poster size on phone screens; display date and
  length in app bar.
* 🔧 Dialogs: highlight primary button in some dialogs for improved usability, in remove dialog
  make removing the secondary action.
* 🔧 Shows: display indicator on tab icon if a general or a stream and purchase filter is active.
* 🔧 Widget: update preview to include more items, remove old settings button.
* 🔧 Update message when there is no description to reflect that there can also be no details at
  all, not just a missing translation.
* 📝 Import latest user interface translations.
* 📝 Removed the unsupported release for devices running Android 4 from Google Play, it is no longer
  working properly.

### 2025.2.0 - 2025-03-27 🧪

* 🔨 Movies: when connecting Cloud or Trakt, movies might not have been added if downloading them
  failed. Any missing movies will be added on the next sync.
* 🔨 Movies: prefer translated title when viewing details.
* 🔧 Movies: at least display a more helpful error message when updating a movie fails because it
  was removed from TMDB.
* 📝 Devices running Android 5.0 and 5.1 will receive no more updates in the future.
* 📝 Import latest user interface translations.

## Version 2025.1

* 🔧 Trakt: improved error messages when an account limit is reached.
* 🔧 Trakt: add shortcut to view account limits to Connect Trakt screen.
* 📝 Add Latin Serbian user interface translation, thanks to kesaa89!

### 2025.1.2 - 2025-05-22

* 🔨 Trakt: potentially resolves frequently asking to reconnect to Trakt.

### 2025.1.1 - 2025-03-13

* 📝 Imported latest user interface translations.

### 2025.1.0 - 2025-02-27 🧪

* 🔧 Trakt: more general error message when the maximum number of something is reached.
* 🔧 Trakt: display error when uploading notes fails because the account limit of notes is reached.
* 🔧 Trakt: add shortcut to view account limits to Connect Trakt screen.
* 📝 Add Latin Serbian user interface translation, thanks to kesaa89!
* 📝 Imported latest user interface translations.

## Version 2024.5

* 🌟 Shows: add a note to a show, synced with SeriesGuide Cloud or Trakt (VIP only).
* 🌟 Movies: add link to all release dates.
* 🔧 Shows: increase resolution of episode images.

### 2024.5.4 - 2024-12-13

* 🔨 Trakt: retry if show not yet in Trakt profile failed to upload during initial sync.
* 📝 Latest user interface translations from Crowdin.

### 2024.5.3 - 2024-12-11 🧪

* 🔧 Shows: revert to search symbol for primary button on discover screen.
* 🔧 Lists: ask for confirmation before deleting a list, actually call it delete instead of "just"
  remove.
* 📝 Latest user interface translations from Crowdin.

### 2024.5.2 - 2024-12-04 🧪

* 🔧 Shows: when viewing the stream or purchase provider filters, the reset button is shown as
  disabled when no provider is selected. Also tabs are renamed and display icons to differentiate
  filter from sort options.
* 🔨 Show scrollbars for show filter and sort options.
* 🔧 Use common "Sort by" action name.
* 🔧 Android 15: turn predictive back animation back on after more issues are resolved.
* 📝 Latest user interface translations from Crowdin.

### 2024.5.1 - 2024-11-21 🧪

* 🌟 Shows: add a note to a show, synced with SeriesGuide Cloud or Trakt (VIP only).
* 🔧 Shows: increase resolution of episode images.
* 🔧 Shows: also use plus symbol for button on discover screen to be consistent.

### 2024.5.0 - 2024-11-06 🧪

* 🌟 Movies: add link to all release dates.
* 🔧 Overview: use local number format for absolute episode number.
* 📝 Latest user interface translations from Crowdin.

## Version 2024.4

* 🌟 Movies: display the collection (like a movie series) a movie is in, support to view the 
  collection.
* 🔧 Shows: display cast and crew of all seasons instead of just the latest one.
* 🔧 Episodes: in list view, can again set watched with one tap.
* 🔧 Display more options of list items on touch and hold and right click (for example in Samsung
  Dex) as well.

### 2024.4.6 - 2024-11-01

* 🔨 Trakt: do not crash when opening Connect Trakt screen on Android 6 or older.
* 📝 Latest user interface translations from Crowdin.

### 2024.4.5 - 2024-10-23

* 🔧 Trakt: use new logo.
* 📝 Latest user interface translations from Crowdin.

### 2024.4.4 - 2024-10-16 🧪

* 🔧 Episodes: in list view, can again set watched with one tap.
* 🔧 Calendar: add discoverable more options button on items, does the same thing as touch and hold.
* 🔧 Display more options of list items on touch and hold and right click (for example in Samsung
  Dex) as well.
* 🔧 Where touch and hold copies text, support right click to copy as well.
* 🔧 Add manual sync action back to show Upcoming, Recent tabs, new to lists and movie Watchlist,
  Collection, Watched tabs.
* 🔧 Seasons: move skipped and collection indicators to the right to work better with multi-line
  status text.
* 🔨 Show overview: do not display no spoiler warning if there is no image.
* 🔨 Consistently style more options menus.
* 🔨 Episodes: can use watched button also after dismissing the popup menu.
* 🔨 Consistently show tooltips for actions.
* 🔨 Discover: after (dis)connecting Trakt, Trakt features get hidden/shown on refresh.
* 📝 Latest user interface translations from Crowdin.

### 2024.4.3 - 2024-09-13 🧪

* 🔧 Show details: display full show title, touch and hold to copy it to clipboard (like for episode
  and movie titles).
* 🔧 Show details: move shortcut and search buttons to top buttons.
* 🔧 Overview/Episode details: add more obvious no spoiler warning where the image would be.
* 🔨 Android 14: turn off predictive back animation due to unresolved issues.
* 📝 Latest user interface translations from Crowdin.

### 2024.4.2 - 2024-08-09 🧪

* 🔧 Shows: display cast and crew of all seasons instead of just the latest one.
* 📝 Latest user interface translations from Crowdin.

### 2024.4.1 - 2024-08-07 🧪

* 🔨 Upcoming/Recent: do not crash when scrolling and data changes.
* 📝 Latest user interface translations from Crowdin.

### 2024.4.0 - 2024-08-02 🧪

* 🌟 Movies: display the collection (like a movie series) a movie is in, view the collection.
* 📝 Latest user interface translations from Crowdin.

## Version 2024.3

* 🔧 Shows: move Discover screen to top-level.
* 🌟 Shows: add permanent year and language filters to Discover tab.
* 🌟 Discover: add current year option to year filter.
* 🔧 Shows: display all search results, support to filter by release year.
* 🔧 Movies: move Watchlist next to Discover so order is similar to Shows screen.
* 🌟 Trakt: support to edit and delete (new) comments.
* 🌟 Trakt: support to remove a rating.

### 2024.3.8 - 2024-08-29

* 🔨 Android 14: turn off predictive back animation due to unresolved issues.
* 🔨 Backups: do not crash due to obscure errors when restoring.

### 2024.3.7 - 2024-08-16

* 🔨 Android 14: do not crash when updating from version 40 or older.

### 2024.3.6 - 2024-07-25

* 📝 This version was released only on the Amazon Appstore.
* 🔨 Amazon account and active purchases are correctly recognized again.

### 2024.3.5 - 2024-07-18

* 🔨 Widgets: content does not longer protrude from the widget.
* 🔧 More: add link to what's new.
* 🔧 Re-set remembered selected tab for shows, lists and movies.
* 📝 Latest user interface translations from Crowdin.

### 2024.3.4 - 2024-07-12 🧪

* 🔧 Android 15: turn on predictive back animation.
* 🌟 Shows: add permanent year and language filters to Discover tab.
* 🌟 Discover: add current year option to year filter.
* 📝 Latest user interface translations from Crowdin.

### 2024.3.3 - 2024-07-04 🧪

* 🔨 Calendar: do not crash when scrolling and data changes.
* 🔨 Images: on Android 7.0 or older, images load correctly again.
* 📝 Latest user interface translations from Crowdin.

### 2024.3.2 - 2024-06-14 🧪

* 🔧 Shows: move Discover tab first, drop add show action, add search action, rename Shows to Added.
* 🔧 Movies: move Watchlist next to Discover so order is similar to Shows screen.
* 🔧 Shows: restore search option on history and calendar tabs.
* 🔧 Show search: add Add show action, passes search query if there is one.
* 🔨 Discover: properly display no results or no connection messages.
* 🔨 Lists: hide set watched action when there is no next episode.
* 📝 Latest user interface translations from Crowdin.

### 2024.3.1 - 2024-06-07 🧪

* 🔧 Shows: move Discover screen to top-level.
* 🔧 Shows: display all search results, filter by release year.
* 🔧 Crew: display directors and writers first, then in order of job title.
* 📝 Latest user interface translations from Crowdin.

### 2024.3.0 - 2024-05-24 🧪

* 🌟 Trakt ratings: support removing a rating.
* 🌟 Trakt comments: edit and delete comments.
* 🔨 Trakt comments: refresh will actually show the latest comments, including after posting one.
* 📝 Latest user interface translations from Crowdin.

## Version 2024.2

* 🌟 Discover: add year and original language filters where possible.
* 🌟 Ratings: display TMDB rating for shows. Tap rating to open TMDB or Trakt page.
* 🔧 Display run time in hours and minutes.

### 2024.2.7 - 2024-07-05

* 🔨 Images: on Android 7.0 or older, images load correctly again.

### 2024.2.6 - 2024-05-17

* 🔨 Discover: on older versions of Android, do not crash when rotating the screen.
* 🔧 Discover: sort languages by top 5, then alphabetically.

### 2024.2.5 - 2024-05-10

* 🔧 Mark some supporter-only features with stars. Drop X Pass link, not longer available for new users.

### 2024.2.4 - 2024-05-03 🧪

* 🌟 Movies: add year and original language filter where possible for discover and search screens.
* 🔧 Search: hide keyboard after submitting query.

### 2024.2.3 - 2024-04-26 🧪

* 🌟 Ratings: display TMDB rating for shows and episodes. Tap rating to open TMDB or Trakt page.
* 📝 Latest user interface translations from Crowdin.
* 📝 Change to year-based versioning scheme.

### 73.0.2 - 2024-04-19 🧪

* 🌟 Discover: add new episodes screen with year and language filters, similar to popular shows screen.
* 🔧 Stream and purchase filter: sort providers by name.
* 🔧 History: sort Trakt friends by latest activity first.
* 🔨 Comments: do not display misleading "Could not modify data" message when commenting on a show.
* 📝 Latest user interface translations from Crowdin.

### 73.0.1 - 2024-04-12 🧪

* 🔧 Display run time in hours and minutes.
* 🔧 Calendar: avoid changing scroll position after viewing details or refreshing data.
* 📝 Latest user interface translations from Crowdin.

### 73.0.0 - 2024-04-05 🧪

* 🌟 Year and original language filter for popular shows screen.

## Version 72

* 🌟 Shows: stream or purchase filter. See which shows are available on a service.
  After updating it may take a day or so for filter options to appear.
* 🔧 Seasons: show total number of episodes (excluding specials), show indicator if episodes are in
  collection, show count of skipped and in collection.
* 🔧 Lists: quickly set next episode watched.
* 🔧 Episodes: allow to re-watch from episode list.
* 🔨 Overview: prevent skipping already watched episodes.
* 🔨 History: add entries when marking multiple episodes as watched.

### 72.0.8 - 2024-04-24

* 🔨 Resolve crash when viewing show or movie details and cast or crew members failed to load.

### 72.0.7 - 2024-03-22

* 🔨 Do not show copied to clipboard notification if Android already does.
* 🔨 Seasons: do not say all episodes are watched if all are skipped.
* 📝 Latest user interface translations from Crowdin.

### 72.0.6 - 2024-03-15 🧪

* 🔧 Seasons: different indicators if only some or all episodes are skipped or in collection.
* 🔧 Lists: add option to watch next episode, update to shows more options menu.
* 🔧 Lists: replace favorite with set next watched button to match Shows section.
* 🔨 Overview: prevent skipping already watched episodes which would remove existing number of times watched.
* 🔧 Episodes: allow to re-watch from episode list, integrate all options into watched button.
* 🔧 Overview: display "Special Episodes" instead of "Season 0" for the next episode as well.
* 📝 Latest user interface translations from Crowdin.

### 72.0.5 - 2024-03-08 🧪

* 🔧 Seasons: show total number of episodes (excluding specials), show indicator if episodes are in
  collection, show count of skipped and in collection.
* 🔧 Add history entry when marking multiple episodes as watched.
* 🔨 Android 5: use correct color for show status and stream search configure button.
* 📝 Latest user interface translations from Crowdin.

### 72.0.4 - 2024-02-29 🧪

* 🔧 Help: add link to new Discord server, make actions more understandable (like "Send email"
  instead of "Send feedback").
* 🔨 Stream or purchase filter: support using system colors on Android 12 and newer.

### 72.0.3 - 2024-02-29 🧪

* 🔨 Sync: properly handle interruptions by the system again.

### 72.0.2 - 2024-02-23 🧪

* 🔨 Stream or purchase filter: make it readable in dark mode.
* 📝 Latest user interface translations from Crowdin.

### 72.0.1 - 2024-02-23 🧪

* 🌟 Add stream or purchase filter for shows: see which shows are available on a service.
  After updating it may take a day or so for filter options to appear.
* 🔧 Discover: use check boxes for stream or purchase filter.
* 🌟 Movies: option to create a calendar event for to be released movies.

### 72.0.0 - 2024-02-09 🧪

* 🔧 Display a separate notification for each episode, including title and description if available
  and its own actions.
* 📝 Latest user interface translations from Crowdin.

## Version 71

* 🌟 Shows: link to trailers, if available.
* 🔨 Shows/Movies: repair Metacritic search links.
* 🔨 Shows: do not display all episodes are collected, if there are none collected. Also consider all
  episodes, not just released ones. This now matches with what episodes the collect all or none
  buttons change.

### 71.0.3 - 2024-01-12

* 🔧 Shows: adjust edit time dialog to avoid buttons moving around when changing values.
* 🔧 Shows: make switching between episode pages fast again.
* 🔨 Update Metacritic search links.
* 🔧 Add acknowledgement messages for actions that may take longer (that do network requests), drop
  them for actions that complete immediately (e.g. set watched).
* 📝 Latest user interface translations from Crowdin.

### 71.0.2 - 2023-12-15 🧪

* 🔨 Shows: do not consider all episodes collected, if there are none collected. Also consider all
  episodes, not just released ones. This now matches with what episodes the collect all or none
  buttons change.
* 📝 Latest user interface translations from Crowdin.

### 71.0.1 - 2023-12-07 🧪

* 🔧 Shows/Movies: move more secondary actions to the top, organize them in a chain.
* 🔧 People: use rounded images and placeholders.
* 🔨 Cloud: do not fail when uploading more than 500 new shows.
* 🔨 Show details: color source info with a readable color again.
* 📝 Latest user interface translations from Crowdin.

### 71.0.0 - 2023-11-17 🧪

* 🌟 Shows: support opening show trailer when adding a show and in details.

## Version 70

* 🌟 Movies: support displaying similar movies.
* 🔧 Switch to TMDBs recommendations when looking for similar shows or movies.
* 🔨 Android 14: fix automatic syncing.

### 70.0.3 - 2023-11-10

* 🔨 Movies: do not skip items when refreshing search results, popular, released or upcoming movies.

### 70.0.2 - 2023-11-03

* 🔨 Android 14: fix automatic syncing.
* 📝 Latest user interface translations from Crowdin.

### 70.0.1 - 2023-10-20 🧪

* 🔨 Get started: allow notification button now stays hidden after granting notification permission.
* 🔨 User interface: restore scrollbars wherever they were missing. Add fast scroller for lists and comments.
* 📝 Latest user interface translations from Crowdin.

### 70.0.0 - 2023-09-21 🧪

* 🌟 Movies: support displaying similar movies.
* 🔧 Switch to TMDBs recommendations when looking for similar shows or movies.

## Version 69

* 🔧 Streaming search: add latest supported countries.
* 🔧 Notifications: on Android 12 and newer, ask for permission to set precise alarm.
* 🔧 Trakt: add link to delete account.
* 🔧 Appearance: small adjustments.

### 69.0.3 - 2023-09-14

* 🔨 Appearance: to get a matching navigation bar color on Samsung devices use a non-transparent
  background on all devices.
* 🔧 Appearance: adjust light theme highlight color to be less washed out.
* 🔧 Trakt: add link to delete account.
* 📝 Latest user interface translations from Crowdin.

### 69.0.2 - 2023-08-18 🧪

* 🔧 Calendar: change calendar settings symbol from eye to a filter to hopefully be more intuitive.
* 🔧 Notifications: on Android 12 and newer, ask for permission to set precise alarm.
* 📝 Latest user interface translations from Crowdin.

### 69.0.1 - 2023-08-10 🧪

* 🔨 Do not apply corrections (e.g. for US time zones) on episodes when a custom time is set and the
  other way around.
* 📝 Latest user interface translations from Crowdin.

### 69.0.0 - 2023-08-04 🧪

* 🔧 Streaming search: add latest supported countries.
* 🔧 Cloud: do not automatically sign in (if e.g. signed in previously or on other devices).
* 🔧 Device backup: include the database when backing up to e.g. Google One. Only include auto
  backup files for device-to-device transfer (Android 12 and newer).
* 📝 Latest user interface translations from Crowdin.

## Version 68

* 🌟 Shows: support setting custom release time and day offset.
* 🔧 Use in-app browser only for websites related to the app.
* 🔧 Design: update to latest Material version.

### 68.0.6 - 2023-08-11

* 🔨 Do not apply corrections (e.g. for US timezones) on episodes when a custom time is set and the
  other way around.

### 68.0.5 - 2023-07-07

* 🔨 Movies: when removing movie from watchlist or collection actually remove it visually.
* 🔧 History: try to refresh watched episodes from Trakt after setting watched in other tabs.
* 🔧 History: display Trakt logo if history is fetched from there.

### 68.0.4 - 2023-06-30

* 🔨 Custom time: fix resetting, simplify dialog, suggest day offset matching time zone conversion
  of official time.
* 📝 Latest user interface translations from Crowdin.

### 68.0.3 - 2023-06-23 🧪

* 🔧 Show details: make release time edit button more obvious, re-arrange some things.
* 🔧 Overview: add button to edit release time.

### 68.0.2 - 2023-05-25 🧪

* 🔧 Design: update to latest Material version, with new time picker and new dialog animations.
* 📝 Latest user interface translations from Crowdin.

### 68.0.1 - 2023-05-19 🧪

* 🌟 Edit release time of a show: specify time and number of days earlier or later. Currently only
  from the details view, tap the pen symbol. Use this for shows that e.g. release later or earlier
  in your region.
* 📝 Latest user interface translations from Crowdin. Many updates to Traditional Chinese used in
  Taiwan, thanks jackiexyz!

### 68.0.0 - 2023-03-30 🧪

* 🔧 Use in-app browser only for websites related to the app.
* 📝 Latest user interface translations from Crowdin.

## Version 67

* 🔧 On Android 8 and newer, use system settings to configure notification settings.
* 🔧 On Android 13, ask to allow notifications.
* 🔧 Use in-app browser whenever possible to open websites.
* 🔧 Appearance: use card to separate details on large episodes and people screens.

### 67.0.3 - 2023-02-24

* 🔧 Trakt: show default browser in-app when signing in, drop integrated browser option.
* 🔧 Use in-app browser (Custom Tab) whenever possible to open websites.
* 📝 Latest user interface translations from Crowdin.

### 67.0.2 - 2023-02-16 🧪

* 🔧 Internal changes to ensure compatibility with Play Store billing.
* 📝 Latest user interface translations from Crowdin.

### 67.0.1 - 2023-02-03 🧪

* 🔧 On Android 8 and newer, use system settings to configure notification settings.
* 🔧 On Android 13 and newer, ask to allow notifications.
* 🔨 Do not open comments screen if required data is not available, yet.
* 📝 Latest user interface translations from Crowdin.

### 67.0.0 - 2023-01-27 🧪

* 🔧 Appearance: use card to separate details on large episodes and people screens.

## Version 66

* 🔧 Appearance updates.
* 🔧 Trakt: show special error messages if watchlist limit is exceeded.
* 🔧 Show filters: change option to remove all filters to instead restore defaults (exclude hidden).
* 🔨 Manage lists: fix an item getting removed from other lists if there are many lists.

### 66.0.7 - 2023-01-19

* 🔨 Do not crash if time zone of device is not known, report and default to "America/New_York" instead.
* 📝 Latest user interface translations from Crowdin.

### 66.0.6 - 2023-01-13 🧪

* 🔧 Trakt: show special error messages if account is locked or list limit exceeded.
* 📝 Latest user interface translations from Crowdin.

### 66.0.5 - 2022-12-21 🧪

* 🔧 Support adding shows when Trakt is down, use default values (e.g. for release time and time zone).
* 🔧 Trakt: add button to support with VIP; button to open website (or dashboard when signed in).
* 🔨 Do not crash when devices are using the renamed "Europe/Kyiv" or new "America/Ciudad_Juarez" time zone.
* 📝 Latest user interface translations from Crowdin.

### 66.0.4 - 2022-12-16 🧪

* 🔧 Appearance: draw behind navigation bar in movie details screen.

### 66.0.3 - 2022-12-10 🧪

* 🔨 Fix crash when loading movie watch history of Trakt friends and the device is offline.

### 66.0.2 - 2022-12-09 🧪

* 🔧 Appearance: draw behind status and navigation bar whenever possible.
* 🔧 Show filters: change option to remove all filters to instead restore defaults (exclude hidden).
* 🔨 Fix network detection on some Android 11 devices.
* 📝 Latest user interface translations from Crowdin.

### 66.0.1 - 2022-10-21 🧪

* 🔨 Fix crash when resizing the app while on the episodes screen.

### 66.0.0 - 2022-10-20 🧪

* 🔨 Manage lists: fix an item getting removed from other lists if there are many lists.
* 📝 Latest user interface translations from Crowdin.

## Version 65

* 🔧 Clean up supported languages for show and movie content. Languages not longer supported are
  changed to US English.
* 🔧 When making changes to a season, always apply to all episodes. Previously, e.g. set watched
  only affected episodes with a past release date, but add to collection affects all.
* 🔧 Streaming search: add latest supported countries.
* 🔧 Android 13: support per-app language setting.

### 65.0.5 - 2022-08-26

* 🔧 Support "Themed icons" beta feature of Android 13.
* 🔧 Support per-app language setting of Android 13.
* 📝 Latest user interface translations from Crowdin.

### 65.0.4 - 2022-08-19 🧪

* 🔧 Add latest supported countries to streaming search.
* 🔧 Restored subscription expired notification as in-app message.
* 📝 Latest user interface translations from Crowdin.

### 65.0.3 - 2022-08-12 🧪

* 🔧 When making changes to a season, always apply to all episodes. Previously, setting watched or
skipped only affected episodes with a release date and released in the past. But adding to/removing
from collection affected all.
* 🔧 When updating shows, do not fail if just Trakt info can not be updated.

### 65.0.2 - 2022-08-05 🧪

* 🔨 Trakt: prevent adding a duplicate play in the rare case a play was already sent and stored at
  Trakt, but no confirmation was received.
* 🔨 Movies: instead of a future date, show no date if release date is unknown in watched movies list.

### 65.0.1 - 2022-07-22 🧪

* 🔧 Clean up supported languages for show and movie content. Languages not longer supported are
changed to US English. As a side-effect should resolve issues with translations switching back to
English at random.
* 📝 Latest user interface translations from Crowdin.

### 65.0.0 - 2022-05-27 🧪

* 🔧 Choose newest unwatched episode as next when adding a show that has watched episodes, e.g.
after signing into Cloud on a new device.

## Version 64

* 🔧 Show filters: move no released episodes option there, display status also as text.
* 🔧 Clearly label the next episode to watch in the overview screen.
* 🔧 Display the last time a show or movie was updated.
* 🔨 Do not crash if permission to set alarms and reminders has been removed (Android 12), schedule
inexact episode notifications instead.

### 64.0.7 - 2022-05-18

* 🔨 Resolve Android not responding issue when viewing a show or its episodes.

### 64.0.6 - 2022-05-15 🧪

* 🔨 Do not crash when selecting backup file to restore from.

### 64.0.5 - 2022-05-13 🧪

* 🔨 Do not crash if permission to set alarms and reminders has been removed, schedule inexact
  episode notifications instead.
* 🔧 Improve when the app wakes the device to notify about upcoming episodes.
* 📝 Latest user interface translations from Crowdin.

### 64.0.4 - 2022-05-06 🧪

* 🔨 Movies: properly clean up movies watched by friends data on refresh.
* 📝 Latest user interface translations from Crowdin.

### 64.0.3 - 2022-04-14 🧪

* 🔧 Clearly label next episode to watch in show overview screen.
* 🔧 Improve updating of shows, on failure display exact service that failed, better suggestions.
* 🔧 Display the last time a show or movie (if added to a list) was updated.
* 🔨 Movie details screen: always display the oldest theatrical release date, matching other screens.
* 📝 Latest user interface translations from Crowdin.

### 64.0.2 - 2022-03-10 🧪

* 🔨 Cloud: when signing in do not add shows that were removed after the TMDB migration.
* 🔨 Fixed: when first tapping any episode in a season always displays the first episode.
* 🔨 Fixed: upcoming/recent does not update over time.
* 🔨 Fixed: guest stars, writers, etc. not separated by comma.
* 📝 Latest user interface translations from Crowdin.

### 64.0.1 - 2022-03-03 🧪

* 🔧 Move no aired episodes option to shows filter view.
* 🔧 Display state of show filters also as text.
* 🔧 Rephrase TMDB migration info and suggest action (search for a replacement).
* 🔧 Create first list when installing app instead of when visiting lists screen.
* 🔧 Various small design tweaks.
* 🔨 Display error if managing lists of a show is not possible.
* 📝 Latest user interface translations from Crowdin.

## Version 63

* 🌟 Some design updates to fit with latest Material style.
* 🌟 Add setting to use system colors on Android 12.
* 🔨 Sort by title properly handles characters with accents.

### 63.1.0 - 2022-03-11

* 🔨 Cloud: when signing in do not add shows that were removed after the TMDB migration.
* 🔨 Fixed: when first tapping any episode in a season always displays the first episode.

### 63-beta4 - 2022-02-10

* 🌟 Appearance setting to use system colors on Android 12.
* 📝 Latest user interface translations from Crowdin.

### 63-beta3 - 2022-02-04

* 🔨 Fix crash when using email sign-in on Android 12.
* 🔨 Fix missing state description for show filters (accessibility).
* 📝 Latest user interface translations from Crowdin.

### 63-beta2 - 2022-02-03

* 🌟 Some design updates to fit with latest Material style.
* 📝 Latest user interface translations from Crowdin.

### 63-beta1 - 2022-01-21

* 🔧 Move movie trailer button right below title to make it easier to discover.
* 🔨 Sort by title properly handles characters with accents.
* 🔨 Display correct colors if check-in dialog is launched from notification.
* 📝 Optimization for Android 12.
* 📝 Latest translations from Crowdin.

## Version 62

* 🌟 Discover: filter popular shows and shows with new episodes by streaming service.
* 🌟 Discover: filter popular movies and movies with digital release by streaming service.
* 🔧 App widget: add option to hide watch button.

### 62.2 - 2021-12-21

* 🔨 Potential fix for crash when updating next episode on Android 5.1 devices.
* 🔨 Fix Amazon extension search link.

### 62.1 - 2021-12-16

* 🔨 Potential fix for crash when updating next episode on Android 5.1 devices.
* 🔨 Amazon version: do not crash when re-launching the app.
* 📝 Latest translations from Crowdin.

### 62 - 2021-12-09

* 📝 Add Welsh translation, thanks to NMulholland.

### 62-beta5 - 2021-12-03

* 🔧 Discover: highlight new show and movie filters.
* 🔨 Do not crash when selecting backup files and no supported file picker is available.
* 🔨 Do not display options in toolbar not applicable when first opening some screens.
* 📝 Latest translations from Crowdin.

### 62-beta4 - 2021-11-25

* 🌟 Discover: filter popular shows and shows with new episodes by streaming service.
* 🌟 Discover: filter popular movies and movies with digital release by streaming service.
* 🔧 Episode search: match independent of case for Unicode characters, e.g. German umlauts.
* 📝 Latest translations from Crowdin.

### 62-beta3 - 2021-10-20

* 🔧 Revert the next episode selection change, because Anime. The next episode is again selected
  by release date. Note: to never choose specials as next, see More > Settings > Advanced.
* 📝 Latest translations from Crowdin.

### 62-beta2 - 2021-10-13

* 🔧 The next episode is now selected based on number, no longer by release date. This means specials
  released in between episodes will no longer appear as next, you'll have to look for them in the
  specials season. Decided to change this based on feedback over the years as many were confused
  why an apparently random episode was next e.g. after marking all specials watched/skipped.
* 🔧 Modernize remaining pager components (e.g. episode tabs, movie tabs) for more reliable behavior.
* 📝 Latest translations from Crowdin.

### 62-beta1 - 2021-09-30

* 🔧 App widget: add option to hide watch button.
* 🔧 Display error message if creating a shortcut fails.
* 🔨 Only move shows by a day if released in the hour past midnight for the CBS and NBC networks.

## Version 61

* 🌟 App widget: add system theme that supports dark mode, colors on Android 12. Modernize existing themes.
* 🌟 App widget: add watch button.
* 🌟 Add upcoming movies screen.
* 🔧 Correctly choose the next episode if episodes are watched multiple times. This may change the next episode for some shows. Just set an episode watched to update.

### 61 - 2021-09-23

* 🔨 Do not crash episode list if number of episodes and selection changes.
* 📝 Latest translations from Crowdin.

### 61-beta5 - 2021-09-16

* 🌟 App widget: add system theme that supports dark mode, colors on Android 12.
* 🔧 App widget: modernize theme, update preview.
* 🔧 App widget: use system reconfigure button on Android 12.
* 📝 Latest translations from Crowdin.

### 61-beta4 - 2021-09-10

* 🔧 Finally add fast scroller to episode list.
* 📝 Latest translations from Crowdin.

### 61-beta3 - 2021-09-03

* 🌟 Add watch button to app widget.
* 🌟 Add upcoming movies screen.
* 🔨 Set a show not watched actually does something.
* 📝 Latest translations from Crowdin.

### 61-beta2 - 2021-08-12

* 🔨 Do not select skipped episode as next to watch.
* 🔨 Cloud: on sync update last watched episode to correctly determine next to watch on all devices.
* 🔨 Cloud: do not fail when uploading a lot of movies.
* 📝 Latest translations from Crowdin.

### 61-beta1 - 2021-08-04

* 🌟 When re-watching an episode, correctly choose the next one to rewatch. Display watch count in overview screen.
* 🔨 Prevent crash when removing show while it was getting updated.
* 🔧 Drop unused storage permissions, should have no impact on supported devices.
* 📝 Latest translations from Crowdin.

## Version 60

* 🌟 Support signing into Cloud with email and password. With this update you are asked to sign into Cloud again.
  Choose email if you use other devices that do not support Google sign-in, like Amazon devices.
* 🔧 An episode IMDb link will open the episode page again, if the IMDb ID is available on TMDb.  

### 60 - 2021-07-28

* 🔧 Auto-dismiss support the dev message.

### 60-beta4 - 2021-07-21

* 🔧 An episode IMDb link will open the episode page again, if the IMDb ID is available on TMDb.
* 🔨 Fix duplicate error string when adding a show fails.
* 📝 Latest translations from Crowdin.

### 60-beta3 - 2021-07-08

* 🔨 Do not crash during Cloud sync or actions due to issues with new sign-in.

### 60-beta2 - 2021-07-07

* 🌟 Sign into Cloud with only email and password. With this update you are signed out of Cloud.
  When signing in again choose whether to switch to email and password or continue to use Google sign-in.
  If you use other devices that do not support Google sign-in, like Amazon Fire devices,
  you should choose email and password sign in.
* 🔧 When signed out of Cloud due to an error, will resume syncing without a full sync when signing
  in again with the same email address.
* 📝 Latest translations from crowdin.

### 60-beta1 - 2021-06-24

* 🔨 Lists: do not jump to top on database changes.
* 📝 Errors: drop Countly reporting.

## Version 59

* 🌟 Add language setting for person details.
* 🌟 Shows: add sort by status option.
* 🔧 Small design updates.
* 🔧 Widget: when displaying shows, exclude shows without next episode.  
* 🔨 Backup: export episodes even if a show was never opened.
* 🔨 Statistics: do not count canceled shows as with next episodes.

### 59.1 - 2021-06-23

* 🔨 Person details: do not crash in landscape layout.

### 59 - 2021-06-17

* 🔨 Backup: export episodes even if a show was never opened.

### 59-beta3 - 2021-06-16

* 🌟 Add language setting for person details.
* 🔧 Add Esperanto translation for app.

### 59-beta2 - 2021-06-02

* 🔧 If sorting by oldest, latest or remaining episode again sort by status.
* 🔧 Add upcoming range option to include shows with any future episode.
* 🔧 Widget: when displaying shows, never display shows without next episode.
* 🔨 Use correct theme on About screen.

### 59-beta1 - 2021-05-22

* 🔧 Small design updates.
* 🔧 Sort shows: add new sort by status option. If by oldest, latest or remaining episode do no longer sort by status.
* 🔧 Filter shows: if excluding upcoming, exclude any with future next episode (ignoring upcoming range setting).
* 🔨 If lists Cloud migration fails due to unsaved list, require lists merge.
* 🌟 Statistics: add shows finished watching (all episodes watched, show is canceled or ended). Thanks to @ippschi!
* 🔨 Statistics: do not count canceled shows as with next episodes.
* 🔨 Fix Chinese, French, Spanish language variants.

## Version 58

* 🌟 Switch show data source to themoviedb.org (TMDB). Shows need to update before all features,
  like Trakt or Cloud, can be used again. Shows or episodes not available on TMDB remain in your
  library until you remove that show, Trakt and Cloud features do no longer work with them.
* 🌟 Display most popular watch provider. Supports more regions. Powered by JustWatch via TMDb.
* 🔧 Lists can now only contain shows. Existing season and episode list items can only be removed.

### 58.5 - 2021-05-14

* 🔨 Do not crash when generating list item IDs.

### 58.4 - 2021-05-13

* 🔨 Fix issues when migrating list items that would prevent Cloud sync to succeed.
* 🔧 Ensure valid list item IDs when importing.
* 🔧 Import legacy season and episode list items again. If restored together with an old shows backup, those can still be displayed.

### 58.3 - 2021-05-12

* 🔨 Fix issues when migrating list items that would prevent Cloud sync to succeed.

### 58.2 - 2021-05-01

* 🔧 Display details for legacy season and episode list items.
* 🔨 Display any recently watched show from Trakt in history tab.

### 58.1 - 2021-04-21

* 🔧 Update episodes not found on TMDb if they get added to TMDb at some later point, if the number matches.

### 58 - 2021-04-15

* 🔨 Resolve crash when connecting to Trakt in some cases.
* 📝 Latest translations from crowdin.

### 58-beta9 - 2021-04-01

* 🔧 The add show dialog is now sized bigger in most cases.
* 🔧 Move about screen up to more options.
* 📝 Latest translations from crowdin.

### 58-beta8 - 2021-03-26

* 🔧 Occasionally suggest to support with a sub as apparently many users do not know it is possible.
* 📝 Latest translations from crowdin.

### 58-beta7 - 2021-03-19

* 🔧 Lists again display (unsupported) seasons and episodes (as long as a show is not removed and re-added).
* 🔧 Display X Pass detected message, restore X Pass App button, sort subscription tiers by price.
* 📝 Add Twitter account link.
* 📝 Latest translations from crowdin.

### 58-beta6 - 2021-03-17

* 🌟 Display most popular watch provider inside stream or purchase button. Support more regions. Powered by JustWatch via TMDb.
* 🔧 Do no longer remove episodes that are not on TMDB, instead display info message.
* 🔧 When filtering for continuing shows, include pilot and in production shows. Include canceled if excluding.
* 📝 Latest translations from crowdin.

### 58-beta5 - 2021-03-06

* 🔨 Resolve (auto) backup failing if lists or movies are missing some properties.
* 🔨 Notification selection: show empty text only if list is empty.
* 📝 Latest translations from crowdin.

### 58-beta4 - 2021-03-05

* 🔧 Allow adding show with duplicate TheTVDB ID if the TMDB ID is different. Background: TheTVDB
  recently started combining shows that are still separate on TMDB. TMDB may then link to the same TheTVDB ID.
* 🔨 Do not crash if show fallback response failed.
* 🔨 Limit episode search results to 500 to avoid crash.
* 🔧 Report auto backup errors.

### 58-beta3 - 2021-03-04

* 🔧 Seasons and episodes that are not on TMDB are now removed from shows. This should be a better
  user experience, e.g. it avoids errors when setting watched on Trakt.
* 🔨 Resolve crash when downloading not watched/collected episode info from Cloud.
* 🔨 Do not fail update if a show can not be found on Trakt.

### 58-beta2 - 2021-03-03

* 🔨 Resolve crash when adding show without run time info.
* 🔨 Resolve crash when downloading not watched/collected episode info from Cloud.

### 58-beta1 - 2021-03-03

* 🌟 Show data is now powered by themoviedb.org (TMDB). Your shows need to update before some
  functionality, incl. Trakt or Cloud can be used again. Shows or episodes not available on TMDB
  currently remain in your library, but functionality is reduced (e.g. no Trakt or Cloud support).
* 🌟 Share a TMDB show url to SeriesGuide to add a show (support for TVDB URLs was removed).
* 🔧 Lists can now only contain shows. Note: use the backup tool to export your season and episode lists.
* 🔧 Allow longer check in messages.
* 📝 Latest translations from crowdin.

## Version 57

* 🌟 For shows, add Portuguese (Brazil) to supported languages.
* 🔧 Detect locked Trakt accounts during sign-in.
* 🔧 Optimization for Android 11.

### 57 - 2020-12-04

* 🔧 Always display hint about Cloud disabling Trakt features.
* 📝 Latest translations from crowdin.

### 57-beta5 - 2020-11-27

* 🔧 Detect locked Trakt accounts during sign-in.
* 📝 Latest translations from crowdin.

### 57-beta4 - 2020-11-26

* 🔧 Experiment with using self-hosted Countly instance to track some network errors.
* 📝 Latest translations from crowdin.

### 57-beta3 - 2020-11-06

* 🔨 Resolve connection issues due to outdated security settings on some devices.

### 57-beta2 - 2020-10-29

* 🔨 On Android 11 allow detection of X Pass.
* 🔧 Support Trakt API rate limiting.
* 📝 Latest translations from crowdin.

### 57-beta1 - 2020-10-23

* 🌟 For shows, add Portuguese (Brazil) to supported languages.
* 🔧 Sort languages in selection dialog.
* 🔨 In movie details, display country if Portuguese is selected as language.
* 🔨 Restore feedback when tapping buttons at bottom, in rate dialog.
* 🔧 Optimization for Android 11 (this time for real).
* 📝 Latest translations from crowdin.

## Version 56

* 🌟 Limited support for watching episodes and movies multiple times. Synced with Cloud or Trakt.
  Only available for supporters.
* 🌟 Metacritic search link for shows and movies. Note that only English titles get good results.
* 🔧 Display movies in collection in statistics.
* 🔧 Shorter English episode number formats by default (S01E01 -> S1:E1). The older formats are
  still available in Settings.

### 56 - 2020-10-15

* 🔧 Upload multiple plays to Trakt during first sync (previously would only upload one).

### 56-beta4 - 2020-10-09

* 🔧 Improvements to background tasks, billing.
* 🔨 Do not crash when loading movie with invalid release date.

### 56-beta3 - 2020-10-01

* 🔧 Display number and share of movies in collection in statistics, drop redundant progress bar.
* 📝 Latest translations from crowdin.

### 56-beta2 - 2020-09-11

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

### 56-beta1 - 2020-08-07

* 🌟 Metacritic search link for shows and movies. Note that only English titles get good results.
* 🔧 Shorter English episode number formats by default (S01E01 -> S1:E1). The older formats are still available in Settings.
* 🔧 Show details layout again always includes status, network and time.
* 🔧 Show overview multi-pane layout requires larger screen width in landscape.
  Most tall phones using gesture navigation should support it.
* 🔧 Update older movies more often (180 -> 90 days), this should resolve broken posters more quickly.
* 🔧 Fast scroller for watched movie list.
* 🔧 Link to battery settings/app info page from notification settings to make users aware of these system settings.
* 📝 Latest translations from crowdin.

## Version 55

* 🌟 Movie search results, popular, digital and disc release lists now display all items.
* 🔧 More compact and cleaner statistics.
* 🔧 Small design and layout tweaks.
* 🔨 The app respects the system font size on Android 7 and older again.
* 🔨 Removed subscription expired notification.

### 55.1 - 2020-09-16

* 🔨 Potential fix for Trakt sign-in issues for some users.
* 🔧 Add additional reporting to help diagnose Trakt sign-in issues.
* 🔨 Display movie info instead of nothing if sending movie action to Trakt fails.

### 55 - 2020-07-16

* No changes.

### 55-beta6 - 2020-07-10

* 🔧 Internal updates to Trakt history page, resolves rare crash.
* 🔧 Add more prominent link to full history, fast scroller to Trakt history page.
* 🔧 Display sync status and errors directly under More.
* 🔨 Shows list did not update despite next episodes changing.

### 55-beta5 - 2020-07-02

* 🔧 Added fast scroller back to shows tab.
* 🔧 Fast scroller indicator should track finger position more closely, feel more precise.
* 📝 Latest translations from crowdin. Dropped Hindi, Latvian, Lithuanian and Slovenian due to
  largely incomplete translation.

### 55-beta4 - 2020-06-26

* 📝 Make adjustments to meet Google Play requirements.

### 55-beta3 - 2020-06-25

* 🔧 Update more text styles.
* 🔧 Episode, show and movie buttons display state if enabled instead of action. To display the action tap and hold the buttons as usual.
* 🔨 Fix and update style of some buttons.
* 🔨 Fix the app not respecting system font size on Android 7 and older.
* 📝 Latest translations from crowdin.

### 55-beta2 - 2020-05-20

* 🔧 Refreshed show info layout, more compact ratings display.
* 🔧 Add remove action if there are no more episodes.
* 🔧 Drop sometimes misleading subscription expired notification, sometimes it is just a temporary
  error with Google Play.
* 🔨 Don't say sending to Cloud when changing a (Trakt) rating.

### 55-beta1 - 2020-05-08

* 🌟 Movie search results, popular, digital and disc release lists are now (almost) endless.
* 🔧 More compact and cleaner statistics.
* 📝 Latest translations from crowdin.

## Version 54

* 🌟 Switch to bottom navigation bar.
* 🔧 Auto backups are created in an app specific directory, no longer requiring any setup. For most
  users it will be backed up by Android (Android 6+, up to 25 MB total) and can be restored from
  after re-installing the app.
* 🔧 When connecting Trakt, do not clear movies that are only watched on the device. Instead upload
  them. Trakt will set them as watched on their release date.
* 🔨 Support adding and updating shows without episodes (e.g. upcoming shows).
* 🔧 Add JustWatch Turkey to streaming search links.

### 54 - 2020-04-30

* 📝 Latest translations from crowdin.

### 54-beta4 - 2020-04-24

* 🔧 Add JustWatch Turkey to streaming search links.
* 🔧 Tapping a bottom nav item now scrolls the visible list to the top.
* 🔨 Fix widget crashing if an item has no poster.
* 🔨 Fix crash when changing calendar settings in some situations.
* 🔨 Fix crash when pinning shortcut in some situations.
* 🔨 Fix crash if external storage is not available to read auto backups from.

### 54-beta3 - 2020-04-03

* 🔧 Move community and translation links to More from Settings.
* 🔧 Less bright empty message icons on dark theme.
* 🔨 Fix conflict that prevented side-by-side installation of the Amazon and Play Store version.
* 🔨 Correctly color add all icon on Trakt lists screens.
* 🔧 A bunch of internal improvements.
* 📝 Latest translations from crowdin.

### 54-beta2 - 2020-03-26

* 🌟 Replaced the navigation drawer with an easier to use and discover bottom navigation bar.
* 🔧 Removed unlock and services settings that are now shown under More.
* 🔧 When connecting Trakt, do not clear movies that are only watched on the device, instead upload
  them. Trakt will set them as watched on their release date.
* 🔨 Support adding and updating shows without episodes (e.g. upcoming shows).
* 🔨 The last auto backup date was off by a month.
* 📝 Latest translations from crowdin.

### 54-beta1 - 2020-03-20

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

## Version 53

* New Dark and Light app and widget theme.
* By default, set app theme based on system setting (Android 10) or by Battery Saver (Android 9 and older).
* New notification option to only notify if the new episode is the next episode to watch.

### 53 - 2020-03-12

* 🔨 In some cases when backing up and the new backup is smaller,
  the resulting JSON might be corrupted.
* 🔧 If a show or movie failed to update, display which one (see Trakt/Cloud screens).
* 📝 Latest translations from crowdin.

### 53-beta5 - 2020-03-05

* 🔧 Replace compass with link icon for movie links option.
* 🔧 Display country for Portuguese variants when selecting movie language.
* 🔨 Use less bright selected state for people list as well.
* 🔨 Restore icon for add to home screen button.
* 🔨 Crash when a movie result does not exist.
* 📝 Latest translations from crowdin.

### 53-beta4 - 2020-02-20

* 🌟 Notifications: option to only notify if the new episode is the next episode to watch.
* 🔧 Add link to release announcements from app update notification.
* 🔨 Fix colors in debug view.
* 📝 Latest translations from crowdin.

### 53-beta3 - 2020-02-13

* 🌟 New Dark and Light widget themes replace old themes, with more compact and less colorful header.
* 🔧 Widgets: prevent setting only premieres option if displaying shows, it has no effect.
* 🔨 Crash when using the new backup agent.
* 🔨 Crash when receiving malformed response from Cloud.
* 🔨 List add and edit dialog text box not full width.
* 📝 Latest translations from crowdin.

### 53-beta2 - 2020-02-07

* 🌟 New Dark and Light theme replace old themes. By default theme is chosen by system setting
  (Android 10) or depending on Battery Saver being active (Android 9 and older). Set the theme
  permanently to Dark or Light in Settings.
* 📝 The theme update is still incomplete (e.g. widgets) or might be broken on some devices. Let me know!
* 🔧 Confirm set all episodes up to here watched.
* 🔧 On Android 6 and newer improve system app data backup by only including settings.
* 📝 Latest translations from crowdin.

### 53-beta1 - 2020-01-10

* 🔧 Fetch images from new TheTVDB artworks subdomain, provide fall back for old image links.
* 🔨 Episodes screen may crash in certain situations.
* 🔨 Background work may crash on some devices in certain situations.
* 📝 Latest translations from crowdin.

## Version 52

* 🌟 Calendar: add option to only display premieres.
* 🔧 Episodes: button to set all episodes watched up to (including) the current one.
* 🔧 Episodes: on phones, combine list and page view, add switch view button.
* 🔧 Discover: also use improved search by TheTVDB.com when set to English.
* 🔧 Discover: drop any language option, just type a show title in any language to get a match.

### 52 - 2019-12-05

* 📝 Latest translations from crowdin.

### 52-beta5 - 2019-11-28

* 🔧 Add new languages supported by TheTVDB.com.
* 📝 Add more translations of the new description on Play Store. Thanks to all translators!
* 📝 Latest translations from crowdin.

### 52-beta4 - 2019-11-21

* 🔧 Switch English language show search to the new and improved search by TheTVDB.com.
* 🔧 Also drop any languages option. Just enter a show title in any language to get a match.
* 🔧 Discover: add Trakt logo to links connected to current Trakt profile.
* 📝 Latest translations from crowdin. Now including Hindi thanks to a new translator!

### 52-beta3 - 2019-11-15

* 🔧 Episode view remembers if season was last viewed as list, goes back to list if page was shown
  by tapping on list.
* 🔨 Correctly tint switch view icon on light theme.
* 🔨 Resolve crash when opening episodes view.
* 📝 Latest translations from crowdin.

### 52-beta2 - 2019-11-08

* 🔧 On phones, combine episode list and page view into one.
  Switch between them with a button in the top right.
* 🔧 Move episode share, add to calendar and manage lists buttons to bottom of screen.
* 🔨 Watched up to here no longer marks unreleased episodes watched.
* 🔨 In debug mode, log show TheTVDB ID if it fails to update.
* 🔨 Do not crash on backing up if file provider has issues.
* 📝 Latest translations from crowdin.

### 52-beta1 - 2019-10-31

* 🌟 Calendar: add option to only display premieres (first episodes).
* 🔧 Show overview: if there is no next episode, suggest to look for similar shows.
* 🔧 Episode details: button to set all episodes watched up to (including) the current one.
* 📝 Latest translations from crowdin.

## Version 51

* 🌟 Display similar shows from the show details dialog and screen. Powered by themoviedb.org!
* 🔧 Display streaming search in show details dialog, if it was configured.
* 🔧 Move advanced settings up to the first settings section.
* 🔧 Remove DashClock extension, DashClock has been unpublished for a long time.
* 🔧 Allow users to enable debug mode, for example to share log output.

### 51 - 2019-10-02

* 🔨 Do not crash when trying to display details for a show not existing on TheTVDB.com.
* 🔨 Do not crash if there is no app available to select notification sound.
* 📝 Latest translations from crowdin.

### 51-beta6 - 2019-09-26

* 🔧 Show a close instead of an up button for screens that have no parent screen.
* 🔧 Migrate widget settings and Amazon extension settings to new implementation.
* 🔧 Allow users to enable debug mode, for example to share log output.
* 🔨 Do not show movie history tab at wrong position after connecting trakt. Wait until the movies
  section is left and visited again.
* 📝 Latest translations from crowdin.

### 51-beta5 - 2019-09-20

* 🌟 Display similar shows from the show details dialog and screen. Powered by themoviedb.org!
* 🔧 Display streaming search in show details dialog if it was configured.
* 📝 Latest translations from crowdin.

### 51-beta4 - 2019-09-14

* 🔨 Add movies to watchlist, collection or watched in all cases when syncing with trakt or Cloud.
  On upgrading to this version the next sync will add missing movies.
* 🔧 Remove DashClock extension, DashClock has been unpublished for a long time.
* 🔧 Switch settings to new underlying implementation.
* 🔧 Move basic settings link up to the first section, rename it to Advanced.

### 51-beta3 - 2019-09-04

* 🔨 Resolve connection issues with TheTVDB and trakt.

### 51-beta2 - 2019-08-30

* 🔨 Do not crash when viewing an episode and there is no show title or poster.

### 51-beta1 - 2019-08-29

* 🔧 Fetch show small poster path instead of constructing it, to future proof for upcoming changes at TheTVDB.com.
* 🔨 Fix discover screen displaying shows that can not be added.
* 🔨 Fix the subscriptions screen displaying a developer error in some cases.

## Version 50

* 🌟 Add Sponsor and Supporter subscriptions. If you can or want to you can now make a more
  significant contribution to help me make future updates.
* 📝 Existing subscription is now All Access. Reduced price (for existing subscribers, too) so
  more people can get access to Cloud.
* 🔧 Add option to turn off infinite calendar.
* 🔧 Movie release times setting also affects popular, search. Watchlist, collection, watched and
  details views will start using it.

### 50.1 - 2019-09-18

* 🔨 Add movies to watchlist, collection or watched in all cases when syncing with trakt or Cloud.
  On updating to this version the next sync will add missing movies.

### 50 - 2019-08-14

* 📝 Latest translations from crowdin.

### 50-beta5 - 2019-07-25

* 🔧 The list of popular movies and movie search display release dates depending on the selected
  region.
* 🔧 The movie watchlist, collection, watched tab and the details view will start to display the
  release date depending on the selected region. Preferably the theatrical one.
* 🔨 Correctly detect active subscription after restarting the app.
* 🔨 Do not crash if subscription title can not be parsed.
* 📝 Distribute as Android App Bundle. This can not be sideloaded, use the official APK from the website!
* 📝 Latest translations from crowdin.

### 50-beta4 - 2019-07-19

* 🌟 Support upgrading subscription to new Sponsor and Supporter tiers.
* 🔧 Show icon which subscription tier is active.
* 📝 Latest translations from crowdin.

### 50-beta3 - 2019-07-18

* 🌟 Introduce Sponsor and Supporter subscriptions so people who can or want to can make a more
  significant contribution. This helps me make future updates.
* 📝 Rename existing subscription to All Access. Reduced price for new and existing subscribers so
  more people can get access to Cloud.
* 🔧 Move subscriptions to new Google Play billing library.
* 📝 Latest translations from crowdin.

### 50-beta2 - 2019-07-06

* 🔧 Shows/Movies: move search action left-most as likely most used. Show refresh action on history tabs.
* 🔧 Show overview: move share action to more options to reduce clutter.
* 🔧 Episodes list: show sort by action.
* 🔨 Enable crash reporting.

### 50-beta1 - 2019-07-05

* 🔧 Restore infinite calendar option due to feedback. Defaults to enabled for new and existing users.
* 🔨 Potential fixes for crashes due to extensions.

## Version 49

* 🔧 Calendar is always infinite, uses all available space on large screens, has larger fast
  scroller that is easier to grab.
* 🌟 Add setting to ignore hidden shows for notifications (defaults to enabled).
* 🌟 Filters: add option to make all hidden shows visible at once.
* 🔧 History: Add link to trakt history website. Show up to 50 items (was 25).
* 🔧 Streaming search: add JustWatch for Poland.
* 🔧 Movies: Add set watched option to more options (three dots) menu.

### 49 - 2019-06-28

* 📝 Latest translations from crowdin.

### 49-beta6 - 2019-06-21

* 🔨 When making all hidden shows visible also upload changes to Cloud.
* 🔨 trakt sign-in: do not crash if WebView is currently unavailable (e.g. it is updated).
* 🔨 Potential fix for crashes when receiving actions from extensions.
* 🔧 When changing the state of a show (e.g. favoriting or hiding it), will wait until sent to Cloud
  before applying the change locally.
* 📝 Latest translations from crowdin.

### 49-beta5 - 2019-06-07

* 🌟 Add setting to ignore hidden shows for notifications (defaults to enabled).
* 🌟 Filters: add option to make all hidden shows visible at once.
* 🔨 Do not crash when changing show states (favorite, hidden, notify).
* 📝 Latest translations from crowdin.

### 49-beta4 - 2019-05-31

* 🔧 Streaming search: add JustWatch for Portugal (but appears to be broken) and Poland.
* 🔧 Add set watched option to movie more options (three dots) menu.
* 🔨 Movie not in collection or watchlist is properly added after setting it watched.
* 🔨 trakt sync adds movies that are just watched.
* 🔧 TMDb sync now reports failure if any movie failed to update.
* 📝 Latest translations from crowdin.

### 49-beta3 - 2019-05-24

* 🔧 Experimental internal improvements when changing favorite, notify or hidden state of a show.
* 🔨 Potential fix for calendar jumping away from first item.
* 📝 Latest translations from crowdin.

### 49-beta2 - 2019-05-10

* 🔧 The new calendar is now always infinite. If multiple columns are shown, groups are no longer
  broken into a new row, instead using all available space.
* 📝 Latest translations from crowdin.

### 49-beta1 - 2019-05-10

* 🔧 Add link to trakt history website on history screen. Show up to 50 items (was 25).
* 🔧 If connected to trakt, show at most 10 recently watched episodes or movies on history tabs (was 25).
* 🔨 Switched upcoming/recent tabs to RecyclerView, should resolve various crashes.
* 📝 The infinite calendar option has been removed. Instead upcoming/recent now show up to 50 episodes.
* 📝 Drop support for beaming shows from overview screen. Share the TheTVDB link instead.

## Version 48

* Support 'Upcoming' status for shows.
* Add watched movies tab.
* Statistics: display number and run time of watched movies.
* Color navigation bar black for dark themes, white on light theme.

### 48 - 2019-05-02

* 🔨 Do not crash if updating security provider fails.
* 📝 Latest translations from crowdin.

### 48-beta6 - 2019-04-17

* 🔧 Support 'Upcoming' status for shows.
* 🔧 Ask Google Play Services (if available) to update security provider.
* 🔨 Fix crashes in movie details view and when pinning shortcuts.

### 48-beta5 - 2019-04-12

* 🔧 Experiment: refresh season watched counts using new Worker API.
* 📝 Latest translations from crowdin.

### 48-beta4 - 2019-03-29

* 🔧 Do not ask for storage permission in backup/restore tool (still required for auto-backup).
* 📝 Latest translations from crowdin.

### 48-beta3 - 2019-03-21

* 🌟 Statistics: display number and run time of watched movies. Might be incorrect until movies are updated.

### 48-beta2 - 2019-03-15

* 🌟 Add watched movies tab. Might show blank items until movies are updated.
* 🔧 Force black navigation bar on OnePlus devices as well.
* 🔧 Use white navigation bar on light theme if on Android 8.1 or higher for burn-in protection.
* 📝 Latest translations from crowdin.

### 48-beta1 - 2019-03-08

* 🔧 Force black navigation bar.
* 🔧 Use darker overlay action and status bar for better readability (movie details).
* 🔧 Backup screens: show file path below button for better readability.
* 📝 Latest translations from crowdin.

## Version 47

* 🔧 Show list: replace favorite button with set watched button.
* 🌟 Set movies watched (previously only when connected to trakt).
* 🌟 New show list filters that can be set to include (+), exclude (-) or disabled.
* 🌟 Added filter for continuing shows (exclude to display ended shows).
* 📝 Show list filter settings are set back to defaults.
* 🔧 Sharing old TheTVDB links to SeriesGuide to add shows works again.

### 47 - 2019-02-22

* 🔧 Sharing old TheTVDB links to SeriesGuide to add shows works again.
* 🔧 Switch to improved error reporting to better pinpoint issues.
* 📝 Latest translations from crowdin.

### 47-beta7 - 2019-02-16

* 🔧 Experiment with improved error reporting to better pinpoint issues.
* 🔨 Crashes and errors are reported again.

### 47-beta6 - 2019-02-08

* 🔨 Do not crash when loading show discover screen.

### 47-beta5 - 2019-02-08

* 🔨 Do not crash when opening movie with unknown running time.
* 🔨 Do not crash when opening show sort options with deprecated sort order.

### 47-beta4 - 2019-02-01

* 🌟 Set movies watched (previously only when connected to trakt).
* 🌟 Cloud: sync watched movies. If trakt is connected, too, will upload existing watched movies,
  then use Cloud to sync them going forward. Watched changes are still sent to trakt.
* 📝 Latest translations from crowdin.

### 47-beta3 - 2019-01-25

* 🌟 New show list filters that can be set to include (+), exclude (-) or disabled.
* 🌟 Added filter for continuing shows (exclude to display ended shows).
* 📝 Show list filter settings are set back to defaults.
* 🔧 Upcoming range setting moved to button next to upcoming filter.
* 🔧 Show list filter view scrolls if screen is not tall enough.
* 📝 Target Android 9.0 (Pie).
* 📝 Latest translations from crowdin.

### 47-beta2 - 2019-01-18

* 🔧 After changing the language of a show or the alternative language in Settings, episode descriptions are updated properly again.
* 🔧 Combine show filter and sort options into single view. Stays visible until tap outside or back button press.
* 📝 Latest translations from crowdin.

### 47-beta1 - 2019-01-11

* 🔧 Show list: replace favorite button with set watched button. Display an indicator if a show is a favorite instead.
* 🔧 Cloud: update and improve Google Sign-In.
* 🔧 Tablets: move add show button on discover screen to top right to match placement of primary action in other places.
* 🔧 Discover: remove trakt recommendations. They were never useful. Send in feedback if they are for you!
* 🔨 Only remove movie from watchlist if it actually was on it. This avoids a confusing confirmation message.
* 📝 Only support Android 5.0 (Lollipop) and newer.
* 📝 Latest translations from crowdin.

## Version 46 and older

See [CHANGELOG-K.md](CHANGELOG-K.md).
