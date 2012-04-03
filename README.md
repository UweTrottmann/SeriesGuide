SeriesGuide Show Manager
========================

This GitHub repository hosts the code for the Android app SeriesGuide.

For more information have a look at http://seriesgui.de

Branch structure
----------------

The repository is made up of three main development branches: master (stable), beta and dev.

* **master** has the latest stable code, its tags are released as the stable SeriesGuide version in market.
* **beta** is somewhat stable and contains new features early. Its tags are released as SeriesGuide beta on Android Market.
* **dev** includes the latest unstable code, no releases are made of it.

Contributing
------------

Want to contribute? Great! Fork the repository, code, tell me about it!

To setup your environment clone the repository. Then import the three projects SeriesGuide, com_actionbarsherlock and viewpagerindicator into eclipse. To successfully build, you should create a keys.xml file in the SeriesGuide/res/values folder and add the string values 

    <resources>
        <string name="tvdb_apikey"></string>
        <string name="getglue_consumer_key"></string>
        <string name="getglue_consumer_secret"></string>
        <string name="trakt_apikey"></string>
    </resources>
	
to it.

License
-------

    Copyright 2011 Uwe Trottmann

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.