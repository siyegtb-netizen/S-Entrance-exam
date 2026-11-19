package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.QuizSlideScreen
import com.example.ui.screens.ResultScreen
import com.example.ui.screens.SetupScreen
import com.example.ui.theme.SEntranceQuizTheme
import com.example.ui.viewmodel.QuizViewModel

private const val ROUTE_SETUP = "setup"
private const val ROUTE_QUIZ = "quiz"
private const val ROUTE_RESULT = "result"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            SEntranceQuizApp()
        }
    }
}

@Composable
fun SEntranceQuizApp(
    quizViewModel: QuizViewModel = viewModel()
) {
    val systemDark = isSystemInDarkTheme()
    val darkModeOverride by quizViewModel.darkModeOverride.collectAsStateWithLifecycle()
    val isDarkMode = darkModeOverride ?: systemDark

    val setupState by quizViewModel.setupState.collectAsStateWithLifecycle()
    val quizState by quizViewModel.quizState.collectAsStateWithLifecycle()
    val navController = rememberNavController()

    SEntranceQuizTheme(darkTheme = isDarkMode) {
        NavHost(
            navController = navController,
            startDestination = ROUTE_SETUP
        ) {
            composable(ROUTE_SETUP) {
                SetupScreen(
                    uiState = setupState,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { quizViewModel.toggleDarkMode(isDarkMode) },
                    onPdfSelected = quizViewModel::onPdfSelected,
                    onClearPdf = quizViewModel::onClearPdf,
                    onTopicChanged = quizViewModel::onTopicChanged,
                    onQuestionCountSelected = quizViewModel::onQuestionCountSelected,
                    onDifficultySelected = quizViewModel::onDifficultySelected,
                    onDismissError = quizViewModel::dismissError,
                    onGenerateClick = {
                        quizViewModel.generateQuiz {
                            navController.navigate(ROUTE_QUIZ) {
                                launchSingleTop = true
                            }
                        }
                    },
                    onCancelGeneration = quizViewModel::cancelGeneration
                )
            }

            composable(ROUTE_QUIZ) {
                QuizSlideScreen(
                    uiState = quizState,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { quizViewModel.toggleDarkMode(isDarkMode) },
                    onSelectAnswer = quizViewModel::selectAnswer,
                    onNextSlide = {
                        quizViewModel.nextSlide {
                            navController.navigate(ROUTE_RESULT) {
                                popUpTo(ROUTE_QUIZ) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onPreviousSlide = quizViewModel::previousSlide,
                    onJumpToSlide = quizViewModel::jumpToSlide,
                    onExitQuiz = {
                        navController.popBackStack(ROUTE_SETUP, inclusive = false)
                    }
                )
            }

            composable(ROUTE_RESULT) {
                ResultScreen(
                    session = quizState.session,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { quizViewModel.toggleDarkMode(isDarkMode) },
                    onRetryQuiz = {
                        quizViewModel.retryQuiz()
                        navController.navigate(ROUTE_QUIZ) {
                            popUpTo(ROUTE_RESULT) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onNewQuiz = {
                        navController.popBackStack(ROUTE_SETUP, inclusive = false)
                    }
                )
            }
        }
    }
}
