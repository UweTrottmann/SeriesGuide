# AGENTS.md

Instructions for coding agents (and humans using them) working on SeriesGuide, an Android app to 
track TV shows and movies.

Before writing code, read the [guidelines](docs/guidelines.md). They apply to all new and updated
code and resources. Also see [CONTRIBUTING.md](CONTRIBUTING.md) for project setup notes and how to
contribute changes. And [docs/](docs/) for other helpful information.

## Project map

- `app` – the Android app. Use the `pureDebug` build variant.
  - `src/main` – shared code and resources
  - `src/pure` – Play Store flavor
  - `src/amazon` – Amazon Appstore flavor
- `api` – public extension API library (Apache 2.0, not GPL)
- `backend` – client library for the SeriesGuide Cloud endpoints
- `widgets` – custom Android views used by the app
- `build-logic` – Gradle convention plugins

`dev` is the main development branch, `main` has the latest stable release.

## Converting Java to Kotlin

When changes would modify files written in Java, offer to convert them to Kotlin before.

Follow the two-commit procedure in the guidelines. Without Android Studio, the
`Rename .java to .kt` commit has to be created manually: only rename the file in that commit,
change nothing else.

## Verifying changes

Don't run Gradle builds, tests or lint. The user builds and tests in Android Studio. Output to the
user what should be checked instead.

## Copyright headers

Source files start with SPDX headers like:

```kotlin
// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright © 2026 Uwe Trottmann <uwe@uwetrottmann.com>
```

- New files: add the header with the current year.
- Existing files with non-trivial changes: add the header if it is missing. Never change the
  year of an existing header.
- Except for files in `api`, when making non-trivial changes and the license isn't GPL, yet, add the
  GPL license like `SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-or-later`.

## Commits

- Format the subject as `Area: short imperative description`, for example
  `Sync: restore interrupt state cleared by Cloud auth token task`. The area is a feature,
  component or class name.
- In the body, briefly explain the motivation for the change: why it is needed, not what the
  diff already shows. Trivial commits, like `Rename .java to .kt`, don't need a body.
- Keep commits focused, one logical change each.
- Don't add attribution lines (like `Co-Authored-By`) to commit messages.

## Changelog

Don't edit [CHANGELOG.md](CHANGELOG.md). For user-facing changes, suggest an entry in the reply to
the user using its format, for example `* 🔨 Sync: don't crash when …`.

## Ask before

- Editing translated strings. Only edit `values/strings.xml`. The `values-*/` translations are
  imported from Crowdin.
- Touching secrets or signing config: `secret.properties`, `local.properties`,
  `google-services.json`, keystores, Amazon key files.
- Changing the app version, or adding or upgrading dependencies. A new dependency also needs an
  entry in [CREDITS.txt](CREDITS.txt).
- Changing Room entities or migrations, or the [backup JSON schema](docs/backup-json-schema.md).
