# Release Workflows

This directory contains automated workflows for managing releases of `react-native-google-fit`.

## Workflows

### 1. `prepare-release.yml` - Prepare a Release

**Trigger:** Manual (workflow_dispatch)

**Purpose:** Creates a release PR for review before publishing

**Steps:**
1. Bumps version (patch/minor/major)
2. Generates changelog from git commits
3. Updates CHANGELOG.md
4. Creates `release/vX.Y.Z` branch
5. Opens PR to master with release notes

**How to use:**
1. Go to Actions → "Prepare Release"
2. Click "Run workflow"
3. Select version bump type (patch/minor/major)
4. Click "Run workflow"
5. Review the created PR

### 2. `publish-release.yml` - Publish a Release

**Trigger:** Automatic (when PR from `release/*` branch is merged)

**Purpose:** Publishes the release to npm and GitHub

**Steps:**
1. Creates git tag (vX.Y.Z)
2. Creates GitHub Release with notes
3. Publishes package to npm
4. Comments on PR with success links

**How it works:**
- Automatically runs when a release PR is merged
- No manual intervention needed
- Will post results as a comment on the merged PR

## Release Process

```
┌─────────────────────────────────────────┐
│  1. Prepare Release (Manual)            │
│     • Actions → "Prepare Release"       │
│     • Select version bump type          │
│     • Creates PR automatically          │
└─────────────────┬───────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────┐
│  2. Review Release PR                   │
│     • Check version bump                │
│     • Review changelog                  │
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
│  4. Publish Release (Automatic)         │
│     • Creates git tag                   │
│     • Creates GitHub Release            │
│     • Publishes to npm                  │
│     • Posts success comment on PR       │
└─────────────────────────────────────────┘
```

## Benefits

✅ **Safe** - Review changes before publishing to npm  
✅ **Automated** - Minimal manual steps required  
✅ **Auditable** - Full PR history of all releases  
✅ **Flexible** - Can edit changelog/version before publishing  
✅ **Reversible** - Close PR to abort release without publishing  

## Testing

See [docs/development/testing-github-actions-workflow.md](/docs/development/testing-github-actions-workflow.md) for detailed testing instructions.

## Troubleshooting

### Release PR not created
- Check workflow run logs in Actions tab
- Ensure you have proper permissions
- Verify branch name doesn't already exist

### Publish workflow didn't run
- Verify PR was from a `release/*` branch
- Check that PR was merged (not just closed)
- Review workflow logs in Actions tab

### npm publish failed
- Verify `NPM_TOKEN` secret is set
- Check that version doesn't already exist on npm
- Review npm publish logs in workflow output

## Required Secrets

- `NPM_TOKEN` - npm authentication token with publish access

Set this in: Repository Settings → Secrets and variables → Actions → New repository secret

