package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.OpenInBrowser
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.GitHubRepoDto
import com.example.data.model.GitHubUserDto
import com.example.ui.components.CenteredLoadingPanel
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ErrorBanner
import com.example.ui.components.LanguageDot
import com.example.ui.components.StatusPill
import com.example.ui.components.formatIsoDateShort
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberModified
import com.example.ui.theme.BorderSubtle
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
import com.example.ui.viewmodel.RepoListUiState
import com.example.ui.viewmodel.RepoVisibilityFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    user: GitHubUserDto?,
    activeScratchpadName: String,
    repoListState: RepoListUiState,
    searchQuery: String,
    visibilityFilter: RepoVisibilityFilter,
    isCreatingRepo: Boolean,
    createRepoError: String?,
    onSearchChange: (String) -> Unit,
    onFilterChange: (RepoVisibilityFilter) -> Unit,
    onRefresh: () -> Unit,
    onConnectGitHub: () -> Unit,
    onJumpToEditor: () -> Unit,
    onJumpToAi: () -> Unit,
    onOpenRepo: (GitHubRepoDto) -> Unit,
    onCreateRepo: (
        name: String,
        description: String,
        isPrivate: Boolean,
        autoInit: Boolean,
        gitignoreTemplate: String?,
        onDone: () -> Unit
    ) -> Unit,
    onDismissCreateError: () -> Unit
) {
    var showCreateSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GraphiteSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (user != null && !user.avatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = "Avatar ${user.login}",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.ic_kodegit_logo),
                                contentDescription = "Logo KodeGit",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }
                        Column {
                            Text(
                                text = if (user != null) "Halo, ${user.name ?: user.login}" else "KodeGit Studio",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (user != null) "@${user.login}" else "Workspace Lokal Aktif",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (user != null) EmeraldSuccess else ElectricCyan
                            )
                        }
                    }
                },
                actions = {
                    if (user != null) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("refresh_repos_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Muat ulang repository"
                            )
                        }
                    } else {
                        StatusPill(
                            text = "Offline",
                            containerColor = GraphiteElevated,
                            contentColor = TextSecondary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (user != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        onDismissCreateError()
                        showCreateSheet = true
                    },
                    containerColor = ElectricCyan,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("create_repo_fab"),
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("Repo Baru", fontWeight = FontWeight.Bold) }
                )
            }
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
                    .widthIn(max = 840.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Bento Quick Actions Row
                item {
                    Text(
                        text = "Aksi Cepat",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BentoActionCard(
                            title = if (user != null) "Repo Baru" else "Hubungkan",
                            subtitle = if (user != null) "Buat di GitHub" else "Akun GitHub",
                            icon = if (user != null) Icons.Rounded.CreateNewFolder else Icons.Rounded.OpenInBrowser,
                            accent = ElectricCyan,
                            container = ElectricCyanContainer,
                            onClick = {
                                if (user != null) {
                                    onDismissCreateError()
                                    showCreateSheet = true
                                } else {
                                    onConnectGitHub()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_action_repo")
                        )
                        BentoActionCard(
                            title = "Editor Kode",
                            subtitle = activeScratchpadName,
                            icon = Icons.Rounded.Code,
                            accent = EmeraldSuccess,
                            container = EmeraldContainer,
                            onClick = onJumpToEditor,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_action_editor")
                        )
                        BentoActionCard(
                            title = "Asisten AI",
                            subtitle = "High Thinking",
                            icon = Icons.Rounded.AutoAwesome,
                            accent = LuminousViolet,
                            container = LuminousVioletContainer,
                            onClick = onJumpToAi,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("quick_action_ai")
                        )
                    }
                }

                if (user == null) {
                    item {
                        GitHubConnectCard(onConnect = onConnectGitHub)
                    }
                    item {
                        ScratchpadResumeCard(
                            fileName = activeScratchpadName,
                            onOpen = onJumpToEditor
                        )
                    }
                } else {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onSearchChange,
                                placeholder = { Text("Cari repository GitHub...") },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("repo_search_input")
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RepoVisibilityFilter.entries.forEach { filter ->
                                    FilterChip(
                                        selected = visibilityFilter == filter,
                                        onClick = { onFilterChange(filter) },
                                        label = { Text(filter.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricCyanContainer,
                                            selectedLabelColor = ElectricCyan
                                        ),
                                        modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }

                    when (repoListState) {
                        is RepoListUiState.Loading -> {
                            item {
                                Box(modifier = Modifier.height(220.dp)) {
                                    CenteredLoadingPanel(label = "Memuat daftar repository...")
                                }
                            }
                        }
                        is RepoListUiState.Error -> {
                            item {
                                ErrorBanner(message = repoListState.message, onRetry = onRefresh)
                            }
                        }
                        is RepoListUiState.Success -> {
                            val filtered = repoListState.repos.filter { repo ->
                                val matchesQuery = searchQuery.isBlank() ||
                                    repo.name.contains(searchQuery, ignoreCase = true) ||
                                    (repo.description?.contains(searchQuery, ignoreCase = true) == true)
                                val matchesVis = when (visibilityFilter) {
                                    RepoVisibilityFilter.ALL -> true
                                    RepoVisibilityFilter.PUBLIC -> !repo.isPrivate
                                    RepoVisibilityFilter.PRIVATE -> repo.isPrivate
                                }
                                matchesQuery && matchesVis
                            }

                            if (filtered.isEmpty()) {
                                item {
                                    EmptyStatePanel(
                                        icon = Icons.Rounded.FolderOpen,
                                        title = "Belum ada repository yang cocok",
                                        subtitle = "Buat repository baru langsung di GitHub atau ubah filter pencarian Anda.",
                                        actionLabel = "Buat Repo Baru",
                                        onAction = { showCreateSheet = true }
                                    )
                                }
                            } else {
                                items(filtered, key = { it.id }) { repo ->
                                    DashboardRepoCard(
                                        repo = repo,
                                        onClick = { onOpenRepo(repo) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateSheet) {
        CreateRepoModalSheet(
            isCreating = isCreatingRepo,
            errorMessage = createRepoError,
            onDismiss = { if (!isCreatingRepo) showCreateSheet = false },
            onSubmit = { name, desc, isPrivate, autoInit, gitignore ->
                onCreateRepo(name, desc, isPrivate, autoInit, gitignore) {
                    showCreateSheet = false
                }
            }
        )
    }
}

@Composable
private fun BentoActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    container: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = GraphiteSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(container),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun GitHubConnectCard(onConnect: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            ElectricCyanContainer.copy(alpha = 0.7f),
                            GraphiteSurface
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                StatusPill(
                    text = "SINKRONISASI GITHUB",
                    containerColor = ObsidianBg.copy(alpha = 0.7f),
                    contentColor = ElectricCyan,
                    mono = true
                )
                Text(
                    text = "Sambungkan ke Akun GitHub Anda",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Akses seluruh repository pribadi & publik, buat repository baru, kelola branch, commit perubahan kode, dan pantau Issues.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Button(
                    onClick = onConnect,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("dashboard_connect_github_button")
                ) {
                    Icon(Icons.Rounded.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Masuk dengan GitHub", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ScratchpadResumeCard(
    fileName: String,
    onOpen: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen),
        color = GraphiteSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EmeraldContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Code,
                    contentDescription = null,
                    tint = EmeraldSuccess
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Lanjutkan di Editor Kode",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Berkas aktif: $fileName • Siap dianalisis AI",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Buka editor",
                tint = TextSecondary
            )
        }
    }
}

@Composable
private fun DashboardRepoCard(
    repo: GitHubRepoDto,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("repo_card_${repo.name}")
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = GraphiteSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (repo.isPrivate) Icons.Rounded.Lock else Icons.Rounded.Public,
                        contentDescription = if (repo.isPrivate) "Privat" else "Publik",
                        tint = if (repo.isPrivate) AmberModified else ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = repo.fullName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                StatusPill(
                    text = if (repo.isPrivate) "Privat" else "Publik",
                    containerColor = if (repo.isPrivate) AmberContainer else ElectricCyanContainer,
                    contentColor = if (repo.isPrivate) AmberModified else ElectricCyan
                )
            }

            if (!repo.description.isNullOrBlank()) {
                Text(
                    text = repo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LanguageDot(language = repo.language)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.StarOutline,
                        contentDescription = "Bintang",
                        tint = TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = repo.stargazersCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                StatusPill(
                    text = repo.defaultBranch,
                    containerColor = GraphiteElevated,
                    contentColor = TextSecondary,
                    mono = true
                )
                Spacer(Modifier.weight(1f))
                if (repo.updatedAt.isNotBlank()) {
                    Text(
                        text = formatIsoDateShort(repo.updatedAt),
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
private fun CreateRepoModalSheet(
    isCreating: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (String, String, Boolean, Boolean, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(false) }
    var autoInit by remember { mutableStateOf(true) }
    var selectedGitignore by remember { mutableStateOf<String?>(null) }
    val templates = listOf(null to "Tanpa Template", "Android" to "Android", "Kotlin" to "Kotlin", "Python" to "Python")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = GraphiteSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Buat Repository Baru",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.replace(" ", "-") },
                label = { Text("Nama Repository") },
                placeholder = { Text("mis. proyek-kodegit") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_repo_name_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi (Opsional)") },
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("new_repo_desc_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Repository Privat", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Hanya Anda dan kolaborator yang dapat melihat kode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Switch(
                    checked = isPrivate,
                    onCheckedChange = { isPrivate = it },
                    modifier = Modifier.testTag("new_repo_private_switch")
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { autoInit = !autoInit }
            ) {
                Checkbox(checked = autoInit, onCheckedChange = { autoInit = it })
                Spacer(Modifier.width(4.dp))
                Column {
                    Text("Inisialisasi dengan README.md", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Disarankan agar branch awal langsung siap diedit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            if (autoInit) {
                Text(
                    text = "Template .gitignore",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    templates.forEach { (value, label) ->
                        FilterChip(
                            selected = selectedGitignore == value,
                            onClick = { selectedGitignore = value },
                            label = { Text(label, maxLines = 1) }
                        )
                    }
                }
            }

            if (errorMessage != null) {
                ErrorBanner(message = errorMessage)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss, enabled = !isCreating) {
                    Text("Batal")
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = { onSubmit(name, description, isPrivate, autoInit, selectedGitignore) },
                    enabled = !isCreating && name.isNotBlank(),
                    modifier = Modifier.testTag("confirm_create_repo_button")
                ) {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Buat Repository")
                }
            }
        }
    }
}
