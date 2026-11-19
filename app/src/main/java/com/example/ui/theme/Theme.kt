package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class QuizFeedbackColors(
    val correct: Color,
    val correctContainer: Color,
    val correctBorder: Color,
    val incorrect: Color,
    val incorrectContainer: Color,
    val incorrectBorder: Color
)

private val LightQuizFeedbackColors = QuizFeedbackColors(
    correct = CorrectEmerald,
    correctContainer = CorrectEmeraldSoft,
    correctBorder = CorrectEmeraldBorder,
    incorrect = IncorrectCrimson,
    incorrectContainer = IncorrectCrimsonSoft,
    incorrectBorder = IncorrectCrimsonBorder
)

private val DarkQuizFeedbackColors = QuizFeedbackColors(
    correct = DarkCorrectEmerald,
    correctContainer = DarkCorrectSoft,
    correctBorder = DarkCorrectEmerald,
    incorrect = DarkIncorrectCrimson,
    incorrectContainer = DarkIncorrectSoft,
    incorrectBorder = DarkIncorrectCrimson
)

val LocalQuizFeedbackColors = staticCompositionLocalOf { LightQuizFeedbackColors }

private val LightColorScheme = lightColorScheme(
    primary = OxfordNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6F7),
    onPrimaryContainer = OxfordNavy,
    secondary = AcademicAmber,
    onSecondary = Color.White,
    secondaryContainer = AcademicAmberSoft,
    onSecondaryContainer = AcademicAmberOnSoft,
    tertiary = OxfordNavyLight,
    onTertiary = Color.White,
    background = WarmIvory,
    onBackground = DeepInk,
    surface = CrispPaper,
    onSurface = DeepInk,
    surfaceVariant = SoftParchment,
    onSurfaceVariant = MutedSlate,
    outline = StoneBorder,
    outlineVariant = Color(0xFFECE6DA),
    error = IncorrectCrimson,
    onError = Color.White,
    errorContainer = IncorrectCrimsonSoft,
    onErrorContainer = IncorrectCrimson
)

private val DarkColorScheme = darkColorScheme(
    primary = LuminousSky,
    onPrimary = MidnightSlate,
    primaryContainer = Color(0xFF1D3557),
    onPrimaryContainer = ChalkWhite,
    secondary = LuminousAmber,
    onSecondary = MidnightSlate,
    secondaryContainer = DarkAmberSoft,
    onSecondaryContainer = LuminousAmber,
    tertiary = Color(0xFF93C5FD),
    onTertiary = MidnightSlate,
    background = MidnightSlate,
    onBackground = ChalkWhite,
    surface = DarkCardSurface,
    onSurface = ChalkWhite,
    surfaceVariant = DarkCardElevated,
    onSurfaceVariant = CoolGray,
    outline = DarkBorder,
    outlineVariant = Color(0xFF1E2D45),
    error = DarkIncorrectCrimson,
    onError = MidnightSlate,
    errorContainer = DarkIncorrectSoft,
    onErrorContainer = DarkIncorrectCrimson
)

@Composable
fun SEntranceQuizTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val feedbackColors = if (darkTheme) DarkQuizFeedbackColors else LightQuizFeedbackColors

    androidx.compose.runtime.CompositionLocalProvider(
        LocalQuizFeedbackColors provides feedbackColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content
        )
    }
}
