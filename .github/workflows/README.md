# Release Workflows

This directory contains automated workflows for managing releases of `react-native-google-fit`.

## Workflows

### 1. `prepare-release.yml` - Prepare a Release

**Trigger:** Manual (workflow_dispatch)

**Purpose:** Creates a pre-release and PR for review before publishing

**Steps:**
1. Bumps version (patch/minor/major)
2. Creates and pushes git tag
3. Creates GitHub pre-release with auto-generated notes
4. Updates CHANGELOG.md with release notes
5. Creates `release/vX.Y.Z` branch
6. Opens PR to master with release notes

**How to use:**
1. Go to Actions → "Prepare Release"
2. Click "Run workflow"
3. Select version bump type (patch/minor/major)
4. Click "Run workflow"
5. Review the created PR and pre-release

### 2. `promote-release.yml` - Promote Pre-release to Release

**Trigger:** Automatic (when PR from `release/*` branch is merged)

**Purpose:** Marks the pre-release as a full release

**Steps:**
1. Marks the pre-release as a full release (removes pre-release flag)
2. Comments on PR with success message

**How it works:**
- Automatically runs when a release PR is merged
- Promotes the existing pre-release to a full release
- This triggers the npm publish workflow

### 3. `publish-release.yml` - Publish to npm

**Trigger:** Automatic (when a release is published)

**Purpose:** Publishes the package to npm

**Steps:**
1. Checks out code at the release tag
2. Installs dependencies
3. Publishes package to npm
4. Comments on release with npm link

**How it works:**
- Automatically runs when a release is published (not pre-release)
- Uses the exact code from the git tag
- Posts npm installation instructions as a comment

## Release Process

```
┌─────────────────────────────────────────┐
│  1. Prepare Release (Manual)            │
│     • Actions → "Prepare Release"       │
│     • Select version bump type          │
│     • Creates tag + pre-release         │
│     • Creates PR with GitHub notes      │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  2. Review Release                      │
│     • Check pre-release on GitHub       │
│     • Review auto-generated notes       │
│     • Check version bump in PR          │
│     • Run any final tests               │
│     • Approve the PR                    │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  3. Merge PR (Manual)                   │
│     • Click "Merge pull request"        │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  4. Promote Release (Automatic)         │
│     • Marks pre-release as release      │
│     • Comments on PR                    │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  5. Publish to npm (Automatic)          │
│     • Triggered by release published    │
│     • Publishes package to npm          │
│     • Comments on release with link     │
└─────────────────────────────────────────┘
```

## Benefits

✅ **Safe** - Review changes before publishing to npm  
✅ **Native GitHub Features** - Uses GitHub's auto-generated release notes  
✅ **Pre-release Testing** - Create pre-release first for validation  
✅ **Automated** - Minimal manual steps required  
✅ **Auditable** - Full PR history + GitHub releases  
✅ **Flexible** - Can edit release notes before promoting  
✅ **Reversible** - Close PR or delete pre-release to abort  

## Testing

See [docs/development/testing-github-actions-workflow.md](/docs/development/testing-github-actions-workflow.md) for detailed testing instructions.

## Troubleshooting

### Pre-release not created
- Check workflow run logs in Actions tab
- Ensure you have proper permissions
- Verify tag doesn't already exist

### Release not promoted
- Verify PR was from a `release/*` branch
- Check that PR was merged (not just closed)
- Review promote workflow logs in Actions tab

### npm publish didn't run
- Verify release was promoted (not pre-release)
- Check that release was published (not just created)
- Review publish workflow logs in Actions tab

### npm publish failed
- Verify `NPM_TOKEN` secret is set
- Check that version doesn't already exist on npm
- Ensure the code at the tag builds successfully
- Review npm publish logs in workflow output

## Required Secrets

- `NPM_TOKEN` - npm authentication token with publish access

Set this in: Repository Settings → Secrets and variables → Actions → New repository secret

