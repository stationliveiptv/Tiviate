namespace StationLive.Windows;

/// <summary>
/// Provider endpoint configuration. Not shown in the login UI.
/// Values embedded in a desktop binary can be extracted; this is not a secret.
/// </summary>
internal static class ProviderConfig
{
    internal const string Host = "global.linkmac.uk";
    internal const int Port = 2086;
    internal const string BaseUrl = "http://global.linkmac.uk:2086";
}
