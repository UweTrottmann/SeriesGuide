# Release process

- Collect changes in release branch

  ```bash
  # If it does not exist, create a release branch
  git checkout -b release-2026.1
  
  # If it exists, merge latest changes
  git checkout release-2026.1
  git merge dev
  ```

- Optional: [update translations](/translations/README.md)

  ```bash
  git commit --all --message "Import latest translations"
  ```

- Change version code and name in [`build.gradle.kts`](/build.gradle.kts)
- Update [`CHANGELOG.md`](/CHANGELOG.md)

  ```bash
  ./insert-next-release.sh build.gradle.kts CHANGELOG.md
  ```

- Commit with the suggested message and push

  ```shell
  git commit --all --message "Prepare version 2026.1.1 (23260101)"
  git push --set-upstream origin release-2026.1
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
  git tag v2026.1.2
  git push origin v2026.1.2
  git checkout dev
  git merge release-2026.1
  git push origin dev
  ```

- Promote to beta channel
- Create or update preview release post on forum

### Production

- `bundleAmazonRelease`
- Merge release pull request to `main`
- Download universal APK from Play Store
- [Create GitHub release](https://github.com/UweTrottmann/SeriesGuide/releases/new)
  - title like `SeriesGuide 2026.1.2`
  - get release notes from [`CHANGELOG.md`](/CHANGELOG.md)
  - attach APK
- Prepare release post on forum
- Promote to production
- Publish to Amazon App Store
- Publish release post [on forum](https://discuss.seriesgui.de)
- Post [on Mastodon](https://mastodon.social/@SeriesGuide)
- Merge changes to dev branch

  ```shell
  git checkout dev
  git merge --no-ff release-2026.1
  ```