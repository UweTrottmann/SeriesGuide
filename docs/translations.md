# SeriesGuide user interface translations

Translations are managed by a Crowdin project and are imported into this repo by scripts.

[download-translations.ps1](/download-translations.ps1)

If there are not multiple region or script variants, strips the region specifier so translations are
used by Android for any language variants.

Does renames to support regional variants:

- Chinese `zh`
  - Mainland or Simplified `values-zh-rCN`
  - Taiwan or Traditional `values-zh-rTW`
- Portuguese `pt`
  - Brazil `values-pt-rBR`
  - Portugal `values-pt-rPT`

And renames to support script variants:

- Serbian `sr`
  - keeps Cyrillic Serbian as `values-sr`
  - Latin Serbian `values-b+sr+Latn`
    - Only use BCP 47 tag (`b+sr+Latn`) for script variant as officially only Android 7.0+ supports
      them. This is what `androidx.appcompat` does. However, this seems to work even on an Android
      5.0 emulator (incl. using a BCP 47 tag for the Cyrillic variant, but not doing that in case
      other devices don't support it).

## Adding or removing a language

- Add or remove the `values-<tags>` directory.
  When adding, take the file from the download script above!
- Update [locales_config.xml](/app/src/main/res/xml/locales_config.xml) to 
  [support per-app language preferences](https://developer.android.com/guide/topics/resources/app-languages#sample-config).
  See the link for supported language codes. They differ from the resource directory name!
