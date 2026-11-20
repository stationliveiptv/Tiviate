package com.stationlive.iptv4k

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

enum class PlaybackEngine(val label: String) {
    MEDIA3("Media3 (Built-in)"),
    VLC("VLC"),
    MPV("MPV")
}

object ExternalPlayback {
    fun launch(context: Context, engine: PlaybackEngine, streamUrl: String): Result<Unit> = runCatching {
        require(streamUrl.startsWith("http://", true) || streamUrl.startsWith("https://", true)) {
            "Unsupported stream URL"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(streamUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            when (engine) {
                PlaybackEngine.VLC -> setPackage("org.videolan.vlc")
                PlaybackEngine.MPV -> setPackage("is.xyz.mpv")
                PlaybackEngine.MEDIA3 -> error("Media3 is handled inside the app")
            }
        }
        try {
            context.startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            throw IllegalStateException(
                when (engine) {
                    PlaybackEngine.VLC -> "VLC is not installed. Install VLC for Android or select Media3."
                    PlaybackEngine.MPV -> "mpv-android is not installed. Install mpv-android or select Media3."
                    PlaybackEngine.MEDIA3 -> "Select Media3."
                }, error
            )
        }
    }
}
