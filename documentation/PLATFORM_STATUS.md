# Platform status

| Platform | Current state | Next work |
|---|---|---|
| Android / Android TV | Kotlin Compose connection screen and Gradle module scaffold | Build verification, backend client, catalogue, playback and TV navigation |
| Windows | WPF .NET 8 connection screen scaffold | Build verification, backend client, catalogue and playback |
| iOS / iPadOS | SwiftUI connection screen scaffold | Xcode project, backend client, catalogue and AVPlayer playback |
| Backend | OpenAPI contract draft only | Deploy HTTPS service, secure provider integration, token lifecycle and tests |

All three client screens currently validate required fields locally and then display a not-configured message. They do not contact the IPTV provider. No APK, EXE, IPA or production backend has been built or released.

The provider address supplied by the owner uses HTTP. Do not transmit real customer credentials until an HTTPS-protected integration is available and tested.
