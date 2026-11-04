#!/usr/bin/env bash
set -euo pipefail
cd /workspace/moto
source /workspace/toolchains/activate.sh
# The external host must supply its own supported, authenticated Codex CLI.
exec codex exec --sandbox workspace-write -C /workspace/moto - < automation/DEVELOPMENT_PROMPT.md
