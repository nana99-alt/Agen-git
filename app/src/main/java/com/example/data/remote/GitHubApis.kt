package com.example.data.remote

import com.example.data.model.ContentItemDto
import com.example.data.model.CreateIssueRequest
import com.example.data.model.CreateRefRequest
import com.example.data.model.CreateRepoRequest
import com.example.data.model.GitHubBranchDto
import com.example.data.model.GitHubCommitWrapperDto
import com.example.data.model.GitHubIssueDto
import com.example.data.model.GitHubRepoDto
import com.example.data.model.GitHubTokenResponse
import com.example.data.model.GitHubUserDto
import com.example.data.model.GitRefResponseDto
import com.example.data.model.UpdateFileRequest
import com.example.data.model.UpdateFileResponseDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface GitHubOAuthApi {
    @FormUrlEncoded
    @Headers("Accept: application/json")
    @POST("login/oauth/access_token")
    suspend fun exchangeCodeForToken(
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("code_verifier") codeVerifier: String
    ): Response<GitHubTokenResponse>
}

interface GitHubApiService {
    @GET("user")
    suspend fun getCurrentUser(
        @Header("Authorization") authHeader: String
    ): Response<GitHubUserDto>

    @GET("user/repos")
    suspend fun getUserRepos(
        @Header("Authorization") authHeader: String,
        @Query("sort") sort: String = "updated",
        @Query("per_page") perPage: Int = 50,
        @Query("affiliation") affiliation: String = "owner,collaborator,organization_member"
    ): Response<List<GitHubRepoDto>>

    @POST("user/repos")
    suspend fun createRepo(
        @Header("Authorization") authHeader: String,
        @Body request: CreateRepoRequest
    ): Response<GitHubRepoDto>

    @GET("repos/{owner}/{repo}/branches")
    suspend fun getBranches(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 100
    ): Response<List<GitHubBranchDto>>

    @POST("repos/{owner}/{repo}/git/refs")
    suspend fun createRef(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateRefRequest
    ): Response<GitRefResponseDto>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFolderContents(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") branch: String
    ): Response<List<ContentItemDto>>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Query("ref") branch: String
    ): Response<ContentItemDto>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun putFileContent(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body request: UpdateFileRequest
    ): Response<UpdateFileResponseDto>

    @GET("repos/{owner}/{repo}/commits")
    suspend fun getCommits(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("sha") branch: String,
        @Query("per_page") perPage: Int = 30
    ): Response<List<GitHubCommitWrapperDto>>

    @GET("repos/{owner}/{repo}/commits/{sha}")
    suspend fun getCommitDetail(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String
    ): Response<GitHubCommitWrapperDto>

    @GET("repos/{owner}/{repo}/issues")
    suspend fun getIssues(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("per_page") perPage: Int = 40
    ): Response<List<GitHubIssueDto>>

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Header("Authorization") authHeader: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: CreateIssueRequest
    ): Response<GitHubIssueDto>
}

object NetworkModule {
    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("User-Agent", "KodeGit-Android/1.0")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .build()
    }

    val oauthApi: GitHubOAuthApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://github.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubOAuthApi::class.java)
    }

    val githubApi: GitHubApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GitHubApiService::class.java)
    }
}
