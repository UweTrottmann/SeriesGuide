# SeriesGuide user interface translations

Translations are managed by a Crowdin project and are imported into this repo by a script.

## Downloading translations

1. On the [translations page](https://crowdin.com/project/seriesguide-translations/translations),
in "Download as ZIP" choose to "Build & Download".

2. Place the `zip` file into this directory and run 
   [the update script](update-translations.sh).

By default, it strips the region specifier of the values directory so translations are used by
Android for any language variants.

But keeps region suffix to support regional variants for:

- Chinese `zh`
   - Mainland or Simplified `values-zh-rCN`
   - Taiwan or Traditional `values-zh-rTW`
- Portuguese `pt`
   - Brazil `values-pt-rBR`
   - Portugal `values-pt-rPT`

And renames the whole suffix to support script variants for:

- Serbian `sr`
   - keeps Cyrillic Serbian as `values-sr`
   - Latin Serbian `values-b+sr+Latn`
      - Only use BCP 47 tag (`b+sr+Latn`) for script variant as officially only Android 7.0+ supports
        them. This is what `androidx.appcompat` does. However, this seems to work even on an Android
        5.0 emulator (incl. using a BCP 47 tag for the Cyrillic variant, but not doing that in case
        other devices don't support it).

## Uploading strings

1. Take [strings.xml](/app/src/main/res/values/strings.xml)
2. [Upload it to Crowdin](https://crowdin.com/project/seriesguide-translations/sources/files)

    - Title: SeriesGuide App
    - File name: strings.xml
    - Resulting file after export: `/seriesguide/values-%android_code%/strings.xml`
      Note: don't use a Bundle, it drops comments from the exported XML files.
