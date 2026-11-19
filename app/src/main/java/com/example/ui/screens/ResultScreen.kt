package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.QuizQuestion
import com.example.data.model.QuizSession
import com.example.ui.components.DarkModeToggleButton
import com.example.ui.components.ScoreGaugeRing
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalQuizFeedbackColors

@Composable
fun ResultScreen(
    session: QuizSession?,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onRetryQuiz: () -> Unit,
    onNewQuiz: () -> Unit
) {
    if (session == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = onNewQuiz) { Text("Back to Setup") }
        }
        return
    }

    val feedbackColors = LocalQuizFeedbackColors.current
    val pct = session.scorePercentage
    val tierTitle = when {
        pct >= 85 -> "Entrance Exam Ready!"
        pct >= 65 -> "Strong Mastery"
        pct >= 45 -> "Solid Foundation"
        else -> "Keep Reviewing"
    }
    val tierSubtitle = when {
        pct >= 85 -> "Outstanding command of ${session.topic}."
        pct >= 65 -> "You're doing well—review missed slides to sharpen edge cases."
        else -> "Revisit the highlighted explanations below and retry the slides."
    }
    val ringColor = when {
        pct >= 70 -> feedbackColors.correct
        pct >= 45 -> MaterialTheme.colorScheme.secondary
        else -> feedbackColors.incorrect
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 660.dp)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Final Score",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = session.topic,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DarkModeToggleButton(
                isDarkMode = isDarkMode,
                onToggle = onToggleDarkMode
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 660.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Score Gauge Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("final_score_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ScoreGaugeRing(
                            percentage = pct,
                            ringColor = ringColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(152.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$pct%",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${session.correctCount} of ${session.totalQuestions}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = tierTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = tierSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Stats Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatPill(
                                label = "Correct",
                                value = "${session.correctCount}",
                                containerColor = feedbackColors.correctContainer,
                                contentColor = feedbackColors.correct,
                                modifier = Modifier.weight(1f)
                            )
                            StatPill(
                                label = "Incorrect",
                                value = "${session.incorrectCount}",
                                containerColor = feedbackColors.incorrectContainer,
                                contentColor = feedbackColors.incorrect,
                                modifier = Modifier.weight(1f)
                            )
                            StatPill(
                                label = "Slides",
                                value = "${session.totalQuestions}",
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetryQuiz,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("retry_quiz_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Replay,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Slides")
                    }

                    Button(
                        onClick = onNewQuiz,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("new_quiz_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Topic / PDF")
                    }
                }
            }

            // Review Header
            item {
                Text(
                    text = "Slide-by-Slide Review",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // All Questions Review
            itemsIndexed(session.questions) { index, question ->
                val userChoice = session.selectedAnswers[question.id]
                val isCorrect = userChoice == question.correctIndex

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isCorrect) feedbackColors.correctBorder.copy(alpha = 0.5f)
                        else feedbackColors.incorrectBorder.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isCorrect) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                                contentDescription = if (isCorrect) "Correct" else "Incorrect",
                                tint = if (isCorrect) feedbackColors.correct else feedbackColors.incorrect,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Slide ${index + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = JetBrainsMonoFontFamily,
                                color = if (isCorrect) feedbackColors.correct else feedbackColors.incorrect
                            )
                        }

                        Text(
                            text = question.question,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (userChoice != null && !isCorrect) {
                            val chosenText = question.options.getOrElse(userChoice) { "" }
                            Text(
                                text = "Your answer: ${QuizQuestion.optionLetter(userChoice)}. $chosenText",
                                style = MaterialTheme.typography.bodyMedium,
                                color = feedbackColors.incorrect
                            )
                        }

                        val correctText = question.options.getOrElse(question.correctIndex) { "" }
                        Text(
                            text = "Correct answer: ${question.correctOptionLetter}. $correctText",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = feedbackColors.correct
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = question.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontFamily = JetBrainsMonoFontFamily,
                color = contentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor
            )
        }
    }
}
