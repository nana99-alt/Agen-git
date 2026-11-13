package com.example.data.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

data class AiChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val extractedCode: String? = null
)

class AiCodeAssistantRepository {

    companion object {
        private const val TAG = "AiCodeAssistantRepo"
        const val PRIMARY_MODEL_NAME = "gemini-3.8-flash"
        const val FALLBACK_MODEL_NAME = "gemini-3.5-flash-lite"
        private const val TIMEOUT_MS = 60_000L

        private const val SYSTEM_INSTRUCTION_TEXT =
            "Anda adalah asisten rekayasa perangkat lunak senior di dalam aplikasi editor kode Android bernama KodeGit. " +
                "Jawablah dalam Bahasa Indonesia yang ringkas, jelas, dan teknis. " +
                "Ketika pengguna meminta perubahan, perbaikan bug, atau refaktor kode, berikan penjelasan singkat " +
                "diikuti SATU blok kode lengkap yang siap menggantikan isi berkas saat ini di dalam pagar kode markdown ```...```."
    }

    // Inisialisasi lazy sesuai panduan Firebase AI Logic: gemini-3.8-flash mendukung Free Tier tanpa billing + ThinkingLevel.HIGH tanpa maxOutputTokens
    private val reasoningModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_MODEL_NAME,
            generationConfig = generationConfig {
                thinkingConfig = thinkingConfig {
                    thinkingLevel = ThinkingLevel.HIGH
                }
            },
            systemInstruction = content {
                text(SYSTEM_INSTRUCTION_TEXT)
            }
        )
    }

    // Model cadangan ultra-ringan (Free Tier) bila kuota per-menit model utama penuh
    private val liteFallbackModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_MODEL_NAME,
            systemInstruction = content {
                text(SYSTEM_INSTRUCTION_TEXT)
            }
        )
    }

    private fun isQuotaOrRateLimitError(error: Throwable): Boolean {
        val msg = error.message.orEmpty()
        return msg.contains("Quota exceeded", ignoreCase = true) ||
            msg.contains("rate-limit", ignoreCase = true) ||
            msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
            msg.contains("429")
    }

    private fun formatAiError(error: Throwable): IllegalStateException {
        val raw = error.localizedMessage ?: error.message ?: "Terjadi kesalahan pada layanan AI."
        return if (isQuotaOrRateLimitError(error)) {
            IllegalStateException(
                "Batas kuota atau laju permintaan Gemini sementara penuh ($raw). " +
                    "Tunggu sebentar lalu coba lagi."
            )
        } else {
            IllegalStateException(raw, error)
        }
    }

    fun streamCodeQuery(
        repoFullName: String,
        branch: String,
        filePath: String,
        fileContent: String,
        userPrompt: String,
        previousTurns: List<AiChatMessage>
    ): Flow<String> = flow {
        val history = previousTurns.takeLast(6).map { msg ->
            content(role = if (msg.isUser) "user" else "model") {
                text(msg.text)
            }
        }
        val contextualPrompt = buildString {
            appendLine("Repository: $repoFullName (branch: $branch)")
            appendLine("Berkas aktif: $filePath")
            appendLine("Isi berkas saat ini:")
            appendLine("```")
            appendLine(fileContent)
            appendLine("```")
            appendLine()
            appendLine("Permintaan pengguna: $userPrompt")
        }

        val modelsToTry = listOf(reasoningModel, liteFallbackModel)
        var lastError: Exception? = null

        for ((index, model) in modelsToTry.withIndex()) {
            val chat = try {
                model.startChat(history = history)
            } catch (e: IllegalStateException) {
                if (e.message?.contains("FirebaseApp", ignoreCase = true) == true) {
                    throw IllegalStateException(
                        "Konfigurasi Firebase (google-services.json) belum ikut terkemas di APK ini. " +
                            "Salin isi app/google-services.json dari AI Studio ke GitHub Secret bernama GOOGLE_SERVICES_JSON, lalu build ulang APK."
                    )
                }
                throw e
            }

            val accumulated = StringBuilder()
            try {
                withTimeout(TIMEOUT_MS) {
                    chat.sendMessageStream(contextualPrompt).collect { chunk ->
                        val piece = chunk.text.orEmpty()
                        if (piece.isNotEmpty()) {
                            accumulated.append(piece)
                            emit(accumulated.toString())
                        }
                    }
                }
                if (accumulated.isBlank()) {
                    throw IllegalStateException("Model AI mengembalikan respons kosong.")
                }
                return@flow
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI inference timed out", e)
                throw IllegalStateException("Permintaan AI melebihi batas waktu 60 detik. Coba sederhanakan kueri.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI inference failed on model index=$index", e)
                lastError = e
                val canFallback = index == 0 && accumulated.isEmpty() && isQuotaOrRateLimitError(e)
                if (!canFallback) {
                    throw formatAiError(e)
                }
            }
        }

        if (lastError != null) {
            throw formatAiError(lastError)
        }
    }

    suspend fun suggestCommitMessage(
        filePath: String,
        originalContent: String,
        editedContent: String
    ): String {
        val prompt = buildString {
            appendLine("Buat SATU pesan commit Git singkat dan deskriptif dalam Bahasa Indonesia atau gaya Conventional Commits (maksimal 72 karakter, tanpa tanda kutip, tanpa penjelasan tambahan) untuk perubahan pada berkas `$filePath`.")
            appendLine("Isi sebelum:")
            appendLine("```")
            appendLine(originalContent.take(3500))
            appendLine("```")
            appendLine("Isi sesudah:")
            appendLine("```")
            appendLine(editedContent.take(3500))
            appendLine("```")
        }
        return try {
            withTimeout(TIMEOUT_MS) {
                val response = try {
                    reasoningModel.generateContent(prompt)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (isQuotaOrRateLimitError(e)) {
                        liteFallbackModel.generateContent(prompt)
                    } else {
                        throw e
                    }
                }
                response.text?.trim()?.lineSequence()?.firstOrNull()?.trim('"', '`', ' ')
                    ?.takeIf { it.isNotBlank() }
                    ?: "chore: perbarui ${filePath.substringAfterLast('/')}"
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            "chore: perbarui ${filePath.substringAfterLast('/')}"
        }
    }

    fun extractLargestCodeBlock(markdown: String): String? {
        val regex = Regex("```[a-zA-Z0-9_+-]*\\r?\\n([\\s\\S]*?)```")
        val matches = regex.findAll(markdown).map { it.groupValues[1].trimEnd() }.toList()
        return matches.maxByOrNull { it.length }?.takeIf { it.isNotBlank() }
    }
}
