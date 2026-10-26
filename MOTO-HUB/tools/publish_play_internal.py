#!/usr/bin/env python3
"""Upload a *signed* Android App Bundle to the Google Play internal test track.

Requires existing Play Console app, Google Play Developer API enabled,
a service account with release-management access and explicit opt-in.
This tool never creates or targets a production/open/closed release.
"""
import argparse
import json
import pathlib
import sys
from urllib.parse import quote

try:
    import google.auth.transport.requests
    from google.oauth2 import service_account
    import requests
except ImportError as e:
    raise SystemExit("Install google-auth requests before publishing") from e

PACKAGE = "com.motohub.next"
BASE = "https://androidpublisher.googleapis.com/androidpublisher/v3"
UPLOAD = "https://androidpublisher.googleapis.com/upload/androidpublisher/v3"
SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]


def api_call(session, method, url, *, body=None, payload=None):
    args = {"timeout": 90}
    if body is not None:
        args["json"] = body
    if payload is not None:
        args["data"] = payload
        args["headers"] = {"Content-Type": "application/octet-stream"}
    response = session.request(method, url, **args)
    if not response.ok:
        # Google errors can mention keys/account details; only expose status and bounded text.
        raise RuntimeError(f"Google Play API HTTP {response.status_code}: {response.text[:600]}")
    return response.json() if response.content else {}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--bundle", required=True, type=pathlib.Path)
    parser.add_argument("--service-account", required=True, type=pathlib.Path)
    parser.add_argument("--confirm-internal", required=True)
    args = parser.parse_args()
    if args.confirm_internal != "PUBLISH_INTERNAL_ONLY":
        raise SystemExit("Explicit internal-only confirmation required.")
    if not args.bundle.is_file() or args.bundle.stat().st_size < 100_000:
        raise SystemExit("A signed, nonempty AAB file is required.")
    if not args.service_account.is_file():
        raise SystemExit("Service account credential file unavailable.")

    credentials = service_account.Credentials.from_service_account_file(
        str(args.service_account), scopes=SCOPES
    )
    credentials.refresh(google.auth.transport.requests.Request())
    session = requests.Session()
    session.headers.update({"Authorization": f"Bearer {credentials.token}"})
    app = f"{BASE}/applications/{quote(PACKAGE)}"
    edit = api_call(session, "POST", f"{app}/edits", body={})
    edit_id = edit["id"]
    base = f"{app}/edits/{quote(edit_id)}"
    try:
        with args.bundle.open("rb") as bundle:
            # Upload API supports the media upload protocol.
            uploaded = api_call(
                session, "POST",
                f"{UPLOAD}/applications/{quote(PACKAGE)}/edits/{quote(edit_id)}/bundles?uploadType=media",
                payload=bundle,
            )
        version = int(uploaded["versionCode"])
        if version < 521:
            raise RuntimeError("Unexpected old bundle version. Refusing to publish.")
        api_call(session, "PUT", f"{base}/tracks/internal", body={
            "track": "internal",
            "releases": [{
                "name": f"MOTO HUB internal {version}",
                "status": "completed",
                "versionCodes": [str(version)],
                "releaseNotes": [{"language": "ko-KR", "text": "네이버지도·티맵·카카오내비 선택과 라이딩 안정성 개선"}],
            }],
        })
        api_call(session, "POST", f"{base}:validate", body={})
        api_call(session, "POST", f"{base}:commit", body={})
        print(json.dumps({"package": PACKAGE, "track": "internal",
                          "versionCode": version, "committed": True}))
    except Exception:
        # If validation fails, abandon this edit rather than committing a broken release.
        try:
            session.delete(base, timeout=20)
        except Exception:
            pass
        raise


if __name__ == "__main__":
    main()
