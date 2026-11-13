package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FileDocument
import com.example.data.repository.AiChatMessage
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AiAssistantUiState
import com.example.ui.viewmodel.EditorDocumentState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudioTabScreen(
    documentState: EditorDocumentState,
    aiState: AiAssistantUiState,
    messages: List<AiChatMessage>,
    onAskAi: (String) -> Unit,
    onApplyCodeAndOpenEditor: (String) -> Unit,
    onClearChat: () -> Unit
) {
    val activeDoc = (documentState as? EditorDocumentState.Ready)?.document
    var promptText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val isBusy = aiState is AiAssistantUiState.Thinking || aiState is AiAssistantUiState.Streaming

    val quickPrompts = listOf(
        "Analisis Bug" to "Analisis kode ini secara mendalam, temukan potensi bug atau edge-case, lalu berikan versi kode yang sudah diperbaiki.",
        "Refaktor Kode" to "Refaktor kode ini agar lebih bersih, modular, dan idiomatik tanpa mengubah fungsi utamanya.",
        "Buat Kode Contoh" to "Buatkan implementasi fungsi Kotlin/Jetpack Compose yang bersih dan siap pakai sesuai konteks berkas ini.",
        "Jelaskan Logika" to "Jelaskan cara kerja kode ini langkah demi langkah dengan ringkas dalam Bahasa Indonesia."
    )

    LaunchedEffect(messages.size, aiState) {
        if (messages.isNotEmpty() || aiState is AiAssistantUiState.Streaming) {
            listState.animateScrollToItem(messages.size.coerceAtLeast(0))
        }
    }

    Scaffold(
        containerColor = ObsidianBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GraphiteSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = LuminousViolet
                        )
                        Column {
                            Text(
                                text = "Asisten AI KodeGit",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "gemini-4-carbon • Thinking HIGH",
                                style = MaterialTheme.typography.labelSmall,
                                color = LuminousViolet
                            )
                        }
                    }
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(
                            onClick = onClearChat,
                            enabled = !isBusy,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "Bersihkan percakapan AI"
                            )
                        }
                    }
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
            Column(
                modifier = Modifier
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (activeDoc != null) {
                    Surface(
                        color = GraphiteElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Konteks Editor: ${activeDoc.path}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                            StatusPill(
                                text = if (activeDoc.editedContent.isBlank()) "Kosong" else "${activeDoc.editedContent.lines().size} baris",
                                containerColor = ObsidianBg,
                                contentColor = TextSecondary,
                                mono = true
                            )
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickPrompts) { (label, fullPrompt) ->
                        FilterChip(
                            selected = false,
                            enabled = !isBusy && activeDoc != null,
                            onClick = { onAskAi(fullPrompt) },
                            label = { Text(label) }
                        )
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (messages.isEmpty() && aiState is AiAssistantUiState.Idle) {
                        item {
                            AiEmptyStudioCard(activeDoc = activeDoc)
                        }
                    }

                    items(messages, key = { it.id }) { msg ->
                        StudioChatBubble(
                            isUser = msg.isUser,
                            text = msg.text,
                            extractedCode = msg.extractedCode,
                            onCopyCode = {
                                scope.launch { snackbarHostState.showSnackbar("Kode disalin ke papan klip.") }
                            },
                            onApplyCode = { code ->
                                onApplyCodeAndOpenEditor(code)
                            }
                        )
                    }

                    when (aiState) {
                        is AiAssistantUiState.Thinking -> {
                            item {
                                Surface(
                                    color = LuminousVioletContainer.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = BorderStroke(1.dp, LuminousViolet.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.5.dp,
                                            color = LuminousViolet
                                        )
                                        Text(
                                            text = "Model gemini-4-carbon sedang berpikir mendalam (Thinking HIGH)...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = LuminousViolet
                                        )
                                    }
                                }
                            }
                        }
                        is AiAssistantUiState.Streaming -> {
                            item {
                                StudioChatBubble(
                                    isUser = false,
                                    text = aiState.text,
                                    extractedCode = aiState.extractedCode,
                                    onCopyCode = {},
                                    onApplyCode = null
                                )
                            }
                        }
                        is AiAssistantUiState.Error -> {
                            item {
                                ErrorBanner(message = aiState.message)
                            }
                        }
                        else -> Unit
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        placeholder = { Text("Tanya AI atau minta ubah kode...") },
                        maxLines = 3,
                        enabled = !isBusy && activeDoc != null,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("studio_ai_prompt_input")
                    )
                    Button(
                        onClick = {
                            val q = promptText
                            promptText = ""
                            onAskAi(q)
                        },
                        enabled = !isBusy && activeDoc != null && promptText.isNotBlank(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuminousViolet,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("studio_send_ai_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Kirim pertanyaan ke AI"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiEmptyStudioCard(activeDoc: FileDocument?) {
    Surface(
        color = GraphiteSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatusPill(
                text = "MODE PENALARAN MENDALAM",
                containerColor = LuminousVioletContainer,
                contentColor = LuminousViolet,
                mono = true
            )
            Text(
                text = "Siap Membantu Analisis & Refaktor Kode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (activeDoc != null) {
                    "Asisten membaca isi berkas '${activeDoc.path}' di Editor. Minta AI membuat kode baru, mencari bug, atau merapikan struktur kode lalu terapkan ke Editor dalam satu ketukan."
                } else {
                    "Buka berkas kode terlebih dahulu agar Asisten AI dapat menganalisisnya."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun StudioChatBubble(
    isUser: Boolean,
    text: String,
    extractedCode: String?,
    onCopyCode: () -> Unit,
    onApplyCode: ((String) -> Unit)?
) {
    val clipboard = LocalClipboardManager.current
    Surface(
        color = if (isUser) ElectricCyanContainer.copy(alpha = 0.55f) else GraphiteSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isUser) ElectricCyan.copy(alpha = 0.4f) else BorderSubtle
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isUser) "Anda" else "Asisten AI • High Thinking",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUser) ElectricCyan else LuminousViolet,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary
            )
            if (!isUser && extractedCode != null && onApplyCode != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(extractedCode))
                            onCopyCode()
                        },
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.weight(0.42f)
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Salin")
                    }
                    Button(
                        onClick = { onApplyCode(extractedCode) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldSuccess,
                            contentColor = ObsidianBg
                        ),
                        modifier = Modifier
                            .weight(0.58f)
                            .testTag("studio_apply_ai_code_button")
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Terapkan ke Editor", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
