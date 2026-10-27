# UX53 — direct APK update and ride assistance (2026-10-27)

## Direct APK delivery (Google Play not required)
- Non-Play build uses the existing signed APK manifest updater, existing package `com.motohub.next`, exact signer equality, SHA-256 and increasing versionCode.
- Preserve existing release keystore. **Do not uninstall** a differently signed installed app to force an update.
- Main update screen explicitly documents local encrypted backup and Android installation consent.
- Play-specific distribution variant is retained only for compatibility and is not the chosen delivery channel.

## New functions
- Home one-tap shortcut requests explicit GPS consent, uses the current vehicle binding, starts location foreground service, checks whether it has started and only then launches the saved Kakao/NAVER/TMAP app. No OBD link is silently started.
- A stationary hint appears in the ride UI only once per session, after >=12 minutes without a trustworthy >8 km/h GPS movement, with the ride active >=5 minutes, distance >=0.3 km and current GPS fix fresh. **Never auto-stops**, never loses a track for signal loss.
- Foreground ride and existing OBD monitoring remain independent; lost OBD must not stop a GPS ride.
- Unit regressions cover long idle, low-speed drift, missing GPS, moving traffic, invalid elapsed timestamps and single prompt.

## Existing UX50–52 functionality retained
Stable home, repair/maintenance scheduling, offline backup/restore preview, multi-navigation choice, read-only optional OBD, lost-GPS recovery, diagnostic checks, foreground/battery hygiene, and Android UI fit.

## Quality gate
Run `:app:assembleDebug :app:assembleRelease :app:lintDebug :app:testDebugUnitTest` on Android SDK 36.
This source PR **does not** by itself create a same-key signed APK. Releasing requires the **existing original fixed private key** and independent apksigner checks. Physical Galaxy/Duke 390 OBD, long GPS ride and in-place installation still need device verification.
