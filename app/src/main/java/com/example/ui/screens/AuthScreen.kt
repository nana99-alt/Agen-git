package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Source
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.CenteredLoadingPanel
import com.example.ui.components.ErrorBanner
import com.example.ui.components.StatusPill
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.LuminousVioletContainer
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AuthUiState

private data class OnboardingSlide(
    val badge: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color,
    val container: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    uiState: AuthUiState,
    showConfigSheet: Boolean,
    onDismissConfigSheet: () -> Unit,
    onLoginClick: () -> Unit,
    onExploreWorkspaceClick: () -> Unit,
    onRetrySessionCheck: () -> Unit
) {
    val slides = remember {
        listOf(
            OnboardingSlide(
                badge = "GITHUB WORKSPACE",
                title = "Kelola Repository & Branch",
                subtitle = "Buat repository baru, telusuri berkas proyek, pindah branch, pantau riwayat commit, dan tangani Issues langsung dari ponsel.",
                icon = Icons.Rounded.Source,
                accent = ElectricCyan,
                container = ElectricCyanContainer
            ),
            OnboardingSlide(
                badge = "EDITOR MOBILE",
                title = "Tulis & Commit Kode Cepat",
                subtitle = "Nikmati editor monospace dengan nomor baris, sorotan sintaks real-time, bar simbol kode cepat, dan commit instan.",
                icon = Icons.Rounded.Code,
                accent = EmeraldSuccess,
                container = Color(0xFF12331D)
            ),
            OnboardingSlide(
                badge = "HIGH THINKING AI",
                title = "Asisten Coding Gemini 4 Carbon",
                subtitle = "Audit bug, refaktor kode kompleks, buat pesan commit otomatis, dan terapkan kode usulan AI dalam satu ketukan.",
                icon = Icons.Rounded.AutoAwesome,
                accent = LuminousViolet,
                container = LuminousVioletContainer
            )
        )
    }

    Scaffold(
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        if (uiState is AuthUiState.CheckingSession) {
            CenteredLoadingPanel(
                label = "Menyiapkan KodeGit...",
                modifier = Modifier.padding(innerPadding)
            )
            return@Scaffold
        }

        val isAuthenticating = uiState is AuthUiState.Authenticating
        val errorMessage = (uiState as? AuthUiState.Error)?.message
        val pagerState = rememberPagerState(pageCount = { slides.size })

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Hero header full-bleed bergaya aplikasi Play Store
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_login_hero),
                        contentDescription = "Ilustrasi ruang kerja KodeGit",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        ObsidianBg.copy(alpha = 0.25f),
                                        ObsidianBg.copy(alpha = 0.80f),
                                        ObsidianBg
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.5.dp, ElectricCyan.copy(alpha = 0.45f)),
                            shadowElevation = 8.dp
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_kodegit_logo),
                                contentDescription = "Logo KodeGit",
                                modifier = Modifier.size(64.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "KodeGit",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Studio Kode Mobile, GitHub & Asisten AI",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Carousel onboarding interaktif
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        val slide = slides[page]
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            color = GraphiteSurface,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    StatusPill(
                                        text = slide.badge,
                                        containerColor = slide.container,
                                        contentColor = slide.accent,
                                        mono = true
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(slide.container),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = slide.icon,
                                            contentDescription = null,
                                            tint = slide.accent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = slide.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = slide.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Indikator halaman
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(slides.size) { index ->
                            val selected = pagerState.currentPage == index
                            val dotColor by animateColorAsState(
                                targetValue = if (selected) ElectricCyan else BorderSubtle,
                                label = "pagerDot"
                            )
                            Box(
                                modifier = Modifier
                                    .height(7.dp)
                                    .width(if (selected) 24.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                        }
                    }
                }

                // Area aksi bawah yang bersih
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (errorMessage != null) {
                        ErrorBanner(
                            message = errorMessage,
                            onRetry = onRetrySessionCheck
                        )
                    }

                    Button(
                        onClick = onLoginClick,
                        enabled = !isAuthenticating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("github_login_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Menghubungkan ke GitHub...")
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.OpenInBrowser,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Masuk dengan GitHub",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onExploreWorkspaceClick,
                        enabled = !isAuthenticating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("enter_guest_workspace_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = GraphiteElevated,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Code,
                            contentDescription = null,
                            tint = LuminousViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Mulai di Editor & Asisten AI",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "Login GitHub menggunakan OAuth 2.0 + PKCE melalui browser aman.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showConfigSheet) {
        OAuthSetupGuideSheet(onDismiss = onDismissConfigSheet)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OAuthSetupGuideSheet(onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GraphiteSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusPill(
                text = "PANDUAN GITHUB OAUTH",
                containerColor = ElectricCyanContainer,
                contentColor = ElectricCyan,
                mono = true
            )
            Text(
                text = "Hubungkan GitHub OAuth App",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Agar login browser GitHub berfungsi dengan akun Anda, buat GitHub OAuth App dan isi kredensial pada panel Secrets di AI Studio:",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Surface(
                color = ObsidianBg,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. Authorization callback URL:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "kodegit://oauth/callback",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ElectricCyan
                        )
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString("kodegit://oauth/callback"))
                                copied = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = "Salin Callback URL",
                                tint = ElectricCyan
                            )
                        }
                    }
                    if (copied) {
                        Text(
                            text = "Callback URL disalin ke papan klip.",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess
                        )
                    }
                    Text(
                        text = "2. Tambahkan GITHUB_CLIENT_ID dan GITHUB_CLIENT_SECRET di panel Secrets.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Tutup")
                }
            }
        }
    }
}
