package com.example.data.model

import android.net.Uri

data class SelectedPdf(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long
) {
    val formattedSize: String
        get() = when {
            sizeBytes <= 0L -> "Unknown size"
            sizeBytes < 1024L -> "$sizeBytes B"
            sizeBytes < 1024L * 1024L -> "${sizeBytes / 1024L} KB"
            else -> String.format(java.util.Locale.US, "%.1f MB", sizeBytes / (1024.0 * 1024.0))
        }
}

enum class DifficultyMode(val label: String, val promptInstruction: String) {
    STANDARD(
        label = "Standard Review",
        promptInstruction = "Balance conceptual understanding, definitions, and key unit facts."
    ),
    ENTRANCE_CHALLENGE(
        label = "Entrance Exam",
        promptInstruction = "Focus on entrance-exam rigor: analytical application, tricky distractors, multi-step reasoning, and common exam pitfalls."
    )
}

data class QuizQuestion(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val sourceReference: String = ""
) {
    val correctOptionLetter: String
        get() = optionLetter(correctIndex)

    companion object {
        fun optionLetter(index: Int): String = when (index) {
            0 -> "A"
            1 -> "B"
            2 -> "C"
            3 -> "D"
            else -> "?"
        }
    }
}

data class QuizSession(
    val title: String,
    val topic: String,
    val pdfName: String,
    val questions: List<QuizQuestion>,
    val selectedAnswers: Map<Int, Int> = emptyMap()
) {
    val totalQuestions: Int get() = questions.size

    val answeredCount: Int get() = selectedAnswers.size

    val correctCount: Int
        get() = questions.count { q -> selectedAnswers[q.id] == q.correctIndex }

    val incorrectCount: Int
        get() = questions.count { q ->
            val selected = selectedAnswers[q.id]
            selected != null && selected != q.correctIndex
        }

    val scorePercentage: Int
        get() = if (questions.isEmpty()) 0 else ((correctCount.toFloat() / questions.size) * 100).toInt()
}

enum class GenerationStage(val title: String, val subtitle: String) {
    READING_PDF("Reading PDF Document", "Preparing pages from your study file..."),
    CRAFTING_QUIZ("Analyzing Topic & Crafting Slides", "Generating multiple-choice questions and explanations...")
}
