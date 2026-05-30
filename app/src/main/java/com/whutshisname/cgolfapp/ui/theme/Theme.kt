package com.whutshisname.cgolfapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary                = GreenPrimary,
    onPrimary              = GreenOnPrimary,
    primaryContainer       = GreenPrimaryContainer,
    onPrimaryContainer     = GreenOnPrimaryContainer,
    secondary              = GreenSecondary,
    onSecondary            = GreenOnSecondary,
    secondaryContainer     = GreenSecondaryContainer,
    onSecondaryContainer   = GreenOnSecondaryContainer,
    tertiary               = GreenTertiary,
    onTertiary             = GreenOnTertiary,
    tertiaryContainer      = GreenTertiaryContainer,
    onTertiaryContainer    = GreenOnTertiaryContainer,
)

private val DarkColorScheme = darkColorScheme(
    primary                = GreenPrimaryDark,
    onPrimary              = GreenOnPrimaryDark,
    primaryContainer       = GreenPrimaryContainerDark,
    onPrimaryContainer     = GreenOnPrimaryContainerDark,
    secondary              = GreenSecondaryDark,
    onSecondary            = GreenOnSecondaryDark,
    secondaryContainer     = GreenSecondaryContainerDark,
    onSecondaryContainer   = GreenOnSecondaryContainerDark,
    tertiary               = GreenTertiaryDark,
    onTertiary             = GreenOnTertiaryDark,
    tertiaryContainer      = GreenTertiaryContainerDark,
    onTertiaryContainer    = GreenOnTertiaryContainerDark,
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
