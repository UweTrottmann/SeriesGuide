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

This project is built with ADT using the Android library concept for dependency management. First, clone this repository. You will also need clones of my [AndroidUtils][5], [ActionBarSherlock][6], [ViewPagerIndicator][7], [android-menudrawer][8] and [StickyGridHeaders][9] forks. Check out their seriesguide branches (if it exists). See the project.properties files on how the projects depend on another.
To successfully build, you should also create a keys.xml file in the SeriesGuide/res/values folder and add the string values 

    <resources>
        <string name="tvdb_apikey"></string>
        <string name="getglue_consumer_key"></string>
        <string name="getglue_consumer_secret"></string>
        <string name="trakt_apikey"></string>
    </resources>
	
to it. These are not shared with the public mainly for security reasons.

To build any of the free, X or beta version use the appropiate flavor project. They all use the SeriesGuide project as an Android library project and just change the package names and icons as well as the Content Provider URI.

Why all the forking?
--------------------

For one, I add a more up to date copy of the Android Support Library to ABS and ViewPagerIndicator. Second, until I have switched to a Maven powered build, this is the only way I can ensure everybody uses the same version of all those libraries as I do.

If anybody can come up with a better solution or help me switch this build setup to Maven, please feel free to contact me!

 [1]: http://seriesgui.de
 [2]: https://play.google.com/store/apps/details?id=com.battlelancer.seriesguide
 [3]: https://play.google.com/store/apps/details?id=com.battlelancer.seriesguide.x
 [4]: https://play.google.com/store/apps/details?id=com.battlelancer.seriesguide.beta
 [5]: https://github.com/UweTrottmann/AndroidUtils
 [6]: https://github.com/UweTrottmann/ActionBarSherlock
 [7]: https://github.com/UweTrottmann/Android-ViewPagerIndicator
 [8]: https://github.com/UweTrottmann/android-menudrawer
 [9]: https://github.com/UweTrottmann/StickyGridHeaders
