Contributing
============

#### Would you like to contribute code?

1. [Fork SeriesGuide][11].
2. `git checkout -b descriptive-branch-name dev` and make [great commits + messages][10].
3. [Start a pull request][6] against dev. Reference [existing issues][7] when possible.

#### No code!
* You can [suggest features][9].
* You can [discuss a bug][7] or if it was not reported yet [submit a bug][8]!
* You can [translate strings][4].

Branch structure
----------------

The repository is made up of two main branches: master (stable) and dev (unstable).

* **master** has the latest stable code, its tags are released as [SeriesGuide][1] on Google Play.
* **dev** includes the latest unstable code from contributers (you!).

Setup
-----

This project is built with Gradle, the [Android Gradle plugin][3] and uses jars or Maven dependencies. Clone this repository inside your working folder. Import the build.gradle file in the root folder into e.g. Android Studio. (You can also have a look at the build.gradle files on how the projects depend on another.)

Before your first build create the following files:

* `gradle.properties`, add the following values (do not need to be valid if you do not plan to use that functionality):
```
ossrhUsername=<your sonatype username>
ossrhPassword=<your sonatype password>

TMDB_API_KEY=<your api key>
TRAKT_API_KEY=<your api key>
TVDB_API_KEY=<your api key>
TVTAG_CLIENT_ID=<your client id>
TVTAG_CLIENT_SECRET=<your client secret>

IAP_KEY_A=dummy
IAP_KEY_B=dummy
IAP_KEY_C=dummy
IAP_KEY_D=dummy
```

* `SeriesGuide/src/free/AndroidManifest.xml`, add the following content:
```
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.battlelancer.seriesguide">

    <application>
        <!-- Crashlytics -->
        <meta-data android:name="com.crashlytics.ApiKey" android:value="0000000000000000000000000000000000000000"/>
    </application>
</manifest>
```

Now build any variant of the free flavor (flavor + build type, see [instructions about product flavors][5]) defined in `SeriesGuide/build.gradle`.

 [1]: https://play.google.com/store/apps/details?id=com.battlelancer.seriesguide
 [2]: https://github.com/UweTrottmann/SeriesGuide/wiki/Beta
 [3]: http://tools.android.com/tech-docs/new-build-system/user-guide
 [4]: https://crowdin.net/project/seriesguide-translations
 [5]: http://tools.android.com/tech-docs/new-build-system/user-guide#TOC-Product-flavors
 [6]: https://github.com/UweTrottmann/SeriesGuide/compare
 [7]: https://github.com/UweTrottmann/SeriesGuide/issues
 [8]: https://github.com/UweTrottmann/SeriesGuide/issues/new
 [9]: https://seriesguide.uservoice.com
 [10]: http://robots.thoughtbot.com/post/48933156625/5-useful-tips-for-a-better-commit-message
 [11]: https://github.com/UweTrottmann/SeriesGuide/fork