# Release process

- If stable, create `release-<nr>` branch. If beta, stay on `dev`.
- Optional: Update translations.
- Change version code and name in `build.gradle`.
- Amend `CHANGELOG.md`.
- Deploy to test device.
- Push to GitHub and check build succeeds, tests are green and Lint file is OK.


## Play Store (betas + stable)

- `bundlePureRelease`
- Publish to alpha channel, test.

Published to beta:
- Tag like `v12` or `v12-beta1`.

Published to production:
- Download universal APK from Play Store and attach to GitHub tag.


## Amazon App Store (stable only)

- `bundleAmazonRelease`
- Test update on test device.
