# Project status

## Completed in this foundation branch
- Created an isolated feature branch from `main`.
- Added Android Gradle project configuration and a Compose connection starter screen.
- Added Xtream username/password and M3U URL input modes.
- Added platform scope notes for Windows and iOS.
- Documented the backend security boundary and project status.

## Not completed
- No provider API requests or playback implementation.
- No M3U parser, Xtream API client, EPG, VOD or series catalogue.
- No backend service, database or token service.
- No Windows or iOS app implementation.
- No APK/EXE/IPA release artifact.
- No live-provider integration test or Android build verification.

## Next milestones
1. Add Gradle wrapper and verify an Android debug build.
2. Agree the backend API contract and configure provider details securely on the server.
3. Implement Xtream and M3U parsing behind repository interfaces.
4. Add authorized playback using a maintained media player library.
5. Add EPG, VOD/series, favorites, parental controls and accessibility.
6. Implement Windows and iOS clients against the same documented API contract.
7. Add CI tests, signing and release workflows.
