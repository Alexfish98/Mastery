package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

fun getThemeColorScheme(darkTheme: Boolean, accent: String): androidx.compose.material3.ColorScheme {
    return when (accent) {
        "Teal" -> if (darkTheme) {
            darkColorScheme(
                primary = TealDarkPrimary,
                onPrimary = Color(0xFF042F2A),
                primaryContainer = Color(0xFF164E46),
                onPrimaryContainer = Color(0xFFA7F3D0),
                background = Color(0xFF0E1513),
                surface = Color(0xFF16201D)
            )
        } else {
            lightColorScheme(
                primary = TealPrimary,
                onPrimary = TealOnPrimary,
                primaryContainer = TealPrimaryContainer,
                onPrimaryContainer = Color(0xFF042F2A),
                background = Color(0xFFF3F6F4),
                surface = Color.White
            )
        }

        "Blue" -> if (darkTheme) {
            darkColorScheme(
                primary = BlueDarkPrimary,
                onPrimary = Color.White,
                primaryContainer = Color(0xFF14304F),
                background = Color(0xFF121418),
                surface = Color(0xFF1A1C22)
            )
        } else {
            lightColorScheme(
                primary = BluePrimary,
                onPrimary = Color.White,
                primaryContainer = BluePrimaryContainer,
                background = Color(0xFFF6F8FA),
                surface = Color.White
            )
        }

        "Purple" -> if (darkTheme) {
            darkColorScheme(
                primary = PurpleDarkAccent,
                onPrimary = Color.White,
                primaryContainer = Color(0xFF35204A),
                background = Color(0xFF141218),
                surface = Color(0xFF1E1A24)
            )
        } else {
            lightColorScheme(
                primary = PurpleAccent,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFF3E5FB),
                background = Color(0xFFFAF7FC),
                surface = Color.White
            )
        }

        "Pink" -> if (darkTheme) {
            darkColorScheme(
                primary = PinkDarkAccent,
                onPrimary = Color.White,
                primaryContainer = Color(0xFF4A1B26),
                background = Color(0xFF181214),
                surface = Color(0xFF241A1E)
            )
        } else {
            lightColorScheme(
                primary = PinkAccent,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFE5EA),
                background = Color(0xFFFCF7F8),
                surface = Color.White
            )
        }

        "Orange" -> if (darkTheme) {
            darkColorScheme(
                primary = OrangeDarkAccent,
                onPrimary = Color(0xFF1C1000),
                primaryContainer = Color(0xFF47300F),
                background = Color(0xFF181410),
                surface = Color(0xFF241E18)
            )
        } else {
            lightColorScheme(
                primary = OrangeAccent,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFF1DC),
                background = Color(0xFFFCF9F5),
                surface = Color.White
            )
        }

        "Green" -> if (darkTheme) {
            darkColorScheme(
                primary = GreenDarkAccent,
                onPrimary = Color(0xFF04210D),
                primaryContainer = Color(0xFF173A22),
                background = Color(0xFF101812),
                surface = Color(0xFF18241C)
            )
        } else {
            lightColorScheme(
                primary = GreenAccent,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE3F6E8),
                background = Color(0xFFF5FCF7),
                surface = Color.White
            )
        }

        "Graphite" -> if (darkTheme) {
            darkColorScheme(
                primary = GraphiteDarkAccent,
                onPrimary = Color(0xFF111111),
                primaryContainer = Color(0xFF38383C),
                background = Color(0xFF161618),
                surface = Color(0xFF222226)
            )
        } else {
            lightColorScheme(
                primary = GraphiteAccent,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFECECF0),
                background = Color(0xFFF4F4F6),
                surface = Color.White
            )
        }

        else -> { // "Indigo" (Default EUEE Mastery Palette)
            if (darkTheme) {
                darkColorScheme(
                    primary = IndigoDarkPrimary,
                    onPrimary = IndigoDarkOnPrimary,
                    primaryContainer = IndigoDarkPrimaryContainer,
                    onPrimaryContainer = IndigoDarkOnPrimaryContainer,
                    background = Color(0xFF0B1020),
                    surface = Color(0xFF121A2D),
                    surfaceVariant = Color(0xFF1E293F),
                    outline = Color(0xFF26334D),
                    outlineVariant = Color(0xFF1D283E)
                )
            } else {
                lightColorScheme(
                    primary = IndigoPrimary,
                    onPrimary = IndigoOnPrimary,
                    primaryContainer = IndigoPrimaryContainer,
                    onPrimaryContainer = IndigoOnPrimaryContainer,
                    background = Color(0xFFF3F6FC),
                    surface = Color.White,
                    surfaceVariant = Color(0xFFE8EEFA),
                    outline = Color(0xFFDFE6F2),
                    outlineVariant = Color(0xFFD3DDEB)
                )
            }
        }
    }
}

@Composable
fun ChemistryMasteryTheme(
    themeMode: String = "auto", // "light", "dark", "auto"
    accentColor: String = "Indigo",
    fontSelection: String = "Modern Sans",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = getThemeColorScheme(darkTheme, accentColor)
    val typography = createAppTypography(getAppFontFamily(fontSelection))

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
