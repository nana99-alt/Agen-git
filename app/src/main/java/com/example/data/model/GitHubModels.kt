package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubTokenResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "scope") val scope: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubUserDto(
    @Json(name = "id") val id: Long,
    @Json(name = "login") val login: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "public_repos") val publicRepos: Int = 0
)

@JsonClass(generateAdapter = true)
data class GitHubOwnerDto(
    @Json(name = "login") val login: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRepoDto(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "owner") val owner: GitHubOwnerDto,
    @Json(name = "description") val description: String? = null,
    @Json(name = "private") val isPrivate: Boolean = false,
    @Json(name = "default_branch") val defaultBranch: String = "main",
    @Json(name = "language") val language: String? = null,
    @Json(name = "stargazers_count") val stargazersCount: Int = 0,
    @Json(name = "open_issues_count") val openIssuesCount: Int = 0,
    @Json(name = "updated_at") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class CreateRepoRequest(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "private") val isPrivate: Boolean = false,
    @Json(name = "auto_init") val autoInit: Boolean = true,
    @Json(name = "gitignore_template") val gitignoreTemplate: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubBranchCommitDto(
    @Json(name = "sha") val sha: String
)

@JsonClass(generateAdapter = true)
data class GitHubBranchDto(
    @Json(name = "name") val name: String,
    @Json(name = "commit") val commit: GitHubBranchCommitDto,
    @Json(name = "protected") val isProtected: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CreateRefRequest(
    @Json(name = "ref") val ref: String,
    @Json(name = "sha") val sha: String
)

@JsonClass(generateAdapter = true)
data class GitRefObjectDto(
    @Json(name = "sha") val sha: String,
    @Json(name = "type") val type: String? = null
)

@JsonClass(generateAdapter = true)
data class GitRefResponseDto(
    @Json(name = "ref") val ref: String,
    @Json(name = "object") val refObject: GitRefObjectDto
)

@JsonClass(generateAdapter = true)
data class ContentItemDto(
    @Json(name = "name") val name: String,
    @Json(name = "path") val path: String,
    @Json(name = "sha") val sha: String,
    @Json(name = "size") val size: Long = 0,
    @Json(name = "type") val type: String, // "file" or "dir"
    @Json(name = "content") val content: String? = null,
    @Json(name = "encoding") val encoding: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateFileRequest(
    @Json(name = "message") val message: String,
    @Json(name = "content") val contentBase64: String,
    @Json(name = "branch") val branch: String,
    @Json(name = "sha") val sha: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdatedContentInfoDto(
    @Json(name = "name") val name: String,
    @Json(name = "path") val path: String,
    @Json(name = "sha") val sha: String
)

@JsonClass(generateAdapter = true)
data class UpdateFileResponseDto(
    @Json(name = "content") val content: UpdatedContentInfoDto? = null
)

@JsonClass(generateAdapter = true)
data class CommitAuthorInfoDto(
    @Json(name = "name") val name: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "date") val date: String = ""
)

@JsonClass(generateAdapter = true)
data class CommitDetailCoreDto(
    @Json(name = "message") val message: String = "",
    @Json(name = "author") val author: CommitAuthorInfoDto? = null
)

@JsonClass(generateAdapter = true)
data class CommitFileChangeDto(
    @Json(name = "filename") val filename: String,
    @Json(name = "status") val status: String,
    @Json(name = "additions") val additions: Int = 0,
    @Json(name = "deletions") val deletions: Int = 0,
    @Json(name = "patch") val patch: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubCommitWrapperDto(
    @Json(name = "sha") val sha: String,
    @Json(name = "commit") val commit: CommitDetailCoreDto,
    @Json(name = "author") val author: GitHubOwnerDto? = null,
    @Json(name = "files") val files: List<CommitFileChangeDto>? = null
)

@JsonClass(generateAdapter = true)
data class GitHubLabelDto(
    @Json(name = "name") val name: String,
    @Json(name = "color") val color: String = "38BDF8"
)

@JsonClass(generateAdapter = true)
data class GitHubIssueDto(
    @Json(name = "id") val id: Long,
    @Json(name = "number") val number: Int,
    @Json(name = "title") val title: String,
    @Json(name = "body") val body: String? = null,
    @Json(name = "state") val state: String = "open",
    @Json(name = "user") val user: GitHubOwnerDto? = null,
    @Json(name = "labels") val labels: List<GitHubLabelDto> = emptyList(),
    @Json(name = "comments") val comments: Int = 0,
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "pull_request") val pullRequest: Map<String, Any>? = null
)

@JsonClass(generateAdapter = true)
data class CreateIssueRequest(
    @Json(name = "title") val title: String,
    @Json(name = "body") val body: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubApiErrorDto(
    @Json(name = "message") val message: String? = null
)

// Model Domain UI
data class FileDocument(
    val owner: String,
    val repo: String,
    val branch: String,
    val path: String,
    val sha: String?,
    val originalContent: String,
    val editedContent: String,
    val isNewFile: Boolean = false
) {
    val fileName: String get() = path.substringAfterLast('/')
    val isDirty: Boolean get() = isNewFile || editedContent != originalContent
}
