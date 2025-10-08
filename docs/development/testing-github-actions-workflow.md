# Testing GitHub Actions Workflows Locally

This guide explains how to test the release workflows before running them in production.

## Overview

The release process is split into two workflows:

1. **`prepare-release.yml`** - Manually triggered to prepare a release
   - Bumps version (patch/minor/major)
   - Updates CHANGELOG.md
   - Creates release branch (`release/vX.Y.Z`)
   - Opens PR to master for review

2. **`publish-release.yml`** - Auto-triggered when release PR is merged
   - Creates git tag
   - Creates GitHub Release
   - Publishes to npm
   - Comments on PR with links

## Option 1: Test with `act` (Local Simulation)

### Setup

1. **Install act** (already done):
   ```bash
   brew install act
   ```

2. **List available workflows**:
   ```bash
   act -l
   ```

3. **List specific workflow jobs**:
   ```bash
   act workflow_dispatch -l
   ```

### Running Tests

#### Test Prepare Release Workflow:
```bash
# Dry run to see what steps would execute
act workflow_dispatch -W .github/workflows/prepare-release.yml -n

# Test with patch bump (creates branch and PR simulation)
act workflow_dispatch -W .github/workflows/prepare-release.yml --input version_bump=patch

# Test with minor bump
act workflow_dispatch -W .github/workflows/prepare-release.yml --input version_bump=minor
```

#### Test Publish Release Workflow:
```bash
# Dry run to see what steps would execute
act pull_request -W .github/workflows/publish-release.yml -n

# Note: Testing the publish workflow locally is tricky since it requires
# a merged PR context. Better to test on a feature branch (see Option 2).
```

### Limitations of `act`

- ❌ Cannot actually push to GitHub
- ❌ Cannot create real GitHub releases
- ❌ Cannot create real PRs
- ✅ CAN test all the logic, version bumping, changelog generation
- ✅ CAN test npm publish (if you provide token)

## Option 2: Test on GitHub (Recommended)

The safest way is to test the workflows on GitHub:

### Testing Prepare Release Workflow:

1. Go to **Actions** → **"Prepare Release"**
2. Click **"Run workflow"**
3. Select:
   - Branch: `master` (or your feature branch)
   - Version bump: `patch` (for testing)
4. Review the workflow execution:
   - ✅ Version is bumped correctly
   - ✅ CHANGELOG.md is updated
   - ✅ Release branch is created
   - ✅ PR is opened with proper description

### Testing Publish Release Workflow:

The publish workflow runs automatically when a release PR is merged. To test it safely:

1. **Create a test release** using the prepare workflow
2. **Review the PR** that was created
3. **Option A (Safe)**: Just review the PR and close it without merging
4. **Option B (Full test)**: Merge the PR and let it publish, then:
   - Verify GitHub Release was created
   - Verify npm package was published
   - Verify PR comment was added
   - **Important**: If this was a test, unpublish from npm using `npm unpublish react-native-google-fit@X.Y.Z` within 72 hours

### Cleanup After Testing:

```bash
# Delete test release branch
git push --delete origin release/vX.Y.Z

# Delete test tag (if created)
git push --delete origin vX.Y.Z

# Delete local tag
git tag -d vX.Y.Z

# Delete GitHub Release (via GitHub UI or CLI)
gh release delete vX.Y.Z

# Unpublish from npm (within 72 hours only)
npm unpublish react-native-google-fit@X.Y.Z
```

## Option 3: Test Individual Steps Locally

You can test each step manually:

### Test version bump:
```bash
npm version patch --no-git-tag-version
node -p "require('./package.json').version"
git checkout package.json  # restore after testing
```

### Test changelog generation:
```bash
PREV_TAG=$(git describe --tags --abbrev=0)
echo "### [0.21.1] - $(date +%Y-%m-%d)"
git log $PREV_TAG..HEAD --pretty=format:"- %s (%h)" --no-merges
```

### Test dependencies install:
```bash
corepack enable
yarn install
```

### Test npm publish (dry-run):
```bash
npm publish --dry-run
```

## Recommended Testing Flow

1. **First time**: Test individual steps manually (see Option 3)
2. **Before committing**: Run dry-run with `act` to validate workflow syntax
3. **Before production release**: Test on a feature branch with mock version
4. **When ready**: Run the workflow from master branch for real release

## Useful `act` Commands

```bash
# List all workflows
act -l

# See what would run without executing
act -n

# Run specific job
act -j release

# Use secrets file
act --secret-file .secrets

# Verbose output
act -v

# Use specific event
act workflow_dispatch

# See available inputs
act workflow_dispatch --list
```

## Creating Secrets File for Local Testing

Create `.secrets` file (never commit this!):
```bash
cat > .secrets << EOF
NPM_TOKEN=your_npm_token_here
GITHUB_TOKEN=your_github_pat_here
EOF
```

Then run:
```bash
act workflow_dispatch --secret-file .secrets
```

## Common Issues

### Docker requirement
`act` requires Docker to run. Install Docker Desktop if you see Docker errors.

### Permission issues
Some GitHub API calls won't work locally. That's expected - test those on GitHub instead.

### Platform differences
Tests run in Linux containers, so results might differ slightly from your Mac environment.

## Quick Test Commands

```bash
# Validate workflow syntax (dry-run)
act workflow_dispatch -W .github/workflows/prepare-release.yml -n
act pull_request -W .github/workflows/publish-release.yml -n

# Test individual steps manually
npm version patch --no-git-tag-version && \
  echo "Version: $(node -p "require('./package.json').version")" && \
  git checkout package.json

# Test changelog generation
PREV_TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "v0.21.0") && \
  git log $PREV_TAG..HEAD --pretty=format:"- %s (%h)" --no-merges
```

## Production Release Flow

When ready to release for real:

1. **Prepare**: Actions → "Prepare Release" → Run workflow
2. **Review**: Check the PR that was created
3. **Approve**: Review and approve the PR
4. **Merge**: Merge the PR to master
5. **Automatic**: Publish workflow runs automatically
6. **Verify**: Check that tag, release, and npm package were created

