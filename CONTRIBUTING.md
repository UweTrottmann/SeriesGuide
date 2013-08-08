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

This project is built with [Android Developer Tools (ADT)][3] using the Android library concept for dependency management. Clone this repository inside your working folder. Add all projects inside the root and the libraries folder. (You can also have a look at the project.properties files on how the projects depend on another.)

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

 [1]: https://play.google.com/store/apps/details?id=com.battlelancer.seriesguide
 [2]: https://github.com/UweTrottmann/SeriesGuide/wiki/Beta
 [3]: http://developer.android.com/tools/help/adt.html
 [4]: http://crowdin.net/project/seriesguide-translations