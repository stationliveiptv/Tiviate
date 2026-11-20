package com.stationlive.iptv4k

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val Night = Color(0xFF070A10)
private val Panel = Color(0xFF121A29)
private val Gold = Color(0xFFD9B35F)
private val Muted = Color(0xFF9AA8BC)
private val Accent = Color(0xFF6650A4)

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        setContent {
            StationLiveApp(
                loadPlaylist = { url, done -> lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { M3uRepository.loadFromUrl(url) } }
                    done(result)
                } },
                loginXtream = { user, pass, done -> lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { XtreamRepository.loadCatalog(user, pass) } }
                    done(result)
                } },
                loadEpisodes = { user, pass, id, done -> lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { XtreamRepository.loadSeriesEpisodes(user, pass, id) } }
                    done(result)
                } },
                loadEpg = { url, done -> lifecycleScope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { XmlTvRepository.load(url) } }
                    done(result)
                } },
                play = { url -> player.setMediaItem(MediaItem.fromUri(url)); player.prepare(); player.play() },
                player = player
            )
        }
    }

    override fun onStop() {
        player.pause()
        super.onStop()
    }

    override fun onDestroy() {
        player.release()
        super.onDestroy()
    }
}

private enum class SourceMode { XTREAM, M3U }
private enum class LibraryTab(val label: String) { LIVE("LIVE TV"), MOVIES("MOVIES"), SERIES("SERIES"), EPG("TV GUIDE") }

@Composable
private fun StationLiveApp(
    loadPlaylist: (String, (Result<List<M3uEntry>>) -> Unit) -> Unit,
    loginXtream: (String, String, (Result<XtreamRepository.Catalog>) -> Unit) -> Unit,
    loadEpisodes: (String, String, String, (Result<List<M3uEntry>>) -> Unit) -> Unit,
    loadEpg: (String, (Result<List<EpgProgram>>) -> Unit) -> Unit,
    play: (String) -> Unit,
    player: ExoPlayer
) {
    var source by remember { mutableStateOf(SourceMode.XTREAM) }
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var playlistUrl by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Connect your authorized Xtream account or M3U playlist.") }
    var live by remember { mutableStateOf(emptyList<M3uEntry>()) }
    var movies by remember { mutableStateOf(emptyList<M3uEntry>()) }
    var series by remember { mutableStateOf(emptyList<M3uEntry>()) }
    var epg by remember { mutableStateOf(emptyList<EpgProgram>()) }
    var tab by remember { mutableStateOf(LibraryTab.LIVE) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var selectedSeries by remember { mutableStateOf<M3uEntry?>(null) }
    var episodes by remember { mutableStateOf(emptyList<M3uEntry>()) }
    var selectedChannel by remember { mutableStateOf<M3uEntry?>(null) }
    var playbackMessage by remember { mutableStateOf("Ready to play") }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackMessage = when (state) {
                    Player.STATE_BUFFERING -> "Buffering stream…"
                    Player.STATE_READY -> "Playing"
                    Player.STATE_ENDED -> "Playback ended"
                    else -> playbackMessage
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playbackMessage = "Playback error: ${error.errorCodeName}. Check stream URL, account access, or provider format."
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("station_live_favorites", Context.MODE_PRIVATE) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("urls", emptySet())?.toSet() ?: emptySet()) }

    val current = when (tab) {
        LibraryTab.LIVE -> live
        LibraryTab.MOVIES -> movies
        LibraryTab.SERIES -> series
        LibraryTab.EPG -> live
    }
    val categories = remember(current, tab) { listOf("All") + current.mapNotNull { it.group?.takeIf(String::isNotBlank) }.distinct().sorted() }
    val filtered = remember(current, query, category, favoritesOnly, favorites, tab) {
        current.filter { entry ->
            (category == "All" || entry.group == category) &&
                (query.isBlank() || entry.title.contains(query, true) || entry.group.orEmpty().contains(query, true)) &&
                (!favoritesOnly || favorites.contains(entry.url))
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Night, surface = Panel)) {
        Surface(Modifier.fillMaxSize(), color = Night) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = "https://raw.githubusercontent.com/stationliveiptv/panel/main/logo.jfif",
                        contentDescription = "STATION LIVE IPTV 4K logo",
                        modifier = Modifier.size(width = 76.dp, height = 56.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("STATION LIVE", color = Gold, fontSize = 25.sp, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
                        Text("IPTV 4K  •  ENTERTAINMENT, YOUR WAY", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
                    }
                    Text("4K", color = Gold, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                }
                Spacer(Modifier.height(14.dp))

                if (!connected) {
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("WELCOME TO STATION LIVE", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = source == SourceMode.XTREAM, onClick = { source = SourceMode.XTREAM }, label = { Text("Xtream Codes") })
                                FilterChip(selected = source == SourceMode.M3U, onClick = { source = SourceMode.M3U }, label = { Text("M3U / M3U8") })
                            }
                            if (source == SourceMode.XTREAM) {
                                OutlinedTextField(user, { user = it }, Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true)
                                OutlinedTextField(pass, { pass = it }, Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                                OutlinedTextField(epgUrl, { epgUrl = it }, Modifier.fillMaxWidth(), label = { Text("Optional XMLTV EPG URL (HTTPS)") }, singleLine = true)
                                Text("Provider server is configured in the app. HTTP is unencrypted; use a test account only.", color = Muted, fontSize = 11.sp)
                            } else {
                                OutlinedTextField(playlistUrl, { playlistUrl = it }, Modifier.fillMaxWidth(), label = { Text("HTTPS M3U playlist URL") }, singleLine = true)
                                OutlinedTextField(epgUrl, { epgUrl = it }, Modifier.fillMaxWidth(), label = { Text("Optional XMLTV EPG URL (HTTPS)") }, singleLine = true)
                            }
                            Button(
                                onClick = {
                                    busy = true
                                    message = "Connecting…"
                                    if (source == SourceMode.XTREAM) {
                                        loginXtream(user.trim(), pass) { result ->
                                            busy = false
                                            result.onSuccess { catalog ->
                                                live = catalog.live; movies = catalog.movies; series = catalog.series
                                                connected = true
                                                message = "Connected: ${live.size} live, ${movies.size} movies, ${series.size} series"
                                                if (epgUrl.isNotBlank()) loadEpg(epgUrl.trim()) { it.onSuccess { data -> epg = data } }
                                            }.onFailure { message = it.message ?: "Unable to connect." }
                                        }
                                    } else {
                                        loadPlaylist(playlistUrl.trim()) { result ->
                                            busy = false
                                            result.onSuccess { live = it; connected = true; message = "Playlist loaded: ${it.size} entries" }
                                                .onFailure { message = it.message ?: "Unable to load playlist." }
                                            if (result.isSuccess && epgUrl.isNotBlank()) loadEpg(epgUrl.trim()) { it.onSuccess { data -> epg = data } }
                                        }
                                    }
                                },
                                enabled = !busy && if (source == SourceMode.XTREAM) user.isNotBlank() && pass.isNotBlank() else playlistUrl.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Night)
                            ) { Text(if (busy) "CONNECTING…" else "CONNECT", fontWeight = FontWeight.Bold) }
                            Text(message, color = Muted, fontSize = 12.sp)
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(when(tab) { LibraryTab.LIVE -> "${live.size} LIVE CHANNELS"; LibraryTab.MOVIES -> "${movies.size} MOVIES"; LibraryTab.SERIES -> "${series.size} SERIES"; LibraryTab.EPG -> "TV PROGRAM GUIDE" }, color = Gold, fontWeight = FontWeight.Bold)
                            Text("Premium entertainment", color = Muted, fontSize = 10.sp)
                        }
                        TextButton(onClick = { favoritesOnly = !favoritesOnly }) { Text(if (favoritesOnly) "★ FAVORITES" else "☆ FAVORITES", color = Gold) }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(LibraryTab.values()) { item ->
                            FilterChip(selected = tab == item, onClick = { tab = item; category = "All"; query = ""; selectedSeries = null }, label = { Text(item.label) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (tab == LibraryTab.EPG) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(epgUrl, { epgUrl = it }, Modifier.weight(1f), label = { Text("XMLTV HTTPS URL") }, singleLine = true)
                            Button(onClick = { if (epgUrl.isNotBlank()) { busy = true; loadEpg(epgUrl.trim()) { r -> busy = false; r.onSuccess { epg = it; message = "Loaded ${it.size} programmes" }.onFailure { message = it.message ?: "EPG failed" } } } }) { Text("LOAD") }
                        }
                        Text(message, color = Muted, fontSize = 11.sp)
                        val epgByChannel = remember(epg) { epg.groupBy { it.channelId } }
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(live) { channel ->
                                val programmes = epgByChannel[channel.tvgId.orEmpty()].orEmpty()
                                    .ifEmpty { epgByChannel[channel.title].orEmpty() }
                                    .sortedBy { it.startUtc }
                                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(13.dp)) {
                                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (!channel.logo.isNullOrBlank()) AsyncImage(model = channel.logo, contentDescription = channel.title, modifier = Modifier.size(34.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Text(channel.title, color = Gold, fontWeight = FontWeight.Bold)
                                        }
                                        if (programmes.isEmpty()) {
                                            Text("No programme data for this channel", color = Muted, fontSize = 11.sp)
                                        } else {
                                            programmes.take(3).forEach { programme ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(programme.startUtc?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "--:--", color = Gold, fontSize = 11.sp)
                                                    Spacer(Modifier.width(10.dp))
                                                    Text(programme.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                                }
                                                programme.description?.takeIf { it.isNotBlank() }?.let {
                                                    Text(it, color = Muted, fontSize = 10.sp, maxLines = 2)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search ${tab.label.lowercase()}") }, singleLine = true)
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(categories) { group ->
                                val count = if (group == "All") current.size else current.count { it.group == group }
                                FilterChip(selected = category == group, onClick = { category = group }, label = { Text("$group ($count)") })
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        if (selectedSeries != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { selectedSeries = null; episodes = emptyList() }) { Text("← SERIES", color = Gold) }
                                Text(selectedSeries!!.title, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            items(if (selectedSeries != null) episodes else filtered) { entry ->
                                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                                    Row(Modifier.fillMaxWidth().clickable {
                                        if (tab == LibraryTab.SERIES && selectedSeries == null && entry.url.startsWith("series:")) {
                                            selectedSeries = entry
                                            busy = true
                                            loadEpisodes(user, pass, entry.url.removePrefix("series:")) { r -> busy = false; r.onSuccess { episodes = it }.onFailure { message = it.message ?: "Unable to load episodes" } }
                                        } else { selectedChannel = entry; if (!entry.url.startsWith("series:")) play(entry.url) }
                                    }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        if (!entry.logo.isNullOrBlank()) AsyncImage(model = entry.logo, contentDescription = entry.title, modifier = Modifier.size(48.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(entry.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                            Text(entry.group ?: if (tab == LibraryTab.SERIES) "Series" else "Uncategorized", color = Muted, fontSize = 11.sp)
                                        }
                                        TextButton(onClick = {
                                            val changed = favorites.toMutableSet()
                                            if (!changed.add(entry.url)) changed.remove(entry.url)
                                            favorites = changed
                                            prefs.edit().putStringSet("urls", changed).apply()
                                        }) { Text(if (favorites.contains(entry.url)) "★" else "☆", color = Gold, fontSize = 20.sp) }
                                        Text(if (tab == LibraryTab.SERIES && selectedSeries == null) "OPEN" else "PLAY", color = Gold, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold)
                        if (selectedChannel != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.Black),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(selectedChannel?.title.orEmpty(), color = Gold, modifier = Modifier.weight(1f), maxLines = 1)
                                        TextButton(onClick = { player.stop(); selectedChannel = null; playbackMessage = "Ready to play" }) { Text("CLOSE", color = Gold) }
                                    }
                                    AndroidView(
                                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
                                        factory = { ctx -> PlayerView(ctx).apply {
                                            this.player = player
                                            useController = true
                                            controllerAutoShow = true
                                            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
                                        } },
                                        update = { it.player = player }
                                    )
                                    Text(playbackMessage, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                    TextButton(onClick = { player.stop(); connected = false; live = emptyList(); movies = emptyList(); series = emptyList(); epg = emptyList(); selectedSeries = null; message = "Disconnected" }) {
                        Text("CHANGE PLAYLIST / LOGOUT", color = Gold)
                    }
                }
            }
        }
    }
}
