# SeriesGuide user interface translations

Translations are managed by a Crowdin project and are imported into this repo by scripts.

[download-translations.ps1](/download-translations.ps1)

Strips all region specifiers so translations are used by Android for all regional variants.

Except for when multiple variants are translated on Crowdin:

- Chinese: Mainland or Simplified (`values-zh-rCN`) and Taiwan or Traditional (`values-zh-rTW`)
- Portuguese: Brazil (`values-pt-rBR`) and Portugal (`values-pt-rPT`)
- Serbian: Cyrillic (`values-sr-rSP`) and Latin (`values-sr-rCS`)
