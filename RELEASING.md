# Release process

- If stable, create `release-<nr>` branch. If beta, stay on `dev`.
- Optional: Update translations.
- Change version code and name in [`build.gradle.kts`](/build.gradle.kts).
- Amend [`CHANGELOG.md`](/CHANGELOG.md).
- Deploy to test device.
- Push to GitHub and check build succeeds, tests are green and Lint file is OK.


## Play Store (testing + production)

- `bundlePureRelease`
- Publish to alpha channel, test.

Published to beta channel:
- Tag like `v12.0.3`.

Published to production:
- Download universal APK from Play Store and attach to GitHub tag.


## Amazon App Store (stable only)

- `assembleAmazonRelease`
- Test update on test device.
