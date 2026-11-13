package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.core.KodeGitContainer
import com.example.core.configureAppCheck
import com.example.data.model.GitHubUserDto
import com.example.data.repository.GitHubRepository
import com.example.ui.screens.AiStudioTabScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CodeEditorScreen
import com.example.ui.screens.GitHubConnectSheet
import com.example.ui.screens.HomeDashboardScreen
import com.example.ui.screens.RepoWorkspaceScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanContainer
import com.example.ui.theme.GraphiteSurface
import com.example.ui.theme.KodeGitTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.EditorAiViewModel
import com.example.ui.viewmodel.EditorDocumentState
import com.example.ui.viewmodel.WorkspaceViewModel

private const val ROUTE_AUTH = "auth"
private const val ROUTE_MAIN_SHELL = "main_shell"
private const val ROUTE_WORKSPACE = "workspace"
private const val ROUTE_EDITOR_DETAIL = "editor_detail"

private data class MainTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {

    private val container by lazy { KodeGitContainer(applicationContext) }

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModel.Factory(container.gitHubRepository, container.sessionDataStore)
    }
    private val workspaceViewModel: WorkspaceViewModel by viewModels {
        WorkspaceViewModel.Factory(container.gitHubRepository)
    }
    private val editorAiViewModel: EditorAiViewModel by viewModels {
        EditorAiViewModel.Factory(
            container.gitHubRepository,
            container.aiAssistantRepository,
            container.sessionDataStore
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // WAJIB: Inisialisasi App Check dengan launch intent sebelum UI/ViewModel/Firebase beroperasi
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        handleOAuthIntent(intent)

        setContent {
            KodeGitTheme {
                KodeGitApp(
                    authViewModel = authViewModel,
                    workspaceViewModel = workspaceViewModel,
                    editorAiViewModel = editorAiViewModel,
                    onLaunchOAuthBrowser = ::openOAuthBrowser
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val data = intent?.data
        val isCallback = data?.scheme == "kodegit" && data.host == "oauth"
        if (!isCallback) {
            authViewModel.cancelAuthenticatingIfIdle()
        }
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        if (data.scheme == "kodegit" && data.host == "oauth" && data.path == "/callback") {
            intent.data = null
            authViewModel.handleOAuthRedirect(data)
        }
    }

    private fun openOAuthBrowser(uri: Uri) {
        try {
            val colorParams = CustomTabColorSchemeParams.Builder()
                .setToolbarColor(GraphiteSurface.toArgb())
                .build()
            val customTabsIntent = CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(colorParams)
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(this, uri)
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}

@Composable
private fun KodeGitApp(
    authViewModel: AuthViewModel,
    workspaceViewModel: WorkspaceViewModel,
    editorAiViewModel: EditorAiViewModel,
    onLaunchOAuthBrowser: (Uri) -> Unit
) {
    val navController = rememberNavController()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val isVerifyingToken by authViewModel.isVerifyingToken.collectAsStateWithLifecycle()
    val tokenLoginError by authViewModel.tokenLoginError.collectAsStateWithLifecycle()
    var showConnectSheet by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthUiState.Authenticated -> {
                showConnectSheet = false
                workspaceViewModel.loadRepositories()
                if (navController.currentDestination?.route == ROUTE_AUTH) {
                    navController.navigate(ROUTE_MAIN_SHELL) {
                        popUpTo(ROUTE_AUTH) { inclusive = true }
                    }
                }
            }
            is AuthUiState.GuestWorkspace -> {
                if (navController.currentDestination?.route == ROUTE_AUTH) {
                    navController.navigate(ROUTE_MAIN_SHELL) {
                        popUpTo(ROUTE_AUTH) { inclusive = true }
                    }
                }
            }
            is AuthUiState.Unauthenticated -> {
                if (navController.currentDestination?.route != ROUTE_AUTH) {
                    navController.navigate(ROUTE_AUTH) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> Unit
        }
    }

    NavHost(
        navController = navController,
        startDestination = ROUTE_AUTH
    ) {
        composable(ROUTE_AUTH) {
            AuthScreen(
                uiState = authState,
                showConfigSheet = false,
                onDismissConfigSheet = {},
                onLoginClick = {
                    authViewModel.clearTokenLoginError()
                    showConnectSheet = true
                },
                onExploreWorkspaceClick = { authViewModel.enterGuestWorkspace() },
                onRetrySessionCheck = { authViewModel.checkExistingSession() }
            )
        }

        composable(ROUTE_MAIN_SHELL) {
            val currentUser = (authState as? AuthUiState.Authenticated)?.user
            MainTabbedShell(
                user = currentUser,
                workspaceViewModel = workspaceViewModel,
                editorAiViewModel = editorAiViewModel,
                onConnectGitHub = {
                    authViewModel.clearTokenLoginError()
                    showConnectSheet = true
                },
                onLogout = authViewModel::logout,
                onOpenRepoWorkspace = { repo ->
                    workspaceViewModel.openRepository(repo)
                    navController.navigate(ROUTE_WORKSPACE)
                }
            )
        }

        composable(ROUTE_WORKSPACE) {
            val detailState by workspaceViewModel.detailState.collectAsStateWithLifecycle()
            val repo = detailState.repo
            if (repo == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
                return@composable
            }

            RepoWorkspaceScreen(
                state = detailState,
                onBack = { navController.popBackStack() },
                onSwitchBranch = workspaceViewModel::switchBranch,
                onCreateBranch = workspaceViewModel::createBranch,
                onOpenFolder = { path -> workspaceViewModel.loadFolderContents(path) },
                onNavigateUpFolder = workspaceViewModel::navigateUpFolder,
                onOpenFile = { path ->
                    editorAiViewModel.openRemoteFile(
                        owner = repo.owner.login,
                        repo = repo.name,
                        branch = detailState.activeBranch,
                        path = path
                    )
                    navController.navigate(ROUTE_EDITOR_DETAIL)
                },
                onCreateNewFile = { fullPath ->
                    editorAiViewModel.startNewFile(
                        owner = repo.owner.login,
                        repo = repo.name,
                        branch = detailState.activeBranch,
                        fullPath = fullPath
                    )
                    navController.navigate(ROUTE_EDITOR_DETAIL)
                },
                onRefreshFiles = { workspaceViewModel.loadFolderContents() },
                onSelectCommit = workspaceViewModel::loadCommitDetail,
                onDismissCommitDetail = workspaceViewModel::dismissCommitDetail,
                onChangeIssueFilter = { filter -> workspaceViewModel.loadIssues(filter) },
                onCreateIssue = workspaceViewModel::createIssue,
                onClearFeedback = workspaceViewModel::clearFeedback
            )
        }

        composable(ROUTE_EDITOR_DETAIL) {
            val docState by editorAiViewModel.documentState.collectAsStateWithLifecycle()
            val commitState by editorAiViewModel.commitState.collectAsStateWithLifecycle()
            val suggestedMsg by editorAiViewModel.suggestedCommitMsg.collectAsStateWithLifecycle()
            val isSuggesting by editorAiViewModel.isSuggestingCommit.collectAsStateWithLifecycle()
            val aiState by editorAiViewModel.aiState.collectAsStateWithLifecycle()
            val aiMessages by editorAiViewModel.aiMessages.collectAsStateWithLifecycle()
            val editorPrefs by editorAiViewModel.editorPrefs.collectAsStateWithLifecycle()
            val detailState by workspaceViewModel.detailState.collectAsStateWithLifecycle()

            CodeEditorScreen(
                documentState = docState,
                commitState = commitState,
                suggestedCommitMsg = suggestedMsg,
                isSuggestingCommit = isSuggesting,
                aiState = aiState,
                aiMessages = aiMessages,
                editorPrefs = editorPrefs,
                showBackButton = true,
                onBack = { navController.popBackStack() },
                onRetryLoad = {
                    val repo = detailState.repo
                    val doc = (docState as? EditorDocumentState.Ready)?.document
                    if (repo != null && doc != null) {
                        editorAiViewModel.openRemoteFile(
                            owner = repo.owner.login,
                            repo = repo.name,
                            branch = doc.branch,
                            path = doc.path
                        )
                    } else {
                        navController.popBackStack()
                    }
                },
                onContentChange = editorAiViewModel::updateEditorContent,
                onRevert = editorAiViewModel::revertToOriginal,
                onFontSizeChange = editorAiViewModel::changeFontSize,
                onToggleWordWrap = editorAiViewModel::toggleWordWrap,
                onPrepareCommit = editorAiViewModel::prepareCommitSuggestion,
                onSuggestCommitMsg = editorAiViewModel::generateAiCommitMessage,
                onCommit = { msg ->
                    editorAiViewModel.commitChanges(msg) {
                        workspaceViewModel.loadFolderContents()
                        workspaceViewModel.loadCommits()
                    }
                },
                onClearCommitFeedback = editorAiViewModel::clearCommitFeedback,
                onAskAi = editorAiViewModel::askAi,
                onApplyAiCode = editorAiViewModel::applyAiCodeToEditor,
                onClearAiChat = editorAiViewModel::clearAiConversation
            )
        }
    }

    if (showConnectSheet) {
        GitHubConnectSheet(
            oauthConfigured = authViewModel.isOAuthConfigured(),
            isVerifyingToken = isVerifyingToken,
            tokenError = tokenLoginError,
            onDismiss = {
                authViewModel.clearTokenLoginError()
                showConnectSheet = false
            },
            onOpenTokenGenerator = {
                onLaunchOAuthBrowser(Uri.parse(GitHubRepository.GITHUB_TOKEN_CREATE_URL))
            },
            onSubmitToken = { token ->
                authViewModel.loginWithToken(token) {
                    showConnectSheet = false
                }
            },
            onStartOAuthBrowser = {
                authViewModel.startOAuthLogin(
                    onNeedsConfig = { showConnectSheet = true },
                    onLaunchBrowser = onLaunchOAuthBrowser
                )
            }
        )
    }
}

@Composable
private fun MainTabbedShell(
    user: GitHubUserDto?,
    workspaceViewModel: WorkspaceViewModel,
    editorAiViewModel: EditorAiViewModel,
    onConnectGitHub: () -> Unit,
    onLogout: () -> Unit,
    onOpenRepoWorkspace: (com.example.data.model.GitHubRepoDto) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }
    val tabs = remember {
        listOf(
            MainTabItem("Beranda", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
            MainTabItem("Editor", Icons.Rounded.Code, Icons.Outlined.Code, "nav_editor"),
            MainTabItem("Asisten AI", Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_ai"),
            MainTabItem("Pengaturan", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
        )
    }

    val repoListState by workspaceViewModel.repoListState.collectAsStateWithLifecycle()
    val searchQuery by workspaceViewModel.searchQuery.collectAsStateWithLifecycle()
    val visibilityFilter by workspaceViewModel.visibilityFilter.collectAsStateWithLifecycle()
    val isCreatingRepo by workspaceViewModel.isCreatingRepo.collectAsStateWithLifecycle()
    val createRepoError by workspaceViewModel.createRepoError.collectAsStateWithLifecycle()

    val docState by editorAiViewModel.documentState.collectAsStateWithLifecycle()
    val commitState by editorAiViewModel.commitState.collectAsStateWithLifecycle()
    val suggestedMsg by editorAiViewModel.suggestedCommitMsg.collectAsStateWithLifecycle()
    val isSuggesting by editorAiViewModel.isSuggestingCommit.collectAsStateWithLifecycle()
    val aiState by editorAiViewModel.aiState.collectAsStateWithLifecycle()
    val aiMessages by editorAiViewModel.aiMessages.collectAsStateWithLifecycle()
    val editorPrefs by editorAiViewModel.editorPrefs.collectAsStateWithLifecycle()
    val activeDocName = (docState as? EditorDocumentState.Ready)?.document?.fileName ?: "Main.kt"

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            containerColor = ObsidianBg,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = GraphiteSurface,
                        tonalElevation = 0.dp
                    ) {
                        tabs.forEachIndexed { index, item ->
                            val selected = selectedTab == index
                            NavigationBarItem(
                                selected = selected,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyanContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = GraphiteSurface
                    ) {
                        tabs.forEachIndexed { index, item ->
                            val selected = selectedTab == index
                            NavigationRailItem(
                                selected = selected,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricCyan,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyanContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (selectedTab) {
                        0 -> HomeDashboardScreen(
                            user = user,
                            activeScratchpadName = activeDocName,
                            repoListState = repoListState,
                            searchQuery = searchQuery,
                            visibilityFilter = visibilityFilter,
                            isCreatingRepo = isCreatingRepo,
                            createRepoError = createRepoError,
                            onSearchChange = workspaceViewModel::updateSearchQuery,
                            onFilterChange = workspaceViewModel::updateVisibilityFilter,
                            onRefresh = workspaceViewModel::loadRepositories,
                            onConnectGitHub = onConnectGitHub,
                            onJumpToEditor = { selectedTab = 1 },
                            onJumpToAi = { selectedTab = 2 },
                            onOpenRepo = onOpenRepoWorkspace,
                            onCreateRepo = { name, desc, isPrivate, autoInit, gitignore, onDone ->
                                workspaceViewModel.createRepository(
                                    name = name,
                                    description = desc,
                                    isPrivate = isPrivate,
                                    autoInit = autoInit,
                                    gitignoreTemplate = gitignore
                                ) { created ->
                                    onDone()
                                    onOpenRepoWorkspace(created)
                                }
                            },
                            onDismissCreateError = workspaceViewModel::clearCreateRepoError
                        )
                        1 -> CodeEditorScreen(
                            documentState = docState,
                            commitState = commitState,
                            suggestedCommitMsg = suggestedMsg,
                            isSuggestingCommit = isSuggesting,
                            aiState = aiState,
                            aiMessages = aiMessages,
                            editorPrefs = editorPrefs,
                            showBackButton = false,
                            onNewLocalScratchpad = { name ->
                                editorAiViewModel.resetToNewLocalFile(name)
                            },
                            onRetryLoad = { editorAiViewModel.openLocalScratchpad() },
                            onContentChange = editorAiViewModel::updateEditorContent,
                            onRevert = editorAiViewModel::revertToOriginal,
                            onFontSizeChange = editorAiViewModel::changeFontSize,
                            onToggleWordWrap = editorAiViewModel::toggleWordWrap,
                            onPrepareCommit = editorAiViewModel::prepareCommitSuggestion,
                            onSuggestCommitMsg = editorAiViewModel::generateAiCommitMessage,
                            onCommit = { msg ->
                                editorAiViewModel.commitChanges(msg) {
                                    workspaceViewModel.loadFolderContents()
                                    workspaceViewModel.loadCommits()
                                }
                            },
                            onClearCommitFeedback = editorAiViewModel::clearCommitFeedback,
                            onAskAi = editorAiViewModel::askAi,
                            onApplyAiCode = editorAiViewModel::applyAiCodeToEditor,
                            onClearAiChat = editorAiViewModel::clearAiConversation
                        )
                        2 -> AiStudioTabScreen(
                            documentState = docState,
                            aiState = aiState,
                            messages = aiMessages,
                            onAskAi = editorAiViewModel::askAi,
                            onApplyCodeAndOpenEditor = { code ->
                                editorAiViewModel.applyAiCodeToEditor(code)
                                selectedTab = 1
                            },
                            onClearChat = editorAiViewModel::clearAiConversation
                        )
                        3 -> SettingsScreen(
                            user = user,
                            editorPrefs = editorPrefs,
                            onConnectGitHub = onConnectGitHub,
                            onLogout = onLogout,
                            onFontSizeChange = editorAiViewModel::changeFontSize,
                            onToggleWordWrap = editorAiViewModel::toggleWordWrap
                        )
                    }
                }
            }
        }
    }
}
