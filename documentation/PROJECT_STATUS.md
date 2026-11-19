# Project status

## Implemented in the Android foundation
- Isolated feature branch: `feature/station-live-iptv-4k-foundation`; `main` remains untouched.
- Android Compose application shell with STATION LIVE IPTV 4K dark, gold and blue styling.
- Separate M3U/M3U8 and Xtream login mode selection.
- HTTPS-only M3U playlist retrieval and M3U metadata parsing (title, group, logo URL, TVG ID and duration).
- Live channel list with search, category filtering and locally persisted favorites.
- Media3 ExoPlayer playback surface and player release on Activity destruction.
- XMLTV programme parser.
- GitHub Actions debug APK workflow.
- M3U parser unit tests and XMLTV parser unit tests.

## Verified build
- Android Debug Build run #19 succeeded.
- Artifact: `station-live-iptv-4k-debug`.
- This verifies compilation and packaging only; it does not prove playback against a real provider account.

## Not yet implemented / not verified
- Xtream authentication and live/VOD/series catalogue requests.
- EPG download, channel-to-programme matching and EPG screens.
- Movie and series browsing/details.
- Secure HTTPS backend, token/session service, database and deployment.
- Real provider integration and stream playback validation.
- Android TV-specific remote navigation and layout validation.
- Windows client functionality and EXE packaging.
- iOS/iPadOS client functionality and signed IPA packaging.
- Release signing, privacy/legal review and store readiness.

## Security boundary
- Provider credentials and playlist URLs must not be committed to the repository or written to logs.
- The provider endpoint supplied for this project uses HTTP. Do not send customer credentials directly from the app over HTTP.
- Public Xtream login requires a deployed HTTPS backend proxy with server-side provider configuration and short-lived client sessions.
- Host/port are hidden from the login UI but cannot be treated as secret when embedded in a client binary.

## Next implementation sequence
1. Complete Android M3U/EPG data flow and tests.
2. Implement and deploy the HTTPS backend boundary; configure provider details only as server-side secrets.
3. Connect Xtream login and live/VOD/series/EPG repositories to the Android UI.
4. Run integration tests using a temporary authorized test account after secure transport is ready.
5. Build and validate Android TV, Windows and iOS clients separately.
6. Prepare signed release builds and store assets.
