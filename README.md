# STATION LIVE IPTV 4K

An independent, multi-platform IPTV client project. This is a clean implementation, not a rebrand or copy of another proprietary player.

## Planned platforms
- `android/`: Android phones and Android TV (Kotlin + Jetpack Compose)
- `windows/`: Windows desktop client (implementation to be selected)
- `ios/`: iPhone and iPad client (implementation to be selected)
- `backend/`: secure provider integration boundary
- `documentation/`: architecture, security and build notes

## Initial Android scope
- Xtream Codes username/password connection mode.
- M3U/M3U8 playlist URL mode.
- Premium dark UI with gold and blue accents.

## Security
- Never commit provider passwords, customer credentials, API keys, signing keys, or private playlist URLs.
- A hostname or port embedded in an APK is extractable; hiding it in the interface does not make it secret. Production should use a secured HTTPS backend and short-lived scoped tokens.
- Use only streams and playlists you are authorized to access and distribute.

## Status
Foundation branch contains Android starter UI and cross-platform documentation. No live provider connection, playback, release artifact, or verified build exists yet.

Open `android/` in Android Studio. Use JDK 17 and an Android Gradle Plugin-compatible Gradle installation.
