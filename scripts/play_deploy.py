#!/usr/bin/env python3
"""
play_deploy.py — upload and release an AAB via the Google Play Developer API.

This script automates the steps that DO have API coverage:
    1. Create an "edit" on the Play Developer API
    2. Upload a signed .aab
    3. Assign it to a track (internal / alpha / beta / production) with release notes
    4. Commit the edit (submits the update for Google review)

Steps that DO NOT have API coverage and must be done in the web console once:
    - Upload-key reset (if you've lost your keystore)
    - Data safety form
    - App access / Ads / Target audience / Content rating questionnaires
    - Enrolling in Play App Signing
    - The INITIAL publish of a removed app

Prerequisites (one-time, in the browser):
    1. Create a Google Cloud project and enable the "Google Play Android
       Developer API" for it.
    2. Create a service account in that project, generate a JSON key, and
       save it somewhere safe (for example: keystore/play-service-account.json).
       Do NOT commit the JSON key.
    3. In Play Console -> Users and permissions -> Invite, add the service
       account's email (ends in .gserviceaccount.com) with at least these
       app-level permissions for Sunderkand Path and Chalisa:
         - View app information and download bulk reports
         - Manage production releases
         - Manage testing track releases
    4. Accept the invite (automatic for service accounts).

Usage:
    python scripts/play_deploy.py \\
        --service-account keystore/play-service-account.json \\
        --aab app/build/outputs/bundle/release/app-release.aab \\
        --track internal \\
        --release-name "3.0 (3)" \\
        --release-notes "v3.0 release notes here"

Tracks:
    internal    - fastest, up to 100 internal testers, no review
    alpha       - closed testing
    beta        - open testing
    production  - public release (review takes 1-7 days)

Tip: always test with --track internal first, then promote.
"""

from __future__ import annotations

import argparse
import sys
from pathlib import Path

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

# The single OAuth scope required for the Play Developer API.
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]

# Hard-coded for this project so we never publish the wrong package by accident.
PACKAGE_NAME = "com.nv.user.sunderkand"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Upload a signed AAB to Google Play via the Developer API."
    )
    parser.add_argument(
        "--service-account",
        required=True,
        type=Path,
        help="Path to the service-account JSON key file.",
    )
    parser.add_argument(
        "--aab",
        required=True,
        type=Path,
        help="Path to the signed .aab to upload.",
    )
    parser.add_argument(
        "--track",
        choices=("internal", "alpha", "beta", "production"),
        default="internal",
        help="Release track. Default: internal (safest).",
    )
    parser.add_argument(
        "--release-name",
        default=None,
        help="Human-readable release name. Defaults to the versionName(versionCode).",
    )
    parser.add_argument(
        "--release-notes",
        default="Bug fixes and improvements.",
        help="Release notes (en-US). Keep under 500 characters.",
    )
    parser.add_argument(
        "--rollout-fraction",
        type=float,
        default=1.0,
        help="Staged rollout fraction for production (0.0 - 1.0). Default: 1.0 (full).",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Upload and stage the release, but do NOT commit the edit.",
    )
    return parser.parse_args()


def build_service(service_account_path: Path):
    if not service_account_path.exists():
        sys.exit(f"Service account key not found: {service_account_path}")
    credentials = service_account.Credentials.from_service_account_file(
        str(service_account_path), scopes=SCOPES
    )
    return build("androidpublisher", "v3", credentials=credentials, cache_discovery=False)


def deploy(args: argparse.Namespace) -> None:
    if not args.aab.exists():
        sys.exit(f"AAB not found: {args.aab}")

    service = build_service(args.service_account)
    edits = service.edits()

    print(f"-> Creating edit for {PACKAGE_NAME} ...")
    edit = edits.insert(body={}, packageName=PACKAGE_NAME).execute()
    edit_id = edit["id"]
    print(f"   edit id: {edit_id}")

    print(f"-> Uploading {args.aab} ...")
    media = MediaFileUpload(
        str(args.aab),
        mimetype="application/octet-stream",
        resumable=True,
    )
    bundle = edits.bundles().upload(
        packageName=PACKAGE_NAME,
        editId=edit_id,
        media_body=media,
    ).execute()
    version_code = bundle["versionCode"]
    print(f"   uploaded versionCode: {version_code}")

    release = {
        "name": args.release_name or f"Release {version_code}",
        "versionCodes": [str(version_code)],
        "status": "completed" if args.track != "production" else (
            "completed" if args.rollout_fraction >= 1.0 else "inProgress"
        ),
        "releaseNotes": [
            {"language": "en-US", "text": args.release_notes},
        ],
    }
    if args.track == "production" and args.rollout_fraction < 1.0:
        release["userFraction"] = args.rollout_fraction

    print(f"-> Assigning versionCode {version_code} to track '{args.track}' ...")
    edits.tracks().update(
        packageName=PACKAGE_NAME,
        editId=edit_id,
        track=args.track,
        body={"track": args.track, "releases": [release]},
    ).execute()

    if args.dry_run:
        print("-> Dry run: skipping commit. The edit will be discarded by Google.")
        return

    print("-> Committing edit (submits for review if track is production) ...")
    committed = edits.commit(packageName=PACKAGE_NAME, editId=edit_id).execute()
    print(f"   committed: {committed.get('id')}")
    print("Done.")


def main() -> None:
    try:
        deploy(parse_args())
    except Exception as exc:  # noqa: BLE001
        sys.exit(f"ERROR: {exc}")


if __name__ == "__main__":
    main()
