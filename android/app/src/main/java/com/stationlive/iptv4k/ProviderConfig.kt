package com.stationlive.iptv4k

/**
 * Provider endpoint configuration.
 * Not shown in the login UI. Values in a client binary are extractable;
 * this is configuration, not a secret.
 */
internal object ProviderConfig {
    const val HOST = "global.linkmac.uk"
    const val PORT = 2086
    const val BASE_URL = "http://$HOST:$PORT"
}
