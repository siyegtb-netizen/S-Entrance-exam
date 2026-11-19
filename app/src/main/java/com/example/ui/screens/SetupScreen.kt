package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DifficultyMode
import com.example.ui.components.DarkModeToggleButton
import com.example.ui.components.StudyHeroIllustration
import com.example.ui.theme.LocalQuizFeedbackColors
import com.example.ui.viewmodel.SetupUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SetupScreen(
    uiState: SetupUiState,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onPdfSelected: (android.net.Uri) -> Unit,
    onClearPdf: () -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onClearApiKey: () -> Unit,
    onTopicChanged: (String) -> Unit,
    onQuestionCountSelected: (Int) -> Unit,
    onDifficultySelected: (DifficultyMode) -> Unit,
    onDismissError: () -> Unit,
    onGenerateClick: () -> Unit,
    onCancelGeneration: () -> Unit
) {
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onPdfSelected(uri)
        }
    }

    var showApiKey by remember { mutableStateOf(false) }
    val feedbackColors = LocalQuizFeedbackColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 660.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Header Row with App Emblem, App Title & Dark/Light Mode Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_emblem),
                        contentDescription = "S Entrance Quiz emblem",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(14.dp)
                            )
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "PDF to Interactive Slide Quiz",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DarkModeToggleButton(
                        isDarkMode = isDarkMode,
                        onToggle = onToggleDarkMode
                    )
                }

                // Editorial Hero Banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StudyHeroIllustration(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(112.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "GEMINI 2.5 FLASH",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = "Instant A/B/C/D Feedback",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.setup_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Error Banner
                AnimatedVisibility(visible = uiState.errorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("error_banner"),
                        shape = RoundedCornerShape(16.dp),
                        color = feedbackColors.incorrectContainer,
                        border = BorderStroke(1.dp, feedbackColors.incorrectBorder.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ErrorOutline,
                                contentDescription = "Error",
                                tint = feedbackColors.incorrect
                            )
                            Text(
                                text = uiState.errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = feedbackColors.incorrect,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onDismissError,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Dismiss error",
                                    tint = feedbackColors.incorrect
                                )
                            }
                        }
                    }
                }

                // Step 1: Upload PDF Card
                SectionCard(
                    title = stringResource(id = R.string.step_pdf_title),
                    icon = Icons.Rounded.PictureAsPdf
                ) {
                    val pdf = uiState.selectedPdf
                    if (pdf == null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { pdfPickerLauncher.launch(arrayOf("application/pdf")) }
                                .testTag("upload_pdf_button"),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.UploadFile,
                                        contentDescription = "Upload PDF",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = stringResource(id = R.string.step_pdf_action),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Textbooks, study guides, or entrance exam PDFs (up to 19 MB)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = feedbackColors.correctContainer.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, feedbackColors.correctBorder.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(feedbackColors.correct.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "PDF Ready",
                                        tint = feedbackColors.correct
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pdf.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${pdf.formattedSize} • PDF Ready",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = onClearPdf,
                                    modifier = Modifier.testTag("clear_pdf_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove PDF",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("replace_pdf_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(stringResource(id = R.string.step_pdf_change))
                        }
                    }
                }

                // Step 2: Gemini API Key Card
                SectionCard(
                    title = stringResource(id = R.string.step_api_key_title),
                    icon = Icons.Rounded.Key
                ) {
                    OutlinedTextField(
                        value = uiState.apiKey,
                        onValueChange = onApiKeyChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_key_input"),
                        placeholder = {
                            Text(text = stringResource(id = R.string.step_api_key_hint))
                        },
                        singleLine = true,
                        visualTransformation = if (showApiKey) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { showApiKey = !showApiKey },
                                    modifier = Modifier.testTag("toggle_api_key_visibility")
                                ) {
                                    Icon(
                                        imageVector = if (showApiKey) {
                                            Icons.Rounded.VisibilityOff
                                        } else {
                                            Icons.Rounded.Visibility
                                        },
                                        contentDescription = if (showApiKey) "Hide API key" else "Show API key"
                                    )
                                }
                                if (uiState.apiKey.isNotEmpty()) {
                                    IconButton(
                                        onClick = onClearApiKey,
                                        modifier = Modifier.testTag("clear_api_key_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Clear API key"
                                        )
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (uiState.apiKey.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = feedbackColors.correct,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "API key entered • Saved locally on this device",
                                style = MaterialTheme.typography.bodySmall,
                                color = feedbackColors.correct
                            )
                        } else {
                            Text(
                                text = "Stored only on your device and sent directly to Google Gemini API.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Step 3: Topic / Unit & Quiz Options
                SectionCard(
                    title = stringResource(id = R.string.step_topic_title),
                    icon = Icons.Rounded.Book
                ) {
                    OutlinedTextField(
                        value = uiState.topic,
                        onValueChange = onTopicChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("topic_input"),
                        placeholder = {
                            Text(text = stringResource(id = R.string.step_topic_hint))
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Quick presets",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val presets = listOf(
                            "Grade 12 Unit 1 and 2",
                            "Chapter 1 Core Concepts",
                            "Key Definitions & Formulas",
                            "Full Document Review"
                        )
                        presets.forEach { preset ->
                            FilterChip(
                                selected = uiState.topic.equals(preset, ignoreCase = true),
                                onClick = { onTopicChanged(preset) },
                                label = { Text(preset, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Number of Slides",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(5, 10, 15, 20).forEach { count ->
                            val selected = uiState.questionCount == count
                            FilterChip(
                                selected = selected,
                                onClick = { onQuestionCountSelected(count) },
                                label = {
                                    Text(
                                        text = "$count Qs",
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("count_chip_$count"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Question Style",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DifficultyMode.entries.forEach { mode ->
                            val selected = uiState.difficulty == mode
                            FilterChip(
                                selected = selected,
                                onClick = { onDifficultySelected(mode) },
                                label = { Text(mode.label) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            )
                        }
                    }
                }

                // Generate Quiz Primary Button
                Button(
                    onClick = onGenerateClick,
                    enabled = uiState.canGenerate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("generate_quiz_button"),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(id = R.string.generate_quiz_cta),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (!uiState.canGenerate && !uiState.isGenerating) {
                    val missingHint = when {
                        uiState.selectedPdf == null -> "Upload a PDF document to continue"
                        uiState.apiKey.isBlank() -> "Enter your Gemini API key to continue"
                        uiState.topic.isBlank() -> "Enter a topic or unit (e.g., Grade 12 Unit 1 and 2)"
                        else -> ""
                    }
                    if (missingHint.isNotEmpty()) {
                        Text(
                            text = missingHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Loading Overlay when generating quiz from PDF
        AnimatedVisibility(
            visible = uiState.isGenerating,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                    .clickable(enabled = false, onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 400.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(52.dp),
                            strokeWidth = 4.dp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = uiState.generationStage.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = uiState.generationStage.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Topic: ${uiState.topic}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        TextButton(
                            onClick = onCancelGeneration,
                            modifier = Modifier.testTag("cancel_generation_button")
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
