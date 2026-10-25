"""One current-source version/CI consistency check, reusable by historical feature tests.

Older feature suites should verify the latest build metadata instead of freezing an old
Alpha version (and failing every correctly versioned checkpoint).
"""
from pathlib import Path
import re

ROOT=Path(__file__).resolve().parents[1]

def current_version():
    gradle=(ROOT/'app/build.gradle').read_text()
    match=re.search(r"versionCode\s+(\d+);\s*versionName\s+'4\.0\.0-alpha6-([a-z]+)'",gradle)
    if not match: return None
    code=int(match.group(1));tag=match.group(2)
    return (code,tag) if code>=490 else None

def version_declaration_ok():return current_version() is not None

def debug_alignment_ok():
    version=current_version()
    if not version:return False
    code,tag=version
    workflow=(ROOT/'.github/workflows/android-debug.yml').read_text()
    return f'versionCode {code}' in workflow and f'alpha6-{tag}-source.zip' in workflow

def signed_alignment_ok():
    version=current_version()
    if not version:return False
    _,tag=version
    workflow=(ROOT/'.github/workflows/android-signed-release.yml').read_text()
    return f'moto-hub-alpha6-{tag}-signed-release' in workflow and 'MOTO_SIGNING_STORE_B64' in workflow
