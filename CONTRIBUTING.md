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

1. [Fork SeriesGuide](https://github.com/UweTrottmann/SeriesGuide/fork). See further setup instructions below.
2. Create a new branch ([using GitHub](https://help.github.com/articles/creating-and-deleting-branches-within-your-repository/)
   or the command `git checkout -b descriptive-branch-name dev`) and make
   [great commits + messages](http://robots.thoughtbot.com/post/48933156625/5-useful-tips-for-a-better-commit-message).
3. [Start a pull request](https://github.com/UweTrottmann/SeriesGuide/compare). Reference [existing issues](https://github.com/UweTrottmann/SeriesGuide/issues) when possible.

#### No code!
* You can [get help](https://seriesgui.de/help).
* You can [suggest features](https://seriesguide.uservoice.com).
* You can [translate the app](https://crowdin.com/project/seriesguide-translations).
* You can [discuss bugs](https://github.com/UweTrottmann/SeriesGuide/issues) or [submit a bug](https://github.com/UweTrottmann/SeriesGuide/issues/new).

## Building

This project is built with Gradle and uses the 
[Android Gradle plugin](https://developer.android.com/studio/build/index.html). To get started:

1. Clone this repository, for example using `git clone https://github.com/UweTrottmann/SeriesGuide.git`.

- `dev` is the main development and [test release](https://github.com/UweTrottmann/SeriesGuide/wiki/Beta) branch.
- `master` has always the latest [stable version](https://seriesgui.de).

2. In Android Studio import the `SeriesGuide` folder as a new project.

### Debug

Select the `pureDebug` build variant (defined in `app/build.gradle`). 
[Learn about product flavors](https://developer.android.com/studio/build/build-variants.html#product-flavors)).

Debug builds should just work.

### TheTVDB, TMDB, trakt
To add shows or movies you need to create API keys for 
[TheTVDB](https://www.thetvdb.com/member/api), [TMDB](https://www.themoviedb.org/settings/api) 
and OAuth credentials for [trakt](https://trakt.tv/oauth/applications). 
Place them in `gradle.properties` in the root directory (where `settings.gradle` is):

```
TVDB_API_KEY=<your api key>
TMDB_API_KEY=<your api key>
TRAKT_CLIENT_ID=<your trakt client id>
TRAKT_CLIENT_SECRET=<your trakt client secret>
```

### Release
To release some additional `gradle.properties` values might be necessary:
```
# Play Store in-app billing public key
IAP_KEY_A=<keypart>
IAP_KEY_B=<keypart>
IAP_KEY_C=<keypart>
IAP_KEY_D=<keypart>

# Credentials to publish the API jar
ossrhUsername=<your sonatype username>
ossrhPassword=<your sonatype password>
```

#### Crashlytics

To use [Crashlytics](https://get.fabric.io/crashlytics) create `app/fabric.properties` and 
add your [API key and secret](https://docs.fabric.io/android/fabric/settings/api-keys.html):

```
# app/fabric.properties
apiSecret=<secret>
apiKey=<key>
```
 