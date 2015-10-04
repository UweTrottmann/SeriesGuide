Contributing
============

#### Would you like to contribute code?

1. [Fork SeriesGuide][11]. See further setup instructions below.
2. `git checkout -b descriptive-branch-name dev` and make [great commits + messages][10].
3. [Start a pull request][6] against dev. Reference [existing issues][7] when possible.

#### No code!
* You can [get help][12].
* You can [suggest features][9].
* You can [discuss a bug][7] or if it was not reported yet [submit a bug][8]!
* You can [translate strings][4].

Branch structure
----------------

The repository is made up of two main branches: master (stable) and dev (unstable).

* **master** has the latest stable code, its tags are released as [SeriesGuide][1] on Google Play.
* **dev** includes the latest unstable code, contributers (you!) should submit pull requests against it.

Setup
-----

This project is built with Gradle, the [Android Gradle plugin][3] and uses jar and Maven dependencies. Clone this repository inside your working folder. Import the `settings.gradle` file in the root folder into e.g. Android Studio. (You can also have a look at the `build.gradle` files on how the projects depend on another.)

Before your first build create `gradle.properties` in the root directory (where `settings.gradle` is), add the following values (do not need to be valid if you do not plan to use that functionality):

```
# Credentials to publish the API jar
ossrhUsername=<your sonatype username>
ossrhPassword=<your sonatype password>

# API keys for integrated services
TMDB_API_KEY=<your api key>
TRAKT_CLIENT_ID=<your trakt client id>
TRAKT_CLIENT_SECRET=<your trakt client secret>
TVDB_API_KEY=<your api key>

# Play Store in-app billing public key
IAP_KEY_A=dummy
IAP_KEY_B=dummy
IAP_KEY_C=dummy
IAP_KEY_D=dummy
```

Also create `SeriesGuide/fabric.properties` for [Crashlytics][13]. You may use the dummy values below:

```
# crashlytics dummy values
apiSecret=0000000000000000000000000000000000000000000000000000000000000000
apiKey=0
```

Now build any variant of the **free flavor**, for developing probably `freeDebug` (flavor + build type, see [instructions about product flavors][5]) defined in `SeriesGuide/build.gradle`.

 [1]: https://play.google.com/store/apps/details?id=com.battlelancer.seriesguide
 [2]: https://github.com/UweTrottmann/SeriesGuide/wiki/Beta
 [3]: http://tools.android.com/tech-docs/new-build-system/user-guide
 [4]: https://crowdin.com/project/seriesguide-translations
 [5]: http://tools.android.com/tech-docs/new-build-system/user-guide#TOC-Product-flavors
 [6]: https://github.com/UweTrottmann/SeriesGuide/compare
 [7]: https://github.com/UweTrottmann/SeriesGuide/issues
 [8]: https://github.com/UweTrottmann/SeriesGuide/issues/new
 [9]: https://seriesguide.uservoice.com
 [10]: http://robots.thoughtbot.com/post/48933156625/5-useful-tips-for-a-better-commit-message
 [11]: https://github.com/UweTrottmann/SeriesGuide/fork
 [12]: http://seriesgui.de/help
 [13]: https://get.fabric.io/crashlytics
 