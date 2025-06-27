# Release process

- If it does not exist, create a release branch. If it exists, merge latest changes.

  ```shell
  git checkout -b release-2025.2
  # or
  git merge dev
  ```

- Optional: update translations (run script in PowerShell)

  ```powershell
  .\download-translations.ps1
  git commit --all --message "Import latest translations"
  ```

- Change version code and name in [`build.gradle.kts`](/build.gradle.kts)
- Update [`CHANGELOG.md`](/CHANGELOG.md)
- Commit and push

  ```shell
  git commit --all --message "Prepare version 2025.2.4 (21250204)"
  git push --set-upstream origin release-2025.2
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
  
  ```shell
  git tag v2025.2.4
  git push origin v2025.2.4
  git checkout dev
  git merge release-2025.2
  git push origin dev
  ```

- Promote to beta channel
- Create or update preview release post on forum

### Production

- `bundleAmazonRelease`
- Merge release pull request to `main`
- Download universal APK from Play Store
- [Create GitHub release](https://github.com/UweTrottmann/SeriesGuide/releases/new)
  - title like `SeriesGuide 2025.1.1`
  - get release notes from [`CHANGELOG.md`](/CHANGELOG.md)
  - attach APK
- Prepare release post on forum
- Promote to production
- Publish to Amazon App Store
- Publish release post on forum, post on Mastodon
- Test Amazon update on test device
- Merge changes to dev branch

  ```shell
  git checkout dev
  git merge --no-ff release-2025.1
  ```