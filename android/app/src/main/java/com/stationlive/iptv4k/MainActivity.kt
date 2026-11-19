package com.stationlive.iptv4k

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF080B12)
private val Panel = Color(0xFF121927)
private val Gold = Color(0xFFD6AD55)
private val Blue = Color(0xFF3987D8)
private val Muted = Color(0xFF9AA8BC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StationLiveApp() }
    }
}

private enum class SourceMode { XTREAM, M3U }

@Composable
private fun StationLiveApp() {
    var mode by remember { mutableStateOf(SourceMode.XTREAM) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var playlistUrl by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Choose your connection method to get started.") }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Night) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("STATION LIVE", color = Gold, fontSize = 29.sp, fontWeight = FontWeight.Black)
                Text("IPTV 4K", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("ENTERTAINMENT, YOUR WAY", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(34.dp))

                Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp)) {
                        Text("Connect to your service", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Select a playlist format", color = Muted, fontSize = 13.sp)
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ModeButton("Xtream Codes", mode == SourceMode.XTREAM, Modifier.weight(1f)) { mode = SourceMode.XTREAM; message = "Enter the account details supplied to you." }
                            ModeButton("M3U / M3U8", mode == SourceMode.M3U, Modifier.weight(1f)) { mode = SourceMode.M3U; message = "Paste an authorized playlist URL." }
                        }
                        Spacer(Modifier.height(18.dp))
                        if (mode == SourceMode.XTREAM) {
                            OutlinedTextField(
                                value = username, onValueChange = { username = it }, modifier = Modifier.fillMaxWidth(),
                                label = { Text("Username") }, singleLine = true
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(),
                                label = { Text("Password") }, singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )
                        } else {
                            OutlinedTextField(
                                value = playlistUrl, onValueChange = { playlistUrl = it }, modifier = Modifier.fillMaxWidth(),
                                label = { Text("M3U / M3U8 playlist URL") }, singleLine = true
                            )
                        }
                        Spacer(Modifier.height(18.dp))
                        Button(
                            onClick = {
                                message = when {
                                    mode == SourceMode.XTREAM && username.isBlank() -> "Please enter your username."
                                    mode == SourceMode.XTREAM && password.isBlank() -> "Please enter your password."
                                    mode == SourceMode.M3U && playlistUrl.isBlank() -> "Please enter your playlist URL."
                                    else -> "Connection service is not configured yet. Backend integration is the next step."
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Night),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("CONTINUE", fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(message, color = Muted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(22.dp))
                Text("Secure connection • Live TV • Movies • Series", color = Muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = modifier.background(if (selected) Blue.copy(alpha = 0.22f) else Night, RoundedCornerShape(12.dp))
    ) {
        Text(label, color = if (selected) Gold else Muted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}
