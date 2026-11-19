import Foundation

/// Provider endpoint configuration. Not shown in the login UI.
/// Values embedded in an app binary can be extracted; this is not a secret.
enum ProviderConfig {
    static let host = "global.linkmac.uk"
    static let port = 2086
    static let baseURL = URL(string: "http://global.linkmac.uk:2086")!
}
