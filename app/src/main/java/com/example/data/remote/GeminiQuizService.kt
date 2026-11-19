package com.example.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.data.model.DifficultyMode
import com.example.data.model.GenerationStage
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizSession
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(65, TimeUnit.SECONDS)
            .writeTimeout(65, TimeUnit.SECONDS)
            .build()
    }

    suspend fun generateQuizFromPdf(
        apiKey: String,
        pdfUri: Uri,
        pdfDisplayName: String,
        topic: String,
        questionCount: Int,
        difficulty: DifficultyMode,
        onStageChanged: (GenerationStage) -> Unit
    ): QuizSession = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) {
            throw GeminiQuizException("Please enter your Gemini API key first.")
        }

        val cleanTopic = topic.trim()
        if (cleanTopic.isEmpty()) {
            throw GeminiQuizException("Please enter a topic or unit (e.g., Grade 12 Unit 1 and 2).")
        }

        if (!hasInternetConnection()) {
            throw GeminiQuizException("No internet connection detected. Connect to Wi-Fi or mobile data and try again.")
        }

        onStageChanged(GenerationStage.READING_PDF)
        val pdfBytes = readPdfBytes(pdfUri)
        val base64Pdf = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)

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

        val requestPayload = buildRequestJson(base64Pdf = base64Pdf, prompt = prompt)
        val request = Request.Builder()
            .url(GEMINI_ENDPOINT)
            .addHeader("x-goog-api-key", cleanKey)
            .addHeader("Content-Type", "application/json")
            .post(requestPayload.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val rawResponseBody = try {
            withTimeout(65_000L) {
                executeCall(request)
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini API quiz generation timed out", e)
            throw GeminiQuizException("Quiz generation timed out after 65 seconds. Try fewer questions or a smaller PDF.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: GeminiQuizException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API quiz generation failed", e)
            throw GeminiQuizException(e.localizedMessage ?: "Failed to generate quiz from PDF.")
        }

        val responseJsonText = extractCandidateText(rawResponseBody)

        val parsed = parseQuizResponse(
            rawJson = responseJsonText,
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

    private suspend fun executeCall(request: Request): String = suspendCancellableCoroutine { continuation ->
        val call = okHttpClient.newCall(request)
        continuation.invokeOnCancellation {
            call.cancel()
        }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isCancelled) return
                continuation.resumeWithException(
                    GeminiQuizException("Network error contacting Gemini API: ${e.localizedMessage ?: "Connection failed"}")
                )
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { res ->
                    val bodyString = res.body?.string().orEmpty()
                    if (!res.isSuccessful) {
                        val apiErrorMessage = parseApiErrorMessage(bodyString, res.code)
                        continuation.resumeWithException(GeminiQuizException(apiErrorMessage))
                        return
                    }
                    if (bodyString.isBlank()) {
                        continuation.resumeWithException(
                            GeminiQuizException("Gemini API returned an empty response. Please try again.")
                        )
                        return
                    }
                    continuation.resume(bodyString)
                }
            }
        })
    }

    private fun buildRequestJson(base64Pdf: String, prompt: String): String {
        val pdfPart = JSONObject().put(
            "inline_data",
            JSONObject()
                .put("mime_type", "application/pdf")
                .put("data", base64Pdf)
        )
        val textPart = JSONObject().put("text", prompt)
        val parts = JSONArray().put(pdfPart).put(textPart)
        val contentObj = JSONObject().put("parts", parts)

        val questionProps = JSONObject()
            .put("question", JSONObject().put("type", "STRING").put("description", "Multiple-choice question grounded in the PDF"))
            .put(
                "options",
                JSONObject()
                    .put("type", "ARRAY")
                    .put("items", JSONObject().put("type", "STRING"))
                    .put("description", "Exactly 4 distinct answer choices without A/B/C/D prefixes")
            )
            .put("correctIndex", JSONObject().put("type", "INTEGER").put("description", "0-based index (0..3) of the correct option"))
            .put("explanation", JSONObject().put("type", "STRING").put("description", "1-2 sentence explanation citing why the answer is right"))
            .put("sourceReference", JSONObject().put("type", "STRING").put("description", "Unit, section, or page reference from the PDF"))

        val questionItemSchema = JSONObject()
            .put("type", "OBJECT")
            .put("properties", questionProps)
            .put("required", JSONArray().put("question").put("options").put("correctIndex").put("explanation"))

        val rootSchema = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject()
                    .put("quizTitle", JSONObject().put("type", "STRING").put("description", "Descriptive title for the quiz"))
                    .put(
                        "questions",
                        JSONObject()
                            .put("type", "ARRAY")
                            .put("items", questionItemSchema)
                    )
            )
            .put("required", JSONArray().put("quizTitle").put("questions"))

        val generationConfig = JSONObject()
            .put("temperature", 0.35)
            .put("responseMimeType", "application/json")
            .put("responseSchema", rootSchema)

        return JSONObject()
            .put("contents", JSONArray().put(contentObj))
            .put("generationConfig", generationConfig)
            .toString()
    }

    internal fun extractCandidateText(rawResponseBody: String): String {
        val root = try {
            JSONObject(rawResponseBody)
        } catch (e: Exception) {
            throw GeminiQuizException("Unexpected response from Gemini API.")
        }

        val candidates = root.optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            val blockReason = root.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
            if (blockReason.isNotBlank()) {
                throw GeminiQuizException("Gemini blocked this request ($blockReason). Try a different topic or PDF.")
            }
            throw GeminiQuizException("Gemini did not return any quiz candidates. Please try again.")
        }

        val firstCandidate = candidates.optJSONObject(0)
        val parts = firstCandidate?.optJSONObject("content")?.optJSONArray("parts")
        val text = buildString {
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val partText = parts.optJSONObject(i)?.optString("text").orEmpty()
                    if (partText.isNotEmpty()) append(partText)
                }
            }
        }.trim()

        if (text.isEmpty()) {
            throw GeminiQuizException("Gemini returned empty quiz content. Please try again.")
        }
        return stripMarkdownFences(text)
    }

    private fun stripMarkdownFences(raw: String): String {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("```")) return trimmed
        return trimmed
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun parseApiErrorMessage(bodyString: String, statusCode: Int): String {
        val parsedMsg = runCatching {
            JSONObject(bodyString).optJSONObject("error")?.optString("message")
        }.getOrNull()?.takeIf { it.isNotBlank() }

        return when (statusCode) {
            400, 401, 403 -> parsedMsg?.let { "API Key / Request Error: $it" }
                ?: "Invalid Gemini API key or unauthorized request (HTTP $statusCode)."
            429 -> "Gemini API rate limit reached. Wait a moment or check your API quota (HTTP 429)."
            else -> parsedMsg ?: "Gemini API request failed with HTTP $statusCode."
        }
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
            quizAdapter.fromJson(stripMarkdownFences(rawJson))
        } catch (e: Exception) {
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
        private const val MODEL_NAME = "gemini-2.5-flash"
        private const val GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val MAX_PDF_BYTES = 19 * 1024 * 1024
    }
}
