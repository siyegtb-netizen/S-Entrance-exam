package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizSession
import com.example.data.remote.GeminiQuizService
import com.example.ui.screens.QuizSlideScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.theme.SEntranceQuizTheme
import com.example.ui.viewmodel.QuizSlideUiState
import com.example.ui.viewmodel.SetupUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w411dp-h915dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `app_name matches S Entrance Quiz`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("S Entrance Quiz", appName)
    }

    @Test
    fun `parseQuizResponse strips option prefixes and builds QuizSession`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = GeminiQuizService(context)
        val rawJson = """
            ```json
            {
              "quizTitle": "Grade 12 Unit 1 and 2",
              "questions": [
                {
                  "question": "What is the powerhouse of the cell?",
                  "options": ["A) Nucleus", "B. Mitochondria", "C: Ribosome", "D- Golgi apparatus"],
                  "correctIndex": 1,
                  "explanation": "Mitochondria generate most of the cell's ATP.",
                  "sourceReference": "Unit 1.2"
                }
              ]
            }
            ```
        """.trimIndent()

        val session = service.parseQuizResponse(rawJson, "Grade 12 Unit 1 and 2", "biology.pdf")
        assertEquals("Grade 12 Unit 1 and 2", session.title)
        assertEquals(1, session.questions.size)
        val q = session.questions.first()
        assertEquals(listOf("Nucleus", "Mitochondria", "Ribosome", "Golgi apparatus"), q.options)
        assertEquals(1, q.correctIndex)
        assertEquals("B", q.correctOptionLetter)
    }

    @Test
    fun `extractCandidateText unwraps Gemini REST response JSON`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = GeminiQuizService(context)
        val restBody = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      { "text": "{\"quizTitle\":\"Unit 1\",\"questions\":[]}" }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val extracted = service.extractCandidateText(restBody)
        assertEquals("""{"quizTitle":"Unit 1","questions":[]}""", extracted)
    }

    @Test
    fun `SetupScreen displays upload, API key input, topic input, and dark mode toggle`() {
        var toggled = false
        var enteredKey = ""
        composeRule.setContent {
            SEntranceQuizTheme(darkTheme = false) {
                SetupScreen(
                    uiState = SetupUiState(),
                    isDarkMode = false,
                    onToggleDarkMode = { toggled = true },
                    onPdfSelected = {},
                    onClearPdf = {},
                    onApiKeyChanged = { enteredKey = it },
                    onClearApiKey = {},
                    onTopicChanged = {},
                    onQuestionCountSelected = {},
                    onDifficultySelected = {},
                    onDismissError = {},
                    onGenerateClick = {},
                    onCancelGeneration = {}
                )
            }
        }

        composeRule.onNodeWithText("S Entrance Quiz").assertIsDisplayed()
        composeRule.onNodeWithTag("dark_mode_toggle_button").performClick()
        assertTrue(toggled)
        composeRule.onNodeWithTag("upload_pdf_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("api_key_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("api_key_input").performTextInput("AIzaTestKey123")
        assertEquals("AIzaTestKey123", enteredKey)
        composeRule.onNodeWithTag("topic_input").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `QuizSlideScreen shows instant feedback on clicking option button`() {
        val question = QuizQuestion(
            id = 1,
            question = "Which unit measures electric resistance?",
            options = listOf("Volt", "Ampere", "Ohm", "Watt"),
            correctIndex = 2,
            explanation = "Resistance is measured in Ohms (Ω).",
            sourceReference = "Unit 2"
        )
        val session = QuizSession(
            title = "Physics Unit 1 and 2",
            topic = "Grade 12 Unit 1 and 2",
            pdfName = "physics.pdf",
            questions = listOf(question),
            selectedAnswers = mapOf(1 to 2)
        )

        composeRule.setContent {
            SEntranceQuizTheme(darkTheme = true) {
                QuizSlideScreen(
                    uiState = QuizSlideUiState(session = session, currentIndex = 0),
                    isDarkMode = true,
                    onToggleDarkMode = {},
                    onSelectAnswer = {},
                    onNextSlide = {},
                    onPreviousSlide = {},
                    onJumpToSlide = {},
                    onExitQuiz = {}
                )
            }
        }

        composeRule.onNodeWithTag("option_button_A").assertIsDisplayed()
        composeRule.onNodeWithTag("option_button_B").assertIsDisplayed()
        composeRule.onNodeWithTag("option_button_C").assertIsDisplayed()
        composeRule.onNodeWithTag("option_button_D").assertIsDisplayed()
        composeRule.onNodeWithTag("instant_feedback_card").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Correct!").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `ResultScreen computes and displays final score percentage`() {
        val questions = listOf(
            QuizQuestion(1, "Q1", listOf("A", "B", "C", "D"), 0, "Exp 1"),
            QuizQuestion(2, "Q2", listOf("A", "B", "C", "D"), 1, "Exp 2")
        )
        val session = QuizSession(
            title = "Unit 1",
            topic = "Unit 1",
            pdfName = "notes.pdf",
            questions = questions,
            selectedAnswers = mapOf(1 to 0, 2 to 3)
        )

        composeRule.setContent {
            SEntranceQuizTheme(darkTheme = false) {
                ResultScreen(
                    session = session,
                    isDarkMode = false,
                    onToggleDarkMode = {},
                    onRetryQuiz = {},
                    onNewQuiz = {}
                )
            }
        }

        composeRule.onNodeWithTag("final_score_card").assertIsDisplayed()
        composeRule.onNodeWithText("50%").assertIsDisplayed()
        composeRule.onNodeWithText("1 of 2").assertIsDisplayed()
    }
}
