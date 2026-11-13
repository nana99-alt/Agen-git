package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.ContentItemDto
import com.example.data.model.GitHubBranchDto
import com.example.data.model.GitHubCommitWrapperDto
import com.example.data.model.GitHubIssueDto
import com.example.data.model.GitHubRepoDto
import com.example.data.repository.GitHubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RepoVisibilityFilter(val label: String) {
    ALL("Semua"),
    PUBLIC("Publik"),
    PRIVATE("Privat")
}

sealed interface RepoListUiState {
    data object Loading : RepoListUiState
    data class Success(val repos: List<GitHubRepoDto>) : RepoListUiState
    data class Error(val message: String) : RepoListUiState
}

data class WorkspaceDetailState(
    val repo: GitHubRepoDto? = null,
    val branches: List<GitHubBranchDto> = emptyList(),
    val activeBranch: String = "main",
    val currentPath: String = "",
    val files: List<ContentItemDto> = emptyList(),
    val isLoadingFiles: Boolean = false,
    val filesError: String? = null,
    val commits: List<GitHubCommitWrapperDto> = emptyList(),
    val isLoadingCommits: Boolean = false,
    val commitsError: String? = null,
    val selectedCommitDetail: GitHubCommitWrapperDto? = null,
    val isLoadingCommitDetail: Boolean = false,
    val issues: List<GitHubIssueDto> = emptyList(),
    val issueStateFilter: String = "open",
    val isLoadingIssues: Boolean = false,
    val issuesError: String? = null,
    val isMutating: Boolean = false,
    val feedbackMessage: String? = null,
    val feedbackIsError: Boolean = false
)

class WorkspaceViewModel(
    private val gitHubRepository: GitHubRepository
) : ViewModel() {

    private val _repoListState = MutableStateFlow<RepoListUiState>(RepoListUiState.Loading)
    val repoListState: StateFlow<RepoListUiState> = _repoListState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _visibilityFilter = MutableStateFlow(RepoVisibilityFilter.ALL)
    val visibilityFilter: StateFlow<RepoVisibilityFilter> = _visibilityFilter.asStateFlow()

    private val _isCreatingRepo = MutableStateFlow(false)
    val isCreatingRepo: StateFlow<Boolean> = _isCreatingRepo.asStateFlow()

    private val _createRepoError = MutableStateFlow<String?>(null)
    val createRepoError: StateFlow<String?> = _createRepoError.asStateFlow()

    private val _detailState = MutableStateFlow(WorkspaceDetailState())
    val detailState: StateFlow<WorkspaceDetailState> = _detailState.asStateFlow()

    fun loadRepositories() {
        viewModelScope.launch {
            _repoListState.value = RepoListUiState.Loading
            gitHubRepository.fetchUserRepos()
                .onSuccess { repos ->
                    _repoListState.value = RepoListUiState.Success(repos)
                }
                .onFailure { err ->
                    _repoListState.value = RepoListUiState.Error(
                        err.message ?: "Gagal memuat daftar repository."
                    )
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateVisibilityFilter(filter: RepoVisibilityFilter) {
        _visibilityFilter.value = filter
    }

    fun clearCreateRepoError() {
        _createRepoError.value = null
    }

    fun createRepository(
        name: String,
        description: String,
        isPrivate: Boolean,
        autoInit: Boolean,
        gitignoreTemplate: String?,
        onCreated: (GitHubRepoDto) -> Unit
    ) {
        if (name.isBlank()) {
            _createRepoError.value = "Nama repository tidak boleh kosong."
            return
        }
        viewModelScope.launch {
            _isCreatingRepo.value = true
            _createRepoError.value = null
            gitHubRepository.createRepository(
                name = name,
                description = description,
                isPrivate = isPrivate,
                autoInit = autoInit,
                gitignoreTemplate = gitignoreTemplate
            ).onSuccess { repo ->
                _isCreatingRepo.value = false
                loadRepositories()
                onCreated(repo)
            }.onFailure { err ->
                _isCreatingRepo.value = false
                _createRepoError.value = err.message ?: "Gagal membuat repository baru."
            }
        }
    }

    fun openRepository(repo: GitHubRepoDto) {
        _detailState.value = WorkspaceDetailState(
            repo = repo,
            activeBranch = repo.defaultBranch,
            currentPath = "",
            isLoadingFiles = true
        )
        viewModelScope.launch {
            gitHubRepository.fetchBranches(repo.owner.login, repo.name)
                .onSuccess { branches ->
                    val branchName = branches.firstOrNull { it.name == repo.defaultBranch }?.name
                        ?: branches.firstOrNull()?.name
                        ?: repo.defaultBranch
                    _detailState.update { it.copy(branches = branches, activeBranch = branchName) }
                    loadFolderContents("")
                    loadCommits()
                    loadIssues("open")
                }
                .onFailure {
                    // Repo mungkin masih kosong tanpa branch
                    _detailState.update { it.copy(branches = emptyList()) }
                    loadFolderContents("")
                    loadCommits()
                    loadIssues("open")
                }
        }
    }

    fun switchBranch(branchName: String) {
        _detailState.update {
            it.copy(
                activeBranch = branchName,
                currentPath = "",
                selectedCommitDetail = null
            )
        }
        loadFolderContents("")
        loadCommits()
    }

    fun createBranch(newBranchName: String) {
        val state = _detailState.value
        val repo = state.repo ?: return
        val sourceSha = state.branches.firstOrNull { it.name == state.activeBranch }?.commit?.sha
            ?: state.branches.firstOrNull()?.commit?.sha
        if (sourceSha.isNullOrBlank()) {
            _detailState.update {
                it.copy(
                    feedbackMessage = "Repository belum memiliki commit awal untuk membuat branch.",
                    feedbackIsError = true
                )
            }
            return
        }
        viewModelScope.launch {
            _detailState.update { it.copy(isMutating = true, feedbackMessage = null) }
            gitHubRepository.createBranch(
                owner = repo.owner.login,
                repo = repo.name,
                newBranchName = newBranchName,
                sourceSha = sourceSha
            ).onSuccess {
                gitHubRepository.fetchBranches(repo.owner.login, repo.name)
                    .onSuccess { updatedBranches ->
                        _detailState.update {
                            it.copy(
                                isMutating = false,
                                branches = updatedBranches,
                                activeBranch = newBranchName.trim(),
                                currentPath = "",
                                feedbackMessage = "Branch '${newBranchName.trim()}' berhasil dibuat.",
                                feedbackIsError = false
                            )
                        }
                        loadFolderContents("")
                        loadCommits()
                    }
                    .onFailure {
                        _detailState.update {
                            it.copy(
                                isMutating = false,
                                activeBranch = newBranchName.trim(),
                                feedbackMessage = "Branch '${newBranchName.trim()}' berhasil dibuat.",
                                feedbackIsError = false
                            )
                        }
                    }
            }.onFailure { err ->
                _detailState.update {
                    it.copy(
                        isMutating = false,
                        feedbackMessage = err.message ?: "Gagal membuat branch.",
                        feedbackIsError = true
                    )
                }
            }
        }
    }

    fun loadFolderContents(path: String = _detailState.value.currentPath) {
        val state = _detailState.value
        val repo = state.repo ?: return
        viewModelScope.launch {
            _detailState.update {
                it.copy(currentPath = path, isLoadingFiles = true, filesError = null)
            }
            gitHubRepository.fetchFolderContents(
                owner = repo.owner.login,
                repo = repo.name,
                path = path,
                branch = state.activeBranch
            ).onSuccess { items ->
                _detailState.update {
                    it.copy(files = items, isLoadingFiles = false, filesError = null)
                }
            }.onFailure { err ->
                _detailState.update {
                    it.copy(
                        files = emptyList(),
                        isLoadingFiles = false,
                        filesError = err.message ?: "Folder kosong atau berkas tidak ditemukan."
                    )
                }
            }
        }
    }

    fun navigateUpFolder(): Boolean {
        val current = _detailState.value.currentPath
        if (current.isBlank()) return false
        val parent = current.substringBeforeLast('/', "")
        loadFolderContents(parent)
        return true
    }

    fun loadCommits() {
        val state = _detailState.value
        val repo = state.repo ?: return
        viewModelScope.launch {
            _detailState.update { it.copy(isLoadingCommits = true, commitsError = null) }
            gitHubRepository.fetchCommits(repo.owner.login, repo.name, state.activeBranch)
                .onSuccess { commits ->
                    _detailState.update {
                        it.copy(commits = commits, isLoadingCommits = false, commitsError = null)
                    }
                }
                .onFailure { err ->
                    _detailState.update {
                        it.copy(
                            commits = emptyList(),
                            isLoadingCommits = false,
                            commitsError = err.message ?: "Belum ada riwayat commit."
                        )
                    }
                }
        }
    }

    fun loadCommitDetail(sha: String) {
        val repo = _detailState.value.repo ?: return
        viewModelScope.launch {
            _detailState.update { it.copy(isLoadingCommitDetail = true) }
            gitHubRepository.fetchCommitDetail(repo.owner.login, repo.name, sha)
                .onSuccess { detail ->
                    _detailState.update {
                        it.copy(selectedCommitDetail = detail, isLoadingCommitDetail = false)
                    }
                }
                .onFailure { err ->
                    _detailState.update {
                        it.copy(
                            isLoadingCommitDetail = false,
                            feedbackMessage = err.message ?: "Gagal memuat detail commit.",
                            feedbackIsError = true
                        )
                    }
                }
        }
    }

    fun dismissCommitDetail() {
        _detailState.update { it.copy(selectedCommitDetail = null) }
    }

    fun loadIssues(stateFilter: String = _detailState.value.issueStateFilter) {
        val repo = _detailState.value.repo ?: return
        viewModelScope.launch {
            _detailState.update {
                it.copy(issueStateFilter = stateFilter, isLoadingIssues = true, issuesError = null)
            }
            gitHubRepository.fetchIssues(repo.owner.login, repo.name, stateFilter)
                .onSuccess { issues ->
                    _detailState.update {
                        it.copy(issues = issues, isLoadingIssues = false, issuesError = null)
                    }
                }
                .onFailure { err ->
                    _detailState.update {
                        it.copy(
                            issues = emptyList(),
                            isLoadingIssues = false,
                            issuesError = err.message ?: "Gagal memuat daftar Issues."
                        )
                    }
                }
        }
    }

    fun createIssue(title: String, body: String) {
        val repo = _detailState.value.repo ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            _detailState.update { it.copy(isMutating = true, feedbackMessage = null) }
            gitHubRepository.createIssue(repo.owner.login, repo.name, title, body)
                .onSuccess { issue ->
                    _detailState.update {
                        it.copy(
                            isMutating = false,
                            feedbackMessage = "Issue #${issue.number} berhasil dibuat.",
                            feedbackIsError = false
                        )
                    }
                    loadIssues("open")
                }
                .onFailure { err ->
                    _detailState.update {
                        it.copy(
                            isMutating = false,
                            feedbackMessage = err.message ?: "Gagal membuat Issue.",
                            feedbackIsError = true
                        )
                    }
                }
        }
    }

    fun clearFeedback() {
        _detailState.update { it.copy(feedbackMessage = null) }
    }

    class Factory(
        private val gitHubRepository: GitHubRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return WorkspaceViewModel(gitHubRepository) as T
        }
    }
}
