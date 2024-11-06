# Release process

- If it does not exist, create a `release-<minor-version>` branch
- Merge latest changes from `dev`
- Optional: Update translations
- Change version code and name in [`build.gradle.kts`](/build.gradle.kts)
- Update [`CHANGELOG.md`](/CHANGELOG.md)
- Push to GitHub
- If it does not exist, create a merge request against `main`
- Check build succeeds, tests are green and lint output is as expected

## Play Store (testing + production)

- `bundlePureRelease`
- Publish to alpha channel, test.

Published to beta channel:

- Tag like `v12.0.3`.

Published to production:

- Download universal APK from Play Store and attach to GitHub tag.

## Amazon App Store (production only)

- `bundleAmazonRelease`
- Test update on test device.
