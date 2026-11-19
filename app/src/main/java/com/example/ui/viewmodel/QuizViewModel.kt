package com.example.ui.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.QuizPreferences
import com.example.data.model.DifficultyMode
import com.example.data.model.GenerationStage
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizSession
import com.example.data.model.SelectedPdf
import com.example.data.remote.GeminiQuizException
import com.example.data.remote.GeminiQuizService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SetupUiState(
    val selectedPdf: SelectedPdf? = null,
    val topic: String = "",
    val questionCount: Int = 10,
    val difficulty: DifficultyMode = DifficultyMode.ENTRANCE_CHALLENGE,
    val isGenerating: Boolean = false,
    val generationStage: GenerationStage = GenerationStage.READING_PDF,
    val errorMessage: String? = null
) {
    val canGenerate: Boolean
        get() = selectedPdf != null && topic.isNotBlank() && !isGenerating
}

data class QuizSlideUiState(
    val session: QuizSession? = null,
    val currentIndex: Int = 0,
    val slideDirection: Int = 1
) {
    val currentQuestion: QuizQuestion?
        get() = session?.questions?.getOrNull(currentIndex)

    val selectedOptionForCurrent: Int?
        get() {
            val q = currentQuestion ?: return null
            return session?.selectedAnswers?.get(q.id)
        }

    val isCurrentAnswered: Boolean
        get() = selectedOptionForCurrent != null

    val isLastSlide: Boolean
        get() = session != null && currentIndex >= session.questions.lastIndex
}

class QuizViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = QuizPreferences(application)
    private val geminiService = GeminiQuizService(application)

    private val _setupState = MutableStateFlow(SetupUiState())
    val setupState: StateFlow<SetupUiState> = _setupState.asStateFlow()

    private val _quizState = MutableStateFlow(QuizSlideUiState())
    val quizState: StateFlow<QuizSlideUiState> = _quizState.asStateFlow()

    val darkModeOverride: StateFlow<Boolean?> = preferences.darkModeOverride
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            val savedTopic = preferences.savedLastTopic.first()
            _setupState.update { state ->
                state.copy(
                    topic = state.topic.ifBlank { savedTopic }
                )
            }
        }
    }

    fun toggleDarkMode(currentlyDark: Boolean) {
        viewModelScope.launch {
            preferences.setDarkMode(!currentlyDark)
        }
    }

    fun onPdfSelected(uri: Uri) {
        val resolver = getApplication<Application>().contentResolver
        runCatching {
            resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        var displayName = "Selected Document.pdf"
        var sizeBytes = 0L

        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) {
                        displayName = cursor.getString(nameIdx) ?: displayName
                    }
                    if (sizeIdx >= 0) {
                        sizeBytes = cursor.getLong(sizeIdx)
                    }
                }
            }

        _setupState.update {
            it.copy(
                selectedPdf = SelectedPdf(uri = uri, displayName = displayName, sizeBytes = sizeBytes),
                errorMessage = null
            )
        }
    }

    fun onClearPdf() {
        _setupState.update { it.copy(selectedPdf = null, errorMessage = null) }
    }

    fun onTopicChanged(value: String) {
        _setupState.update { it.copy(topic = value, errorMessage = null) }
    }

    fun onQuestionCountSelected(count: Int) {
        _setupState.update { it.copy(questionCount = count, errorMessage = null) }
    }

    fun onDifficultySelected(mode: DifficultyMode) {
        _setupState.update { it.copy(difficulty = mode, errorMessage = null) }
    }

    fun dismissError() {
        _setupState.update { it.copy(errorMessage = null) }
    }

    fun generateQuiz(onSuccess: () -> Unit) {
        val current = _setupState.value
        val pdf = current.selectedPdf
        if (pdf == null) {
            _setupState.update { it.copy(errorMessage = "Please upload a PDF file first.") }
            return
        }
        if (current.topic.isBlank()) {
            _setupState.update { it.copy(errorMessage = "Please enter a topic or unit (e.g., Grade 12 Unit 1 and 2).") }
            return
        }

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _setupState.update {
                it.copy(
                    isGenerating = true,
                    generationStage = GenerationStage.READING_PDF,
                    errorMessage = null
                )
            }

            try {
                preferences.saveLastTopic(current.topic)

                val session = geminiService.generateQuizFromPdf(
                    pdfUri = pdf.uri,
                    pdfDisplayName = pdf.displayName,
                    topic = current.topic,
                    questionCount = current.questionCount,
                    difficulty = current.difficulty,
                    onStageChanged = { stage ->
                        _setupState.update { it.copy(generationStage = stage) }
                    }
                )

                _quizState.value = QuizSlideUiState(
                    session = session,
                    currentIndex = 0,
                    slideDirection = 1
                )
                _setupState.update { it.copy(isGenerating = false) }
                onSuccess()
            } catch (e: CancellationException) {
                _setupState.update { it.copy(isGenerating = false) }
                throw e
            } catch (e: GeminiQuizException) {
                _setupState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.message ?: "Unable to generate quiz."
                    )
                }
            } catch (e: Exception) {
                _setupState.update {
                    it.copy(
                        isGenerating = false,
                        errorMessage = e.localizedMessage ?: "Unexpected error generating quiz."
                    )
                }
            }
        }
    }

    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        _setupState.update { it.copy(isGenerating = false) }
    }

    fun selectAnswer(optionIndex: Int) {
        _quizState.update { state ->
            val session = state.session ?: return@update state
            val question = session.questions.getOrNull(state.currentIndex) ?: return@update state
            if (session.selectedAnswers.containsKey(question.id)) {
                return@update state
            }
            val updatedAnswers = session.selectedAnswers + (question.id to optionIndex)
            state.copy(session = session.copy(selectedAnswers = updatedAnswers))
        }
    }

    fun nextSlide(onFinishQuiz: () -> Unit) {
        val state = _quizState.value
        val session = state.session ?: return
        if (state.currentIndex < session.questions.lastIndex) {
            _quizState.update {
                it.copy(currentIndex = it.currentIndex + 1, slideDirection = 1)
            }
        } else {
            onFinishQuiz()
        }
    }

    fun previousSlide() {
        _quizState.update { state ->
            if (state.currentIndex > 0) {
                state.copy(currentIndex = state.currentIndex - 1, slideDirection = -1)
            } else {
                state
            }
        }
    }

    fun jumpToSlide(index: Int) {
        _quizState.update { state ->
            val session = state.session ?: return@update state
            val clamped = index.coerceIn(0, session.questions.lastIndex)
            if (clamped == state.currentIndex) return@update state
            state.copy(
                currentIndex = clamped,
                slideDirection = if (clamped > state.currentIndex) 1 else -1
            )
        }
    }

    fun retryQuiz() {
        _quizState.update { state ->
            val session = state.session ?: return@update state
            state.copy(
                session = session.copy(selectedAnswers = emptyMap()),
                currentIndex = 0,
                slideDirection = 1
            )
        }
    }
}
