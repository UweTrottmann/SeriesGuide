# Release process

- If it does not exist, create a release branch. If it exists, merge latest changes.

  ```shell
  git checkout -b release-2025.1
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
  git commit --all --message "Prepare version 2025.1.1 (21250102)"
  git push --set-upstream origin release-2025.1
  ```

- If it does not exist, [create a merge request](https://github.com/UweTrottmann/SeriesGuide/compare/main...) against `main`
- [Check build succeeds](https://github.com/UweTrottmann/SeriesGuide/actions),
  tests are green and lint output is as expected

## Play Store (testing + production)

- `bundlePureRelease`

### Alpha

- Prepare store release notes (English only)
- Publish to alpha channel
- Test update on test device
    
### Beta

- Tag release
  
  ```shell
  git tag v2025.1.1
  git push origin v2025.1.1
  ```

- Promote to beta channel

### Production

- Prepare store release notes
- Merge release pull request to `main`
- Merge changes to dev branch

  ```shell
  git checkout main
  git pull
  git checkout dev
  git merge main
  ```

- Promote to production
- Download universal APK from Play Store and attach to GitHub tag

## Amazon App Store (production only)

- `bundleAmazonRelease`
- Test update on test device
