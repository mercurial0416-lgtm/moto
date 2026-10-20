MOTO HUB Alpha6-DI(518) SOURCE ONLY — NO APK CREATED

User explicitly requested all feasible code improvements but stopping BEFORE APK build.
Base: previously verified 517 fixed-signature checkpoint.
This source tree adds: encrypted backup crash-temp cleanup; source-only 518 version; live
update manifest endpoint and certificate pin; HTTPS release redirect policy; UI 30-row
paging (all 10,000 records reachable); 180ms listing search debounce; 1200px sampled
local motorcycle photos; future signed-release manifest generator and manual guarded
GitHub Release workflow. Existing consent-first GPS and read-only OBD2 behavior stays.

Run pure-Java/source tests only:
  cd MOTO-HUB
  bash tests/run-di-prebuild-tests.sh

Next Android build when expressly requested must use the SAME fixed certificate:
  212c5b39d0082ab4bed78f8da2b719819cd80a20bc116c84f8bdfabca507021b
The private .p12 key and password are stored outside this ZIP.
GitHub Secrets are NOT configured automatically.
Do not distribute an ephemeral-debug-signed APK as an update.

Before building 518, merge this source branch to main or use a controlled checkout,
then run tests, lint, and assembleRelease with the fixed keystore. Manual publishing
of a signed GitHub Release can update main/updates/manifest.json only after a signed
APK passes certificate validation. Do not bypass key fingerprint checking.
