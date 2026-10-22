#!/usr/bin/env python3
"""Pick the next fixed-signature version without downgrading or reusing a published code."""
import argparse
import json
import re
from pathlib import Path

PACKAGE = "com.motohub.next"
EXPECTED_CERT = "212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b"

def choose(gradle_source: str, manifest: dict) -> int:
    match = re.search(r"\bversionCode\s+(\d+)\b", gradle_source)
    if not match:
        raise ValueError("Android source versionCode missing")
    source_code = int(match.group(1))
    if manifest.get("packageName") != PACKAGE or manifest.get("certificateSha256", "").lower() != EXPECTED_CERT:
        raise ValueError("Published package/signing identity mismatch")
    published = manifest.get("versionCode")
    if isinstance(published, bool) or not isinstance(published, int) or published < 1:
        raise ValueError("Invalid published versionCode")
    next_code = max(source_code, published + 1)
    if not 518 <= next_code <= 999999999:
        raise ValueError("Release versionCode out of supported bounds")
    return next_code

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--gradle", required=True)
    parser.add_argument("--manifest", required=True)
    args = parser.parse_args()
    result = choose(Path(args.gradle).read_text(encoding="utf-8"),
                    json.loads(Path(args.manifest).read_text(encoding="utf-8")))
    print(result)
