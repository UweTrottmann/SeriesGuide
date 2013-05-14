Contributing
============

If you would like to contribute code to SeriesGuide you can do so through GitHub by forking the repository and sending a **pull request against the beta branch**.

Branch structure
----------------

The repository is made up of two main branches: master (stable) and beta (development).

* **master** has the latest stable code, its tags are released as [SeriesGuide][2] and [SeriesGuide X][3] on Google Play.
* **beta** includes the latest unstable code. Its tags are released as [SeriesGuide beta][4] on Google Play.

Setup
-----

**If anyone wants to help me to switch this setup to Maven, feel free to contact me!**

This project is built with [Android Developer Tools (ADT)][9] using the Android library concept for dependency management. First, clone this repository inside your working folder. Then clone the

- [AndroidUtils][5]
- [ActionBarSherlock][6]
- [ViewPagerIndicator][7]
- [android-menudrawer][8]

forks in that same working directory. Switch the ActionBarSherlock and ViewPagerIndicator repos to their seriesguide branches. (You can also have a look at the project.properties files on how the projects depend on another.)

To successfully build with ADT, you should also create a keys.xml file in the SeriesGuide/res/values folder and add the string values 

    <resources>
        <string name="tvdb_apikey"></string>
        <string name="tmdb_apikey"></string>
        <string name="getglue_consumer_key"></string>
        <string name="getglue_consumer_secret"></string>
        <string name="trakt_apikey"></string>
    </resources>
	
to it. These are not shared with the public mainly for security reasons.

To build any of the free, X or beta version use the appropiate flavor project. They all use the SeriesGuide project as an Android library project and just change the package names and icons as well as the Content Provider URI.

Why all the forking?
--------------------

SeriesGuide requires a more up to date copy of the Android Support Library than is used with ABS and ViewPagerIndicator. Forking the library projects also minimizes the risk of everybody using different versions to test.

It is planned to switch all of the build setup slowly to maven or to depend on versionable jars wherever possible.

 [1]: http://seriesgui.de
 [2]: https://play.google.com/store/apps/details?id=com.battlelancer.seriesguide
 [3]: https://play.google.com/store/apps/details?id=com.battlelancer.seriesguide.x
 [4]: https://play.google.com/store/apps/details?id=com.battlelancer.seriesguide.beta
 [5]: https://github.com/UweTrottmann/AndroidUtils
 [6]: https://github.com/UweTrottmann/ActionBarSherlock
 [7]: https://github.com/UweTrottmann/Android-ViewPagerIndicator
 [8]: https://github.com/UweTrottmann/android-menudrawer
 [9]: http://developer.android.com/tools/help/adt.html
