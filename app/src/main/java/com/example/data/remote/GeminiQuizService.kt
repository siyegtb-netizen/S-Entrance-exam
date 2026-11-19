package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import com.example.data.model.DifficultyMode
import com.example.data.model.GenerationStage
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizSession
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiQuizService"

@JsonClass(generateAdapter = true)
data class GeneratedQuizDto(
    @Json(name = "quizTitle") val quizTitle: String? = null,
    @Json(name = "questions") val questions: List<GeneratedQuestionDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GeneratedQuestionDto(
    @Json(name = "question") val question: String = "",
    @Json(name = "options") val options: List<String> = emptyList(),
    @Json(name = "correctIndex") val correctIndex: Int = 0,
    @Json(name = "explanation") val explanation: String = "",
    @Json(name = "sourceReference") val sourceReference: String? = null
)

class GeminiQuizException(message: String) : Exception(message)

class GeminiQuizService(
    private val context: Context
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val quizAdapter = moshi.adapter(GeneratedQuizDto::class.java)

    private val quizSchema = Schema.obj(
        properties = mapOf(
            "quizTitle" to Schema.string(description = "Descriptive title for the quiz"),
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "question" to Schema.string(description = "Multiple-choice question grounded in the PDF"),
                        "options" to Schema.array(
                            items = Schema.string(),
                            description = "Exactly 4 distinct answer choices without A/B/C/D prefixes"
                        ),
                        "correctIndex" to Schema.integer(description = "0-based index (0..3) of the correct option"),
                        "explanation" to Schema.string(description = "1-2 sentence explanation citing why the answer is right"),
                        "sourceReference" to Schema.string(description = "Unit, section, or page reference from the PDF")
                    ),
                    optionalProperties = listOf("sourceReference")
                ),
                description = "List of multiple-choice questions generated from the PDF"
            )
        )
    )

    private val quizConfig = generationConfig {
        temperature = 0.35f
        responseMimeType = "application/json"
        responseSchema = quizSchema
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = quizConfig
        )
    }

    suspend fun generateQuizFromPdf(
        pdfUri: Uri,
        pdfDisplayName: String,
        topic: String,
        questionCount: Int,
        difficulty: DifficultyMode,
        onStageChanged: (GenerationStage) -> Unit
    ): QuizSession = withContext(Dispatchers.IO) {
        val cleanTopic = topic.trim()
        if (cleanTopic.isEmpty()) {
            throw GeminiQuizException("Please enter a topic or unit (e.g., Grade 12 Unit 1 and 2).")
        }

        if (!hasInternetConnection()) {
            throw GeminiQuizException("No internet connection detected. Connect to Wi-Fi or mobile data and try again.")
        }

        onStageChanged(GenerationStage.READING_PDF)
        val pdfBytes = readPdfBytes(pdfUri)

        onStageChanged(GenerationStage.CRAFTING_QUIZ)
        val prompt = buildString {
            appendLine("You are an expert academic and entrance exam question writer.")
            appendLine("Read the attached PDF document carefully and generate exactly $questionCount multiple-choice questions strictly based on the content covering: \"$cleanTopic\".")
            appendLine("Difficulty mode: ${difficulty.label} — ${difficulty.promptInstruction}")
            appendLine()
            appendLine("Requirements:")
            appendLine("1. Every question MUST be grounded in the attached PDF, specifically targeting \"$cleanTopic\". If the topic appears in specific chapters/units/sections, focus exclusively on those sections.")
            appendLine("2. Provide exactly 4 distinct answer choices per question (corresponding to A, B, C, D). Do NOT prefix option text with 'A.', 'B)', etc.")
            appendLine("3. Provide correctIndex as a 0-based integer (0 for A, 1 for B, 2 for C, 3 for D).")
            appendLine("4. Provide a concise, helpful explanation (1-2 sentences) citing why the answer is right based on the PDF.")
            appendLine("5. Provide sourceReference indicating the unit, section, or page topic from the PDF where the concept is found.")
        }

        val responseText = try {
            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        inlineData(pdfBytes, "application/pdf")
                        text(prompt)
                    }
                )
            }
            response.text?.trim().orEmpty()
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Firebase AI quiz generation timed out", e)
            throw GeminiQuizException("Quiz generation timed out after 60 seconds. Try fewer questions or a smaller PDF.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI quiz generation failed", e)
            throw GeminiQuizException(e.localizedMessage ?: "Failed to generate quiz from PDF.")
        }

        if (responseText.isEmpty()) {
            throw GeminiQuizException("Gemini returned an empty response. Please try again.")
        }

        val parsed = parseQuizResponse(
            rawJson = responseText,
            fallbackTopic = cleanTopic,
            pdfName = pdfDisplayName
        )

        if (parsed.questions.isEmpty()) {
            throw GeminiQuizException(
                "Could not extract questions for \"$cleanTopic\" from this PDF. Try a broader unit or verify the PDF contains readable text."
            )
        }

        parsed
    }

    private fun readPdfBytes(uri: Uri): ByteArray {
        val bytes = try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes()
            } ?: throw GeminiQuizException("Could not open the selected PDF file. Please choose it again.")
        } catch (e: GeminiQuizException) {
            throw e
        } catch (e: Exception) {
            throw GeminiQuizException("Failed to read PDF: ${e.localizedMessage ?: "Unknown error"}")
        }

        if (bytes.isEmpty()) {
            throw GeminiQuizException("The selected PDF file is empty.")
        }
        if (bytes.size > MAX_PDF_BYTES) {
            throw GeminiQuizException("PDF exceeds the 19 MB limit for inline analysis. Please select a smaller document.")
        }
        return bytes
    }

    internal fun parseQuizResponse(
        rawJson: String,
        fallbackTopic: String,
        pdfName: String
    ): QuizSession {
        val dto = try {
            quizAdapter.fromJson(rawJson.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse quiz JSON", e)
            throw GeminiQuizException("Could not parse quiz JSON: ${e.localizedMessage ?: "Invalid format"}")
        } ?: throw GeminiQuizException("Received empty quiz data from Gemini.")

        val validQuestions = dto.questions.mapIndexedNotNull { index, item ->
            val qText = item.question.trim()
            if (qText.isEmpty()) return@mapIndexedNotNull null

            val cleanOptions = item.options
                .map { opt -> sanitizeOptionPrefix(opt.trim()) }
                .filter { it.isNotEmpty() }
                .take(4)

            if (cleanOptions.size < 2) return@mapIndexedNotNull null

            val paddedOptions = if (cleanOptions.size == 4) {
                cleanOptions
            } else {
                cleanOptions + List(4 - cleanOptions.size) { "None of the above" }
            }

            val safeCorrectIndex = item.correctIndex.coerceIn(0, 3)

            QuizQuestion(
                id = index + 1,
                question = qText,
                options = paddedOptions,
                correctIndex = safeCorrectIndex,
                explanation = item.explanation.trim().ifEmpty {
                    "Option ${QuizQuestion.optionLetter(safeCorrectIndex)} is supported by the study material."
                },
                sourceReference = item.sourceReference?.trim().orEmpty()
            )
        }

        return QuizSession(
            title = dto.quizTitle?.trim().takeUnless { it.isNullOrEmpty() } ?: fallbackTopic,
            topic = fallbackTopic,
            pdfName = pdfName,
            questions = validQuestions
        )
    }

    private fun sanitizeOptionPrefix(option: String): String {
        return option.replace(Regex("^[A-Da-d][).:\\-]\\s+"), "").trim()
    }

    private fun hasInternetConnection(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    companion object {
        private const val MODEL_NAME = "gemini-3.8-flash"
        private const val MAX_PDF_BYTES = 19 * 1024 * 1024
    }
}
