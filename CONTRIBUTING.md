Contributing
============

If you would like to contribute code to SeriesGuide you can do so through GitHub by forking the repository and sending a **pull request against the beta branch**.

*Note:* If you would like to help translate SeriesGuide, please check out its [Crowdin page][4] instead.

Branch structure
----------------

The repository is made up of two main branches: master (stable) and beta (development).

* **master** has the latest stable code, its tags are released as [SeriesGuide][1] on Google Play.
* **beta** includes the latest unstable code. Its tags are released through the [beta program][2] for SeriesGuide on Google Play.

Setup
-----

This project is built with Gradle and the [Android Gradle plugin][3] using the Android library concept for dependency management. Clone this repository inside your working folder. Import the build.gradle file in the root folder into Android Studio. (You can also have a look at the build.gradle files on how the projects depend on another.)

To successfully build with ADT, you should also create a keys.xml file in the `SeriesGuide/src/main/res/values` folder and add the string values 

    <resources>
        <string name="tvdb_apikey"></string>
        <string name="tmdb_apikey"></string>
        <string name="getglue_consumer_key"></string>
        <string name="getglue_consumer_secret"></string>
        <string name="trakt_apikey"></string>
    </resources>
	
to it. These are not shared with the public mainly for security reasons.

Now build any of the free, X or beta debug build variants (flavor + debug build type, see [instructions about product flavors][5]) defined in `SeriesGuide/build.gradle`. Each flavor just changes the package names and icons as well as the Content Provider URI and some additional properties.

 [1]: https://play.google.com/store/apps/details?id=com.battlelancer.seriesguide
 [2]: https://github.com/UweTrottmann/SeriesGuide/wiki/Beta
 [3]: http://tools.android.com/tech-docs/new-build-system/user-guide
 [4]: http://crowdin.net/project/seriesguide-translations
 [5]: http://tools.android.com/tech-docs/new-build-system/user-guide#TOC-Product-flavors