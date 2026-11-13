package com.example.data.repository

import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import com.example.core.SessionDataStore
import com.example.data.model.ContentItemDto
import com.example.data.model.CreateIssueRequest
import com.example.data.model.CreateRefRequest
import com.example.data.model.CreateRepoRequest
import com.example.data.model.FileDocument
import com.example.data.model.GitHubApiErrorDto
import com.example.data.model.GitHubBranchDto
import com.example.data.model.GitHubCommitWrapperDto
import com.example.data.model.GitHubIssueDto
import com.example.data.model.GitHubRepoDto
import com.example.data.model.GitHubUserDto
import com.example.data.model.UpdateFileRequest
import com.example.data.remote.GitHubApiService
import com.example.data.remote.GitHubOAuthApi
import com.example.data.remote.NetworkModule
import retrofit2.Response
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

class GitHubRepository(
    private val sessionDataStore: SessionDataStore,
    private val oauthApi: GitHubOAuthApi = NetworkModule.oauthApi,
    private val api: GitHubApiService = NetworkModule.githubApi
) {
    companion object {
        const val OAUTH_REDIRECT_URI = "kodegit://oauth/callback"
        const val GITHUB_TOKEN_CREATE_URL =
            "https://github.com/settings/tokens/new?description=KodeGit%20Android&scopes=repo,read:user"
        private const val OAUTH_SCOPES = "repo read:user"
    }

    fun isOAuthConfigured(): Boolean {
        val id = BuildConfig.GITHUB_CLIENT_ID
        val secret = BuildConfig.GITHUB_CLIENT_SECRET
        return id.isNotBlank() &&
            !id.startsWith("YOUR_") &&
            secret.isNotBlank() &&
            !secret.startsWith("YOUR_")
    }

    suspend fun loginWithPersonalToken(rawToken: String): Result<GitHubUserDto> {
        val cleanToken = rawToken.trim()
        if (cleanToken.isBlank()) {
            return Result.failure(IllegalArgumentException("Token / API Key GitHub tidak boleh kosong."))
        }
        return try {
            val response = api.getCurrentUser("Bearer $cleanToken")
            val user = response.body()
            if (response.isSuccessful && user != null) {
                sessionDataStore.saveAccessToken(cleanToken)
                Result.success(user)
            } else {
                val message = when (response.code()) {
                    401 -> "Token / API Key GitHub tidak valid atau sudah kedaluwarsa. Pastikan Anda menyalin token lengkap (ghp_... atau github_pat_...)."
                    403 -> "Token ditolak atau terkena batas akses GitHub (HTTP 403)."
                    else -> "Gagal memverifikasi token GitHub (HTTP ${response.code()})."
                }
                Result.failure(IllegalStateException(message))
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException(e.localizedMessage ?: "Gagal terhubung ke server GitHub.")
            )
        }
    }

    suspend fun createOAuthAuthorizationUri(): Uri {
        val codeVerifier = generateRandomUrlSafeString(64)
        val state = generateRandomUrlSafeString(32)
        val codeChallenge = generatePkceChallenge(codeVerifier)
        sessionDataStore.savePendingOAuth(state = state, codeVerifier = codeVerifier)

        return Uri.Builder()
            .scheme("https")
            .authority("github.com")
            .path("login/oauth/authorize")
            .appendQueryParameter("client_id", BuildConfig.GITHUB_CLIENT_ID)
            .appendQueryParameter("redirect_uri", OAUTH_REDIRECT_URI)
            .appendQueryParameter("scope", OAUTH_SCOPES)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", codeChallenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
    }

    suspend fun handleOAuthCallback(callbackUri: Uri): Result<GitHubUserDto> {
        val errorParam = callbackUri.getQueryParameter("error")
        if (!errorParam.isNullOrBlank()) {
            val desc = callbackUri.getQueryParameter("error_description") ?: errorParam
            return Result.failure(IllegalStateException("Otorisasi GitHub dibatalkan: $desc"))
        }

        val code = callbackUri.getQueryParameter("code")
        val returnedState = callbackUri.getQueryParameter("state")
        if (code.isNullOrBlank() || returnedState.isNullOrBlank()) {
            return Result.failure(IllegalStateException("Respons callback OAuth tidak lengkap."))
        }

        val pending = sessionDataStore.getPendingOAuth()
            ?: return Result.failure(IllegalStateException("Sesi OAuth kedaluwarsa. Silakan coba masuk lagi."))

        if (pending.state != returnedState) {
            return Result.failure(IllegalStateException("Validasi state keamanan OAuth gagal."))
        }

        return try {
            val response = oauthApi.exchangeCodeForToken(
                clientId = BuildConfig.GITHUB_CLIENT_ID,
                clientSecret = BuildConfig.GITHUB_CLIENT_SECRET,
                code = code,
                redirectUri = OAUTH_REDIRECT_URI,
                codeVerifier = pending.codeVerifier
            )
            val body = response.body()
            if (!response.isSuccessful || body == null) {
                return Result.failure(IllegalStateException("Gagal menukar token OAuth (HTTP ${response.code()})."))
            }
            if (!body.error.isNullOrBlank()) {
                return Result.failure(IllegalStateException(body.errorDescription ?: body.error))
            }
            val token = body.accessToken
            if (token.isNullOrBlank()) {
                return Result.failure(IllegalStateException("Token akses GitHub tidak ditemukan pada respons."))
            }
            sessionDataStore.saveAccessToken(token)
            fetchCurrentUser()
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "Gagal terhubung ke server OAuth GitHub."))
        }
    }

    suspend fun fetchCurrentUser(): Result<GitHubUserDto> = authorizedCall { auth ->
        api.getCurrentUser(auth)
    }

    suspend fun fetchUserRepos(): Result<List<GitHubRepoDto>> = authorizedCall { auth ->
        api.getUserRepos(auth)
    }

    suspend fun createRepository(
        name: String,
        description: String?,
        isPrivate: Boolean,
        autoInit: Boolean,
        gitignoreTemplate: String?
    ): Result<GitHubRepoDto> = authorizedCall { auth ->
        api.createRepo(
            authHeader = auth,
            request = CreateRepoRequest(
                name = name.trim(),
                description = description?.trim()?.takeIf { it.isNotEmpty() },
                isPrivate = isPrivate,
                autoInit = autoInit,
                gitignoreTemplate = gitignoreTemplate?.takeIf { it.isNotBlank() && autoInit }
            )
        )
    }

    suspend fun fetchBranches(owner: String, repo: String): Result<List<GitHubBranchDto>> =
        authorizedCall { auth ->
            api.getBranches(auth, owner, repo)
        }

    suspend fun createBranch(
        owner: String,
        repo: String,
        newBranchName: String,
        sourceSha: String
    ): Result<Unit> {
        val result = authorizedCall { auth ->
            api.createRef(
                authHeader = auth,
                owner = owner,
                repo = repo,
                request = CreateRefRequest(
                    ref = "refs/heads/${newBranchName.trim()}",
                    sha = sourceSha
                )
            )
        }
        return result.map { Unit }
    }

    suspend fun fetchFolderContents(
        owner: String,
        repo: String,
        path: String,
        branch: String
    ): Result<List<ContentItemDto>> {
        val cleanPath = path.trim('/')
        val res = authorizedCall { auth ->
            api.getFolderContents(auth, owner, repo, cleanPath, branch)
        }
        return res.map { items ->
            items.sortedWith(compareBy<ContentItemDto> { it.type != "dir" }.thenBy { it.name.lowercase() })
        }
    }

    suspend fun fetchFileDocument(
        owner: String,
        repo: String,
        path: String,
        branch: String
    ): Result<FileDocument> {
        val cleanPath = path.trim('/')
        return authorizedCall { auth ->
            api.getFileContent(auth, owner, repo, cleanPath, branch)
        }.mapCatching { dto ->
            val rawBase64 = dto.content?.replace("\n", "")?.replace("\r", "").orEmpty()
            val decoded = if (rawBase64.isNotEmpty()) {
                String(Base64.decode(rawBase64, Base64.DEFAULT), StandardCharsets.UTF_8)
            } else {
                ""
            }
            FileDocument(
                owner = owner,
                repo = repo,
                branch = branch,
                path = dto.path,
                sha = dto.sha,
                originalContent = decoded,
                editedContent = decoded,
                isNewFile = false
            )
        }
    }

    suspend fun commitFile(
        document: FileDocument,
        commitMessage: String
    ): Result<FileDocument> {
        val encoded = Base64.encodeToString(
            document.editedContent.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP
        )
        return authorizedCall { auth ->
            api.putFileContent(
                authHeader = auth,
                owner = document.owner,
                repo = document.repo,
                path = document.path.trim('/'),
                request = UpdateFileRequest(
                    message = commitMessage.trim(),
                    contentBase64 = encoded,
                    branch = document.branch,
                    sha = if (document.isNewFile) null else document.sha
                )
            )
        }.map { resp ->
            document.copy(
                sha = resp.content?.sha ?: document.sha,
                originalContent = document.editedContent,
                isNewFile = false
            )
        }
    }

    suspend fun fetchCommits(
        owner: String,
        repo: String,
        branch: String
    ): Result<List<GitHubCommitWrapperDto>> = authorizedCall { auth ->
        api.getCommits(auth, owner, repo, branch)
    }

    suspend fun fetchCommitDetail(
        owner: String,
        repo: String,
        sha: String
    ): Result<GitHubCommitWrapperDto> = authorizedCall { auth ->
        api.getCommitDetail(auth, owner, repo, sha)
    }

    suspend fun fetchIssues(
        owner: String,
        repo: String,
        state: String = "open"
    ): Result<List<GitHubIssueDto>> = authorizedCall { auth ->
        api.getIssues(auth, owner, repo, state)
    }.map { list ->
        list.filter { it.pullRequest == null }
    }

    suspend fun createIssue(
        owner: String,
        repo: String,
        title: String,
        body: String?
    ): Result<GitHubIssueDto> = authorizedCall { auth ->
        api.createIssue(
            authHeader = auth,
            owner = owner,
            repo = repo,
            request = CreateIssueRequest(
                title = title.trim(),
                body = body?.trim()?.takeIf { it.isNotEmpty() }
            )
        )
    }

    suspend fun logout() {
        sessionDataStore.clearSession()
    }

    private suspend fun <T> authorizedCall(block: suspend (String) -> Response<T>): Result<T> {
        val token = sessionDataStore.getAccessToken()
            ?: return Result.failure(IllegalStateException("Sesi GitHub belum terhubung."))
        return try {
            val response = block("Bearer $token")
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(IllegalStateException("Respons kosong dari server GitHub."))
                }
            } else {
                if (response.code() == 401) {
                    sessionDataStore.clearSession()
                }
                Result.failure(IllegalStateException(parseErrorMessage(response)))
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "Gagal terhubung ke GitHub."))
        }
    }

    private fun <T> parseErrorMessage(response: Response<T>): String {
        val raw = response.errorBody()?.string()
        val parsed = raw?.let {
            runCatching {
                NetworkModule.moshi.adapter(GitHubApiErrorDto::class.java).fromJson(it)?.message
            }.getOrNull()
        }
        return when (response.code()) {
            401 -> "Sesi GitHub kedaluwarsa. Silakan masuk kembali."
            403 -> parsed ?: "Akses ditolak atau batas API GitHub tercapai."
            404 -> "Repository, branch, atau berkas tidak ditemukan."
            409 -> "Konflik versi berkas (SHA berubah). Muat ulang berkas sebelum commit."
            422 -> parsed ?: "Data tidak valid atau sudah ada di GitHub."
            else -> parsed ?: "Kesalahan GitHub API (HTTP ${response.code()})."
        }
    }

    private fun generateRandomUrlSafeString(byteLength: Int): String {
        val bytes = ByteArray(byteLength)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun generatePkceChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(StandardCharsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
