# Release Process

QuickNotes uses Release Please to prepare releases from Conventional Commits.

## Commit Format

Use Conventional Commit prefixes on `main`:

```text
feat: add note templates
fix: prevent notification crash on reboot
perf: speed up tag suggestions
docs: update release notes
chore: update dependencies
```

`feat` creates a minor release, `fix` creates a patch release, and a commit with
`BREAKING CHANGE:` in the body creates a major release.

## Release Flow

1. Merge Conventional Commit PRs into `main`.
2. The Release Please workflow opens or updates a release PR.
3. Review the generated `CHANGELOG.md` and version bump.
4. Merge the release PR.
5. Release Please creates a tag and GitHub release.
6. The Android CI tag workflow builds and uploads the release AAB/APK.

## GitHub Token

For the tag-created Android release workflow to run automatically, add a
repository secret named `RELEASE_PLEASE_TOKEN` that uses a fine-grained personal
access token with repository contents and pull request access. If that secret is
not present, Release Please falls back to `GITHUB_TOKEN`, but GitHub may not
trigger the follow-up tag workflow from that token.

## Android Versioning

Release Please updates `appVersionName` in `app/build.gradle.kts`.
`versionCode` is derived from SemVer as:

```text
major * 100000 + minor * 1000 + patch
```

For example, `1.0.8` becomes `100008`.
