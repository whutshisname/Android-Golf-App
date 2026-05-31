package com.whutshisname.cgolfapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary                = OrangePrimary,
    onPrimary              = OrangeOnPrimary,
    primaryContainer       = OrangePrimaryContainer,
    onPrimaryContainer     = OrangeOnPrimaryContainer,
    secondary              = WarmSecondary,
    onSecondary            = WarmOnSecondary,
    secondaryContainer     = WarmSecondaryContainer,
    onSecondaryContainer   = WarmOnSecondaryContainer,
    tertiary               = WarmTertiary,
    onTertiary             = WarmOnTertiary,
    tertiaryContainer      = WarmTertiaryContainer,
    onTertiaryContainer    = WarmOnTertiaryContainer,
    surface                = LightSurface,
    onSurface              = LightOnSurface,
    surfaceVariant         = LightSurfaceVariant,
    onSurfaceVariant       = LightOnSurfaceVariant,
    outline                = LightOutline,
    outlineVariant         = LightOutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary                = OrangePrimaryDark,
    onPrimary              = OrangeOnPrimaryDark,
    primaryContainer       = OrangePrimaryContainerDark,
    onPrimaryContainer     = OrangeOnPrimaryContainerDark,
    secondary              = WarmSecondaryDark,
    onSecondary            = WarmOnSecondaryDark,
    secondaryContainer     = WarmSecondaryContainerDark,
    onSecondaryContainer   = WarmOnSecondaryContainerDark,
    tertiary               = WarmTertiaryDark,
    onTertiary             = WarmOnTertiaryDark,
    tertiaryContainer      = WarmTertiaryContainerDark,
    onTertiaryContainer    = WarmOnTertiaryContainerDark,
    surface                = DarkSurface,
    onSurface              = DarkOnSurface,
    surfaceVariant         = DarkSurfaceVariant,
    onSurfaceVariant       = DarkOnSurfaceVariant,
    outline                = DarkOutline,
    outlineVariant         = DarkOutlineVariant,
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
