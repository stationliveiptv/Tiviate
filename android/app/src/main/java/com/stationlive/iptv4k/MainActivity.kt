package com.stationlive.iptv4k

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
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
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Night = Color(0xFF080B12)
private val Panel = Color(0xFF121927)
private val Gold = Color(0xFFD6AD55)
private val Blue = Color(0xFF3987D8)
private val Muted = Color(0xFF9AA8BC)

class MainActivity : ComponentActivity() {
    private lateinit var player: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        player = ExoPlayer.Builder(this).build()
        lifecycleScope.launch {
            setContent {
                StationLiveApp(
                    onLoadPlaylist = { url, done ->
                        lifecycleScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching { M3uRepository.loadFromUrl(url) }
                            }
                            done(result)
                        }
                    },
                    onPlay = { streamUrl -> player.setMediaItem(MediaItem.fromUri(streamUrl)); player.prepare(); player.play() },
                    player = player
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

private enum class SourceMode { XTREAM, M3U }

@Composable
private fun StationLiveApp(
    onLoadPlaylist: (String, (Result<List<M3uEntry>>) -> Unit) -> Unit,
    onPlay: (String) -> Unit,
    player: ExoPlayer
) {
    var mode by remember { mutableStateOf(SourceMode.M3U) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var playlistUrl by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(emptyList<M3uEntry>()) }
    var search by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("All") }
    var message by remember { mutableStateOf("Load an authorized HTTPS M3U/M3U8 playlist to browse channels.") }
    var loading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("station_live_favorites", Context.MODE_PRIVATE) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("urls", emptySet())?.toSet() ?: emptySet()) }
    var favoritesOnly by remember { mutableStateOf(false) }
    val groups = remember(entries) { listOf("All") + entries.mapNotNull { it.group }.distinct().sorted() }
    val visible = remember(entries, search, selectedGroup) {
        entries.filter {
            (selectedGroup == "All" || it.group == selectedGroup) &&
                (search.isBlank() || it.title.contains(search, true) || it.group.orEmpty().contains(search, true)) &&
                (!favoritesOnly || favorites.contains(it.url))
        }
    }

    MaterialTheme {
        Surface(Modifier.fillMaxSize(), color = Night) {
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 22.dp)) {
                Text("STATION LIVE", color = Gold, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text("IPTV 4K  •  ENTERTAINMENT, YOUR WAY", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(16.dp))

                if (entries.isEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Panel)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Connect your playlist", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = mode == SourceMode.M3U, onClick = { mode = SourceMode.M3U }, label = { Text("M3U / M3U8") })
                                FilterChip(selected = mode == SourceMode.XTREAM, onClick = { mode = SourceMode.XTREAM }, label = { Text("Xtream Codes") })
                            }
                            if (mode == SourceMode.M3U) {
                                OutlinedTextField(
                                    value = playlistUrl, onValueChange = { playlistUrl = it },
                                    modifier = Modifier.fillMaxWidth(), label = { Text("HTTPS playlist URL") }, singleLine = true
                                )
                            } else {
                                OutlinedTextField(value = username, onValueChange = { username = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Username") }, singleLine = true)
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                                Text("Xtream login is not connected yet. It requires a secure HTTPS service endpoint.", color = Muted, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                enabled = !loading && mode == SourceMode.M3U && playlistUrl.isNotBlank(),
                                onClick = {
                                    loading = true
                                    message = "Loading playlist…"
                                    onLoadPlaylist(playlistUrl) { result ->
                                        loading = false
                                        result.onSuccess {
                                            entries = it
                                            message = if (it.isEmpty()) "Playlist loaded but contains no playable entries." else "Loaded ${it.size} entries."
                                        }.onFailure { message = it.message ?: "Could not load playlist." }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Night)
                            ) { Text(if (loading) "LOADING…" else "LOAD PLAYLIST", fontWeight = FontWeight.Bold) }
                            Text(message, color = Muted, fontSize = 12.sp)
                        }
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${entries.size} CHANNELS", color = Gold, fontWeight = FontWeight.Bold)
                        FilterChip(selected = favoritesOnly, onClick = { favoritesOnly = !favoritesOnly }, label = { Text("FAVORITES") })
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search channels") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(groups) { group ->
                            FilterChip(selected = selectedGroup == group, onClick = { selectedGroup = group }, label = { Text(group) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(visible) { entry ->
                            Row(
                                Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(12.dp)).clickable { onPlay(entry.url) }.padding(13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text(entry.group ?: "Uncategorized", color = Muted, fontSize = 11.sp)
                                }
                                Text("PLAY", color = Gold, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().height(210.dp).background(Color.Black),
                        factory = { context -> PlayerView(context).apply { this.player = player; useController = true } },
                        update = { it.player = player }
                    )
                    TextButton(onClick = { entries = emptyList(); search = ""; selectedGroup = "All"; player.stop() }) { Text("CHANGE PLAYLIST", color = Gold) }
                }
            }
        }
    }
}
