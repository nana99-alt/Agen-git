package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.core.EditorPreferences
import com.example.data.model.GitHubUserDto
import com.example.ui.components.StatusPill
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.LuminousVioletContainer
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: GitHubUserDto?,
    editorPrefs: EditorPreferences,
    onConnectGitHub: () -> Unit,
    onLogout: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onToggleWordWrap: () -> Unit
) {
    var showGuideSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GraphiteSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Text(
                        text = "Profil & Pengaturan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Kartu Akun GitHub
                item {
                    Surface(
                        color = GraphiteSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (user != null && !user.avatarUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = user.avatarUrl,
                                        contentDescription = "Avatar ${user.login}",
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_kodegit_logo),
                                        contentDescription = "Logo KodeGit",
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user?.name ?: user?.login ?: "Belum Terhubung ke GitHub",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (user != null) "@${user.login} • ${user.publicRepos} repo publik" else "Mode Workspace Lokal",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                StatusPill(
                                    text = if (user != null) "Terhubung" else "Lokal",
                                    containerColor = if (user != null) EmeraldContainer else ElectricCyanContainer,
                                    contentColor = if (user != null) EmeraldSuccess else ElectricCyan
                                )
                            }

                            HorizontalDivider(color = BorderSubtle)

                            if (user != null) {
                                OutlinedButton(
                                    onClick = onLogout,
                                    border = BorderStroke(1.dp, CoralError.copy(alpha = 0.5f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralError),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("settings_logout_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Keluar dari Akun GitHub")
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = onConnectGitHub,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ElectricCyan,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("settings_connect_github_button")
                                    ) {
                                        Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Hubungkan GitHub", fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { showGuideSheet = true },
                                        border = BorderStroke(1.dp, BorderSubtle)
                                    ) {
                                        Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Panduan")
                                    }
                                }
                            }
                        }
                    }
                }

                // Pengaturan Editor Kode
                item {
                    Surface(
                        color = GraphiteSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Code, contentDescription = null, tint = ElectricCyan)
                                Text(
                                    text = "Tampilan Editor Kode",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Ukuran Huruf Monospace", style = MaterialTheme.typography.bodyMedium)
                                StatusPill(
                                    text = "${editorPrefs.fontSizeSp} sp",
                                    containerColor = ObsidianBg,
                                    contentColor = ElectricCyan,
                                    mono = true
                                )
                            }

                            Slider(
                                value = editorPrefs.fontSizeSp.toFloat(),
                                onValueChange = { target ->
                                    val delta = target.toInt() - editorPrefs.fontSizeSp
                                    if (delta != 0) onFontSizeChange(delta)
                                },
                                valueRange = 10f..22f,
                                steps = 11
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Word Wrap Otomatis", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "Bungkus baris panjang agar tidak perlu gulir horizontal.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                Switch(
                                    checked = editorPrefs.wordWrap,
                                    onCheckedChange = { onToggleWordWrap() }
                                )
                            }
                        }
                    }
                }

                // Informasi Mesin AI
                item {
                    Surface(
                        color = GraphiteSurface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = LuminousViolet)
                                Text(
                                    text = "Mesin Asisten AI",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            StatusPill(
                                text = "Firebase AI Logic • gemini-3.8-flash • Thinking HIGH",
                                containerColor = LuminousVioletContainer,
                                contentColor = LuminousViolet,
                                mono = true
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = "Menggunakan Gemini 3.8 Flash (Free Tier tanpa perlu billing, dengan cadangan otomatis gemini-3.5-flash-lite) untuk analisis kode, deteksi bug, refaktor, dan saran pesan commit.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showGuideSheet) {
        OAuthSetupGuideSheet(onDismiss = { showGuideSheet = false })
    }
}
