package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.EditorPreferences
import com.example.core.SessionDataStore
import com.example.data.model.FileDocument
import com.example.data.repository.AiChatMessage
import com.example.data.repository.AiCodeAssistantRepository
import com.example.data.repository.GitHubRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface EditorDocumentState {
    data object Idle : EditorDocumentState
    data object Loading : EditorDocumentState
    data class Ready(val document: FileDocument) : EditorDocumentState
    data class Error(val message: String) : EditorDocumentState
}

sealed interface CommitUiState {
    data object Idle : CommitUiState
    data object Committing : CommitUiState
    data class Success(val message: String) : CommitUiState
    data class Error(val message: String) : CommitUiState
}

sealed interface AiAssistantUiState {
    data object Idle : AiAssistantUiState
    data object Thinking : AiAssistantUiState
    data class Streaming(val text: String, val extractedCode: String?) : AiAssistantUiState
    data class Success(val text: String, val extractedCode: String?) : AiAssistantUiState
    data class Error(val message: String) : AiAssistantUiState
}

class EditorAiViewModel(
    private val gitHubRepository: GitHubRepository,
    private val aiRepository: AiCodeAssistantRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    companion object {
        private const val TAG = "EditorAiViewModel"
    }

    private val _documentState = MutableStateFlow<EditorDocumentState>(EditorDocumentState.Idle)
    val documentState: StateFlow<EditorDocumentState> = _documentState.asStateFlow()

    private val _commitState = MutableStateFlow<CommitUiState>(CommitUiState.Idle)
    val commitState: StateFlow<CommitUiState> = _commitState.asStateFlow()

    private val _suggestedCommitMsg = MutableStateFlow("")
    val suggestedCommitMsg: StateFlow<String> = _suggestedCommitMsg.asStateFlow()

    private val _isSuggestingCommit = MutableStateFlow(false)
    val isSuggestingCommit: StateFlow<Boolean> = _isSuggestingCommit.asStateFlow()

    private val _aiState = MutableStateFlow<AiAssistantUiState>(AiAssistantUiState.Idle)
    val aiState: StateFlow<AiAssistantUiState> = _aiState.asStateFlow()

    private val _aiMessages = MutableStateFlow<List<AiChatMessage>>(emptyList())
    val aiMessages: StateFlow<List<AiChatMessage>> = _aiMessages.asStateFlow()

    val editorPrefs: StateFlow<EditorPreferences> = sessionDataStore.editorPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, EditorPreferences())

    init {
        openLocalScratchpad()
    }

    fun openLocalScratchpad(fileName: String = "Main.kt") {
        val current = _documentState.value
        if (current is EditorDocumentState.Ready && current.document.owner.isNotEmpty()) {
            return
        }
        _documentState.value = EditorDocumentState.Ready(
            FileDocument(
                owner = "",
                repo = "",
                branch = "lokal",
                path = fileName,
                sha = null,
                originalContent = "",
                editedContent = "",
                isNewFile = true
            )
        )
    }

    fun resetToNewLocalFile(fileName: String) {
        _commitState.value = CommitUiState.Idle
        _aiMessages.value = emptyList()
        _aiState.value = AiAssistantUiState.Idle
        _documentState.value = EditorDocumentState.Ready(
            FileDocument(
                owner = "",
                repo = "",
                branch = "lokal",
                path = fileName.trim().ifEmpty { "Catatan.kt" },
                sha = null,
                originalContent = "",
                editedContent = "",
                isNewFile = true
            )
        )
    }

    fun openRemoteFile(owner: String, repo: String, branch: String, path: String) {
        viewModelScope.launch {
            _documentState.value = EditorDocumentState.Loading
            _commitState.value = CommitUiState.Idle
            _aiMessages.value = emptyList()
            _aiState.value = AiAssistantUiState.Idle

            gitHubRepository.fetchFileDocument(owner, repo, path, branch)
                .onSuccess { doc ->
                    _documentState.value = EditorDocumentState.Ready(doc)
                }
                .onFailure { err ->
                    _documentState.value = EditorDocumentState.Error(
                        err.message ?: "Gagal membuka berkas."
                    )
                }
        }
    }

    fun startNewFile(owner: String, repo: String, branch: String, fullPath: String) {
        _commitState.value = CommitUiState.Idle
        _aiMessages.value = emptyList()
        _aiState.value = AiAssistantUiState.Idle
        val cleanPath = fullPath.trim().trimStart('/')
        _documentState.value = EditorDocumentState.Ready(
            FileDocument(
                owner = owner,
                repo = repo,
                branch = branch,
                path = cleanPath,
                sha = null,
                originalContent = "",
                editedContent = "",
                isNewFile = true
            )
        )
    }

    fun updateEditorContent(newContent: String) {
        val current = (_documentState.value as? EditorDocumentState.Ready)?.document ?: return
        _documentState.value = EditorDocumentState.Ready(current.copy(editedContent = newContent))
    }

    fun revertToOriginal() {
        val current = (_documentState.value as? EditorDocumentState.Ready)?.document ?: return
        _documentState.value = EditorDocumentState.Ready(
            current.copy(editedContent = current.originalContent)
        )
    }

    fun prepareCommitSuggestion() {
        val doc = (_documentState.value as? EditorDocumentState.Ready)?.document ?: return
        _suggestedCommitMsg.value = if (doc.isNewFile) {
            "feat: tambah ${doc.fileName}"
        } else {
            "chore: perbarui ${doc.fileName}"
        }
    }

    fun generateAiCommitMessage() {
        val doc = (_documentState.value as? EditorDocumentState.Ready)?.document ?: return
        viewModelScope.launch {
            _isSuggestingCommit.value = true
            try {
                val msg = aiRepository.suggestCommitMessage(
                    filePath = doc.path,
                    originalContent = doc.originalContent,
                    editedContent = doc.editedContent
                )
                _suggestedCommitMsg.value = msg
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gagal membuat saran pesan commit", e)
            } finally {
                _isSuggestingCommit.value = false
            }
        }
    }

    fun commitChanges(message: String, onSuccess: () -> Unit) {
        val doc = (_documentState.value as? EditorDocumentState.Ready)?.document ?: return
        if (doc.owner.isBlank() || doc.repo.isBlank()) {
            _commitState.value = CommitUiState.Error(
                "Berkas ini masih lokal. Buka atau buat repository GitHub di tab Beranda terlebih dahulu untuk melakukan commit."
            )
            return
        }
        if (message.isBlank()) {
            _commitState.value = CommitUiState.Error("Pesan commit wajib diisi.")
            return
        }
        viewModelScope.launch {
            _commitState.value = CommitUiState.Committing
            gitHubRepository.commitFile(doc, message)
                .onSuccess { updatedDoc ->
                    _documentState.value = EditorDocumentState.Ready(updatedDoc)
                    _commitState.value = CommitUiState.Success("Perubahan berhasil di-commit ke ${updatedDoc.branch}.")
                    onSuccess()
                }
                .onFailure { err ->
                    _commitState.value = CommitUiState.Error(
                        err.message ?: "Gagal melakukan commit ke GitHub."
                    )
                }
        }
    }

    fun clearCommitFeedback() {
        _commitState.value = CommitUiState.Idle
    }

    fun askAi(instruction: String) {
        val doc = (_documentState.value as? EditorDocumentState.Ready)?.document ?: return
        val trimmed = instruction.trim()
        if (trimmed.isEmpty()) return

        val priorHistory = _aiMessages.value
        val userMsg = AiChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = trimmed
        )
        _aiMessages.value = priorHistory + userMsg

        viewModelScope.launch {
            _aiState.value = AiAssistantUiState.Thinking
            var latestText = ""
            try {
                aiRepository.streamCodeQuery(
                    repoFullName = "${doc.owner}/${doc.repo}",
                    branch = doc.branch,
                    filePath = doc.path,
                    fileContent = doc.editedContent,
                    userPrompt = trimmed,
                    previousTurns = priorHistory
                ).collect { accumulated ->
                    latestText = accumulated
                    val code = aiRepository.extractLargestCodeBlock(accumulated)
                    _aiState.value = AiAssistantUiState.Streaming(accumulated, code)
                }
                val finalCode = aiRepository.extractLargestCodeBlock(latestText)
                _aiMessages.value = _aiMessages.value + AiChatMessage(
                    id = UUID.randomUUID().toString(),
                    isUser = false,
                    text = latestText,
                    extractedCode = finalCode
                )
                _aiState.value = AiAssistantUiState.Success(latestText, finalCode)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Kesalahan inferensi Firebase AI", e)
                _aiState.value = AiAssistantUiState.Error(
                    e.localizedMessage ?: "Asisten AI gagal memproses permintaan."
                )
            }
        }
    }

    fun applyAiCodeToEditor(newCode: String) {
        updateEditorContent(newCode)
    }

    fun clearAiConversation() {
        _aiMessages.value = emptyList()
        _aiState.value = AiAssistantUiState.Idle
    }

    fun changeFontSize(delta: Int) {
        viewModelScope.launch {
            val next = (editorPrefs.value.fontSizeSp + delta).coerceIn(10, 22)
            sessionDataStore.updateEditorFontSize(next)
        }
    }

    fun toggleWordWrap() {
        viewModelScope.launch {
            sessionDataStore.updateWordWrap(!editorPrefs.value.wordWrap)
        }
    }

    class Factory(
        private val gitHubRepository: GitHubRepository,
        private val aiRepository: AiCodeAssistantRepository,
        private val sessionDataStore: SessionDataStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EditorAiViewModel(gitHubRepository, aiRepository, sessionDataStore) as T
        }
    }
}
