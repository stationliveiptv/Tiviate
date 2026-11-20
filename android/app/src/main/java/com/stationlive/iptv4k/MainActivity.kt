package com.stationlive.iptv4k

import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.widget.Toast
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
                play = { url, engine ->
                    when (engine) {
                        PlaybackEngine.MEDIA3 -> { player.setMediaItem(MediaItem.fromUri(url)); player.prepare(); player.play() }
                        PlaybackEngine.VLC, PlaybackEngine.MPV -> ExternalPlayback.launch(this, engine, url)
                            .onFailure { Toast.makeText(this, it.message ?: "Unable to open external player", Toast.LENGTH_LONG).show() }
                    }
                },
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
    play: (String, PlaybackEngine) -> Unit,
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
    var playbackEngine by remember { mutableStateOf(PlaybackEngine.MEDIA3) }
    var engineMenuExpanded by remember { mutableStateOf(false) }
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
                    Column(
                        Modifier.fillMaxSize().padding(top = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("YOUR ENTERTAINMENT", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                            Spacer(Modifier.height(7.dp))
                            Text("ALL IN ONE PLACE", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                            Spacer(Modifier.height(7.dp))
                            Text("Connect your playlist and enjoy your content", color = Muted, fontSize = 12.sp)
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Panel),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("ADD PLAYLIST", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                Text("Choose your connection", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    Modifier.fillMaxWidth().background(Night, RoundedCornerShape(14.dp)).padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    listOf(SourceMode.XTREAM to "Xtream Codes", SourceMode.M3U to "M3U Playlist").forEach { (mode, label) ->
                                        Box(
                                            Modifier.weight(1f)
                                                .background(if (source == mode) Gold else Color.Transparent, RoundedCornerShape(11.dp))
                                                .clickable { source = mode }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(label, color = if (source == mode) Night else Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                if (source == SourceMode.XTREAM) {
                                    OutlinedTextField(
                                        user, { user = it }, Modifier.fillMaxWidth(),
                                        label = { Text("Username") }, placeholder = { Text("Enter your username") },
                                        singleLine = true, shape = RoundedCornerShape(13.dp)
                                    )
                                    OutlinedTextField(
                                        pass, { pass = it }, Modifier.fillMaxWidth(),
                                        label = { Text("Password") }, placeholder = { Text("Enter your password") },
                                        singleLine = true, shape = RoundedCornerShape(13.dp),
                                        visualTransformation = PasswordVisualTransformation(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                                    )
                                    OutlinedTextField(
                                        epgUrl, { epgUrl = it }, Modifier.fillMaxWidth(),
                                        label = { Text("EPG URL (optional)") }, placeholder = { Text("https://…") },
                                        singleLine = true, shape = RoundedCornerShape(13.dp)
                                    )
                                    Text("Server connection is configured for your service.", color = Muted, fontSize = 10.sp)
                                } else {
                                    OutlinedTextField(
                                        playlistUrl, { playlistUrl = it }, Modifier.fillMaxWidth(),
                                        label = { Text("Playlist URL") }, placeholder = { Text("Paste your M3U / M3U8 link") },
                                        singleLine = true, shape = RoundedCornerShape(13.dp)
                                    )
                                    OutlinedTextField(
                                        epgUrl, { epgUrl = it }, Modifier.fillMaxWidth(),
                                        label = { Text("EPG URL (optional)") }, placeholder = { Text("https://…") },
                                        singleLine = true, shape = RoundedCornerShape(13.dp)
                                    )
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
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Night)
                                ) {
                                    if (busy) CircularProgressIndicator(Modifier.size(18.dp), color = Night, strokeWidth = 2.dp)
                                    else Text("CONNECT & CONTINUE", fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp)
                                }
                                Text(message, color = Muted, fontSize = 11.sp)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text("LIVE TV", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("  •  ", color = Gold)
                            Text("MOVIES", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("  •  ", color = Gold)
                            Text("SERIES", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("  •  ", color = Gold)
                            Text("4K", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                        if (selectedSeries != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { selectedSeries = null; episodes = emptyList() }) { Text("← SERIES", color = Gold) }
                                Text(selectedSeries!!.title, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LazyColumn(Modifier.width(116.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                items(categories) { group ->
                                    val count = if (group == "All") current.size else current.count { it.group == group }
                                    val active = category == group
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable { category = group },
                                        colors = CardDefaults.cardColors(containerColor = if (active) Accent else Panel),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 11.dp)) {
                                            Text(group, color = if (active) Color.White else Muted, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, maxLines = 2)
                                            Text(count.toString(), color = Gold, fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                items(if (selectedSeries != null) episodes else filtered) { entry ->
                                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(13.dp)) {
                                        Row(Modifier.fillMaxWidth().clickable {
                                            if (tab == LibraryTab.SERIES && selectedSeries == null && entry.url.startsWith("series:")) {
                                                selectedSeries = entry
                                                busy = true
                                                loadEpisodes(user, pass, entry.url.removePrefix("series:")) { r -> busy = false; r.onSuccess { episodes = it }.onFailure { message = it.message ?: "Unable to load episodes" } }
                                            } else {
                                                selectedChannel = entry
                                                playbackMessage = "Connecting to stream…"
                                                if (!entry.url.startsWith("series:")) play(entry.url, playbackEngine)
                                            }
                                        }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                            if (!entry.logo.isNullOrBlank()) AsyncImage(model = entry.logo, contentDescription = entry.title, modifier = Modifier.size(42.dp))
                                            Spacer(Modifier.width(8.dp))
                                            Column(Modifier.weight(1f)) {
                                                Text(entry.title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 2)
                                                Text(entry.group ?: "Uncategorized", color = Muted, fontSize = 10.sp, maxLines = 1)
                                            }
                                            TextButton(onClick = {
                                                val changed = favorites.toMutableSet()
                                                if (!changed.add(entry.url)) changed.remove(entry.url)
                                                favorites = changed
                                                prefs.edit().putStringSet("urls", changed).apply()
                                            }, contentPadding = PaddingValues(2.dp)) {
                                                Text(if (favorites.contains(entry.url)) "★" else "☆", color = Gold, fontSize = 18.sp)
                                            }
                                        }
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
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("PLAYER ENGINE", color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                        Box {
                                            TextButton(onClick = { engineMenuExpanded = true }) {
                                                Text(playbackEngine.label, color = Gold, fontWeight = FontWeight.Bold)
                                            }
                                            DropdownMenu(expanded = engineMenuExpanded, onDismissRequest = { engineMenuExpanded = false }) {
                                                PlaybackEngine.values().forEach { engine ->
                                                    DropdownMenuItem(
                                                        text = { Text(engine.label) },
                                                        onClick = {
                                                            playbackEngine = engine
                                                            engineMenuExpanded = false
                                                            playbackMessage = when (engine) {
                                                                PlaybackEngine.MEDIA3 -> "Built-in player selected. Press PLAY on a channel."
                                                                PlaybackEngine.VLC -> "VLC selected. VLC for Android must be installed."
                                                                PlaybackEngine.MPV -> "MPV selected. mpv-android must be installed."
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    if (playbackEngine == PlaybackEngine.MEDIA3) AndroidView(
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
