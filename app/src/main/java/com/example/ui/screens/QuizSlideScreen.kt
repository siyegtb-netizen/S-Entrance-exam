package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.QuizQuestion
import com.example.ui.components.DarkModeToggleButton
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalQuizFeedbackColors
import com.example.ui.viewmodel.QuizSlideUiState

@Composable
fun QuizSlideScreen(
    uiState: QuizSlideUiState,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onSelectAnswer: (Int) -> Unit,
    onNextSlide: () -> Unit,
    onPreviousSlide: () -> Unit,
    onJumpToSlide: (Int) -> Unit,
    onExitQuiz: () -> Unit
) {
    val session = uiState.session
    val question = uiState.currentQuestion
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Leave Quiz?") },
            text = { Text("Your current quiz progress will be lost if you return to setup.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onExitQuiz()
                    }
                ) {
                    Text("Leave", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("Continue Quiz")
                }
            }
        )
    }

    if (session == null || question == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No quiz loaded.", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onExitQuiz) { Text("Back to Setup") }
            }
        }
        return
    }

    val feedbackColors = LocalQuizFeedbackColors.current
    val progress by animateFloatAsState(
        targetValue = (uiState.currentIndex + 1).toFloat() / session.totalQuestions.coerceAtLeast(1),
        animationSpec = tween(300),
        label = "slide_progress"
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { showExitDialog = true },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("exit_quiz_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Exit quiz"
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Slide ${uiState.currentIndex + 1} of ${session.totalQuestions}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Live Score Pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${session.correctCount}/${session.totalQuestions}",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    DarkModeToggleButton(
                        isDarkMode = isDarkMode,
                        onToggle = onToggleDarkMode
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 660.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onPreviousSlide,
                            enabled = uiState.currentIndex > 0,
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("previous_slide_button"),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(id = R.string.quiz_previous))
                        }

                        Button(
                            onClick = onNextSlide,
                            enabled = uiState.isCurrentAnswered,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("next_slide_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isLastSlide) {
                                    feedbackColors.correct
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                text = if (uiState.isLastSlide) {
                                    stringResource(id = R.string.quiz_finish)
                                } else {
                                    stringResource(id = R.string.quiz_next)
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Slide Number Navigation Strip
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 660.dp)
                    .padding(top = 12.dp, bottom = 4.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(session.questions) { index, q ->
                    val selected = session.selectedAnswers[q.id]
                    val isCurrent = index == uiState.currentIndex
                    val pillBg = when {
                        isCurrent -> MaterialTheme.colorScheme.primary
                        selected == null -> MaterialTheme.colorScheme.surfaceVariant
                        selected == q.correctIndex -> feedbackColors.correctContainer
                        else -> feedbackColors.incorrectContainer
                    }
                    val pillText = when {
                        isCurrent -> MaterialTheme.colorScheme.onPrimary
                        selected == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        selected == q.correctIndex -> feedbackColors.correct
                        else -> feedbackColors.incorrect
                    }

                    Surface(
                        modifier = Modifier
                            .size(width = 36.dp, height = 32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onJumpToSlide(index) },
                        shape = RoundedCornerShape(10.dp),
                        color = pillBg
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = pillText
                            )
                        }
                    }
                }
            }

            // Slide Content with Horizontal Slide Transition
            AnimatedContent(
                targetState = uiState.currentIndex,
                transitionSpec = {
                    val direction = if (targetState >= initialState) 1 else -1
                    (slideInHorizontally(animationSpec = tween(260)) { it * direction } + fadeIn(tween(220)))
                        .togetherWith(
                            slideOutHorizontally(animationSpec = tween(260)) { -it * direction } + fadeOut(tween(180))
                        )
                },
                label = "question_slide_transition",
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { targetIndex ->
                val slideQuestion = session.questions[targetIndex]
                val selectedOption = session.selectedAnswers[slideQuestion.id]
                val isAnswered = selectedOption != null

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 660.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Question Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "QUESTION ${targetIndex + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    if (slideQuestion.sourceReference.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Bookmark,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = slideQuestion.sourceReference,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = slideQuestion.question,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // A, B, C, D Clickable Option Buttons
                        slideQuestion.options.forEachIndexed { optionIdx, optionText ->
                            val letter = QuizQuestion.optionLetter(optionIdx)
                            val isSelected = selectedOption == optionIdx
                            val isCorrectOption = optionIdx == slideQuestion.correctIndex

                            val targetBgColor = when {
                                !isAnswered -> MaterialTheme.colorScheme.surface
                                isCorrectOption -> feedbackColors.correctContainer
                                isSelected && !isCorrectOption -> feedbackColors.incorrectContainer
                                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            }
                            val targetBorderColor = when {
                                !isAnswered -> MaterialTheme.colorScheme.outline
                                isCorrectOption -> feedbackColors.correctBorder
                                isSelected && !isCorrectOption -> feedbackColors.incorrectBorder
                                else -> MaterialTheme.colorScheme.outlineVariant
                            }
                            val targetBadgeBg = when {
                                !isAnswered -> MaterialTheme.colorScheme.surfaceVariant
                                isCorrectOption -> feedbackColors.correct
                                isSelected && !isCorrectOption -> feedbackColors.incorrect
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val targetBadgeFg = when {
                                !isAnswered -> MaterialTheme.colorScheme.onSurface
                                isCorrectOption || (isSelected && !isCorrectOption) -> MaterialTheme.colorScheme.surface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            val animatedBg by animateColorAsState(targetBgColor, tween(220), label = "opt_bg")
                            val animatedBorder by animateColorAsState(targetBorderColor, tween(220), label = "opt_border")

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 60.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(enabled = !isAnswered) {
                                        onSelectAnswer(optionIdx)
                                    }
                                    .testTag("option_button_$letter"),
                                shape = RoundedCornerShape(16.dp),
                                color = animatedBg,
                                border = BorderStroke(
                                    width = if (isAnswered && (isCorrectOption || isSelected)) 2.dp else 1.dp,
                                    color = animatedBorder
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(targetBadgeBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = letter,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = targetBadgeFg
                                        )
                                    }

                                    Text(
                                        text = optionText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isAnswered && isCorrectOption) FontWeight.SemiBold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )

                                    if (isAnswered) {
                                        when {
                                            isCorrectOption -> Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = "Correct option",
                                                tint = feedbackColors.correct
                                            )
                                            isSelected -> Icon(
                                                imageVector = Icons.Rounded.Cancel,
                                                contentDescription = "Incorrect option",
                                                tint = feedbackColors.incorrect
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Instant Feedback Explanation Banner
                        AnimatedVisibility(visible = isAnswered) {
                            val gotItRight = selectedOption == slideQuestion.correctIndex
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("instant_feedback_card"),
                                shape = RoundedCornerShape(18.dp),
                                color = if (gotItRight) {
                                    feedbackColors.correctContainer
                                } else {
                                    feedbackColors.incorrectContainer
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (gotItRight) feedbackColors.correctBorder else feedbackColors.incorrectBorder
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (gotItRight) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                            contentDescription = null,
                                            tint = if (gotItRight) feedbackColors.correct else feedbackColors.incorrect
                                        )
                                        Text(
                                            text = if (gotItRight) {
                                                "Correct!"
                                            } else {
                                                "Incorrect — Correct Answer: ${slideQuestion.correctOptionLetter}"
                                            },
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (gotItRight) feedbackColors.correct else feedbackColors.incorrect
                                        )
                                    }
                                    Text(
                                        text = slideQuestion.explanation,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}
