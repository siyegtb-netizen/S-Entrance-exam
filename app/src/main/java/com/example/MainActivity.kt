package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
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

        enableEdgeToEdge()
        setContent {
            val viewModel: QuizViewModel = viewModel()
            val setupState by viewModel.setupState.collectAsStateWithLifecycle()
            val quizState by viewModel.quizState.collectAsStateWithLifecycle()
            val darkModeOverride by viewModel.darkModeOverride.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDarkMode = darkModeOverride ?: systemDark

            SEntranceQuizTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_SETUP
                ) {
                    composable(ROUTE_SETUP) {
                        SetupScreen(
                            uiState = setupState,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { viewModel.toggleDarkMode(isDarkMode) },
                            onPdfSelected = viewModel::onPdfSelected,
                            onClearPdf = viewModel::onClearPdf,
                            onApiKeyChanged = viewModel::onApiKeyChanged,
                            onClearApiKey = viewModel::onClearApiKey,
                            onTopicChanged = viewModel::onTopicChanged,
                            onQuestionCountSelected = viewModel::onQuestionCountSelected,
                            onDifficultySelected = viewModel::onDifficultySelected,
                            onDismissError = viewModel::dismissError,
                            onGenerateClick = {
                                viewModel.generateQuiz(
                                    onSuccess = {
                                        navController.navigate(ROUTE_QUIZ) {
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            },
                            onCancelGeneration = viewModel::cancelGeneration
                        )
                    }

                    composable(ROUTE_QUIZ) {
                        QuizSlideScreen(
                            uiState = quizState,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { viewModel.toggleDarkMode(isDarkMode) },
                            onSelectAnswer = viewModel::selectAnswer,
                            onNextSlide = {
                                viewModel.nextSlide(
                                    onFinishQuiz = {
                                        navController.navigate(ROUTE_RESULT) {
                                            popUpTo(ROUTE_QUIZ) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            },
                            onPreviousSlide = viewModel::previousSlide,
                            onJumpToSlide = viewModel::jumpToSlide,
                            onExitQuiz = {
                                navController.popBackStack(ROUTE_SETUP, inclusive = false)
                            }
                        )
                    }

                    composable(ROUTE_RESULT) {
                        ResultScreen(
                            session = quizState.session,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { viewModel.toggleDarkMode(isDarkMode) },
                            onRetryQuiz = {
                                viewModel.retryQuiz()
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
    }
}
