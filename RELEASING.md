# Release process

- Collect changes in release branch

  ```bash
  # If it does not exist, create a release branch
  git checkout -b releases/2026.5
  
  # If it exists, merge latest changes
  git checkout releases/2026.5
  git merge dev
  ```

- Optional: [update translations](/translations/README.md)
- Change version code and name in [`build.gradle.kts`](/build.gradle.kts)
- Update [`CHANGELOG.md`](/CHANGELOG.md)

  ```bash
  ./insert-next-release.sh build.gradle.kts CHANGELOG.md
  ```

- Commit with the suggested message and push

  ```bash
  git commit --all --message "Prepare version 2026.5.0 (23260101)"
  git push --set-upstream origin releases/2026.5
  ```

- If it does not exist, [create a merge request](https://github.com/UweTrottmann/SeriesGuide/compare/main...) against `main`
- [Check build succeeds](https://github.com/UweTrottmann/SeriesGuide/actions),
  tests are green and lint output is as expected

## Play Store (testing + production)

- `bundlePureRelease`

### Alpha

- Publish to alpha channel
- Test update on test device
    
### Beta

- Tag release commit
  
  ```bash
  git tag v2026.5.0
  git push origin v2026.5.0
  git checkout dev
  git merge releases/2026.5
  git push origin dev
  ```

- Download universal APK from Play Store
- [Create GitHub preview release](https://github.com/UweTrottmann/SeriesGuide/releases/new)
  - title like `2026.5.0`
  - get release notes from [`CHANGELOG.md`](/CHANGELOG.md)
  - attach APK
- Promote to beta channel
- Create or update preview release post on forum

### Production

- Update [`CHANGELOG.md`](/CHANGELOG.md)
- `bundleAmazonRelease`
- Merge release pull request to `main`
- [Create or update GitHub release](https://github.com/UweTrottmann/SeriesGuide/releases/new)
  - title like `2026.5.0`
  - get release notes from [`CHANGELOG.md`](/CHANGELOG.md)
  - attach APK
- Promote to production
- Publish to Amazon App Store
- Publish release post [on forum](https://discuss.seriesgui.de)
- Merge changes to dev branch

  ```bash
  git checkout dev
  git merge --no-ff releases/2026.5
  ```
- Update [`CHANGELOG.md`](/CHANGELOG.md) to add new `### Next release` heading

  ```bash
  git commit --all --message "Changelog: prepare for next release"
  ```
