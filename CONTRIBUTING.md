# Contributing

**Note:** This project is in the [public domain](UNLICENSE). If you contribute any 
[non-trivial](http://www.gnu.org/prep/maintain/maintain.html#Legally-Significant)
patches or translations the following applies:

    I dedicate any and all copyright interest in this software to the
    public domain. I make this dedication for the benefit of the public at
    large and to the detriment of my heirs and successors. I intend this
    dedication to be an overt act of relinquishment in perpetuity of all
    present and future rights to this software under copyright law.

#### Would you like to contribute code?

1. [Fork SeriesGuide](https://github.com/UweTrottmann/SeriesGuide/fork) and clone your fork.
2. See the notes about [building](#building) the app below.
3. Create a new branch ([using GitHub](https://help.github.com/articles/creating-and-deleting-branches-within-your-repository/)
   or the command `git checkout -b descriptive-branch-name dev`).
4. Make [great commits](http://robots.thoughtbot.com/post/48933156625/5-useful-tips-for-a-better-commit-message).
5. [Start a pull request](https://github.com/UweTrottmann/SeriesGuide/compare) and reference [issues](https://github.com/UweTrottmann/SeriesGuide/issues) if needed.

#### No code!
* You can [get help](https://seriesgui.de/help).
* You can [suggest features](https://discuss.seriesgui.de).
* You can [translate the app](https://crowdin.com/project/seriesguide-translations).
* You can [discuss bugs](https://github.com/UweTrottmann/SeriesGuide/issues) or [submit a bug](https://github.com/UweTrottmann/SeriesGuide/issues/new).

## Building

- `dev` is the main development and [test release](https://github.com/UweTrottmann/SeriesGuide/wiki/Beta) branch.
- `master` has always the latest [stable version](https://seriesgui.de).

To get started:

1. Import the `SeriesGuide` folder as a new project in Android Studio.
2. Select the `pureDebug` build variant (defined in `app/build.gradle`). 
   [Learn about product flavors](https://developer.android.com/studio/build/build-variants.html#product-flavors).

### Debug

Debug builds should just work.

### TheTVDB, TMDB, trakt
To add shows or movies you need to create API keys for 
[TheTVDB](https://www.thetvdb.com/member/api), [TMDB](https://www.themoviedb.org/settings/api) 
and OAuth credentials for [trakt](https://trakt.tv/oauth/applications). 
Place them in `secret.properties` in the project directory (where `settings.gradle` is):

```
SG_TVDB_API_KEY=<your api key>
SG_TMDB_API_KEY=<your api key>
SG_TRAKT_CLIENT_ID=<your trakt client id>
SG_TRAKT_CLIENT_SECRET=<your trakt client secret>
```

### Release
To release some additional `secret.properties` values might be necessary:
```
# Play Store in-app billing public key
SG_IAP_KEY_A=<keypart>
SG_IAP_KEY_B=<keypart>
SG_IAP_KEY_C=<keypart>
SG_IAP_KEY_D=<keypart>

# Credentials to publish the API jar
SONATYPE_NEXUS_USERNAME=<your sonatype username>
SONATYPE_NEXUS_PASSWORD=<your sonatype password>
```

#### Crashlytics

To use [Crashlytics](https://get.fabric.io/crashlytics) create `app/fabric.properties` and 
add your [API key and secret](https://docs.fabric.io/android/fabric/settings/api-keys.html):

```
# app/fabric.properties
apiSecret=<secret>
apiKey=<key>
```
 
