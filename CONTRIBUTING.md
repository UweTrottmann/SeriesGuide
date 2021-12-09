# Contributing

**Note:** This work is licensed under the [Apache License 2.0](LICENSE.txt).
If you contribute any 
[non-trivial](http://www.gnu.org/prep/maintain/maintain.html#Legally-Significant)
patches or translations make sure you have read it and agree with it.

#### Would you like to contribute code?

1. [Fork SeriesGuide](https://github.com/UweTrottmann/SeriesGuide/fork) and clone your fork.
2. See the notes about [building](#building) the app below.
3. Create a new branch ([using GitHub](https://help.github.com/articles/creating-and-deleting-branches-within-your-repository/)
   or the command `git checkout -b descriptive-branch-name dev`).
4. Make [great commits](http://robots.thoughtbot.com/post/48933156625/5-useful-tips-for-a-better-commit-message).
5. [Start a pull request](https://github.com/UweTrottmann/SeriesGuide/compare) and reference [issues](https://github.com/UweTrottmann/SeriesGuide/issues) if needed.

#### No code!
* You can [discuss or submit bug reports](https://github.com/UweTrottmann/SeriesGuide/issues).
* You can [suggest features](https://discuss.seriesgui.de).
* You can [translate the app](https://crowdin.com/project/seriesguide-translations).

## Building

- `dev` is the main development and [test release](https://github.com/UweTrottmann/SeriesGuide/wiki/Beta) branch.
- `main` has always the latest [stable version](https://seriesgui.de).

To get started:

1. Import the `SeriesGuide` folder as a new project in Android Studio.
2. Select the `pureDebug` build variant (defined in `app/build.gradle`). 
   [Learn about product flavors](https://developer.android.com/studio/build/build-variants.html#product-flavors).

### Debug

Debug builds should just work.

### TMDB, trakt
To add shows or movies you need to create an API key for [TMDB](https://www.themoviedb.org/settings/api) 
and OAuth credentials for [trakt](https://trakt.tv/oauth/applications). 
Place them in `secret.properties` in the project directory (where `settings.gradle` is):

```
SG_TMDB_API_KEY=<your api key>
SG_TRAKT_CLIENT_ID=<your trakt client id>
SG_TRAKT_CLIENT_SECRET=<your trakt client secret>
```

### Release
To release some additional `secret.properties` values might be necessary:
```
# Play Store in-app billing public key
SG_IAP_KEY_A=<keypart>
SG_IAP_KEY_B=<keypart>
SG_IAP_KEY_C=<keypart>
SG_IAP_KEY_D=<keypart>

# Credentials to publish the API jar
SONATYPE_NEXUS_USERNAME=<your sonatype username>
SONATYPE_NEXUS_PASSWORD=<your sonatype password>
```

#### Crashlytics

To use [Crashlytics](https://firebase.google.com/docs/crashlytics) download and
add your `app/google-services.json`.

#### Amazon Appstore public key

For in-app purchases need to add `AppstoreAuthenticationKey.pem` into `app/src/amazon/assets`.
