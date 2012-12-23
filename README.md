SeriesGuide Show Manager
========================

This GitHub repository hosts the code for the Android app SeriesGuide.

For more information about SeriesGuide have a look at [seriesgui.de][1].

Branch structure
----------------

The repository is made up of two main development branches: master (stable) and beta.

* **master** has the latest stable code, its tags are released as [SeriesGuide][2] and [SeriesGuide X][3] on Google Play.
* **beta** includes the latest unstable code. Its tags are released as [SeriesGuide beta][4] on Google Play.

Contributing
------------

Want to contribute? Great! Fork the repository, code, send a pull request!

To setup your environment clone this repository. You will also need clones of the [AndroidUtils][5], [ActionBarSherlock][6], [ViewPagerIndicator][7] and [SlidingMenu][8] repos. To successfully build, you should create a keys.xml file in the SeriesGuide/res/values folder and add the string values 

    <resources>
        <string name="tvdb_apikey"></string>
        <string name="getglue_consumer_key"></string>
        <string name="getglue_consumer_secret"></string>
        <string name="trakt_apikey"></string>
    </resources>
	
to it. These are not shared with the public mainly for security reasons.

To build any of the free, X or beta version use the appropiate flavor project. They all use the SeriesGuide project as a library project and just change the package names and icons as well as the Content Provider URI.

License
-------

    Copyright 2012 Uwe Trottmann

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


 [1]: http://seriesgui.de
 [2]: https://play.google.com/store/apps/details?id=com.battlelancer.seriesguide
 [3]: https://play.google.com/store/apps/details?id=com.battlelancer.seriesguide.x
 [4]: https://play.google.com/store/apps/details?id=com.battlelancer.seriesguide.beta
 [5]: https://github.com/UweTrottmann/AndroidUtils
 [6]: https://github.com/UweTrottmann/ActionBarSherlock
 [7]: https://github.com/UweTrottmann/Android-ViewPagerIndicator
 [8]: https://github.com/UweTrottmann/SlidingMenu