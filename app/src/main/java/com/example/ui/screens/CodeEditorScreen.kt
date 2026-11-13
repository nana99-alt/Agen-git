package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.automirrored.rounded.WrapText
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.EditorPreferences
import com.example.data.model.FileDocument
import com.example.data.repository.AiChatMessage
import com.example.ui.components.CenteredLoadingPanel
import com.example.ui.components.CodeSyntaxVisualTransformation
import com.example.ui.components.ErrorBanner
import com.example.ui.components.StatusPill
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberModified
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
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AiAssistantUiState
import com.example.ui.viewmodel.CommitUiState
import com.example.ui.viewmodel.EditorDocumentState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    documentState: EditorDocumentState,
    commitState: CommitUiState,
    suggestedCommitMsg: String,
    isSuggestingCommit: Boolean,
    aiState: AiAssistantUiState,
    aiMessages: List<AiChatMessage>,
    editorPrefs: EditorPreferences,
    showBackButton: Boolean = true,
    onBack: () -> Unit = {},
    onNewLocalScratchpad: ((String) -> Unit)? = null,
    onRetryLoad: () -> Unit,
    onContentChange: (String) -> Unit,
    onRevert: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onToggleWordWrap: () -> Unit,
    onPrepareCommit: () -> Unit,
    onSuggestCommitMsg: () -> Unit,
    onCommit: (String) -> Unit,
    onClearCommitFeedback: () -> Unit,
    onAskAi: (String) -> Unit,
    onApplyAiCode: (String) -> Unit,
    onClearAiChat: () -> Unit
) {
    var showAiPanel by remember { mutableStateOf(false) }
    var showCommitDialog by remember { mutableStateOf(false) }
    var showNewScratchpadDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(commitState) {
        if (commitState is CommitUiState.Success) {
            showCommitDialog = false
            snackbarHostState.showSnackbar(commitState.message)
            onClearCommitFeedback()
        }
    }

    val readyDoc = (documentState as? EditorDocumentState.Ready)?.document
    val isRemoteRepoFile = readyDoc != null && readyDoc.owner.isNotBlank() && readyDoc.repo.isNotBlank()

    Scaffold(
        containerColor = ObsidianBg,
        contentWindowInsets = if (showBackButton) WindowInsets.safeDrawing else WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GraphiteSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("editor_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Kembali ke repository"
                            )
                        }
                    }
                },
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = readyDoc?.fileName ?: "Editor Kode",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (readyDoc?.isDirty == true) {
                                StatusPill(
                                    text = if (isRemoteRepoFile) "Diubah" else "Lokal",
                                    containerColor = AmberContainer,
                                    contentColor = AmberModified
                                )
                            }
                        }
                        if (readyDoc != null) {
                            Text(
                                text = if (isRemoteRepoFile) {
                                    "${readyDoc.branch} • ${readyDoc.path}"
                                } else {
                                    "Scratchpad Lokal • ${readyDoc.path}"
                                },
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (readyDoc != null) {
                        if (!isRemoteRepoFile && onNewLocalScratchpad != null) {
                            IconButton(
                                onClick = { showNewScratchpadDialog = true },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Buat file lokal baru",
                                    tint = ElectricCyan
                                )
                            }
                        }
                        IconButton(
                            onClick = { showAiPanel = !showAiPanel },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("open_ai_assistant_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = "Buka Asisten AI",
                                tint = LuminousViolet
                            )
                        }
                        if (isRemoteRepoFile) {
                            Button(
                                onClick = {
                                    onPrepareCommit()
                                    showCommitDialog = true
                                },
                                enabled = readyDoc.isDirty,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricCyan,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("open_commit_dialog_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("Commit", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isWideLayout = maxWidth >= 720.dp

            when (documentState) {
                is EditorDocumentState.Idle,
                is EditorDocumentState.Loading -> {
                    CenteredLoadingPanel(label = "Membuka berkas kode...")
                }
                is EditorDocumentState.Error -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        ErrorBanner(
                            message = documentState.message,
                            onRetry = onRetryLoad
                        )
                    }
                }
                is EditorDocumentState.Ready -> {
                    Row(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .weight(if (isWideLayout && showAiPanel) 0.6f else 1f)
                                .fillMaxHeight()
                        ) {
                            EditorToolbar(
                                document = documentState.document,
                                prefs = editorPrefs,
                                onFontSizeChange = onFontSizeChange,
                                onToggleWordWrap = onToggleWordWrap,
                                onRevert = onRevert,
                                onOpenAi = { showAiPanel = true }
                            )

                            CodeEditorCanvasWithBar(
                                document = documentState.document,
                                prefs = editorPrefs,
                                onContentChange = onContentChange,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (isWideLayout && showAiPanel) {
                            VerticalDivider(color = BorderSubtle)
                            AiAssistantPanel(
                                document = documentState.document,
                                aiState = aiState,
                                messages = aiMessages,
                                onAskAi = onAskAi,
                                onApplyCode = { code -> onApplyAiCode(code) },
                                onClearChat = onClearAiChat,
                                modifier = Modifier
                                    .weight(0.4f)
                                    .fillMaxHeight()
                            )
                        }
                    }

                    if (!isWideLayout && showAiPanel) {
                        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        ModalBottomSheet(
                            onDismissRequest = { showAiPanel = false },
                            sheetState = sheetState,
                            containerColor = GraphiteSurface
                        ) {
                            AiAssistantPanel(
                                document = documentState.document,
                                aiState = aiState,
                                messages = aiMessages,
                                onAskAi = onAskAi,
                                onApplyCode = { code ->
                                    onApplyAiCode(code)
                                    showAiPanel = false
                                },
                                onClearChat = onClearAiChat,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 420.dp, max = 640.dp)
                                    .navigationBarsPadding()
                                    .imePadding()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCommitDialog && readyDoc != null) {
        CommitDialog(
            document = readyDoc,
            initialMessage = suggestedCommitMsg,
            isSuggesting = isSuggestingCommit,
            commitState = commitState,
            onSuggestAi = onSuggestCommitMsg,
            onDismiss = {
                if (commitState !is CommitUiState.Committing) {
                    onClearCommitFeedback()
                    showCommitDialog = false
                }
            },
            onConfirm = onCommit
        )
    }

    if (showNewScratchpadDialog && onNewLocalScratchpad != null) {
        NewScratchpadDialog(
            onDismiss = { showNewScratchpadDialog = false },
            onConfirm = { fileName ->
                showNewScratchpadDialog = false
                onNewLocalScratchpad(fileName)
            }
        )
    }
}

@Composable
private fun EditorToolbar(
    document: FileDocument,
    prefs: EditorPreferences,
    onFontSizeChange: (Int) -> Unit,
    onToggleWordWrap: () -> Unit,
    onRevert: () -> Unit,
    onOpenAi: () -> Unit
) {
    val lineCount = remember(document.editedContent) {
        document.editedContent.count { it == '\n' } + 1
    }
    Surface(
        color = GraphiteElevated,
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            StatusPill(
                text = "$lineCount baris",
                containerColor = ObsidianBg,
                contentColor = TextSecondary,
                mono = true
            )
            IconButton(
                onClick = { onFontSizeChange(-1) },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.TextDecrease,
                    contentDescription = "Perkecil huruf",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "${prefs.fontSizeSp}sp",
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            IconButton(
                onClick = { onFontSizeChange(1) },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Rounded.TextIncrease,
                    contentDescription = "Perbesar huruf",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onToggleWordWrap,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.WrapText,
                    contentDescription = "Aktifkan/matikan word wrap",
                    tint = if (prefs.wordWrap) ElectricCyan else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (document.isDirty && !document.isNewFile) {
                TextButton(onClick = onRevert) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = null,
                        tint = AmberModified,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Kembalikan", color = AmberModified)
                }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onOpenAi,
                border = BorderStroke(1.dp, LuminousViolet.copy(alpha = 0.55f)),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Psychology,
                    contentDescription = null,
                    tint = LuminousViolet,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Tanya AI", color = LuminousViolet, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun CodeEditorCanvasWithBar(
    document: FileDocument,
    prefs: EditorPreferences,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ext = document.fileName.substringAfterLast('.', "")
    val visualTransformation = remember(ext) { CodeSyntaxVisualTransformation(ext) }

    var textFieldValue by remember(document.path, document.sha) {
        mutableStateOf(
            TextFieldValue(
                text = document.editedContent,
                selection = TextRange(document.editedContent.length)
            )
        )
    }

    LaunchedEffect(document.editedContent) {
        if (textFieldValue.text != document.editedContent) {
            textFieldValue = TextFieldValue(
                text = document.editedContent,
                selection = TextRange(document.editedContent.length)
            )
        }
    }

    val lineCount = remember(textFieldValue.text) {
        (textFieldValue.text.count { it == '\n' } + 1).coerceAtLeast(1)
    }
    val lineNumbersText = remember(lineCount) {
        (1..lineCount).joinToString("\n")
    }

    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val lineHeightSp = (prefs.fontSizeSp + 6).sp

    val codeTextStyle = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontSize = prefs.fontSizeSp.sp,
        lineHeight = lineHeightSp,
        color = TextPrimary
    )

    val quickSymbols = remember {
        listOf(
            "Tab" to "    ",
            "{" to "{",
            "}" to "}",
            "(" to "(",
            ")" to ")",
            "->" to " -> ",
            "=" to " = ",
            "\"" to "\"",
            ";" to ";",
            "//" to "// "
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(verticalScroll)
                .padding(vertical = 10.dp)
        ) {
            if (!prefs.wordWrap) {
                Text(
                    text = lineNumbersText,
                    style = codeTextStyle.copy(
                        color = TextMuted,
                        textAlign = TextAlign.End
                    ),
                    modifier = Modifier
                        .background(GraphiteSurface.copy(alpha = 0.55f))
                        .padding(horizontal = 10.dp)
                )
            }

            val editorModifier = if (prefs.wordWrap) {
                Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            } else {
                Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScroll)
                    .padding(horizontal = 12.dp)
            }

            BasicTextField(
                value = textFieldValue,
                onValueChange = { updated ->
                    textFieldValue = updated
                    if (updated.text != document.editedContent) {
                        onContentChange(updated.text)
                    }
                },
                textStyle = codeTextStyle,
                cursorBrush = SolidColor(ElectricCyan),
                visualTransformation = visualTransformation,
                modifier = editorModifier
                    .fillMaxWidth()
                    .testTag("code_editor_input"),
                decorationBox = { innerTextField ->
                    if (textFieldValue.text.isEmpty()) {
                        Text(
                            text = "// Mulai tulis kode di sini atau buka Asisten AI...",
                            style = codeTextStyle.copy(color = TextMuted)
                        )
                    }
                    innerTextField()
                }
            )
        }

        // Bilah Simbol Cepat Mobile IDE
        Surface(
            color = GraphiteSurface,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickSymbols) { (label, snippet) ->
                    Surface(
                        onClick = {
                            val start = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
                            val end = textFieldValue.selection.end.coerceIn(0, textFieldValue.text.length)
                            val minIdx = minOf(start, end)
                            val maxIdx = maxOf(start, end)
                            val newText = textFieldValue.text.replaceRange(minIdx, maxIdx, snippet)
                            val newCursor = minIdx + snippet.length
                            textFieldValue = TextFieldValue(newText, TextRange(newCursor))
                            onContentChange(newText)
                        },
                        color = GraphiteElevated,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = label,
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = ElectricCyan,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiAssistantPanel(
    document: FileDocument,
    aiState: AiAssistantUiState,
    messages: List<AiChatMessage>,
    onAskAi: (String) -> Unit,
    onApplyCode: (String) -> Unit,
    onClearChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var promptText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isBusy = aiState is AiAssistantUiState.Thinking || aiState is AiAssistantUiState.Streaming

    val quickPrompts = listOf(
        "Analisis bug & celah keamanan" to "Analisis kode ini secara mendalam, temukan potensi bug atau edge-case, lalu berikan versi kode yang sudah diperbaiki.",
        "Refaktor lebih bersih" to "Refaktor kode ini agar lebih idiomatik, modular, dan mudah dibaca tanpa mengubah perilaku utamanya.",
        "Jelaskan alur logika" to "Jelaskan cara kerja kode ini langkah demi langkah dengan ringkas dalam Bahasa Indonesia.",
        "Tambah komentar & doc" to "Tambahkan komentar dokumentasi yang jelas dan ringkas pada fungsi/kelas utama di kode ini."
    )

    LaunchedEffect(messages.size, aiState is AiAssistantUiState.Thinking) {
        if (messages.isNotEmpty() || aiState is AiAssistantUiState.Thinking) {
            listState.scrollToItem(messages.size.coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .background(GraphiteSurface)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = LuminousViolet
                )
                Column {
                    Text(
                        text = "Asisten AI KodeGit",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(
                        text = "gemini-4-carbon • Thinking HIGH",
                        containerColor = LuminousVioletContainer,
                        contentColor = LuminousViolet,
                        mono = true
                    )
                }
            }
            if (messages.isNotEmpty()) {
                IconButton(
                    onClick = onClearChat,
                    enabled = !isBusy,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteSweep,
                        contentDescription = "Bersihkan percakapan AI",
                        tint = TextSecondary
                    )
                }
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickPrompts) { (label, fullPrompt) ->
                FilterChip(
                    selected = false,
                    enabled = !isBusy,
                    onClick = { onAskAi(fullPrompt) },
                    label = { Text(label) }
                )
            }
        }

        HorizontalDivider(color = BorderSubtle)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty() && aiState is AiAssistantUiState.Idle) {
                item {
                    Surface(
                        color = ObsidianBg,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Konteks aktif: ${document.fileName}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                            Text(
                                text = "Pilih aksi cepat di atas atau tulis instruksi untuk menganalisis, merefaktor, maupun mengubah kode Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                AiMessageBubble(
                    isUser = msg.isUser,
                    text = msg.text,
                    extractedCode = msg.extractedCode,
                    onApplyCode = onApplyCode
                )
            }

            when (aiState) {
                is AiAssistantUiState.Thinking -> {
                    item {
                        Surface(
                            color = LuminousVioletContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, LuminousViolet.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = LuminousViolet
                                )
                                Text(
                                    text = "Model sedang berpikir mendalam (Thinking HIGH)...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = LuminousViolet
                                )
                            }
                        }
                    }
                }
                is AiAssistantUiState.Streaming -> {
                    item {
                        AiMessageBubble(
                            isUser = false,
                            text = aiState.text,
                            extractedCode = aiState.extractedCode,
                            onApplyCode = null
                        )
                    }
                }
                is AiAssistantUiState.Error -> {
                    item { ErrorBanner(message = aiState.message) }
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
                placeholder = { Text("Tanya atau minta ubah kode...") },
                maxLines = 3,
                enabled = !isBusy,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_prompt_input")
            )
            Button(
                onClick = {
                    val q = promptText
                    promptText = ""
                    onAskAi(q)
                },
                enabled = !isBusy && promptText.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LuminousViolet,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier
                    .height(54.dp)
                    .testTag("send_ai_prompt_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Kirim ke AI"
                )
            }
        }
    }
}

@Composable
private fun AiMessageBubble(
    isUser: Boolean,
    text: String,
    extractedCode: String?,
    onApplyCode: ((String) -> Unit)?
) {
    Surface(
        color = if (isUser) ElectricCyanContainer.copy(alpha = 0.55f) else ObsidianBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            if (isUser) ElectricCyan.copy(alpha = 0.4f) else BorderSubtle
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isUser) "Anda" else "Asisten AI",
                style = MaterialTheme.typography.labelSmall,
                color = if (isUser) ElectricCyan else LuminousViolet,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary
            )
            if (!isUser && extractedCode != null && onApplyCode != null) {
                Button(
                    onClick = { onApplyCode(extractedCode) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldSuccess,
                        contentColor = ObsidianBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_ai_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Terapkan ke Editor", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CommitDialog(
    document: FileDocument,
    initialMessage: String,
    isSuggesting: Boolean,
    commitState: CommitUiState,
    onSuggestAi: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var message by remember(initialMessage) { mutableStateOf(initialMessage) }
    val isCommitting = commitState is CommitUiState.Committing

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GraphiteSurface,
        title = { Text("Commit ke GitHub") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusPill(
                    text = "${document.owner}/${document.repo} • ${document.branch}",
                    containerColor = ElectricCyanContainer,
                    contentColor = ElectricCyan,
                    mono = true
                )
                Text(
                    text = document.path,
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Pesan Commit") },
                    placeholder = { Text("feat: perbarui logika") },
                    minLines = 2,
                    maxLines = 4,
                    enabled = !isCommitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("commit_message_input")
                )
                OutlinedButton(
                    onClick = onSuggestAi,
                    enabled = !isSuggesting && !isCommitting,
                    border = BorderStroke(1.dp, LuminousViolet.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ai_commit_message_button")
                ) {
                    if (isSuggesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = LuminousViolet
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Menyusun pesan commit AI...", color = LuminousViolet)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = LuminousViolet,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Buat Pesan Commit dengan AI", color = LuminousViolet)
                    }
                }

                if (commitState is CommitUiState.Error) {
                    ErrorBanner(message = commitState.message)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(message) },
                enabled = !isCommitting && message.isNotBlank(),
                modifier = Modifier.testTag("confirm_commit_button")
            ) {
                if (isCommitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text("Commit Sekarang")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isCommitting) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun NewScratchpadDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GraphiteSurface,
        title = { Text("Buat Berkas Lokal Baru") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Berkas (mis. Algoritma.kt)") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text("Buat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
