# Connection and credential security

## Confirmed product decisions
- Provider host: `global.linkmac.uk`
- Provider port: `2086`
- Login UI: username and password only for Xtream Codes.
- Playlist mode: user-supplied M3U/M3U8 URL.
- First delivery target: all planned platforms in parallel.

## Important transport limitation
The supplied provider endpoint is HTTP, not HTTPS. Xtream credentials and playlist URLs may contain sensitive access information. Do not send customer credentials to this endpoint from a public client until the provider offers HTTPS or a secured server-side integration is deployed. Do not enable Android cleartext traffic as a shortcut.

## Recommended production flow
1. Client sends credentials over HTTPS to the STATION LIVE backend.
2. Backend validates the request and contacts the provider using a protected server-side configuration.
3. Backend returns only the minimum account/catalogue data required and a short-lived, scoped session token.
4. Client uses HTTPS APIs and authorized playback URLs; credentials are never written to logs.
5. Tokens are stored using Android Keystore/EncryptedSharedPreferences, iOS Keychain, and Windows DPAPI or Credential Locker.

## Host visibility
Removing host and port from the login screen is a UX choice, not secrecy. Any endpoint embedded in a client binary can be extracted. Keep secrets and provider integration credentials server-side.

## Current status
The app screens are prototypes. No provider request, credential validation, token service, catalogue API or playback is implemented. The provider's API paths, response formats, TLS support and usage authorization still need verification.
