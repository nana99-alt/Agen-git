package com.example.core

import android.content.Context
import com.example.data.repository.AiCodeAssistantRepository
import com.example.data.repository.GitHubRepository

class KodeGitContainer(context: Context) {
    val sessionDataStore: SessionDataStore by lazy {
        SessionDataStore(context.applicationContext)
    }

    val gitHubRepository: GitHubRepository by lazy {
        GitHubRepository(sessionDataStore = sessionDataStore)
    }

    val aiAssistantRepository: AiCodeAssistantRepository by lazy {
        AiCodeAssistantRepository()
    }
}
