# Autorelease Workflow

This project uses an automated release workflow that handles version bumping, building, and releasing APKs.

## How It Works

The autorelease workflow performs the following steps:

1. **Pre-release CI Check**: Runs the full CI suite to ensure the current code is working
2. **Version Bump**: Increments the version in `app/build.gradle` using the `make_release` script
3. **Commit & Tag**: Commits the version bump and creates a git tag
4. **Post-release CI Build**: Builds the app again with the new version number
5. **Create Release**: Creates a GitHub release with the built APK attached

## Triggering a Release

The autorelease workflow is triggered manually via GitHub Actions:

1. Go to the **Actions** tab in GitHub
2. Select **Auto Release** workflow
3. Click **Run workflow**
4. Choose the version bump type:
   - **patch** (0.0.x) - For bug fixes and minor changes (default)
   - **minor** (0.x.0) - For new features
   - **major** (x.0.0) - For breaking changes

## Local Development with Mise

This project uses [mise](https://mise.jdx.dev/) for task management. Available tasks:

```bash
# Generate code (placeholder for future codegen)
mise run gen

# Build the app
mise run build

# Install and run on connected device
mise run run

# Run tests
mise run test

# Run full CI suite (gen + test + build)
mise run ci

# Clean build artifacts
mise run clean
```

## Manual Version Bumping

You can also bump the version manually:

```bash
# Bump patch version (0.0.x)
./make_release patch

# Bump minor version (0.x.0)
./make_release minor

# Bump major version (x.0.0)
./make_release major
```

The script will:
- Read the current version from `app/build.gradle`
- Increment both `versionCode` and `versionName`
- Update the file in place
- Output the new version number

## Artifacts

After each release:

- **GitHub Release**: Contains the APK as a downloadable asset
- **Workflow Artifacts**: APKs are stored for 90 days in GitHub Actions
- **CI Artifacts**: Build artifacts from regular CI runs are kept for 30 days

## Requirements

- Java 17 (handled by GitHub Actions)
- mise (installed automatically in CI)
- Android SDK (configured via mise in CI)

## CI Workflow

The CI workflow runs on:
- Every push to any branch
- Every pull request
- Can be called by other workflows (used by autorelease)

It performs:
1. Code generation (when configured)
2. Running tests
3. Building the release APK
4. Uploading APK as artifact

## Future Enhancements

- **Codegen**: The `gen` task is currently a placeholder. Add your code generation steps in `.mise.toml`
- **Automated Releases**: Consider setting up scheduled releases or release-on-merge workflows
- **Changelog**: Generate changelogs from commit messages
