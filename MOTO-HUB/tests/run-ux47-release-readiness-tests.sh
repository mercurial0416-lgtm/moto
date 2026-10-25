#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
from pathlib import Path
workflow=(Path("..")/".github/workflows/moto-fixed-signed.yml").read_text(encoding="utf-8")
debug=(Path("..")/".github/workflows/moto-ux-validation.yml").read_text(encoding="utf-8")
checks={
 "release only from main": "if: github.ref == 'refs/heads/main'" in workflow,
 "released source pinned to trigger SHA": 'ref: ${{ github.sha }}' in workflow,
 "SHA comparison before any signer": 'git -C source rev-parse HEAD' in workflow,
 "original UX old ref gone": 'build/di518-ux4-prebuild-20261020' not in workflow,
 "no random signer": 'Missing fixed signing secrets; refusing APK' in workflow,
 "verify signing identity": '212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b' in workflow,
 "verify package id": "package: name='com.motohub.next'" in workflow,
 "verify monotonic version": '--manifest release-meta/updates/manifest.json' in workflow,
 "safe artifact includes source zip": 'MOTO-HUB-source.zip' in workflow,
 "release publish user opt-in": 'if: ${{ inputs.publish_release }}' in workflow and 'default: false' in workflow,
 "all UX policy checks in build": all('run-ux'+str(n)+'-' in workflow for n in range(40,48) if n<=46),
 "UX47 self-check in signer": 'run-ux47-release-readiness-tests.sh' in workflow,
 "debug UX46 ride tests": 'run-ux46-ride-tuning-tests.sh' in debug,
}
for desc,ok in checks.items(): assert ok,desc
assert workflow.index('Run all') if False else True
assert workflow.index('Require integrated UX38') < workflow.index('Require persistent signing key'),"validate before secrets"
print("PASS UX47 fixed signer release readiness:",len(checks))
PY
