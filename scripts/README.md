# Deploy scripts

Helpers for building and publishing **Sunderkand Path and Chalisa**
(`com.nv.user.sunderkand`) on Google Play.

## Files

| File | Purpose |
|---|---|
| `play_deploy.py` | Uploads a signed AAB and creates a release on a track via the Google Play Developer API. |
| `requirements.txt` | Python dependencies for `play_deploy.py`. |

## What this automates (and what it does not)

**Can** be done via the Google Play Developer API and this script:
- Upload an AAB to an existing, set-up app
- Create a release on `internal` / `alpha` / `beta` / `production`
- Set release notes
- Submit for review (commit edit)
- Staged rollouts on production

**Cannot** be done via any API (Google policy — must be done in the Play
Console web UI):
- Upload-key reset
- Enrolling in Play App Signing
- Filling the Data safety form
- App access / Ads / Target audience / Content rating questionnaires
- Setting the Privacy policy URL in App content

## One-time setup

### 1. Create a Google Cloud project and enable the API

1. Open https://console.cloud.google.com/ and create a project (e.g.
   `naveen-apps-play-publish`). If you already have one for Hiker's Watch,
   reuse it — one service account can publish multiple apps in the same
   Play account.
2. Open https://console.cloud.google.com/apis/library/androidpublisher.googleapis.com
   with that project selected and click **Enable**.

### 2. Create a service account

1. https://console.cloud.google.com/iam-admin/serviceaccounts -> **Create
   service account**. Name it `play-publisher`.
2. Skip granting project-level roles (it only needs Play Console access).
3. Open the created service account -> **Keys** -> **Add key** -> **Create
   new key** -> **JSON**. Save the downloaded file as
   `keystore/play-service-account.json`. It is already git-ignored.

### 3. Invite the service account into Play Console

1. Open Play Console -> **Users and permissions** -> **Invite new users**.
2. Enter the service account's email (looks like
   `play-publisher@<project>.iam.gserviceaccount.com`).
3. Under **App permissions**, add Sunderkand Path and Chalisa with at
   minimum:
   - View app information and download bulk reports
   - Manage production releases
   - Manage testing track releases
4. Send invite (service accounts auto-accept).

### 4. Install Python deps

```bash
pip install -r scripts/requirements.txt
```

## Usage

Always test on the `internal` track first:

```bash
python scripts/play_deploy.py \
  --service-account keystore/play-service-account.json \
  --aab app/build/outputs/bundle/release/app-release.aab \
  --track internal \
  --release-name "3.0 (7)" \
  --release-notes "v3.0 release notes here"
```

Once happy, promote to production:

```bash
python scripts/play_deploy.py \
  --service-account keystore/play-service-account.json \
  --aab app/build/outputs/bundle/release/app-release.aab \
  --track production \
  --release-name "3.0 (7)" \
  --release-notes "v3.0 release notes here"
```

Staged rollout (e.g. 10% of users):

```bash
python scripts/play_deploy.py ... --track production --rollout-fraction 0.10
```

Dry-run (uploads but does not commit):

```bash
python scripts/play_deploy.py ... --dry-run
```

## End-to-end: build + deploy

```bash
# From the repo root, after bumping versionCode in app/build.gradle
./gradlew clean bundleRelease
python scripts/play_deploy.py \
  --service-account keystore/play-service-account.json \
  --aab app/build/outputs/bundle/release/app-release.aab \
  --track production \
  --release-notes "$(git log -1 --format=%s)"
```

## Secrets

Never commit:
- `keystore/play-service-account.json`
- `keystore/upload.jks`
- `keystore.properties`

All three are in `.gitignore`.
