Contributing
============

#### Would you like to contribute code?

1. [Fork SeriesGuide][11].
2. Create a new branch and make [great commits + messages][10].
3. [Start a pull request][6] against beta. Reference [existing issues][7] when possible.

#### You have commit access?

* Small change or fix? Make a [great commit + message][10] against beta and your done! Make sure to reference existing issues.
* Large changes or feature? Create a new branch and [start a pull request][6] against beta.

#### No code!
* You can [suggest features][9].
* You can [discuss a bug][7] or if it was not reported yet [submit a bug][8]!
* You can [translate strings][4].

Branch structure
----------------

The repository is made up of two main branches: master (stable) and beta (development).

* **master** has the latest stable code, its tags are released as [SeriesGuide][1] on Google Play.
* **beta** includes the latest unstable code. Its tags are released through the [beta program][2] for SeriesGuide on Google Play.

Setup
-----

This project is built with Gradle, the [Android Gradle plugin][3] and uses jars or Maven dependencies. Clone this repository inside your working folder. Import the build.gradle file in the root folder into e.g. Android Studio. (You can also have a look at the build.gradle files on how the projects depend on another.)

Before your first build create the following files:

* `SeriesGuide/src/main/res/values/keys.xml`, add the following values:

    <resources>
        <string name="getglue_client_id"></string>
        <string name="getglue_client_secret"></string>
        <string name="tvdb_apikey"></string>
        <string name="tmdb_apikey"></string>
        <string name="trakt_apikey"></string>
        <string name="key_a"></string>
        <string name="key_b"></string>
        <string name="key_c"></string>
        <string name="key_d"></string>
    </resources>

* `SeriesGuide/src/free`, add the following content:

    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
        package="com.battlelancer.seriesguide">

        <application>
            <!-- Crashlytics -->
            <meta-data android:name="com.crashlytics.ApiKey" android:value="0000000000000000000000000000000000000000"/>
        </application>

    </manifest>

Now build any variant of the free flavor (flavor + debug build type, see [instructions about product flavors][5]) defined in `SeriesGuide/build.gradle`.

 [1]: https://play.google.com/store/apps/details?id=com.battlelancer.seriesguide
 [2]: https://github.com/UweTrottmann/SeriesGuide/wiki/Beta
 [3]: http://tools.android.com/tech-docs/new-build-system/user-guide
 [4]: http://crowdin.net/project/seriesguide-translations
 [5]: http://tools.android.com/tech-docs/new-build-system/user-guide#TOC-Product-flavors
 [6]: https://github.com/UweTrottmann/SeriesGuide/compare
 [7]: https://github.com/UweTrottmann/SeriesGuide/issues
 [8]: https://github.com/UweTrottmann/SeriesGuide/issues/new
 [9]: https://seriesguide.uservoice.com/forums/189742-general
 [10]: http://robots.thoughtbot.com/post/48933156625/5-useful-tips-for-a-better-commit-message
 [11]: https://github.com/UweTrottmann/SeriesGuide/fork