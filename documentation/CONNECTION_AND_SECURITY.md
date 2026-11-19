# Connection and credential security

## Confirmed product decisions
- Provider host: `global.linkmac.uk`
- Provider port: `2086`
- Transport requested by owner: HTTP
- Xtream login UI: username and password only.
- Host and port are fixed configuration and are not displayed in the login form.
- M3U/M3U8 playlist mode remains a separate option.
- Target platforms: Android/Android TV, Windows and iOS/iPadOS.

## HTTP security warning
The owner confirmed that the existing provider works over HTTP and wants the app to use it. HTTP does not encrypt credentials or traffic. Anyone able to observe the network path may be able to read Xtream usernames, passwords and playlist URLs. This is not secure for public customer use.

## Host visibility
The host and port can be hidden from the user interface, but values embedded in an APK, EXE or IPA can be extracted. They are configuration, not secrets.

## Required implementation checks
- Confirm supported Xtream API paths and response format before implementing authentication.
- Never log usernames, passwords, tokens or full playlist URLs.
- Prefer an HTTPS backend proxy for public deployment. If the provider only supports HTTP, the proxy must run on a trusted server/network and protect client-to-backend traffic with HTTPS.
- Do not claim login works until a real test account and provider API response have been tested.

## Current status
Provider host and port constants have been added to the Android, Windows and iOS client source code and are not shown in the login UI. Actual authentication requests, catalogue loading and playback are not implemented yet.
