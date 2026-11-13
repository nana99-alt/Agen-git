package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.SessionDataStore
import com.example.data.model.GitHubUserDto
import com.example.data.repository.GitHubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthUiState {
    data object CheckingSession : AuthUiState
    data class Unauthenticated(val oauthConfigured: Boolean) : AuthUiState
    data object Authenticating : AuthUiState
    data class GuestWorkspace(val oauthConfigured: Boolean) : AuthUiState
    data class Authenticated(val user: GitHubUserDto) : AuthUiState
    data class Error(val message: String, val oauthConfigured: Boolean) : AuthUiState
}

class AuthViewModel(
    private val gitHubRepository: GitHubRepository,
    private val sessionDataStore: SessionDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.CheckingSession)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkExistingSession()
    }

    fun isOAuthConfigured(): Boolean = gitHubRepository.isOAuthConfigured()

    fun checkExistingSession() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.CheckingSession
            val configured = gitHubRepository.isOAuthConfigured()
            val token = sessionDataStore.getAccessToken()
            if (token.isNullOrBlank()) {
                _uiState.value = AuthUiState.Unauthenticated(oauthConfigured = configured)
                return@launch
            }
            gitHubRepository.fetchCurrentUser()
                .onSuccess { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                }
                .onFailure { err ->
                    _uiState.value = AuthUiState.Error(
                        message = err.message ?: "Gagal memverifikasi sesi GitHub.",
                        oauthConfigured = configured
                    )
                }
        }
    }

    fun enterGuestWorkspace() {
        _uiState.value = AuthUiState.GuestWorkspace(
            oauthConfigured = gitHubRepository.isOAuthConfigured()
        )
    }

    fun returnToOnboarding() {
        _uiState.value = AuthUiState.Unauthenticated(
            oauthConfigured = gitHubRepository.isOAuthConfigured()
        )
    }

    fun startOAuthLogin(
        onNeedsConfig: () -> Unit,
        onLaunchBrowser: (Uri) -> Unit
    ) {
        if (!gitHubRepository.isOAuthConfigured()) {
            onNeedsConfig()
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Authenticating
            try {
                val uri = gitHubRepository.createOAuthAuthorizationUri()
                onLaunchBrowser(uri)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(
                    message = e.localizedMessage ?: "Gagal memulai otorisasi GitHub.",
                    oauthConfigured = true
                )
            }
        }
    }

    fun cancelAuthenticatingIfIdle() {
        if (_uiState.value is AuthUiState.Authenticating) {
            _uiState.value = AuthUiState.Unauthenticated(
                oauthConfigured = gitHubRepository.isOAuthConfigured()
            )
        }
    }

    fun handleOAuthRedirect(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Authenticating
            gitHubRepository.handleOAuthCallback(uri)
                .onSuccess { user ->
                    _uiState.value = AuthUiState.Authenticated(user)
                }
                .onFailure { err ->
                    _uiState.value = AuthUiState.Error(
                        message = err.message ?: "Autentikasi GitHub gagal.",
                        oauthConfigured = gitHubRepository.isOAuthConfigured()
                    )
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            gitHubRepository.logout()
            _uiState.value = AuthUiState.Unauthenticated(
                oauthConfigured = gitHubRepository.isOAuthConfigured()
            )
        }
    }

    class Factory(
        private val gitHubRepository: GitHubRepository,
        private val sessionDataStore: SessionDataStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(gitHubRepository, sessionDataStore) as T
        }
    }
}
