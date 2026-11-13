package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ContentItemDto
import com.example.data.model.GitHubCommitWrapperDto
import com.example.data.model.GitHubIssueDto
import com.example.ui.components.CenteredLoadingPanel
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ErrorBanner
import com.example.ui.components.StatusPill
import com.example.ui.components.formatByteSize
import com.example.ui.components.formatIsoDateShort
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CoralError
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GraphiteElevated
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.LuminousVioletContainer
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.WorkspaceDetailState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoWorkspaceScreen(
    state: WorkspaceDetailState,
    onBack: () -> Unit,
    onSwitchBranch: (String) -> Unit,
    onCreateBranch: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onNavigateUpFolder: () -> Boolean,
    onOpenFile: (String) -> Unit,
    onCreateNewFile: (String) -> Unit,
    onRefreshFiles: () -> Unit,
    onSelectCommit: (String) -> Unit,
    onDismissCommitDetail: () -> Unit,
    onChangeIssueFilter: (String) -> Unit,
    onCreateIssue: (String, String) -> Unit,
    onClearFeedback: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showBranchMenu by remember { mutableStateOf(false) }
    var showNewBranchDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var showNewIssueDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.feedbackMessage) {
        val msg = state.feedbackMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onClearFeedback()
        }
    }

    BackHandler(enabled = selectedTab == 0 && state.currentPath.isNotBlank()) {
        onNavigateUpFolder()
    }

    val repo = state.repo ?: return

    Scaffold(
        containerColor = ObsidianBg,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = GraphiteSurface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("workspace_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Kembali ke daftar repository"
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = repo.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = repo.owner.login,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    },
                    actions = {
                        Box {
                            OutlinedButton(
                                onClick = { showBranchMenu = true },
                                border = BorderStroke(1.dp, BorderSubtle),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("branch_selector_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AccountTree,
                                    contentDescription = "Pilih branch",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = state.activeBranch,
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showBranchMenu,
                                onDismissRequest = { showBranchMenu = false }
                            ) {
                                state.branches.forEach { branch ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = branch.name,
                                                fontFamily = JetBrainsMonoFamily
                                            )
                                        },
                                        leadingIcon = {
                                            if (branch.name == state.activeBranch) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Aktif",
                                                    tint = ElectricCyan
                                                )
                                            }
                                        },
                                        onClick = {
                                            showBranchMenu = false
                                            onSwitchBranch(branch.name)
                                        }
                                    )
                                }
                                HorizontalDivider(color = BorderSubtle)
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Buat Branch Baru...",
                                            color = ElectricCyan,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = null,
                                            tint = ElectricCyan
                                        )
                                    },
                                    onClick = {
                                        showBranchMenu = false
                                        showNewBranchDialog = true
                                    },
                                    modifier = Modifier.testTag("create_branch_menu_item")
                                )
                            }
                        }
                    }
                )

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = GraphiteSurface,
                    contentColor = ElectricCyan
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Kode") },
                        icon = { Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_code")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Commit") },
                        icon = { Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_commits")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Issues") },
                        icon = { Icon(Icons.Rounded.BugReport, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_issues")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 880.dp)
                    .fillMaxSize()
            ) {
                when (selectedTab) {
                    0 -> CodeTreeTab(
                        state = state,
                        onOpenFolder = onOpenFolder,
                        onNavigateUp = { onNavigateUpFolder() },
                        onOpenFile = onOpenFile,
                        onNewFileClick = { showNewFileDialog = true },
                        onRefresh = onRefreshFiles
                    )
                    1 -> CommitHistoryTab(
                        state = state,
                        onSelectCommit = onSelectCommit
                    )
                    2 -> IssuesTab(
                        state = state,
                        onChangeFilter = onChangeIssueFilter,
                        onNewIssueClick = { showNewIssueDialog = true }
                    )
                }
            }
        }
    }

    if (showNewBranchDialog) {
        NewBranchDialog(
            sourceBranch = state.activeBranch,
            onDismiss = { showNewBranchDialog = false },
            onConfirm = { newName ->
                showNewBranchDialog = false
                onCreateBranch(newName)
            }
        )
    }

    if (showNewFileDialog) {
        NewFileDialog(
            basePath = state.currentPath,
            onDismiss = { showNewFileDialog = false },
            onConfirm = { fullPath ->
                showNewFileDialog = false
                onCreateNewFile(fullPath)
            }
        )
    }

    if (showNewIssueDialog) {
        NewIssueDialog(
            onDismiss = { showNewIssueDialog = false },
            onConfirm = { title, body ->
                showNewIssueDialog = false
                onCreateIssue(title, body)
            }
        )
    }

    if (state.selectedCommitDetail != null) {
        CommitDetailSheet(
            commit = state.selectedCommitDetail,
            onDismiss = onDismissCommitDetail
        )
    }
}

@Composable
private fun CodeTreeTab(
    state: WorkspaceDetailState,
    onOpenFolder: (String) -> Unit,
    onNavigateUp: () -> Unit,
    onOpenFile: (String) -> Unit,
    onNewFileClick: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Bilah Breadcrumb & Tombol File Baru
        Surface(
            color = GraphiteElevated,
            border = BorderStroke(1.dp, BorderSubtle)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { onOpenFolder("") }) {
                        Text(
                            text = "root",
                            fontFamily = JetBrainsMonoFamily,
                            color = if (state.currentPath.isEmpty()) ElectricCyan else TextSecondary
                        )
                    }
                    if (state.currentPath.isNotEmpty()) {
                        val segments = state.currentPath.split('/')
                        var built = ""
                        segments.forEachIndexed { idx, seg ->
                            built = if (built.isEmpty()) seg else "$built/$seg"
                            val target = built
                            Text("/", color = TextMuted, fontFamily = JetBrainsMonoFamily)
                            TextButton(onClick = { onOpenFolder(target) }) {
                                Text(
                                    text = seg,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = if (idx == segments.lastIndex) ElectricCyan else TextSecondary
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = onNewFileClick,
                    border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("create_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NoteAdd,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("File Baru", color = ElectricCyan)
                }
            }
        }

        when {
            state.isLoadingFiles -> CenteredLoadingPanel(label = "Memuat isi direktori...")
            state.filesError != null -> {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ErrorBanner(message = state.filesError, onRetry = onRefresh)
                    EmptyStatePanel(
                        icon = Icons.Rounded.NoteAdd,
                        title = "Belum ada berkas di folder ini",
                        subtitle = "Anda dapat langsung membuat file kode baru dan melakukan commit pertama ke GitHub.",
                        actionLabel = "Buat File Baru",
                        onAction = onNewFileClick
                    )
                }
            }
            state.files.isEmpty() -> {
                EmptyStatePanel(
                    icon = Icons.Rounded.Folder,
                    title = "Direktori masih kosong",
                    subtitle = "Tambahkan berkas kode baru untuk mulai menulis.",
                    actionLabel = "Buat File Baru",
                    onAction = onNewFileClick
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.currentPath.isNotEmpty()) {
                        item(key = "__parent__") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(onClick = onNavigateUp),
                                color = GraphiteSurface,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Folder,
                                        contentDescription = "Folder induk",
                                        tint = TextSecondary
                                    )
                                    Text(
                                        text = ".. (Folder Sebelumnya)",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    items(state.files, key = { it.path }) { item ->
                        TreeNodeRow(
                            item = item,
                            onClick = {
                                if (item.type == "dir") {
                                    onOpenFolder(item.path)
                                } else {
                                    onOpenFile(item.path)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TreeNodeRow(
    item: ContentItemDto,
    onClick: () -> Unit
) {
    val isDir = item.type == "dir"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tree_item_${item.name}")
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = GraphiteSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isDir) Icons.Rounded.Folder else Icons.Rounded.Description,
                contentDescription = if (isDir) "Folder" else "Berkas",
                tint = if (isDir) ElectricCyan else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = item.name,
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!isDir) {
                Text(
                    text = formatByteSize(item.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun CommitHistoryTab(
    state: WorkspaceDetailState,
    onSelectCommit: (String) -> Unit
) {
    when {
        state.isLoadingCommits -> CenteredLoadingPanel(label = "Memuat riwayat commit...")
        state.commitsError != null -> ErrorBanner(
            message = state.commitsError,
            modifier = Modifier.padding(16.dp)
        )
        state.commits.isEmpty() -> EmptyStatePanel(
            icon = Icons.Rounded.History,
            title = "Belum ada riwayat commit",
            subtitle = "Commit pertama Anda akan muncul di lini masa ini."
        )
        else -> {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.commits, key = { it.sha }) { wrapper ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("commit_card_${wrapper.sha.take(7)}")
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectCommit(wrapper.sha) },
                        color = GraphiteSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = wrapper.commit.message.lineSequence().firstOrNull().orEmpty(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StatusPill(
                                    text = wrapper.sha.take(7),
                                    containerColor = ElectricCyanContainer,
                                    contentColor = ElectricCyan,
                                    mono = true
                                )
                                Text(
                                    text = wrapper.commit.author?.name ?: wrapper.author?.login ?: "Developer",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatIsoDateShort(wrapper.commit.author?.date.orEmpty()),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IssuesTab(
    state: WorkspaceDetailState,
    onChangeFilter: (String) -> Unit,
    onNewIssueClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.issueStateFilter == "open",
                    onClick = { onChangeFilter("open") },
                    label = { Text("Terbuka") }
                )
                FilterChip(
                    selected = state.issueStateFilter == "closed",
                    onClick = { onChangeFilter("closed") },
                    label = { Text("Tertutup") }
                )
            }
            Button(
                onClick = onNewIssueClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("create_issue_button")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Issue Baru")
            }
        }

        when {
            state.isLoadingIssues -> CenteredLoadingPanel(label = "Memuat daftar Issues...")
            state.issuesError != null -> ErrorBanner(
                message = state.issuesError,
                modifier = Modifier.padding(16.dp)
            )
            state.issues.isEmpty() -> EmptyStatePanel(
                icon = Icons.Rounded.BugReport,
                title = "Tidak ada Issue",
                subtitle = "Laporkan bug atau catat rencana fitur baru lewat Issue GitHub.",
                actionLabel = "Buat Issue Baru",
                onAction = onNewIssueClick
            )
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.issues, key = { it.id }) { issue ->
                        IssueCard(issue)
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueCard(issue: GitHubIssueDto) {
    val isOpen = issue.state == "open"
    Surface(
        color = GraphiteSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusPill(
                    text = "#${issue.number}",
                    containerColor = if (isOpen) EmeraldContainer else LuminousVioletContainer,
                    contentColor = if (isOpen) EmeraldSuccess else LuminousViolet,
                    mono = true
                )
                Text(
                    text = issue.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!issue.body.isNullOrBlank()) {
                Text(
                    text = issue.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "oleh @${issue.user?.login ?: "anon"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChatBubbleOutline,
                        contentDescription = "Komentar",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = issue.comments.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommitDetailSheet(
    commit: GitHubCommitWrapperDto,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GraphiteSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusPill(
                text = commit.sha.take(12),
                containerColor = ElectricCyanContainer,
                contentColor = ElectricCyan,
                mono = true
            )
            Text(
                text = commit.commit.message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${commit.commit.author?.name ?: "Developer"} • ${formatIsoDateShort(commit.commit.author?.date.orEmpty())}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            HorizontalDivider(color = BorderSubtle)

            val files = commit.files.orEmpty()
            if (files.isEmpty()) {
                Text("Tidak ada ringkasan perubahan berkas.", color = TextSecondary)
            } else {
                files.forEach { file ->
                    Surface(
                        color = ObsidianBg,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = file.filename,
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "+${file.additions}  -${file.deletions}",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldSuccess
                                )
                            }
                            if (!file.patch.isNullOrBlank()) {
                                Text(
                                    text = file.patch.take(900),
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewBranchDialog(
    sourceBranch: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var branchName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GraphiteSurface,
        title = { Text("Buat Branch Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Branch baru akan dibuat dari '$sourceBranch'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = branchName,
                    onValueChange = { branchName = it.replace(" ", "-") },
                    label = { Text("Nama Branch") },
                    placeholder = { Text("mis. feat/perbaikan-ui") },
                    singleLine = true,
                    modifier = Modifier.testTag("new_branch_name_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(branchName) },
                enabled = branchName.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_branch_button")
            ) {
                Text("Buat Branch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun NewFileDialog(
    basePath: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val prefix = if (basePath.isBlank()) "" else "$basePath/"
    var filePath by remember { mutableStateOf(prefix) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GraphiteSurface,
        title = { Text("Buat Berkas Kode Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Masukkan jalur dan nama berkas lengkap (mis. src/App.kt).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedTextField(
                    value = filePath,
                    onValueChange = { filePath = it.trimStart('/') },
                    label = { Text("Path Berkas") },
                    singleLine = true,
                    modifier = Modifier.testTag("new_file_path_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(filePath) },
                enabled = filePath.isNotBlank() && !filePath.endsWith('/'),
                modifier = Modifier.testTag("confirm_create_file_button")
            ) {
                Text("Buka di Editor")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

@Composable
private fun NewIssueDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GraphiteSurface,
        title = { Text("Buat Issue Baru") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Issue") },
                    singleLine = true,
                    modifier = Modifier.testTag("new_issue_title_input")
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Deskripsi / Langkah Reproduksi") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.testTag("new_issue_body_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(title, body) },
                enabled = title.isNotBlank(),
                modifier = Modifier.testTag("confirm_create_issue_button")
            ) {
                Text("Kirim Issue")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}
